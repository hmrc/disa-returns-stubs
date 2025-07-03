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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.disareturnsstubs.models.{EtmpObligations, EtmpReportingWindow}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import javax.inject.Inject
import scala.concurrent.Future

class EtmpController @Inject() (cc: ControllerComponents) extends BackendController(cc) {

  def checkReturnsObligationStatus(isaManagerReferenceNumber: String): Action[AnyContent] = Action.async { request =>
    isaManagerReferenceNumber match {
      case "Z1111" => Future.successful(Unauthorized(Json.toJson(EtmpObligations(obligationAlreadyMet = true))))
      case _       => Future.successful(Ok(Json.toJson(EtmpObligations(obligationAlreadyMet = false))))
    }
  }

  def checkReportingWindowStatus: Action[AnyContent] = Action.async { request =>
    request.headers.get("Test-Scenario") match {
      case Some("reporting-window-closed") =>
        Future.successful(Unauthorized(Json.toJson(EtmpReportingWindow(reportingWindowOpen = false))))
      case _                               =>
        Future.successful(Ok(Json.toJson(EtmpReportingWindow(reportingWindowOpen = true))))
    }
  }
}
