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
import uk.gov.hmrc.disareturnsstubs.models.{EtmpObligations, EtmpReportingWindow}
import uk.gov.hmrc.disareturnsstubs.repositories.{ObligationStatusRepository, ReportingWindowRepository}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EtmpController @Inject() (
  cc: ControllerComponents,
  reportingWindowRepository: ReportingWindowRepository,
  obligationStatusRepository: ObligationStatusRepository
)(implicit
  executionContext: ExecutionContext
) extends BackendController(cc)
    with Logging {

  def checkReportingWindowStatus: Action[AnyContent] = Action.async {
    reportingWindowRepository.getReportingWindowState.map {
      case Some(open) =>
        logger.info(s"ETMP Reporting Window isOpen: $open")
        Ok(Json.toJson(EtmpReportingWindow(reportingWindowOpen = open)))
      case None       =>
        logger.warn(s"No reporting window state found")
        NotFound(Json.obj("error" -> "No reporting window state found"))
    }
  }

  def declare(zReference: String): Action[AnyContent] = Action.async {
    logger.info(s"Declaration received for IM ref: [$zReference], closing obligation status")

    obligationStatusRepository
      .closeObligationStatus(zReference)
      .map(_ => NoContent)
  }

  def checkReturnsObligationStatus(zReference: String): Action[AnyContent] = Action.async {
    obligationStatusRepository
      .getObligationStatus(zReference)
      .flatMap {
        case Some(status) =>
          logger.info(s"Return obligation status for IM ref: [$zReference] with status: [$status]")
          Future.successful(Ok(Json.toJson(EtmpObligations(obligationAlreadyMet = status))))
        case None         =>
          logger.info(s"Return obligation status for IM ref: [$zReference] with status: [false]")
          obligationStatusRepository
            .openObligationStatus(zReference)
            .map(_ => Ok(Json.toJson(EtmpObligations(obligationAlreadyMet = false))))
      }
  }
}
