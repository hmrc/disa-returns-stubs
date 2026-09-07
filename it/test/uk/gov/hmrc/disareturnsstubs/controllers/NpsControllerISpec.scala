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

import org.mongodb.scala.SingleObservableFuture
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.*
import play.api.test.*
import uk.gov.hmrc.disareturnsstubs.BaseISpec
import uk.gov.hmrc.disareturnsstubs.models.generatereport.{ReportEvent, ReportIssueDocument}
import uk.gov.hmrc.disareturnsstubs.models.*
import uk.gov.hmrc.disareturnsstubs.services.RetrieveReportService.encodeCursor

import java.time.Instant

class NpsControllerISpec extends BaseISpec {

  val submitMonthlyReturnEndpoint = "/nps/submit"
  val npsDeclarationEndpoint      = "/nps/declaration"
  val month                       = "APR"
  val taxYear                     = "2025-26"

  val report: MonthlyReport = MonthlyReport(
    zReference = validZReference,
    year = taxYear,
    month = month,
    returnResults = Seq(
      ReturnResult(
        accountNumber = "100000001",
        nino = "AB123457C",
        issueIdentified = IssueIdentifiedMessage(
          code = "UNABLE_TO_IDENTIFY_INVESTOR",
          message = "Unable to identify investor"
        )
      )
    )
  )

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

  "POST /nps/submit/:zReference" should {

    "return 204 NoContent for any non-error ISA ref" in {
      val request = FakeRequest(POST, s"$submitMonthlyReturnEndpoint/$validZReference")
        .withHeaders(AUTHORIZATION -> "Bearer token")
        .withJsonBody(validPayload)

      val result = route(app, request).get
      status(result) mustBe NO_CONTENT
    }

    "return 400 BadRequest for isaRef Z1400" in {
      val request = FakeRequest(POST, s"$submitMonthlyReturnEndpoint/Z1400")
        .withHeaders(AUTHORIZATION -> "Bearer token")
        .withJsonBody(validPayload)

      val result = route(app, request).get
      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "code").asOpt[String] mustBe Some("BAD_REQUEST")
    }

    "return 503 ServiceUnavailable for isaRef Z1503" in {
      val request = FakeRequest(POST, s"$submitMonthlyReturnEndpoint/Z1503")
        .withHeaders(AUTHORIZATION -> "Bearer token")
        .withJsonBody(validPayload)

      val result = route(app, request).get
      status(result) mustBe SERVICE_UNAVAILABLE
      (contentAsJson(result) \ "code").asOpt[String] mustBe Some("SERVICE_UNAVAILABLE")
    }

    "return 403 Forbidden when Authorization header is missing" in {
      val request = FakeRequest(POST, s"$submitMonthlyReturnEndpoint/$validZReference")
        .withJsonBody(validPayload)

      val result = route(app, request).get
      status(result) mustBe FORBIDDEN
      (contentAsJson(result) \ "message").asOpt[String] mustBe Some("Missing required bearer token")
    }

