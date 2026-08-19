/*
 * Copyright 2026 HM Revenue & Customs
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

import jakarta.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.disareturnsstubs.controllers.action.AuthorizationFilter
import uk.gov.hmrc.disareturnsstubs.services.ReportingWindowService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class ReportingWindowController @Inject() (
  cc: ControllerComponents,
  service: ReportingWindowService,
  authorizationFilter: AuthorizationFilter
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def status: Action[AnyContent] =
    (Action andThen authorizationFilter).async { request =>
      request.headers.get("X-Cred-Id").map(_.trim).filter(_.nonEmpty) match {
        case Some(credId) => service.isOpen(credId).map(open => Ok(Json.obj("reportingWindowOpen" -> open)))
        case None         => scala.concurrent.Future.successful(BadRequest(Json.obj("error" -> "Missing X-Cred-Id header")))
      }
    }
}
