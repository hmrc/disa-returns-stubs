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

import uk.gov.hmrc.disareturnsstubs.models._
import uk.gov.hmrc.disareturnsstubs.models.generatereport.GenerateReportRequest
import uk.gov.hmrc.disareturnsstubs.services.GenerateReportIssuesService
import utils.BaseUnitSpec

class GenerateReportIssuesServiceSpec extends BaseUnitSpec {

  val service = new GenerateReportIssuesService

  "generateResults" should {

    "generate the correct number of ReturnResults" in {

      val request = GenerateReportRequest(
        oversubscribed = 2,
        traceAndMatch = 1,
        failedEligibility = 1
      )

      val results = service.generateResults(request)

      results.length shouldBe 4

      results.foreach { result =>
        result.accountNumber should startWith("100")
        result.nino          should startWith("AB")
      }
    }

    "generate the correct number of each issue type" in {

      val request = GenerateReportRequest(
        oversubscribed = 3,
        traceAndMatch = 2,
        failedEligibility = 1
      )

      val results = service.generateResults(request)

      results.count(_.issueIdentified.isInstanceOf[IssueIdentifiedOverSubscribed]) shouldBe 3

      results.count {
        case r: ReturnResult
            if r.issueIdentified.isInstanceOf[IssueIdentifiedMessage] &&
              r.issueIdentified.asInstanceOf[IssueIdentifiedMessage].code == "UNABLE_TO_IDENTIFY_INVESTOR" =>
          true
        case _ => false
      } shouldBe 2

      results.count {
        case r: ReturnResult
            if r.issueIdentified.isInstanceOf[IssueIdentifiedMessage] &&
              r.issueIdentified.asInstanceOf[IssueIdentifiedMessage].code == "FAILED_ELIGIBILITY" =>
          true
        case _ => false
      } shouldBe 1
    }

    "generate empty results when all counts are zero" in {

      val request = GenerateReportRequest(
        oversubscribed = 0,
        traceAndMatch = 0,
        failedEligibility = 0
      )

      val results = service.generateResults(request)

      results shouldBe empty
    }

    "generate correctly formatted account numbers and NINOs" in {

      val request = GenerateReportRequest(
        oversubscribed = 5,
        traceAndMatch = 0,
        failedEligibility = 0
      )

      val results = service.generateResults(request)

      results.foreach { result =>
        result.accountNumber should fullyMatch regex "100\\d{6}"
        result.nino          should fullyMatch regex "AB\\d{6}C"
      }
    }
  }
}
