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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Play.materializer
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.MissingBearerToken
import uk.gov.hmrc.disareturnsstubs.controllers.NpsController
import uk.gov.hmrc.disareturnsstubs.mappers.ErrorMapper.reportNotFoundError
import uk.gov.hmrc.disareturnsstubs.models.{IssueIdentifiedMessage, ReturnResult, ReturnResultResponse}
import uk.gov.hmrc.disareturnsstubs.services.{GenerateReportIssuesService, RetrieveReportService}
import uk.gov.hmrc.disareturnsstubs.services.RetrieveReportService.encodeCursor
import utils.BaseUnitSpec

import scala.concurrent.Future

class NpsControllerSpec extends BaseUnitSpec {

  val mockRetrieveReportService: RetrieveReportService = mock[RetrieveReportService]

  private val controller = new NpsController(
    stubControllerComponents(),
    stubAuthFilter,
    mockRetrieveReportService,
    new GenerateReportIssuesService(),
    mockAuthConnector
  )

  val sampleReturnResult: Seq[ReturnResult] = Seq(
    ReturnResult(
      accountNumber = "100000001",
      nino = "AB123457C",
      issueIdentified = IssueIdentifiedMessage(
        code = "UNABLE_TO_IDENTIFY_INVESTOR",
        message = "Unable to identify investor"
      )
    ),
    ReturnResult(
      accountNumber = "100000002",
      nino = "AB123457C",
      issueIdentified = IssueIdentifiedMessage(
        code = "UNABLE_TO_IDENTIFY_INVESTOR",
        message = "Unable to identify investor"
      )
    ),
    ReturnResult(
      accountNumber = "100000003",
      nino = "AB123457C",
      issueIdentified = IssueIdentifiedMessage(
        code = "UNABLE_TO_IDENTIFY_INVESTOR",
        message = "Unable to identify investor"
      )
    )
  )

  "submitMonthlyReturn" should {
    "return 204 no content for successful submission" in {
      val request =
        FakeRequest(POST, s"/nps/submit/$validZReference")

      val result = controller.submitMonthlyReturn(validZReference)(request)
      status(result) shouldBe NO_CONTENT
    }

    "return 400 Bad Request for ISA Manager Reference Z1400" in {
      val zReference = "Z1400"
      val request    = FakeRequest(POST, s"/nps/submit/$zReference")

      val result = controller.submitMonthlyReturn(zReference)(request)
      status(result)                                 shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String]    shouldBe "BAD_REQUEST"
      (contentAsJson(result) \ "message").as[String] shouldBe "Bad request"
    }

