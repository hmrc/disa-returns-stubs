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
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.util.UUID

@Singleton
class SubmissionsController @Inject() (cc: ControllerComponents) extends BackendController(cc) {

  def createReturn(
    zReference: String,
    taxYear: String,
    month: String
  ): Action[AnyContent] = Action {
    Created(Json.obj("submissionId" -> UUID.randomUUID().toString))
  }

  def storeSubmission(
    zReference: String,
    taxYear: String,
    month: String,
    submissionId: String
  ): Action[AnyContent] = Action(Ok)

  def declare(
    zReference: String,
    taxYear: String,
    month: String
  ): Action[AnyContent] = Action(Ok)
}
