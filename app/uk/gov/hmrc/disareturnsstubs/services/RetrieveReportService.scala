/*
 * Copyright 2026 HM Revenue & Customs
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

import uk.gov.hmrc.disareturnsstubs.models.ErrorResponse.{pageNotFoundError, reportNotFoundError}
import uk.gov.hmrc.disareturnsstubs.models.{ErrorResponse, ReturnResult, ReturnResultResponse}
import uk.gov.hmrc.disareturnsstubs.repositories.generatereport.{ReportEventRepository, ReportIssueRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveReportService @Inject() (
  reportEventRepository: ReportEventRepository,
  reportIssueRepository: ReportIssueRepository
)(implicit ec: ExecutionContext) {

  def getMonthlyReport(
    zReference: String,
    year: String,
    month: String,
    pageIndex: Int,
    pageSize: Int
  ): Future[Either[ErrorResponse, ReturnResultResponse]] = {

    val skip  = pageIndex * pageSize
    val limit = pageSize

    reportEventRepository.find(zReference, year, month).flatMap {
      case None =>
        Future.successful(Left(reportNotFoundError))

      case Some(event) =>
        for {
          total  <- reportIssueRepository.countByReportId(event.reportId)
          issues <- reportIssueRepository.findByReportId(event.reportId, skip, limit)
        } yield
          if (skip >= total) {
            Left(pageNotFoundError(pageIndex))
          } else {
            val results = issues.map { issue =>
              ReturnResult(
                accountNumber = issue.accountNumber,
                nino = issue.nino,
                issueIdentified = issue.issueIdentified
              )
            }

            Right(
              ReturnResultResponse(
                totalRecords = total.toInt,
                returnResults = results
              )
            )
          }
    }
  }
}
