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

package uk.gov.hmrc.disareturnsstubs.mappers

import uk.gov.hmrc.disareturnsstubs.models.ErrorResponse

object ErrorMapper {
  val badRequestError: ErrorResponse         = ErrorResponse("BAD_REQUEST", "Bad request")
  val serviceUnavailableError: ErrorResponse = ErrorResponse("SERVICE_UNAVAILABLE", "Service unavailable")
  val reportNotFoundError: ErrorResponse     = ErrorResponse("REPORT_NOT_FOUND", "Report not found")

  def issueLimitExceeded(limit: Int): ErrorResponse =
    ErrorResponse(
      code = "ISSUE_LIMIT_EXCEEDED",
      message =
        s"The maximum number of records that can be generated in a single request is $limit. Please reduce the number of requested records and try again."
    )

  def pageNotFoundError(pageIndex: Int): ErrorResponse = ErrorResponse("PAGE_NOT_FOUND", s"No page $pageIndex found")

  def internalServerErr(message: String): ErrorResponse = ErrorResponse("INTERNAL_SERVER_ERROR", message)
}
