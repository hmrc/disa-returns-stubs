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


import org.apache.pekko.stream.Materializer
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.disareturnsstubs.repositories.ReportingWindowRepository

import scala.concurrent.Future

class ReportingWindowControllerISpec
  extends PlaySpec
    with GuiceOneAppPerSuite
    with Injecting {

  implicit lazy val mat: Materializer = app.materializer

  val mockReportingWindowState: ReportingWindowRepository = MockitoSugar.mock[ReportingWindowRepository]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure("play.http.router" -> "test.Routes")
      .overrides(
        bind[ReportingWindowRepository].toInstance(mockReportingWindowState)
      )
      .build()

  "setReportingWindowState" should {
    "return 204 NoContent when valid boolean is provided" in {
      when(mockReportingWindowState.setReportingWindowState(true)) thenReturn Future.unit

      val request = FakeRequest(POST, "/setup-obligation-window")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(Json.obj("reportingWindowOpen" -> true))

      val result = route(app, request).get

      status(result) mustBe NO_CONTENT
    }

    "return 400 BadRequest when request body is missing or invalid" in {
      val invalidRequest = FakeRequest(POST, "/setup-obligation-window")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(Json.obj("invalidField" -> "oops"))

      val result = route(app, invalidRequest).get

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "error").as[String] must include("reportingWindowOpen")
    }
  }

  "getReportingWindowState" should {
    "return 200 with correct JSON when data exists" in {
      when(mockReportingWindowState.getReportingWindowState) thenReturn Future.successful(Some(true))

      val request = FakeRequest(GET, "/obligation-window-state")
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("reportingWindowOpen" -> true)
    }

    "return 404 when no data is found" in {
      when(mockReportingWindowState.getReportingWindowState) thenReturn Future.successful(None)

      val request = FakeRequest(GET, "/obligation-window-state")
      val result = route(app, request).get

      status(result) mustBe NOT_FOUND
      (contentAsJson(result) \ "error").as[String] must include("No reporting window state")
    }
  }
}
