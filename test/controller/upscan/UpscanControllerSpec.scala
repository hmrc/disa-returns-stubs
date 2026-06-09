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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
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

    def multipartBody(filename: Option[String]): MultipartFormData[Files.TemporaryFile] = {
      val files = filename.toSeq.map { name =>
        MultipartFormData.FilePart[Files.TemporaryFile](
          key = "file",
          filename = name,
          contentType = Some("text/plain"),
          ref = SingletonTemporaryFileCreator.create("test", ".tmp")
        )
      }
      MultipartFormData(dataParts = defaultDataParts, files = files, badParts = Nil)
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

      val mockResponse = wsResponse(200, responseBody)
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

      val mockResponse = wsResponse(400, """{"error":"bad"}""")
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
      val mockResponse = wsResponse(303, headers = Map("Location" -> Seq(successUrl)))
      when(mockResponse.header("Location")).thenReturn(Some(successUrl))

      when(mockConnector.upload(any(), any()))
        .thenReturn(Future.successful(mockResponse))

      val result =
        controller.upload()(FakeRequest("POST", "/upscan/upload").withBody(multipartBody(Some("valid-return.csv"))))

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(successUrl)
    }

    "proxy a normal file and pass through a non-redirect response from the connector" in new TestSetup {

      val mockResponse = wsResponse(200, "upload accepted", Map("Content-Type" -> Seq("text/plain")))
      when(mockResponse.header("Content-Type")).thenReturn(Some("text/plain"))

      when(mockConnector.upload(any(), any()))
        .thenReturn(Future.successful(mockResponse))

      val result =
        controller.upload()(FakeRequest("POST", "/upscan/upload").withBody(multipartBody(Some("valid-return.csv"))))

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "upload accepted"
    }
  }
}
