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

package controller.upscan

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import play.api.libs.Files
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc.MultipartFormData
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.disareturnsstubs.connectors.UpscanProxyConnector
import uk.gov.hmrc.disareturnsstubs.controllers.upscan.UpscanController
import utils.BaseUnitSpec

import scala.concurrent.Future

class UpscanControllerSpec extends BaseUnitSpec {

  trait TestSetup {

    val mockConnector: UpscanProxyConnector = mock[UpscanProxyConnector]
    val controller: UpscanController        = new UpscanController(stubControllerComponents(), mockConnector)

    val errorRedirectUrl = "http://localhost:1205/obligations/returns/isa/upscan/error"

    val defaultDataParts: Map[String, Seq[String]] =
      Map("error_action_redirect" -> Seq(errorRedirectUrl))

    def multipartBody(
      filename: Option[String],
      contentType: Option[String] = Some("text/csv"),
      extraDataParts: Map[String, Seq[String]] = Map.empty
    ): MultipartFormData[Files.TemporaryFile] = {
      val files = filename.toSeq.map { name =>
        MultipartFormData.FilePart[Files.TemporaryFile](
          key = "file",
          filename = name,
          contentType = contentType,
          ref = SingletonTemporaryFileCreator.create("test", ".tmp")
        )
      }
      MultipartFormData(dataParts = defaultDataParts ++ extraDataParts, files = files, badParts = Nil)
    }

    def wsResponse(statusCode: Int, bodyStr: String = "", headers: Map[String, Seq[String]] = Map.empty): WSResponse = {
      val r = mock[WSResponse]
      when(r.status).thenReturn(statusCode)
      when(r.body).thenReturn(bodyStr)
      when(r.headers).thenReturn(headers)
      r
    }
  }

  "initiate" should {

    "rewrite uploadRequest.href to the stub upload URL and preserve other fields" in new TestSetup {

      val responseBody = Json
        .obj(
          "reference"     -> "ref-123",
          "uploadRequest" -> Json.obj(
            "href"   -> "http://upscan-stub:9570/upscan/upload",
            "fields" -> Json.obj("x-amz-key" -> "some-key")
          )
        )
        .toString()

      val mockResponse = wsResponse(OK, responseBody)
      when(mockConnector.initiate(any()))
        .thenReturn(Future.successful(mockResponse))

      val request: FakeRequest[JsValue] =
        FakeRequest("POST", "/upscan/v2/initiate")
          .withBody(Json.obj("successRedirect" -> "http://localhost:1205/success"))

      val result = controller.initiate()(request)

      status(result) shouldBe OK

      val json = contentAsJson(result)
      (json \ "reference").as[String]                              shouldBe "ref-123"
      (json \ "uploadRequest" \ "fields" \ "x-amz-key").as[String] shouldBe "some-key"
      (json \ "uploadRequest" \ "href").as[String]                 shouldBe "http://localhost/upscan/upload"
    }

    "return the same status code as the connector response" in new TestSetup {

      val mockResponse = wsResponse(BAD_REQUEST, """{"error":"bad"}""")
      when(mockConnector.initiate(any()))
        .thenReturn(Future.successful(mockResponse))

      val request: FakeRequest[JsValue] =
        FakeRequest("POST", "/upscan/v2/initiate").withBody(Json.obj())

      status(controller.initiate()(request)) shouldBe BAD_REQUEST
    }
  }

