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

import play.api.Play.materializer
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.disareturnsstubs.controllers.NpsController
import utils.BaseUnitSpec

class NpsControllerSpec extends BaseUnitSpec {

  val isaManagerReference = "Z1234"
  private val controller  = new NpsController(
    stubControllerComponents(),
    stubAuthFilter
  )

  "submitMonthlyReturn" should {
    "return 204 no content for successful submission" in {
      val request =
        FakeRequest(POST, s"/nps/submit/$isaManagerReference")

      val result = controller.submitMonthlyReturn(isaManagerReference)(request)
      status(result) shouldBe NO_CONTENT
    }

    "return 400 Bad Request for zRef Z1400" in {
      val isaManagerReference = "Z1400"
      val request             = FakeRequest(POST, s"/nps/submit/$isaManagerReference")

      val result = controller.submitMonthlyReturn(isaManagerReference)(request)
      status(result)                                 shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String]    shouldBe "BAD_REQUEST"
      (contentAsJson(result) \ "message").as[String] shouldBe "Bad request"
    }

    "return 503 Service Unavailable for zRef Z1503" in {
      val isaManagerReference = "Z1503"
      val request             = FakeRequest(POST, s"/nps/submit/$isaManagerReference")

      val result = controller.submitMonthlyReturn(isaManagerReference)(request)
      status(result)                                 shouldBe SERVICE_UNAVAILABLE
      (contentAsJson(result) \ "code").as[String]    shouldBe "SERVICE_UNAVAILABLE"
      (contentAsJson(result) \ "message").as[String] shouldBe "Service unavailable"
    }
  }

  "notification" should {
    "return 204 no content for successful notification" in {
      val request =
        FakeRequest(POST, s"/nps/declaration/$isaManagerReference")

      val result = controller.notification(isaManagerReference)(request)
      status(result) shouldBe NO_CONTENT
    }

    "return 500 Internal Server Error for zRef Z5000" in {
      val isaManagerReference = "Z5000"
      val request             = FakeRequest(POST, s"/nps/declaration/$isaManagerReference")

      val result = controller.notification(isaManagerReference)(request)
      status(result)                                 shouldBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "code").as[String]    shouldBe "INTERNAL_SERVER_ERROR"
      (contentAsJson(result) \ "message").as[String] shouldBe "Internal issue, try again later"
    }
  }
}
