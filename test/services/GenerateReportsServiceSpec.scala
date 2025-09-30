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

package services

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.disareturnsstubs.models._
import uk.gov.hmrc.disareturnsstubs.repositories.ReportRepository
import org.mongodb.scala.result.UpdateResult
import uk.gov.hmrc.disareturnsstubs.services.GenerateReportsService
import utils.BaseUnitSpec

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class GenerateReportsServiceSpec extends BaseUnitSpec {

  "GenerateReportsService" should {

    "generate the correct number of ReturnResults and store the MonthlyReport" in {
      val mockRepo = mock[ReportRepository]
      val service  = new GenerateReportsService(mockRepo)(ec)

      val request       = GenerateReportRequest(oversubscribed = 2, traceAndMatch = 1, failedEligibility = 1)
      val isaManagerRef = "Z1234"
      val year          = "2025-26"
      val month         = "JAN"

      when(mockRepo.insertReport(any[MonthlyReport]))
        .thenReturn(Future.successful(mock[UpdateResult]))

      val futureResults = service.generateAndStore(request, isaManagerRef, year, month)

      whenReady(futureResults) { results =>
        results.length shouldBe 4
        results.foreach { returnResult =>
          returnResult.accountNumber should startWith("100")
          returnResult.nino          should startWith("AB")
        }
      }

      val captor: ArgumentCaptor[MonthlyReport] = ArgumentCaptor.forClass(classOf[MonthlyReport])

      verify(mockRepo).insertReport(captor.capture())

      val capturedReport = captor.getValue

      capturedReport.isaManagerReferenceNumber shouldBe isaManagerRef
      capturedReport.year                      shouldBe year
      capturedReport.month                     shouldBe month
      capturedReport.returnResults.length      shouldBe 4
    }

    "generate different types of ReturnResults correctly" in {
      val mockRepo = mock[ReportRepository]
      val service  = new GenerateReportsService(mockRepo)(ec)

      val request       = GenerateReportRequest(oversubscribed = 0, traceAndMatch = 1, failedEligibility = 1)
      val isaManagerRef = "Z456"
      val year          = "2025"
      val month         = "02"

      when(mockRepo.insertReport(any[MonthlyReport]))
        .thenReturn(Future.successful(mock[UpdateResult]))

      val futureResults = service.generateAndStore(request, isaManagerRef, year, month)

      whenReady(futureResults) { results =>
        results.count(_.issueIdentified.isInstanceOf[IssueIdentifiedOverSubscribed]) shouldBe 0
        results.count(_.issueIdentified.isInstanceOf[IssueIdentifiedMessage])        shouldBe 2
      }
    }
  }
}
