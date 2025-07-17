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

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.disareturnsstubs.repositories.ReportingWindowRepository

class EtmpControllerISpec extends PlaySpec with GuiceOneAppPerSuite with DefaultAwaitTimeout {

  val obligationStatusEndpoint = "/etmp/check-obligation-status"
  val reportingWindowEndpoint = "/etmp/check-reporting-window"

  "EtmpController GET /etmp/check-obligation-status/:isaManagerReferenceNumber" should {

    "return 200 with obligationAlreadyMet = true for Z1111" in {
      val request = FakeRequest(GET, s"$obligationStatusEndpoint/Z1111").withJsonBody(Json.obj())
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj(
        "obligationAlreadyMet" -> true
      )
    }

    "return 200 with obligationAlreadyMet = false for any other reference" in {
      val request = FakeRequest(GET, s"$obligationStatusEndpoint/ANY123").withJsonBody(Json.obj())
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj(
        "obligationAlreadyMet" -> false
      )
    }

    "return 500 with error message when stub.reportingWindowScenario is 'failure'" in {
      val request = FakeRequest(GET, s"$obligationStatusEndpoint/Z1234")
      val result = route(app, request).get

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj(
        "message" -> "Upstream error",
        "statusCode" -> 500,
        "reportAs" -> 500,
        "headers" -> Json.obj()
      )
    }
  }

  "EtmpController GET /etmp/check-reporting-window" should {

    "return 200 with reportingWindowOpen = false when stub.reportingWindowScenario is 'closed'" in {
        lazy val mockReportingWindowState: ReportingWindowRepository =
          app.injector.instanceOf[ReportingWindowRepository]
        await(mockReportingWindowState.setReportingWindowState(false))

        val request = FakeRequest(GET, reportingWindowEndpoint)
        val result = route(app, request).get

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.obj("reportingWindowOpen" -> false)
    }

    "return 200 with reportingWindowOpen = true when stub.reportingWindowScenario is 'open'" in {
        lazy val mockReportingWindowState: ReportingWindowRepository =
          app.injector.instanceOf[ReportingWindowRepository]
        await(mockReportingWindowState.setReportingWindowState(true))

        val request = FakeRequest(GET, reportingWindowEndpoint)
        val result = route(app, request).get

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.obj("reportingWindowOpen" -> true)
      }
  }
}