  "upload" should {

    "intercept a filename containing 'empty' and redirect with EntityTooSmall" in new TestSetup {

      val request = FakeRequest("POST", "/upscan/upload")
        .withBody(multipartBody(Some("empty-return.csv")))

      val result = controller.upload()(request)

      status(result) shouldBe SEE_OTHER

      val location = redirectLocation(result).getOrElse(fail("Expected redirect location"))
      location should include(errorRedirectUrl)
      location should include("errorCode=EntityTooSmall")
    }

    "intercept when no file is present and redirect with EntityTooSmall" in new TestSetup {

      val request = FakeRequest("POST", "/upscan/upload")
        .withBody(multipartBody(None))

      val result = controller.upload()(request)

      status(result) shouldBe SEE_OTHER

      val location = redirectLocation(result).getOrElse(fail("Expected redirect location"))
      location should include(errorRedirectUrl)
      location should include("errorCode=EntityTooSmall")
    }

    "return BadRequest when error_action_redirect is missing and filename contains 'empty'" in new TestSetup {

      val body = MultipartFormData[Files.TemporaryFile](
        dataParts = Map.empty,
        files = Seq(
          MultipartFormData
            .FilePart("file", "empty.csv", Some("text/plain"), SingletonTemporaryFileCreator.create("t", ".csv"))
        ),
        badParts = Nil
      )

      status(controller.upload()(FakeRequest("POST", "/upscan/upload").withBody(body))) shouldBe BAD_REQUEST
    }

    "return BadRequest when error_action_redirect is missing and no file is present" in new TestSetup {

      val body = MultipartFormData[Files.TemporaryFile](
        dataParts = Map.empty,
        files = Seq.empty,
        badParts = Nil
      )

      status(controller.upload()(FakeRequest("POST", "/upscan/upload").withBody(body))) shouldBe BAD_REQUEST
    }

    "proxy a normal file and pass through a redirect response from the connector" in new TestSetup {

      val successUrl   = "http://localhost:1205/obligations/returns/isa/upscan/success?key=abc123"
      val mockResponse = wsResponse(SEE_OTHER, headers = Map("Location" -> Seq(successUrl)))
      when(mockResponse.header("Location")).thenReturn(Some(successUrl))

      when(mockConnector.upload(any(), any()))
        .thenReturn(Future.successful(mockResponse))

      val result =
        controller.upload()(FakeRequest("POST", "/upscan/upload").withBody(multipartBody(Some("valid-return.csv"))))

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(successUrl)
    }

    "proxy a normal file and pass through a non-redirect response from the connector" in new TestSetup {

      val mockResponse = wsResponse(OK, "upload accepted", Map("Content-Type" -> Seq("text/plain")))
      when(mockResponse.header("Content-Type")).thenReturn(Some("text/plain"))

      when(mockConnector.upload(any(), any()))
        .thenReturn(Future.successful(mockResponse))

      val result =
        controller.upload()(FakeRequest("POST", "/upscan/upload").withBody(multipartBody(Some("valid-return.csv"))))

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "upload accepted"
    }

    "intercept a file with a disallowed MIME type, redirect to success_action_redirect and send a REJECTED callback" in new TestSetup {

      val successUrl  = "http://localhost:1205/obligations/returns/isa/upscan/success"
      val callbackUrl = "http://localhost:6063/disa-returns-backend/monthly/upscan/callback/Z0000/2026-27/6"
      val reference   = "f24d44f0-c0b7-4cd4-aea6-f76bc276139c"

      val callbackResponse = wsResponse(NO_CONTENT)
      when(mockConnector.sendCallback(any(), any())).thenReturn(Future.successful(callbackResponse))

      val request = FakeRequest("POST", "/upscan/upload")
        .withBody(
          multipartBody(
            filename = Some("return.pdf"),
            contentType = Some("application/pdf"),
            extraDataParts = Map(
              "success_action_redirect"      -> Seq(successUrl),
              "key"                          -> Seq(reference),
              "x-amz-meta-callback-url"      -> Seq(callbackUrl),
              "x-amz-meta-consuming-service" -> Seq("python-requests/2.34.2")
            )
          )
        )

      val result = controller.upload()(request)

      status(result) shouldBe SEE_OTHER

      val location = redirectLocation(result).getOrElse(fail("Expected redirect location"))
      location should include(successUrl)
      location should include(s"key=$reference")

      val urlCaptor  = ArgumentCaptor.forClass(classOf[String])
      val bodyCaptor = ArgumentCaptor.forClass(classOf[JsValue])
      verify(mockConnector).sendCallback(urlCaptor.capture(), bodyCaptor.capture())

      urlCaptor.getValue shouldBe callbackUrl

      val body = bodyCaptor.getValue
      (body \ "reference").as[String]                        shouldBe reference
      (body \ "fileStatus").as[String]                       shouldBe "FAILED"
      (body \ "failureDetails" \ "failureReason").as[String] shouldBe "REJECTED"
      (body \ "failureDetails" \ "message")
        .as[String]                                          shouldBe "MIME type [application/pdf] is not allowed for service: [python-requests/2.34.2]"
    }

    "return BadRequest for a disallowed MIME type when success_action_redirect is missing" in new TestSetup {

      val request = FakeRequest("POST", "/upscan/upload")
        .withBody(
          multipartBody(
            filename = Some("return.pdf"),
            contentType = Some("application/pdf")
          )
        )

      status(controller.upload()(request)) shouldBe BAD_REQUEST
      verify(mockConnector, never()).sendCallback(any(), any())
    }

    "redirect to success_action_redirect without a key parameter and skip the callback when key is missing" in new TestSetup {

      val successUrl = "http://localhost:1205/obligations/returns/isa/upscan/success"

      val request = FakeRequest("POST", "/upscan/upload")
        .withBody(
          multipartBody(
            filename = Some("return.pdf"),
            contentType = Some("application/pdf"),
            extraDataParts = Map("success_action_redirect" -> Seq(successUrl))
          )
        )

      val result = controller.upload()(request)

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(successUrl)

      verify(mockConnector, never()).sendCallback(any(), any())
    }

    "still redirect to success_action_redirect when sending the REJECTED callback fails" in new TestSetup {

      val successUrl  = "http://localhost:1205/obligations/returns/isa/upscan/success"
      val callbackUrl = "http://localhost:6063/disa-returns-backend/monthly/upscan/callback/Z0000/2026-27/6"
      val reference   = "f24d44f0-c0b7-4cd4-aea6-f76bc276139c"

      when(mockConnector.sendCallback(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("callback service unavailable")))

      val request = FakeRequest("POST", "/upscan/upload")
        .withBody(
          multipartBody(
            filename = Some("return.pdf"),
            contentType = Some("application/pdf"),
            extraDataParts = Map(
              "success_action_redirect" -> Seq(successUrl),
              "key"                     -> Seq(reference),
              "x-amz-meta-callback-url" -> Seq(callbackUrl)
            )
          )
        )

      val result = controller.upload()(request)

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"$successUrl?key=$reference")

      verify(mockConnector).sendCallback(any(), any())
    }
  }
}
