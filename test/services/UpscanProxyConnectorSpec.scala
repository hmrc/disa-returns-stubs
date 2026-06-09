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

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import play.api.libs.Files
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.MultipartFormData
import uk.gov.hmrc.disareturnsstubs.config.AppConfig
import utils.BaseUnitSpec

import scala.concurrent.Future

class UpscanProxyConnectorSpec extends BaseUnitSpec {

  trait TestSetup {

    val mockWsClient:  WSClient   = mock[WSClient]
    val mockRequest:   WSRequest  = mock[WSRequest]
    val mockResponse:  WSResponse = mock[WSResponse]
    val mockAppConfig: AppConfig  = mock[AppConfig]

    val baseUrl = "http://localhost:9570"

    when(mockAppConfig.upscanStubBase).thenReturn(baseUrl)

    val connector: UpscanProxyConnector =
      new UpscanProxyConnector(ws = mockWsClient, appConfig = mockAppConfig)

    when(mockWsClient.url(any())).thenReturn(mockRequest)
    when(mockRequest.withFollowRedirects(any[Boolean]())).thenReturn(mockRequest)
  }

  "initiate" should {

    "POST the JSON body to upscan-stub and return the response" in new TestSetup {

      when(mockRequest.addHttpHeaders(any())).thenReturn(mockRequest)
      when(mockRequest.post(any[JsValue]())(any())).thenReturn(Future.successful(mockResponse))

      connector.initiate(Json.obj("key" -> "value")).futureValue shouldBe mockResponse

      verify(mockWsClient).url(s"$baseUrl/upscan/v2/initiate")
    }

    "propagate failures from the WS client" in new TestSetup {

      when(mockRequest.addHttpHeaders(any())).thenReturn(mockRequest)
      when(mockRequest.post(any[JsValue]())(any()))
        .thenReturn(Future.failed(new RuntimeException("downstream failure")))

      connector.initiate(Json.obj()).failed.futureValue.getMessage should include("downstream failure")
    }
  }

  "upload" should {

    "POST a multipart request with the file to upscan-stub" in new TestSetup {

      val filePart = MultipartFormData.FilePart[Files.TemporaryFile](
        key         = "file",
        filename    = "test.txt",
        contentType = Some("text/plain"),
        ref         = SingletonTemporaryFileCreator.create("test", ".txt")
      )

      when(mockRequest.post(any[Source[MultipartFormData.Part[Source[ByteString, _]], _]]()))
        .thenReturn(Future.successful(mockResponse))

      connector.upload(Some(filePart), Map("key" -> Seq("value1"))).futureValue shouldBe mockResponse

      verify(mockWsClient).url(s"$baseUrl/upscan/upload")
      verify(mockRequest).withFollowRedirects(false)
    }

    "POST a multipart request without a file to upscan-stub" in new TestSetup {

      when(mockRequest.post(any[Source[MultipartFormData.Part[Source[ByteString, _]], _]]()))
        .thenReturn(Future.successful(mockResponse))

      connector.upload(None, Map("error_action_redirect" -> Seq("http://localhost:1205/error"))).futureValue shouldBe mockResponse

      verify(mockWsClient).url(s"$baseUrl/upscan/upload")
      verify(mockRequest).withFollowRedirects(false)
    }

    "propagate failures from the WS client" in new TestSetup {

      when(mockRequest.post(any[Source[MultipartFormData.Part[Source[ByteString, _]], _]]()))
        .thenReturn(Future.failed(new RuntimeException("upstream error")))

      connector.upload(None, Map.empty).failed.futureValue.getMessage should include("upstream error")
    }
  }
}