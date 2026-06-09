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
import play.api.http.HttpEntity
import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.disareturnsstubs.connectors.UpscanProxyConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpscanController @Inject()(
                                  cc: ControllerComponents,
                                  upscanProxyConnector: UpscanProxyConnector
                                )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def initiate(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      upscanProxyConnector.initiate(request.body).map { response =>

        val json = Json.parse(response.body)

        val updatedJson = json.transform(
          (__ \ "uploadRequest" \ "href").json.update(
            Reads.pure(JsString(routes.UpscanController.upload.absoluteURL()))
          )
        ).getOrElse(json)

        Status(response.status)(updatedJson)
      }
    }

  def upload(): Action[MultipartFormData[Files.TemporaryFile]] =
    Action.async(parse.multipartFormData) { implicit request =>
      request.body.file("file") match {
        case Some(file) if file.filename.toLowerCase.contains("empty") =>
          Future.successful(errorRedirect("EntityTooSmall", "Your proposed upload is smaller than the minimum allowed size"))

        case None =>
          Future.successful(errorRedirect("EntityTooSmall", "Your proposed upload is smaller than the minimum allowed size"))

        case maybeFile =>
          upscanProxyConnector.upload(maybeFile, request.body.dataParts).map(toResult)
      }
    }

  private def toResult(response: WSResponse): Result =
    if (response.status >= 300 && response.status < 400) {
      val location = response.header("Location")
        .getOrElse(throw new RuntimeException("Missing Location header from Upscan stub"))
      Result(
        header = ResponseHeader(response.status, Map("Location" -> location)),
        body   = HttpEntity.NoEntity
      )
    } else {
      val headers = response.headers.toSeq.flatMap { case (k, vs) => vs.map(v => k -> v) }.toMap
      Result(
        header = ResponseHeader(response.status, headers),
        body   = HttpEntity.Strict(ByteString(response.body), response.header("Content-Type").orElse(Some("text/plain")))
      )
    }

  private def errorRedirect(code: String, message: String)(implicit request: Request[MultipartFormData[Files.TemporaryFile]]): Result = {
    val errorRedirectUrl = request.body.dataParts
      .get("error_action_redirect")
      .flatMap(_.headOption)
    errorRedirectUrl.fold(BadRequest("Missing error_action_redirect"))(url =>
      Redirect(url, Map("errorCode" -> Seq(code), "errorMessage" -> Seq(message)), SEE_OTHER)
    )
  }
}