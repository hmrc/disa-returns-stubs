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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.disareturnsstubs.controllers.action.AuthorizationFilter
import uk.gov.hmrc.disareturnsstubs.models.ErrorResponse._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.Future

@Singleton
class NpsController @Inject() (
  cc: ControllerComponents,
  authorizationFilter: AuthorizationFilter
) extends BackendController(cc)
    with Logging {

  def submitMonthlyReturn(isaReferenceNumber: String): Action[JsValue] =
    (Action andThen authorizationFilter).async(parse.json) { implicit request =>
      handleRequest(isaReferenceNumber, request.body)
    }

  private def handleRequest(isaRef: String, body: JsValue): Future[Result] = {
    logger.info(s"Nps Stub received payload for ISA ref $isaRef: ${Json.prettyPrint(body)}")
    isaRef match {
      case "Z1400" =>
        Future.successful(BadRequest(Json.toJson(BadRequestErr)))
      case "Z1503" =>
        Future.successful(ServiceUnavailable(Json.toJson(ServiceUnavailableErr)))
      case _       => Future.successful(NoContent)
    }
  }

  def getResultsSummary(isaReferenceNumber: String, returnId: String): Action[AnyContent] =
    (Action andThen authorizationFilter).async { implicit request =>
      isaReferenceNumber match {
        case "Z1404" =>
          Future.successful(NotFound(Json.toJson(ReturnNotFoundErr(returnId))))
        case "Z1500" =>
          Future.successful(InternalServerError(Json.toJson(InternalSeverErr)))
        case _       => Future.successful(Ok(Json.obj("totalRecords" -> 10)))
      }
    }

  def getResultsSummaryHeaderless(isaReferenceNumber: String, taxYear: String, month: String): Action[AnyContent] =
    (Action andThen authorizationFilter).async { implicit request =>
      isaReferenceNumber match {
        case "Z1404" =>
          Future.successful(NotFound(Json.toJson(ResultSummaryNotFoundErr(isaReferenceNumber, taxYear, month))))
        case "Z1500" =>
          Future.successful(InternalServerError(Json.toJson(InternalSeverErr)))
        case _       => Future.successful(Ok(Json.obj("totalRecords" -> 10)))
      }
    }
}
