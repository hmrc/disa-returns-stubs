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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.disareturnsstubs.controllers.action.AuthorizationFilter
import uk.gov.hmrc.disareturnsstubs.models.ErrorResponse._
import uk.gov.hmrc.disareturnsstubs.models.ReturnResultResponse
import uk.gov.hmrc.disareturnsstubs.repositories.ReportRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NpsController @Inject() (
  cc: ControllerComponents,
  authorizationFilter: AuthorizationFilter,
  reportRepository: ReportRepository
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def submitMonthlyReturn(isaReferenceNumber: String): Action[AnyContent] =
    (Action andThen authorizationFilter).async { _ =>
      isaReferenceNumber match {
        case "Z1400" => Future.successful(BadRequest(Json.toJson(badRequestError)))
        case "Z1503" => Future.successful(ServiceUnavailable(Json.toJson(serviceUnavailableError)))
        case _       => Future.successful(NoContent)
      }
    }

  def send(isaReferenceNumber: String): Action[AnyContent] = Action {
    isaReferenceNumber match {
      case "Z1500" => InternalServerError(Json.toJson(internalServerErr("Internal issue, try again later")))
      case _       => NoContent
    }
  }

  def getMonthlyReport(
    isaReferenceNumber: String,
    taxYear: String,
    month: String
  ): Action[AnyContent] = Action.async { _ =>
    if (isaReferenceNumber == "Z1500") {
      Future.successful(
        InternalServerError(Json.toJson(internalServerErr("Internal issue, try again later")))
      )
    } else {
      reportRepository
        .getMonthlyReport(isaReferenceNumber, taxYear, month)
        .map {
          case Some(report) =>
            Ok(
              Json.toJson(
                ReturnResultResponse(totalRecords = report.returnResults.size, returnResults = report.returnResults)
              )
            )
          case None         =>
            NotFound(Json.toJson(reportNotFoundError))
        }
        .recover { case ex =>
          InternalServerError(Json.toJson(internalServerErr(s"Failed with exception: ${ex.getMessage}")))
        }
    }
  }
}