    "return 503 Service Unavailable for ISA Manager Reference Z1503" in {
      val zReference = "Z1503"
      val request    = FakeRequest(POST, s"/nps/submit/$zReference")

      val result = controller.submitMonthlyReturn(zReference)(request)
      status(result)                                 shouldBe SERVICE_UNAVAILABLE
      (contentAsJson(result) \ "code").as[String]    shouldBe "SERVICE_UNAVAILABLE"
      (contentAsJson(result) \ "message").as[String] shouldBe "Service unavailable"
    }
  }

  "send" should {
    "return 204 no content when a declaration has been sent successfully" in {
      val request =
        FakeRequest(POST, s"/nps/declaration/$validZReference")

      val result = controller.send(validZReference)(request)
      status(result) shouldBe NO_CONTENT
    }

    "return 500 Internal Server Error for ISA Manager Reference Z1500" in {
      val zReference = "Z1500"
      val request    = FakeRequest(POST, s"/nps/declaration/$zReference")

      val result = controller.send(zReference)(request)
      status(result)                                 shouldBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "code").as[String]    shouldBe "INTERNAL_SERVER_ERROR"
      (contentAsJson(result) \ "message").as[String] shouldBe "Internal issue, try again later"
    }
  }

  "getMonthlyReport" should {

    val limit = 10

    "return 200 OK with returnResults when report exists" in {
      when(
        mockRetrieveReportService.getMonthlyReport(any(), any(), any(), any(), any())
      ).thenReturn(Future.successful(Right(ReturnResultResponse(returnResults = sampleReturnResult))))

      val request = FakeRequest(GET, s"/nps/monthly/$validZReference/2025-26/APR/results")
      val result  = controller.getMonthlyReport(validZReference, "2025-26", "APR", None, limit)(request)

      status(result) shouldBe OK
      val jsonBody = contentAsJson(result)
      (jsonBody \\ "accountNumber").map(_.as[String]) should contain("100000001")
      (jsonBody \ "totalRecords").toOption          shouldBe None
      (jsonBody \ "nextCursor").asOpt[String]       shouldBe None
    }

    "return 200 OK with a raw cursor across multiple pages" in {
      val nextCursor = encodeCursor(2)
      when(
        mockRetrieveReportService.getMonthlyReport(any(), any(), any(), ArgumentMatchers.eq(0), ArgumentMatchers.eq(2))
      )
        .thenReturn(
          Future.successful(
            Right(ReturnResultResponse(sampleReturnResult.take(2), nextCursor = Some(nextCursor)))
          )
        )

      when(
        mockRetrieveReportService.getMonthlyReport(any(), any(), any(), ArgumentMatchers.eq(2), ArgumentMatchers.eq(2))
      )
        .thenReturn(
          Future.successful(Right(ReturnResultResponse(returnResults = sampleReturnResult.drop(2))))
        )
      val request = FakeRequest(GET, s"/nps/monthly/$validZReference/2025-26/APR/results")

      val firstResult  = controller.getMonthlyReport(validZReference, "2025-26", "APR", None, 2)(request)
      val secondResult = controller.getMonthlyReport(validZReference, "2025-26", "APR", Some(nextCursor), 2)(request)

      val firstJson  = contentAsJson(firstResult)
      val secondJson = contentAsJson(secondResult)

      (firstJson \ "returnResults").as[Seq[ReturnResult]].size  shouldBe 2
      (firstJson \ "totalRecords").toOption                     shouldBe None
      (firstJson \ "nextCursor").as[String]                     shouldBe nextCursor
      (secondJson \ "returnResults").as[Seq[ReturnResult]].size shouldBe 1
      (secondJson \ "totalRecords").toOption                    shouldBe None
      (secondJson \ "nextCursor").asOpt[String]                 shouldBe None
    }

    "return 400 BadRequest for an invalid cursor" in {
      val request = FakeRequest(GET, s"/nps/monthly/$validZReference/2025-26/APR/results")
      val result  = controller.getMonthlyReport(validZReference, "2025-26", "APR", Some("not-a-cursor"), limit)(request)

      status(result)                                 shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").asOpt[String] shouldBe Some("INVALID_CURSOR")
    }

    "return 400 BadRequest when limit is outside the supported range" in {
      val request = FakeRequest(GET, s"/nps/monthly/$validZReference/2025-26/APR/results")

      status(controller.getMonthlyReport(validZReference, "2025-26", "APR", None, 0)(request))    shouldBe BAD_REQUEST
      status(controller.getMonthlyReport(validZReference, "2025-26", "APR", None, 1001)(request)) shouldBe BAD_REQUEST
    }

    "return 404 NotFound when no report exists" in {
      when(
        mockRetrieveReportService.getMonthlyReport(any(), any(), any(), any(), any())
      ).thenReturn(Future.successful(Left(reportNotFoundError)))

      val request = FakeRequest(GET, s"/nps/monthly/$validZReference/2025-26/APR/results")
      val result  = controller.getMonthlyReport(validZReference, "2025-26", "APR", None, limit)(request)

      status(result)                                 shouldBe NOT_FOUND
      (contentAsJson(result) \ "code").asOpt[String] shouldBe Some("REPORT_NOT_FOUND")
    }

    "return 500 InternalServerError when zReference is Z1500" in {
      val request = FakeRequest(GET, s"/nps/monthly/Z1500/2025-26/APR/results")
      val result  = controller.getMonthlyReport("Z1500", "2025-26", "APR", None, limit)(request)

      status(result)                                    shouldBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "code").asOpt[String]    shouldBe Some("INTERNAL_SERVER_ERROR")
      (contentAsJson(result) \ "message").asOpt[String] shouldBe Some("Internal issue, try again later")
    }

    "return a generated report without calling RetrieveReportService when authorised with a perf-test credId" in {
      authorisedUser(Some(s"disa-returns-perf-test-$validZReference"))

      val request = FakeRequest(GET, s"/nps/monthly/$validZReference/2025-26/APR/results")
      val result  = controller.getMonthlyReport(validZReference, "2025-26", "APR", None, limit)(request)

      status(result) shouldBe OK
      val jsonBody          = contentAsJson(result)
      val returnResultsSize = (jsonBody \ "returnResults").as[Seq[ReturnResult]].size
      returnResultsSize                       should (be > 0 and be <= limit)
      (jsonBody \ "totalRecords").toOption  shouldBe None
      (jsonBody \ "nextCursor").asOpt[String] should not be empty
    }

    "fall back to Mongo-backed lookup when auth fails to resolve credentials" in {
      when(
        mockAuthConnector.authorise(any(), any())(any(), any())
      ).thenReturn(Future.failed(MissingBearerToken()))

      when(
        mockRetrieveReportService.getMonthlyReport(any(), any(), any(), any(), any())
      ).thenReturn(Future.successful(Right(ReturnResultResponse(returnResults = sampleReturnResult))))

      val request = FakeRequest(GET, s"/nps/monthly/$validZReference/2025-26/APR/results")
      val result  = controller.getMonthlyReport(validZReference, "2025-26", "APR", None, limit)(request)

      status(result) shouldBe OK
      val jsonBody = contentAsJson(result)
      (jsonBody \\ "accountNumber").map(_.as[String]) should contain("100000001")
    }

    "return 500 InternalServerError when RetrieveReportService fails unexpectedly" in {
      when(
        mockRetrieveReportService.getMonthlyReport(any(), any(), any(), any(), any())
      ).thenReturn(Future.failed(new RuntimeException("Mongo unavailable")))

      val request = FakeRequest(GET, s"/nps/monthly/$validZReference/2025-26/APR/results")
      val result  = controller.getMonthlyReport(validZReference, "2025-26", "APR", None, limit)(request)

      status(result)                                    shouldBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "code").asOpt[String]    shouldBe Some("INTERNAL_SERVER_ERROR")
      (contentAsJson(result) \ "message").asOpt[String] shouldBe Some("Failed with exception: Mongo unavailable")
    }
  }
}
