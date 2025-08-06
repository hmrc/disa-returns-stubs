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

import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.disareturnsstubs.models.EtmpReportingWindow
import uk.gov.hmrc.disareturnsstubs.repositories.ReportingWindowRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportingWindowController @Inject() (
  cc: ControllerComponents,
  repository: ReportingWindowRepository
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def setReportingWindowState(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[EtmpReportingWindow] match {
      case JsSuccess(value, _) =>
        repository.setReportingWindowState(value.reportingWindowOpen).map(_ => NoContent)
      case JsError(_)          =>
        Future.successful(BadRequest(Json.obj("error" -> "Missing or invalid 'reportingWindowOpen' field")))
    }
  }

  def getReportingWindowState: Action[AnyContent] = Action.async {
    repository.getReportingWindowState.map {
      case Some(open) => Ok(Json.obj("reportingWindowOpen" -> open))
      case None       => NotFound(Json.obj("error" -> "No reporting window state found"))
    }
  }
}
