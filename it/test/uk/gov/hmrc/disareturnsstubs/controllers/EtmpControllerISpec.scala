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
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

class EtmpControllerISpec extends PlaySpec with GuiceOneAppPerSuite {

  val obligationStatusEndpoint = "/disa-returns-stubs/etmp/check-obligation-status"
  val reportingWindowEndpoint = "/disa-returns-stubs/etmp/check-reporting-window"

  "EtmpController GET /etmp/check-obligation-status/:isaManagerReferenceNumber" should {

    "return 401 with obligationAlreadyMet = true for Z1111" in {
      val request = FakeRequest(GET, s"$obligationStatusEndpoint/Z1111").withJsonBody(Json.obj())
      val result = route(app, request).get

      status(result) mustBe UNAUTHORIZED
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
  }

  "EtmpController GET /etmp/check-reporting-window" should {

    "return 401 with reportingWindowOpen = false for request with 'Test-Scenario - reporting-window-closed' headers provided " in {
      val request = FakeRequest(GET, s"$reportingWindowEndpoint").withJsonBody(Json.obj())
        .withHeaders("Test-Scenario" -> "reporting-window-closed")
      val result = route(app, request).get

      status(result) mustBe UNAUTHORIZED
      contentAsJson(result) mustBe Json.obj(
        "reportingWindowOpen" -> false
      )
    }

    "return 200 with reportingWindowOpen = true with no Test-Scenario header provided" in {
      val request = FakeRequest(GET, s"$reportingWindowEndpoint").withJsonBody(Json.obj())
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj(
        "reportingWindowOpen" -> true
      )
    }
  }
}
