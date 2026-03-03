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

import org.apache.pekko.stream.{Materializer, OverflowStrategy}
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import uk.gov.hmrc.disareturnsstubs.utils.NdJsonGenerator
import uk.gov.hmrc.http.ErrorTimeout.errorTimeout
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestStorageConnector @Inject()(
                                      httpClientV2: HttpClientV2
                                    )(implicit ec: ExecutionContext, materializer: Materializer) {

  private val baseUrl: String = "http://localhost:1208"

  def toStorage(
                     records: Long,
                     payloadSize: Int
                   )(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val url = url"$baseUrl/submissions/records"

    val bodyStream: Source[ByteString, _] =
      NdJsonGenerator.generate(records, payloadSize).buffer(16, OverflowStrategy.backpressure)

    httpClientV2
      .post(url)
      .setHeader("Content-Type" -> "application/x-ndjson")
      .withBody(bodyStream)
      .transform(_.withRequestTimeout(10.minutes))
      .execute[HttpResponse]
  }

  def fromStorage(batchId: String)
                  (implicit hc: HeaderCarrier)
  : Future[Source[ByteString, _]] = {

    val url = url"$baseUrl/submissions/$batchId/records"

    httpClientV2
      .get(url)
      .setHeader("Accept" -> "application/x-ndjson")
      .stream[Source[ByteString, _]]
  }
}