    "return 413 Payload Too Large when request exceeds 10MB" in {
      val oversizedBody = "a" * (10 * 1024 * 1024 + 1)

      val request =
        FakeRequest(POST, s"$submitMonthlyReturnEndpoint/$validZReference")
          .withHeaders(AUTHORIZATION -> "Bearer token")
          .withBody(oversizedBody)

      val result = route(app, request).get

      status(result) mustBe REQUEST_ENTITY_TOO_LARGE
    }
  }

  "POST /nps/declaration/:zReference" should {

    "return 204 NoContent for any non-error ISA ref" in {
      val request = FakeRequest(POST, s"$npsDeclarationEndpoint/$validZReference")
      val result  = route(app, request).get
      status(result) mustBe NO_CONTENT
    }

    "return 500 InternalServerError for ISA ref Z1500" in {
      val request = FakeRequest(POST, s"$npsDeclarationEndpoint/Z1500")

      val result = route(app, request).get
      status(result) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "code").asOpt[String] mustBe Some("INTERNAL_SERVER_ERROR")
      (contentAsJson(result) \ "message").asOpt[String] mustBe Some("Internal issue, try again later")
    }
  }

  val limit = 2

  val reportEventDocument: ReportEvent = ReportEvent(
    reportId = "RPT_TEST",
    zReference = validZReference,
    createdAt = Instant.now()
  )

  val reportIssueDocumentMessage: ReportIssueDocument = ReportIssueDocument(
    reportId = "RPT_TEST",
    accountNumber = "100000001",
    nino = "AB123457C",
    issueIdentified = IssueIdentifiedMessage(
      code = "UNABLE_TO_IDENTIFY_INVESTOR",
      message = "Unable to identify investor"
    ),
    createdAt = Instant.now()
  )

  val reportIssueDocumentOverSubscribed: ReportIssueDocument = ReportIssueDocument(
    reportId = "RPT_TEST",
    accountNumber = "100000001",
    nino = "AB123457C",
    issueIdentified = IssueIdentifiedOverSubscribed(
      code = "OVER_SUBSCRIBED",
      overSubscribedAmount = 123.1
    ),
    createdAt = Instant.now()
  )

  "GET /monthly/:zReference/:taxYear/:month/results" should {

    "return 200 OK using the default limit when a report exists" in {
      val reportEvent = reportEventDocument.copy(reportId = "RPT1")
      await(reportEventRepository.upsert(reportEvent))

      val issue = reportIssueDocumentMessage.copy(reportId = "RPT1")
      await(reportIssueRepository.insertMany(Seq(issue)))

      val request = FakeRequest(GET, s"/monthly/$validZReference/$taxYear/$month/results")
      val result  = route(app, request).get

      status(result) mustBe OK
      val jsonBody = contentAsJson(result)
      val response = jsonBody.as[ReturnResultResponse]

      response.returnResults must have size 1
      response.nextCursor mustBe None
      (jsonBody \ "totalRecords").toOption mustBe None
      (jsonBody \ "nextCursor").toOption mustBe None

      val first = response.returnResults.head
      first.accountNumber mustBe "100000001"
      first.nino mustBe "AB123457C"
      first.issueIdentified.code mustBe "UNABLE_TO_IDENTIFY_INVESTOR"
    }

    "return 200 OK with a raw cursor across multiple pages" in {
      val reportEvent = reportEventDocument.copy(reportId = "RPT2")
      await(reportEventRepository.upsert(reportEvent))

      val issues = Seq(
        reportIssueDocumentOverSubscribed.copy(reportId = "RPT2", accountNumber = "100000001"),
        reportIssueDocumentOverSubscribed.copy(reportId = "RPT2", accountNumber = "100000002"),
        reportIssueDocumentOverSubscribed.copy(reportId = "RPT2", accountNumber = "100000003")
      )
      await(reportIssueRepository.insertMany(issues))

      val firstRequest = FakeRequest(GET, s"/monthly/$validZReference/$taxYear/$month/results?limit=$limit")
      val firstResult  = route(app, firstRequest).get
      status(firstResult) mustBe OK
      val firstJson    = contentAsJson(firstResult)
      val nextCursor   = (firstJson \ "nextCursor").as[String]
      (firstJson \ "returnResults")
        .as[Seq[ReturnResult]]
        .map(_.accountNumber) must contain allOf ("100000001", "100000002")
      (firstJson \ "totalRecords").toOption mustBe None
      nextCursor mustBe "b2Zmc2V0OjI"

      val secondRequest = FakeRequest(
        GET,
        s"/monthly/$validZReference/$taxYear/$month/results?cursor=$nextCursor&limit=$limit"
      )
      val secondResult  = route(app, secondRequest).get
      status(secondResult) mustBe OK
      val secondJson    = contentAsJson(secondResult)
      (secondJson \ "returnResults").as[Seq[ReturnResult]].map(_.accountNumber) must contain("100000003")
      (secondJson \ "totalRecords").toOption mustBe None
      (secondJson \ "nextCursor").toOption mustBe None

      val staleCursorRequest = FakeRequest(
        GET,
        s"/monthly/$validZReference/$taxYear/$month/results?cursor=${encodeCursor(3)}&limit=$limit"
      )
      val staleCursorResult  = route(app, staleCursorRequest).get
      status(staleCursorResult) mustBe BAD_REQUEST
      (contentAsJson(staleCursorResult) \ "code").as[String] mustBe "INVALID_CURSOR"
    }

    "return 400 BadRequest for an invalid cursor" in {
      val request = FakeRequest(GET, s"/monthly/$validZReference/$taxYear/$month/results?cursor=not-a-cursor")
      val result  = route(app, request).get

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "code").asOpt[String] mustBe Some("INVALID_CURSOR")
    }

    "return 400 BadRequest when limit exceeds 1000" in {
      val request = FakeRequest(GET, s"/monthly/$validZReference/$taxYear/$month/results?limit=1001")
      val result  = route(app, request).get

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "code").asOpt[String] mustBe Some("BAD_REQUEST")
    }

    "return 404 NotFound when no report exists for given identifiers" in {
      await(reportEventRepository.collection.drop().toFuture())
      await(reportIssueRepository.collection.drop().toFuture())
      val request = FakeRequest(GET, s"/monthly/$validZReference/$taxYear/$month/results?limit=10")
      val result  = route(app, request).get

      status(result) mustBe NOT_FOUND
      (contentAsJson(result) \ "code").asOpt[String] mustBe Some("REPORT_NOT_FOUND")
      (contentAsJson(result) \ "message").asOpt[String] mustBe Some("Report not found")
    }

    "return 500 InternalServerError when zReference is Z1500" in {
      val request = FakeRequest(GET, s"/monthly/Z1500/$taxYear/$month/results?limit=10")
      val result  = route(app, request).get

      status(result) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "code").asOpt[String] mustBe Some("INTERNAL_SERVER_ERROR")
      (contentAsJson(result) \ "message").asOpt[String] mustBe Some("Internal issue, try again later")
    }
  }
}
