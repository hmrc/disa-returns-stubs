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

import org.scalatest.matchers.should.Matchers._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.disareturnsstubs.BaseISpec

class GenerateReportControllerISpec extends BaseISpec {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure("play.http.router" -> "test.Routes")
      .build()
  val year = "2025-26"
  val month = "JAN"
  val createReportEndpoint = s"/$isaManagerReferenceNumber/$year/$month/reconciliation"

  val validPayload: JsValue = Json.parse(
    """{
      |  "oversubscribed": 2,
      |  "traceAndMatch": 1,
      |  "failedEligibility": 1
      |}""".stripMargin
  )

  val invalidPayload: JsValue = Json.parse(
    """{
      |  "oversubscribed": "two",
      |  "traceAndMatch": 1
      |}""".stripMargin
  )

  "POST /:isaManagerReferenceNumber/:year/:month/reconciliation" should {

    "return 204 NoContent for a valid request" in {
      val request = FakeRequest(POST, createReportEndpoint)
        .withHeaders("Authorization" -> "Bearer token")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(validPayload)

      val result = route(app, request).get

      status(result) mustBe NO_CONTENT

      val report = await(reportsRepository.collection.find().toFuture())

      report.exists(report =>
        report.isaManagerReferenceNumber == isaManagerReferenceNumber &&
          report.month == month &&
          report.year == year) shouldBe true
    }

    "return 400 BadRequest when the payload is invalid JSON" in {
      val request = FakeRequest(POST, createReportEndpoint)
        .withHeaders("Authorization" -> "Bearer token")
        .withJsonBody(invalidPayload)

      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }

    "return 403 Forbidden when Authorization header is missing" in {
      val request = FakeRequest(POST, createReportEndpoint)
        .withJsonBody(validPayload)

      val result = route(app, request).get

      status(result) mustBe FORBIDDEN
      (contentAsJson(result) \ "message").asOpt[String] mustBe Some("Missing required bearer token")
    }
  }
}
