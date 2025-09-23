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

import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.disareturnsstubs.BaseISpec

class NpsControllerISpec extends BaseISpec {

  val submitMonthlyReturnEndpoint = "/nps/submit"
  val getReturnResultsSummaryEndpoint = "/nps/summary-results"
  val month = "APR"
  val taxYear = "2025"

  val validPayload: JsValue = Json.parse("""[
  |  {
  |    "accountNumber": "12345678",
  |    "nino": "AB123456C",
  |    "firstName": "John",
  |    "middleName": "Michael",
  |    "lastName": "Smith",
  |    "dateOfBirth": "1980-01-01",
  |    "isaType": "LIFETIME_CASH",
  |    "reportingATransfer": true,
  |    "dateOfLastSubscription": "2025-05-01",
  |    "totalCurrentYearSubscriptionsToDate": 5000,
  |    "marketValueOfAccount": 10000,
  |    "dateOfFirstSubscription": "2025-04-06",
  |    "lisaQualifyingAddition": 1000,
  |    "lisaBonusClaim": 1000
  |  },
  |  {
  |    "accountNumber": "12345678",
  |    "nino": "AB123456C",
  |    "firstName": "John",
  |    "middleName": "Michael",
  |    "lastName": "Smith",
  |    "dateOfBirth": "1980-01-01",
  |    "isaType": "LIFETIME_CASH",
  |    "reportingATransfer": true,
  |    "dateOfLastSubscription": "2025-05-01",
  |    "totalCurrentYearSubscriptionsToDate": 5000,
  |    "marketValueOfAccount": 10000,
  |    "accountNumberOfTransferringAccount": "87654321",
  |    "amountTransferred": 5000,
  |    "dateOfFirstSubscription": "2025-04-06",
  |    "lisaQualifyingAddition": 1000,
  |    "lisaBonusClaim": 1000
  |  }
  |]""".stripMargin)

  "POST /nps/submit/:isaReferenceNumber" should {

    "return 204 NoContent for any non-error ISA ref" in {
      val request = FakeRequest(POST, s"$submitMonthlyReturnEndpoint/Z1234")
        .withHeaders("Authorization" -> "Bearer token")
        .withJsonBody(validPayload)

      val result = route(app, request).get
      status(result) mustBe NO_CONTENT
    }

    "return 400 BadRequest for isaRef Z1400" in {
      val request = FakeRequest(POST, s"$submitMonthlyReturnEndpoint/Z1400")
        .withHeaders("Authorization" -> "Bearer token")
        .withJsonBody(validPayload)

      val result = route(app, request).get
      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "code").asOpt[String] mustBe Some("BAD_REQUEST")
    }

    "return 503 ServiceUnavailable for isaRef Z1503" in {
      val request = FakeRequest(POST, s"$submitMonthlyReturnEndpoint/Z1503")
        .withHeaders("Authorization" -> "Bearer token")
        .withJsonBody(validPayload)

      val result = route(app, request).get
      status(result) mustBe SERVICE_UNAVAILABLE
      (contentAsJson(result) \ "code").asOpt[String] mustBe Some("SERVICE_UNAVAILABLE")
    }

    "return 403 Forbidden when Authorization header is missing" in {
      val request = FakeRequest(POST, s"$submitMonthlyReturnEndpoint/Z1234")
        .withJsonBody(validPayload)

      val result = route(app, request).get
      status(result) mustBe FORBIDDEN
      (contentAsJson(result) \ "message").asOpt[String] mustBe Some("Missing required bearer token")
    }
  }
}
