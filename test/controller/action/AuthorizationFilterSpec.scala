/*
 * Copyright 2025 HM Revenue & Customs
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

package controller.action

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.disareturnsstubs.controllers.action.AuthorizationFilter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthorizationFilterSpec extends AnyWordSpec with Matchers with ScalaFutures {

  val authorizationFilter = new AuthorizationFilter()

  "AuthorizationFilter" should {

    "allow requests with Authorization header" in {
      val request = FakeRequest().withHeaders("Authorization" -> "Bearer token")

      whenReady(authorizationFilter.refine(request)) {
        case Left(_)    => fail("Expected request to be allowed")
        case Right(req) => req shouldBe request
      }
    }

    "returning 403 forbidden for any requests without Authorization header" in {
      val request = FakeRequest()

      whenReady(authorizationFilter.refine(request)) {
        case Left(result) =>
          val resultF = Future.successful(result)
          status(resultF)        shouldBe FORBIDDEN
          contentAsJson(resultF) shouldBe Json.obj("code" -> "403", "reason" -> "Missing required bearer token")
        case Right(_)     =>
          fail("Expected Forbidden but got a successful refinement")
      }
    }
  }
}
