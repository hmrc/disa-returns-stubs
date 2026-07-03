/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.disareturnsstubs.controllers

import jakarta.inject.Singleton
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, RawBuffer, Result}
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.disareturnsstubs.controllers.action.AuthorizationFilter
import uk.gov.hmrc.disareturnsstubs.models.ErrorResponse._
import uk.gov.hmrc.disareturnsstubs.models.ReturnResultResponse
import uk.gov.hmrc.disareturnsstubs.models.generatereport.GenerateReportRequest
import uk.gov.hmrc.disareturnsstubs.services.{GenerateReportIssuesService, RetrieveReportService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import scala.util.control.NonFatal

@Singleton
class NpsController @Inject() (
  cc: ControllerComponents,
  authorizationFilter: AuthorizationFilter,
  retrieveReportService: RetrieveReportService,
  reportIssuesService: GenerateReportIssuesService,
  val authConnector: AuthConnector
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions
    with Logging {

  private val perfTestCredIdPrefix: String = "disa-returns-perf-test"
  private val perfTestTotalRecords: Int    = 1000

  def submitMonthlyReturn(zReference: String): Action[RawBuffer] =
    (Action(parse.raw) andThen authorizationFilter).async { _ =>
      zReference match {
        case "Z1400" => Future.successful(BadRequest(Json.toJson(badRequestError)))
        case "Z1503" => Future.successful(ServiceUnavailable(Json.toJson(serviceUnavailableError)))
        case _       =>
          logger.info(s"Successfully submitted data for IM ref: [$zReference]")
          Future.successful(NoContent)
      }
    }

  def send(zReference: String): Action[AnyContent] = Action {
    zReference match {
      case "Z1500" => InternalServerError(Json.toJson(internalServerErr("Internal issue, try again later")))
      case _       =>
        logger.info(s"Successfully submitted declaration for IM Ref: [$zReference]")
        NoContent
    }
  }

  def getMonthlyReport(
    zReference: String,
    taxYear: String,
    month: String,
    pageIndex: Int,
    pageSize: Int
  ): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    authorised()
      .retrieve(credentials) {
        case Some(Credentials(credId, _)) if credId.startsWith(perfTestCredIdPrefix) =>
          logger.info(s"[PerfTest] Returning generated report for IM ref: [$zReference], skipping Mongo")
          Future.successful(perfTestMonthlyReport(pageSize))
        case _                                                                       =>
          nonPerfTestReport(zReference, taxYear, month, pageIndex, pageSize)
      }
      .recoverWith { case NonFatal(_) =>
        nonPerfTestReport(zReference, taxYear, month, pageIndex, pageSize)
      }
  }

  private def nonPerfTestReport(
    zReference: String,
    taxYear: String,
    month: String,
    pageIndex: Int,
    pageSize: Int
  ): Future[Result] =
    if (zReference == "Z1500") {
      Future.successful(
        InternalServerError(Json.toJson(internalServerErr("Internal issue, try again later")))
      )
    } else {
      retrieveReportService
        .getMonthlyReport(zReference, taxYear, month, pageIndex, pageSize)
        .map {
          case Right(response) =>
            logger.info(
              s"Successful retrieval of monthly report for IM ref: [$zReference] for [$month][$taxYear]"
            )
            Ok(Json.toJson(response))
          case Left(error)     =>
            logger.warn(
              s"${error.code} for IM ref: [$zReference] for [$month][$taxYear]: ${error.message}"
            )
            NotFound(Json.toJson(error))
        }
        .recover { case ex =>
          logger.error(
            s"Unexpected error retrieving monthly report for IM ref: [$zReference] for [$month][$taxYear] with: [${ex.getMessage}]"
          )
          InternalServerError(
            Json.toJson(internalServerErr(s"Failed with exception: ${ex.getMessage}"))
          )
        }
    }

  private def perfTestMonthlyReport(pageSize: Int): Result = {
    val recordCount = Random.nextInt(pageSize) + 1
    val results     = reportIssuesService.generateResults(GenerateReportRequest(recordCount, 0, 0))
    Ok(Json.toJson(ReturnResultResponse(totalRecords = perfTestTotalRecords, returnResults = results)))
  }
}
