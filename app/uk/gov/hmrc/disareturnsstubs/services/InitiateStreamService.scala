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

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Framing, Sink}
import org.apache.pekko.util.ByteString
import uk.gov.hmrc.disareturnsstubs.connectors.TestStorageConnector
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InitiateStreamService @Inject()(
                                  storageConnector: TestStorageConnector
                                )(implicit ec: ExecutionContext, mat: Materializer) {

  def toStorage(records: Long, payloadSize: Int)(implicit hc: HeaderCarrier): Future[String] = {
    val startNs = System.nanoTime()

    storageConnector.toStorage(
      records = records,
      payloadSize = payloadSize
    ).map { resp =>
      val durMs = (System.nanoTime() - startNs) / 1e6
      s"\nstatus=${resp.status} durationMs=${durMs.toLong}\nbody=${resp.body}\n"
    }
  }

  def fromStorage(batchId: String)
               (implicit hc: HeaderCarrier): Future[String] = {

    val start = System.nanoTime()

    storageConnector
      .fromStorage(batchId)
      .flatMap(_
      .via(
        Framing.delimiter(
          ByteString("\n"),
          maximumFrameLength = 1024 * 1024,
          allowTruncation = true
        )
      )
      .runWith(Sink.fold(0L)((acc, _) => acc + 1))
      .map { count =>
        val durationMs = (System.nanoTime() - start) / 1e6
        s"exportedRecords=$count durationMs=${durationMs.toLong} recordsPerSec=${(count * 1000.0 / durationMs).toInt}\n"
      })
  }
}
