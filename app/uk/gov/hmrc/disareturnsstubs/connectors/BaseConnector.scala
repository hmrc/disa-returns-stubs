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

package uk.gov.hmrc.disareturnsstubs.connectors

import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.RequestBuilder
import uk.gov.hmrc.http.{HttpResponse, Retries, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

trait BaseConnector extends Retries {

  private val retryStatusCodes: Seq[Int] = INTERNAL_SERVER_ERROR to 599

  extension (requestBuilder: RequestBuilder)
    // Raw responses do not fail on 5xx under readRaw, so we need to convert them into errors that retryFor can handle
    protected def executeWithRetryOnServerError(implicit ec: ExecutionContext): Future[HttpResponse] =
      requestBuilder.execute[HttpResponse].flatMap(_.retryOnServerError)

  extension (response: HttpResponse)
    private def retryOnServerError: Future[HttpResponse] =
      if (retryStatusCodes.contains(response.status)) {
        Future.failed(UpstreamErrorResponse(response.body, response.status))
      } else {
        Future.successful(response)
      }

  protected def retryCondition: PartialFunction[Exception, Boolean] = {
    case UpstreamErrorResponse.Upstream5xxResponse(_) => true
  }
}
