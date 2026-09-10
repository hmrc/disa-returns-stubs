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

import uk.gov.hmrc.disareturnsstubs.mappers.ErrorMapper.{invalidCursorError, reportNotFoundError}
import uk.gov.hmrc.disareturnsstubs.models.{ErrorResponse, ReturnResult, ReturnResultResponse}
import uk.gov.hmrc.disareturnsstubs.repositories.generatereport.{ReportEventRepository, ReportIssueRepository}
import uk.gov.hmrc.disareturnsstubs.services.RetrieveReportService.encodeCursor

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class RetrieveReportService @Inject() (
  reportEventRepository: ReportEventRepository,
  reportIssueRepository: ReportIssueRepository
)(implicit ec: ExecutionContext) {

  def getMonthlyReport(
    zReference: String,
    year: String,
    month: String,
    offset: Int,
    limit: Int
  ): Future[Either[ErrorResponse, ReturnResultResponse]] =
    reportEventRepository.find(zReference).flatMap {
      case None =>
        Future.successful(Left(reportNotFoundError))

      case Some(event) =>
        reportIssueRepository.countByReportId(event.reportId).flatMap { total =>
          if (offset > 0 && offset >= total) Future.successful(Left(invalidCursorError))
          else
            reportIssueRepository.findByReportId(event.reportId, offset, limit).map { issues =>
              val results = issues.map { issue =>
                ReturnResult(
                  accountNumber = issue.accountNumber,
                  nino = issue.nino,
                  issueIdentified = issue.issueIdentified
                )
              }

              Right(
                ReturnResultResponse(
                  returnResults = results,
                  nextCursor = Option.when(offset + results.size < total)(encodeCursor(offset + results.size))
                )
              )
            }
        }
    }
}

object RetrieveReportService {
  private val cursorPrefix = "offset:"

  def encodeCursor(offset: Int): String =
    Base64.getUrlEncoder.withoutPadding.encodeToString(
      s"$cursorPrefix$offset".getBytes(StandardCharsets.UTF_8)
    )

  def decodeCursor(cursor: String): Option[Int] =
    Try {
      val decoded = new String(
        Base64.getUrlDecoder.decode(cursor),
        StandardCharsets.UTF_8
      )
      if (decoded.startsWith(cursorPrefix)) decoded.drop(cursorPrefix.length).toInt else -1
    }.toOption
      .filter(_ >= 0)
}
