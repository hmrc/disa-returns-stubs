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

package uk.gov.hmrc.disareturnsstubs.services

import uk.gov.hmrc.disareturnsstubs.models._
import uk.gov.hmrc.disareturnsstubs.repositories.ReportRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode
import scala.util.Random

@Singleton
class GenerateReportsService @Inject() (monthlyReportsRepository: ReportRepository)(implicit ec: ExecutionContext) {

  def generateAndStore(
    generateReportRequest: GenerateReportRequest,
    isaManagerReferenceNumber: String,
    year: String,
    month: String
  ): Future[Seq[ReturnResult]] = {

    val results: Seq[ReturnResult] = generateResults(generateReportRequest)

    val monthlyReport = MonthlyReport(
      isaManagerReferenceNumber = isaManagerReferenceNumber,
      year = year,
      month = month,
      returnResults = results
    )

    monthlyReportsRepository
      .insertReport(monthlyReport)
      .map(_ => results)
  }

  private def generateResults(generateReportRequest: GenerateReportRequest): Seq[ReturnResult] = {

    val oversubscribedResults =
      (0 until generateReportRequest.oversubscribed).map { _ =>
        ReturnResult(
          accountNumber = randomAccountNumber,
          nino = randomNino,
          issueIdentified = IssueIdentifiedOverSubscribed(
            code = "OVER_SUBSCRIBED",
            overSubscribedAmount = randomBigDecimal
          )
        )
      }

    val traceAndMatchResults =
      (0 until generateReportRequest.traceAndMatch).map { _ =>
        ReturnResult(
          accountNumber = randomAccountNumber,
          nino = randomNino,
          issueIdentified = IssueIdentifiedMessage(
            code = "UNABLE_TO_IDENTIFY_INVESTOR",
            message = "Unable To Identify Investor"
          )
        )
      }

    val failedEligibilityResults =
      (0 until generateReportRequest.failedEligibility).map { _ =>
        ReturnResult(
          accountNumber = randomAccountNumber,
          nino = randomNino,
          issueIdentified = IssueIdentifiedMessage(
            code = "FAILED_ELIGIBILITY",
            message = "Failed Eligibility"
          )
        )
      }

    oversubscribedResults ++ traceAndMatchResults ++ failedEligibilityResults
  }

  def randomSixDigit: Int          = 100000 + Random.nextInt(899999)
  def randomBigDecimal: BigDecimal =
    BigDecimal(0.01 + Random.nextDouble() * 9999.99)
      .setScale(2, RoundingMode.HALF_UP)
  def randomNino: String           = s"AB${randomSixDigit}C"
  def randomAccountNumber: String  = s"100$randomSixDigit"

}
