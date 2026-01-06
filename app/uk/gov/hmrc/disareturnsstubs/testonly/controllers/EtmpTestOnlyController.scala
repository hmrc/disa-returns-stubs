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

package uk.gov.hmrc.disareturnsstubs.testonly.controllers

import play.api.Logging
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.disareturnsstubs.models.{EtmpObligations, EtmpReportingWindow}
import uk.gov.hmrc.disareturnsstubs.repositories.{ObligationStatusRepository, ReportingWindowRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EtmpTestOnlyController @Inject() (
  cc: ControllerComponents,
  reportingWindowRepository: ReportingWindowRepository,
  obligationStatusRepository: ObligationStatusRepository
)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with Logging {

  def setReportingWindowState(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[EtmpReportingWindow] match {
      case JsSuccess(value, _) =>
        logger.info(s"[TestOnly] Setting reporting window state as: [${value.reportingWindowOpen}]")
        reportingWindowRepository.setReportingWindowState(value.reportingWindowOpen).map(_ => NoContent)
      case JsError(errors)     =>
        logger.warn(s"[TestOnly] Failed to set reporting window state with errors: [$errors]")
        Future.successful(BadRequest(Json.obj("error" -> "Missing or invalid 'reportingWindowOpen' field")))
    }
  }

  def getReportingWindowState: Action[AnyContent] = Action.async {
    reportingWindowRepository.getReportingWindowState.map {
      case Some(open) =>
        logger.info(s"[TestOnly] Retreived reporting window state as: [$open]")
        Ok(Json.obj("reportingWindowOpen" -> open))
      case None       =>
        logger.warn(s"[TestOnly] Reporting window state not found")
        NotFound(Json.obj("error" -> "No reporting window state found"))
    }
  }

  def openObligationStatus(zReference: String): Action[AnyContent] = Action.async {
    obligationStatusRepository
      .openObligationStatus(zReference)
      .map { _ =>
        logger.info(s"[TestOnly] Created open obligation status for IM Ref: [$zReference]")
        Ok(Json.toJson(EtmpObligations(obligationAlreadyMet = false)))
      }
  }
}
