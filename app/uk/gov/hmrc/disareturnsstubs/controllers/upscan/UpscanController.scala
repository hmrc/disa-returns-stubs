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

package uk.gov.hmrc.disareturnsstubs.controllers.upscan

import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.http.HttpEntity
import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.disareturnsstubs.connectors.UpscanProxyConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class UpscanController @Inject() (
  cc: ControllerComponents,
  upscanProxyConnector: UpscanProxyConnector
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  private val allowedContentTypes: Set[String] = Set(
    "text/csv",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  )

  def initiate(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      upscanProxyConnector.initiate(request.body).map { response =>
        val json = Json.parse(response.body)

        val updatedJson = json
          .transform(
            (__ \ "uploadRequest" \ "href").json.update(
              Reads.pure(JsString(routes.UpscanController.upload.absoluteURL()))
            )
          )
          .getOrElse(json)

        Status(response.status)(updatedJson)
      }
    }

  def upload(): Action[MultipartFormData[Files.TemporaryFile]] =
    Action.async(parse.multipartFormData) { implicit request =>
      request.body.file("file") match {
        case Some(file) if file.filename.toLowerCase.contains("empty") =>
          Future.successful(
            errorRedirect("EntityTooSmall", "Your proposed upload is smaller than the minimum allowed size")
          )

        case None =>
          Future.successful(
            errorRedirect("EntityTooSmall", "Your proposed upload is smaller than the minimum allowed size")
          )

        case Some(file) if !file.contentType.exists(allowedContentTypes.contains) =>
          Future.successful(rejectInvalidMimeType(file))

        case maybeFile =>
          upscanProxyConnector.upload(maybeFile, request.body.dataParts).map(toResult)
      }
    }

  private def toResult(response: WSResponse): Result =
    if (response.status >= MULTIPLE_CHOICES && response.status < BAD_REQUEST) {
      val location = response
        .header("Location")
        .getOrElse(throw new RuntimeException("Missing Location header from Upscan stub"))
      Result(
        header = ResponseHeader(response.status, Map("Location" -> location)),
        body = HttpEntity.NoEntity
      )
    } else {
      val headers = response.headers.toSeq.flatMap { case (k, vs) => vs.map(v => k -> v) }.toMap
      Result(
        header = ResponseHeader(response.status, headers),
        body = HttpEntity.Strict(ByteString(response.body), response.header("Content-Type").orElse(Some("text/plain")))
      )
    }

  private def errorRedirect(code: String, message: String)(implicit
    request: Request[MultipartFormData[Files.TemporaryFile]]
  ): Result = {
    val errorRedirectUrl = request.body.dataParts
      .get("error_action_redirect")
      .flatMap(_.headOption)
    errorRedirectUrl.fold(BadRequest("Missing error_action_redirect"))(url =>
      Redirect(url, Map("errorCode" -> Seq(code), "errorMessage" -> Seq(message)), SEE_OTHER)
    )
  }

  // Mimics real upscan: a file with a disallowed MIME type is accepted on upload (success_action_redirect),
  // then asynchronously REJECTED via a callback to the consuming service's callback URL
  private def rejectInvalidMimeType(
    file: MultipartFormData.FilePart[Files.TemporaryFile]
  )(implicit request: Request[MultipartFormData[Files.TemporaryFile]]): Result = {
    val dataParts = request.body.dataParts

    for {
      callbackUrl <- dataParts.get("x-amz-meta-callback-url").flatMap(_.headOption)
      reference   <- dataParts.get("key").flatMap(_.headOption)
    } yield {
      val consumingService = dataParts.get("x-amz-meta-consuming-service").flatMap(_.headOption).getOrElse("unknown")
      val contentType      = file.contentType.getOrElse("application/octet-stream")

      val callbackBody = Json.obj(
        "reference"      -> reference,
        "fileStatus"     -> "FAILED",
        "failureDetails" -> Json.obj(
          "failureReason" -> "REJECTED",
          "message"       -> s"MIME type [$contentType] is not allowed for service: [$consumingService]"
        )
      )

      upscanProxyConnector.sendCallback(callbackUrl, callbackBody).onComplete {
        case Success(response)  =>
          logger.info(s"Sent REJECTED upscan callback to [$callbackUrl], received status [${response.status}]")
        case Failure(exception) =>
          logger.warn(s"Failed to send REJECTED upscan callback to [$callbackUrl]", exception)
      }
    }

    successRedirect(dataParts)
  }

  private def successRedirect(dataParts: Map[String, Seq[String]]): Result = {
    val successUrl = dataParts.get("success_action_redirect").flatMap(_.headOption)
    val key        = dataParts.get("key").flatMap(_.headOption)

    successUrl.fold(BadRequest("Missing success_action_redirect")) { url =>
      val params = key.fold(Map.empty[String, Seq[String]])(k => Map("key" -> Seq(k)))
      Redirect(url, params, SEE_OTHER)
    }
  }
}
