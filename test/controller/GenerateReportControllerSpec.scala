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

package controller

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.disareturnsstubs.services.GenerateAndStoreReportService
import uk.gov.hmrc.disareturnsstubs.testonly.controllers.GenerateReportController
import utils.BaseUnitSpec

import scala.concurrent.Future

class GenerateReportControllerSpec extends BaseUnitSpec {

  private val mockGenerateAndStoreReportService = mock[GenerateAndStoreReportService]

  private val controller = new GenerateReportController(
    mockGenerateAndStoreReportService,
    stubAuthFilter,
    stubControllerComponents()
  )

  private val year  = "2025-26"
  private val month = "JAN"

  private val validPayload: JsValue = Json.parse(
    """{
      | "oversubscribed": 2,
      | "traceAndMatch": 1,
      | "failedEligibility": 1
      |}""".stripMargin
  )

  private val invalidPayload: JsValue = Json.parse(
    """{
      | "oversubscribed": "two"
      |}""".stripMargin
  )

  private def jsonPostRequest(body: JsValue) =
    FakeRequest(POST, "/")
      .withHeaders("Content-Type" -> "application/json")
      .withBody(body)

  "create" should {

    "return 204 NoContent when report generation succeeds" in {
      when(mockGenerateAndStoreReportService.generateAndStore(any(), any(), any(), any()))
        .thenReturn(Future.successful(()))

      val result = controller.create(validZReference, year, month)(jsonPostRequest(validPayload))

      status(result)          shouldBe NO_CONTENT
      contentAsString(result) shouldBe ""
    }

    "return 500 InternalServerError when service fails" in {
      when(mockGenerateAndStoreReportService.generateAndStore(any(), any(), any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Mongo failure")))

      val result = controller.create(validZReference, year, month)(jsonPostRequest(validPayload))

      status(result)                               shouldBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "message").as[String] should include("Mongo failure")
    }

    "return 400 BadRequest when payload is invalid" in {
      val result = controller.create(validZReference, year, month)(jsonPostRequest(invalidPayload))

      status(result) shouldBe BAD_REQUEST
    }
  }
}
