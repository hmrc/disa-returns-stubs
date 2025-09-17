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
import uk.gov.hmrc.disareturnsstubs.models.ErrorResponse.{InternalSeverErr, ResultSummaryNotFoundErr, ReturnNotFoundErr}

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

  "POST /nps/summary-results/:isaManagerReferenceNumber/:returnId" should {
    "return 200 OK for any non-error ISA ref" in {
      val request = FakeRequest(GET, s"$getReturnResultsSummaryEndpoint/Z1234/$returnId")
        .withHeaders("Authorization" -> "Bearer token")

      val result = route(app, request).get
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("totalRecords" -> 10)
    }

    "return 404 NOT_FOUND for Z1404 ISA ref" in {
      val request = FakeRequest(GET, s"$getReturnResultsSummaryEndpoint/Z1404/$returnId")
        .withHeaders("Authorization" -> "Bearer token")

      val result = route(app, request).get
      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe Json.toJson(ReturnNotFoundErr(returnId))
    }

    "return 500 INTERNAL_SERVER_ERROR for Z1500 ISA ref" in {
      val request = FakeRequest(GET, s"$getReturnResultsSummaryEndpoint/Z1500/$returnId")
        .withHeaders("Authorization" -> "Bearer token")

      val result = route(app, request).get
      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.toJson(InternalSeverErr)
    }
  }

  "POST /nps/summary-results/:isaManagerReferenceNumber/:taxYear/:month" should {
    "return 200 OK for any non-error ISA ref" in {
      val request = FakeRequest(GET, s"$getReturnResultsSummaryEndpoint/Z1234/$taxYear/$month")
        .withHeaders("Authorization" -> "Bearer token")

      val result = route(app, request).get
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("totalRecords" -> 10)
    }

    "return 404 NOT_FOUND for Z1404 ISA ref" in {
      val request = FakeRequest(GET, s"$getReturnResultsSummaryEndpoint/Z1404/$taxYear/$month")
        .withHeaders("Authorization" -> "Bearer token")

      val result = route(app, request).get
      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe Json.toJson(ResultSummaryNotFoundErr("Z1404", taxYear, month))
    }

    "return 500 INTERNAL_SERVER_ERROR for Z1500 ISA ref" in {
      val request = FakeRequest(GET, s"$getReturnResultsSummaryEndpoint/Z1500/$taxYear/$month")
        .withHeaders("Authorization" -> "Bearer token")

      val result = route(app, request).get
      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.toJson(InternalSeverErr)
    }
  }
}
