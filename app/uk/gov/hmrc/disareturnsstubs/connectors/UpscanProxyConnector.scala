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

import com.google.inject.Inject
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.libs.Files
import play.api.libs.json.JsValue
import play.api.libs.ws.WSBodyWritables._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.MultipartFormData
import uk.gov.hmrc.disareturnsstubs.config.AppConfig

import java.nio.file.{Files => NioFiles}
import scala.concurrent.{ExecutionContext, Future}


class UpscanProxyConnector @Inject()(
                                      ws: WSClient,
                                      appConfig: AppConfig
                                    )(implicit ec: ExecutionContext) {

  private val baseUrl = appConfig.upscanStubBase


  def initiate(body: JsValue): Future[WSResponse] =
    ws.url(s"$baseUrl/upscan/v2/initiate")
      .addHttpHeaders("Content-Type" -> "application/json")
      .post(body)


  def upload(
              file: Option[MultipartFormData.FilePart[Files.TemporaryFile]],
              dataParts: Map[String, Seq[String]]
            ): Future[WSResponse] = {

    val formFields: Seq[MultipartFormData.Part[Source[ByteString, _]]] =
      dataParts.toSeq.flatMap { case (k, values) =>
        values.map(MultipartFormData.DataPart(k, _))
      }

    val filePart: Seq[MultipartFormData.Part[Source[ByteString, _]]] =
      file.toSeq.map { f =>
        MultipartFormData.FilePart(
          key         = "file",
          filename    = f.filename,
          contentType = f.contentType,
          ref         = Source.single(ByteString(NioFiles.readAllBytes(f.ref.path)))
        )
      }

    ws.url(s"$baseUrl/upscan/upload")
      .withFollowRedirects(false)
      .post(Source(formFields ++ filePart))
  }
}