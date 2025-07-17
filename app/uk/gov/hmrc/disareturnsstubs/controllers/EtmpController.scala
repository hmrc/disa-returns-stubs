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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.disareturnsstubs.models.JsonFormats._
import uk.gov.hmrc.disareturnsstubs.models.{EtmpObligations, EtmpReportingWindow}
import uk.gov.hmrc.disareturnsstubs.repositories.ReportingWindowRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EtmpController @Inject() (cc: ControllerComponents, reportingWindowRepository: ReportingWindowRepository)(implicit
  executionContext: ExecutionContext
) extends BackendController(cc)
    with Logging {

  def checkReturnsObligationStatus(isaManagerReferenceNumber: String): Action[AnyContent] = Action.async { request =>
    isaManagerReferenceNumber match {
      case "Z1111" =>
        Future.successful(Ok(Json.toJson(EtmpObligations(obligationAlreadyMet = true))))
      case "Z1234" =>
        Future.successful(
          InternalServerError(
            Json.toJson(UpstreamErrorResponse(statusCode = INTERNAL_SERVER_ERROR, message = "Upstream error"))
          )
        )
      case _       =>
        Future.successful(Ok(Json.toJson(EtmpObligations(obligationAlreadyMet = false))))
    }
  }

  def checkReportingWindowStatus: Action[AnyContent] = Action.async {
    reportingWindowRepository.getReportingWindowState.map {
      case Some(open) =>
        logger.info(s"ETMP Reporting Window isOpen: $open")
        Ok(Json.toJson(EtmpReportingWindow(reportingWindowOpen = open)))
      case None       =>
        NotFound(Json.obj("error" -> "No reporting window state found"))
    }
  }
}
