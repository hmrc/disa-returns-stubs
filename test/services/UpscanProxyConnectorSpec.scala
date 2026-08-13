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

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import play.api.libs.Files
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.http.Status.{BAD_GATEWAY, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json
import play.api.mvc.MultipartFormData
import uk.gov.hmrc.disareturnsstubs.config.AppConfig
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}
import utils.BaseUnitSpec

import scala.concurrent.Future

class UpscanProxyConnectorSpec extends BaseUnitSpec {

  trait TestSetup {

    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val mockRequest: RequestBuilder  = mock[RequestBuilder]
    val mockResponse: HttpResponse   = mock[HttpResponse]
    val mockAppConfig: AppConfig     = mock[AppConfig]
    val retryConfig: Config          =
      ConfigFactory.parseString("http-verbs.retries.intervals = [1 millisecond, 1 millisecond, 1 millisecond]")

    val baseUrl         = "http://localhost:9570"
    val callbackBaseUrl = "http://localhost:6063"
    val frontendBaseUrl = "http://localhost:1205"

    when(mockAppConfig.upscanStubBase).thenReturn(baseUrl)

    val connector: UpscanProxyConnector =
      new UpscanProxyConnector(mockHttpClient, mockAppConfig, retryConfig, inject[ActorSystem])

    when(mockHttpClient.post(any())(any())).thenReturn(mockRequest)
    when(mockRequest.transform(any())).thenReturn(mockRequest)
    when(mockRequest.withBody(any())(any(), any(), any())).thenReturn(mockRequest)

    def stubServerError(status: Int): Unit = {
      when(mockResponse.status).thenReturn(status)
      when(mockResponse.body).thenReturn("upscan unavailable")
      when(mockRequest.execute[HttpResponse](any(), any())).thenReturn(Future.successful(mockResponse))
    }

    def verifyRetries(result: Future[HttpResponse], status: Int): Unit = {
      result.failed.futureValue shouldBe UpstreamErrorResponse("upscan unavailable", status)
      verify(mockRequest, times(4)).execute[HttpResponse](any(), any())
    }
  }

  "initiate" should {

    "POST the JSON body to upscan-stub and return the response" in new TestSetup {

      when(mockRequest.execute[HttpResponse](any(), any())).thenReturn(Future.successful(mockResponse))

      connector.initiate(Json.obj("key" -> "value")).futureValue shouldBe mockResponse

      verify(mockHttpClient).post(url"$baseUrl/upscan/v2/initiate")
    }

    "propagate failures from the HTTP client" in new TestSetup {

      when(mockRequest.execute[HttpResponse](any(), any()))
        .thenReturn(Future.failed(new RuntimeException("downstream failure")))

      connector.initiate(Json.obj()).failed.futureValue.getMessage should include("downstream failure")
    }

    "retry server errors" in new TestSetup {

      stubServerError(INTERNAL_SERVER_ERROR)

      verifyRetries(connector.initiate(Json.obj()), INTERNAL_SERVER_ERROR)
    }
  }

  "upload" should {

    "POST a multipart request with the file to upscan-stub" in new TestSetup {

      val filePart = MultipartFormData.FilePart[Files.TemporaryFile](
        key = "file",
        filename = "test.txt",
        contentType = Some("text/plain"),
        ref = SingletonTemporaryFileCreator.create("test", ".txt")
      )

      when(mockRequest.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mockResponse))

      connector.upload(Some(filePart), Map("key" -> Seq("value1"))).futureValue shouldBe mockResponse

      verify(mockHttpClient).post(url"$baseUrl/upscan/upload")
      verify(mockRequest).transform(any())
    }

    "POST a multipart request without a file to upscan-stub" in new TestSetup {

      when(mockRequest.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mockResponse))

      connector
        .upload(None, Map("error_action_redirect" -> Seq(s"$frontendBaseUrl/error")))
        .futureValue shouldBe mockResponse

      verify(mockHttpClient).post(url"$baseUrl/upscan/upload")
      verify(mockRequest).transform(any())
    }

    "propagate failures from the HTTP client" in new TestSetup {

      when(mockRequest.execute[HttpResponse](any(), any()))
        .thenReturn(Future.failed(new RuntimeException("upstream error")))

      connector.upload(None, Map.empty).failed.futureValue.getMessage should include("upstream error")
    }

    Seq(INTERNAL_SERVER_ERROR, BAD_GATEWAY, SERVICE_UNAVAILABLE).foreach { status =>
      s"retry server error $status" in new TestSetup {

        stubServerError(status)

        verifyRetries(connector.upload(None, Map.empty), status)
      }
    }

    "not retry statuses outside the 5xx range" in new TestSetup {

      val nonHttpStatus = 600
      when(mockResponse.status).thenReturn(nonHttpStatus)
      when(mockRequest.execute[HttpResponse](any(), any())).thenReturn(Future.successful(mockResponse))

      connector.upload(None, Map.empty).futureValue shouldBe mockResponse

      verify(mockRequest).execute[HttpResponse](any(), any())
    }
  }

  "sendCallback" should {

    "POST the JSON body to the supplied callback URL" in new TestSetup {

      val callbackUrl = s"$callbackBaseUrl/disa-returns-backend/upscan-callback"
      val body        = Json.obj("reference" -> "ref-123", "fileStatus" -> "FAILED")

      when(mockRequest.execute[HttpResponse](any(), any())).thenReturn(Future.successful(mockResponse))

      connector.sendCallback(callbackUrl, body).futureValue shouldBe mockResponse

      verify(mockHttpClient).post(url"$callbackUrl")
    }

    "propagate failures from the HTTP client" in new TestSetup {

      when(mockRequest.execute[HttpResponse](any(), any()))
        .thenReturn(Future.failed(new RuntimeException("callback failure")))

      connector.sendCallback(s"$callbackBaseUrl/callback", Json.obj()).failed.futureValue.getMessage should
        include("callback failure")
    }

    "retry server errors" in new TestSetup {

      stubServerError(SERVICE_UNAVAILABLE)

      verifyRetries(
        connector.sendCallback(s"$callbackBaseUrl/callback", Json.obj()),
        SERVICE_UNAVAILABLE
      )
    }
  }
}
