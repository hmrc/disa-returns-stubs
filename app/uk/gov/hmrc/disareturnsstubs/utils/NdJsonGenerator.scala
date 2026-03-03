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

package uk.gov.hmrc.disareturnsstubs.utils

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString

import java.nio.charset.StandardCharsets

object NdJsonGenerator {

  def generate(targetRecords: Long, requestSizeMB: Int): Source[ByteString, _] = {

    val maxBytes = requestSizeMB.toLong * 1024 * 1024

    // estimate payload size from target distribution
    val approxBytesPerRecord = maxBytes / targetRecords

    val jsonOverhead = """{"id":1,"payload":""}""".length + 2 // quotes + newline
    val payloadSize = Math.max(1, (approxBytesPerRecord - jsonOverhead).toInt)

    val payload = "x" * payloadSize

    Source.unfold((1L, 0L)) { case (id, bytesSoFar) =>

      if (id > targetRecords) None
      else {

        val line =
          ByteString(s"""{"id":$id,"payload":"$payload"}\n""", StandardCharsets.UTF_8)

        val nextTotal = bytesSoFar + line.size

        if (nextTotal > maxBytes)
          None
        else
          Some(((id + 1, nextTotal), line))
      }
    }
  }
}
