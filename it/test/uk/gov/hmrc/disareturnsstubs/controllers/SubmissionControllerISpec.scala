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

package uk.gov.hmrc.disareturnsstubs.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disareturnsstubs.BaseISpec

class SubmissionControllerISpec extends BaseISpec {

  "PUT /disa-returns-submission/monthly/:zReference/:taxYear/:month/submissions/:submissionId" should {
    "return 200 OK without parsing the request body or headers" in {
      val request = FakeRequest(
        PUT,
        s"/disa-returns-submission/monthly/$validZReference/2026-27/AUG/submissions/submission-id"
      ).withHeaders(CONTENT_TYPE -> "invalid/content-type")
        .withBody("body that must not be parsed")

      val result = route(app, request).get

      status(result) mustBe OK
    }
  }

  "POST /disa-returns-submission/monthly/:zReference/:taxYear/:month/declarations" should {
    "return 200 OK without parsing the request body or headers" in {
      val request = FakeRequest(
        POST,
        s"/disa-returns-submission/monthly/$validZReference/2026-27/AUG/declarations"
      ).withHeaders(
        CONTENT_TYPE  -> "application/json",
        AUTHORIZATION -> "not parsed"
      ).withBody("not valid JSON")

      val result = route(app, request).get

      status(result) mustBe OK
    }
  }
}
