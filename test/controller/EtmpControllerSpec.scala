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
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.disareturnsstubs.controllers.EtmpController
import uk.gov.hmrc.disareturnsstubs.models.{EtmpObligations, EtmpReportingWindow}
import uk.gov.hmrc.disareturnsstubs.repositories.{ObligationStatusRepository, ReportingWindowRepository}
import utils.BaseUnitSpec

import scala.concurrent.Future

class EtmpControllerSpec extends BaseUnitSpec {

  private val mockReportingWindowRepo  = mock[ReportingWindowRepository]
  private val mockObligationRepo       = mock[ObligationStatusRepository]
  private val cc: ControllerComponents = stubControllerComponents()
  private val controller               =
    new EtmpController(cc, mockReportingWindowRepo, mockObligationRepo)

  "checkReportingWindowStatus" should {
    "return 200 with reportingWindowOpen = true when state exists" in {
      when(mockReportingWindowRepo.getReportingWindowState)
        .thenReturn(Future.successful(Some(true)))

      val result: Future[Result] = controller.checkReportingWindowStatus()(FakeRequest())
      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(EtmpReportingWindow(reportingWindowOpen = true))
    }

    "return 404 when no reporting window state found" in {
      when(mockReportingWindowRepo.getReportingWindowState)
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.checkReportingWindowStatus()(FakeRequest())
      status(result)                             shouldBe NOT_FOUND
      (contentAsJson(result) \ "error").as[String] should include("No reporting window state found")
    }
  }

  "closeObligationStatus" should {
    "return 204 no content when closeObligationStatus is successful" in {
      when(mockObligationRepo.closeObligationStatus(any()))
        .thenReturn(Future.unit)

      val result = controller.declare(validZReference)(FakeRequest())
      status(result) shouldBe NO_CONTENT
    }
  }

  "checkReturnsObligationStatus" should {
    "return 200 with obligationAlreadyMet = true if repo returns Some(true)" in {
      when(mockObligationRepo.getObligationStatus(any()))
        .thenReturn(Future.successful(Some(true)))

      val result = controller.checkReturnsObligationStatus(validZReference)(FakeRequest())
      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(EtmpObligations(obligationAlreadyMet = true))
    }

    "return 200 with obligationAlreadyMet = false if repo returns Some(false)" in {
      when(mockObligationRepo.getObligationStatus(any()))
        .thenReturn(Future.successful(Some(false)))

      val result = controller.checkReturnsObligationStatus(validZReference)(FakeRequest())
      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(EtmpObligations(obligationAlreadyMet = false))
    }

    "return 200 with obligationAlreadyMet = false when no existing data is found and new data is created" in {
      when(mockObligationRepo.getObligationStatus(any()))
        .thenReturn(Future.successful(None))
      when(mockObligationRepo.openObligationStatus(any()))
        .thenReturn(Future.successful())

      val result = controller.checkReturnsObligationStatus(validZReference)(FakeRequest())
      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(EtmpObligations(obligationAlreadyMet = false))
    }
  }
}
