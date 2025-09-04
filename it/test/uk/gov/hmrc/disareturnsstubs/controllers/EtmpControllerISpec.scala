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

package uk.gov.hmrc.disareturnsstubs.controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.disareturnsstubs.repositories.{ObligationStatusRepository, ReportingWindowRepository}

import scala.concurrent.Future

class EtmpControllerISpec extends PlaySpec with GuiceOneAppPerSuite with DefaultAwaitTimeout {

  val obligationStatusEndpoint                                        = "/etmp/check-obligation-status"
  val reportingWindowEndpoint                                         = "/etmp/check-reporting-window"
  val isaManagerReference                                             = "Z1111"
  lazy val mockObligationStatusRepository: ObligationStatusRepository =
    app.injector.instanceOf[ObligationStatusRepository]

  "EtmpController GET /etmp/check-obligation-status/:isaManagerReferenceNumber" should {

    "return 200 with obligationAlreadyMet = true" in {
      await(mockObligationStatusRepository.closeObligationStatus(isaManagerReference))
      val request: FakeRequest[AnyContentAsJson] =
        FakeRequest(GET, s"$obligationStatusEndpoint/$isaManagerReference").withJsonBody(Json.obj())
      val result: Future[Result]                 = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj(
        "obligationAlreadyMet" -> true
      )
    }

    "return 200 with obligationAlreadyMet = false" in {
      await(mockObligationStatusRepository.openObligationStatus(isaManagerReference))
      val request: FakeRequest[AnyContentAsJson] =
        FakeRequest(GET, s"$obligationStatusEndpoint/$isaManagerReference").withJsonBody(Json.obj())
      val result: Future[Result]                 = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj(
        "obligationAlreadyMet" -> false
      )
    }
  }

  "EtmpController GET /etmp/check-reporting-window" should {

    "return 200 with reportingWindowOpen = false when stub.reportingWindowScenario is 'closed'" in {
      lazy val mockReportingWindowState: ReportingWindowRepository =
        app.injector.instanceOf[ReportingWindowRepository]
      await(mockReportingWindowState.setReportingWindowState(false))

      val request = FakeRequest(GET, reportingWindowEndpoint)
      val result  = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("reportingWindowOpen" -> false)
    }

    "return 200 with reportingWindowOpen = true when stub.reportingWindowScenario is 'open'" in {
      lazy val mockReportingWindowState: ReportingWindowRepository =
        app.injector.instanceOf[ReportingWindowRepository]
      await(mockReportingWindowState.setReportingWindowState(true))

      val request = FakeRequest(GET, reportingWindowEndpoint)
      val result  = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("reportingWindowOpen" -> true)
    }
  }
}
