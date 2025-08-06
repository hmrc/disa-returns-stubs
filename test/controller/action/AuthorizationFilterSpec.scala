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

    "allow requests with an Authorization header" in {
      val request = FakeRequest().withHeaders("Authorization" -> "Bearer token")

      whenReady(authorizationFilter.filter(request)) { resultOption =>
        resultOption shouldBe None
      }
    }

    "return 403 Forbidden for requests without an Authorization header" in {
      val request = FakeRequest()

      whenReady(authorizationFilter.filter(request)) { resultOption =>
        val result = resultOption.get
        result.header.status                     shouldBe FORBIDDEN
        contentAsJson(Future.successful(result)) shouldBe Json.obj(
          "code"    -> "FORBIDDEN",
          "message" -> "Missing required bearer token"
        )
      }
    }
  }
}
