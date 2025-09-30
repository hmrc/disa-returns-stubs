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

package uk.gov.hmrc.disareturnsstubs

import org.apache.pekko.stream.Materializer
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.disareturnsstubs.repositories.{ReportRepository, ObligationStatusRepository, ReportingWindowRepository}

abstract class BaseISpec
  extends PlaySpec
    with GuiceOneAppPerSuite
    with DefaultAwaitTimeout {

  implicit lazy val application: Application = app
  implicit lazy val mat: Materializer = app.materializer

  lazy val reportingWindowRepository: ReportingWindowRepository =
    app.injector.instanceOf[ReportingWindowRepository]

  lazy val obligationStatusRepository: ObligationStatusRepository =
    app.injector.instanceOf[ObligationStatusRepository]

  lazy val reportRepository: ReportRepository =
  app.injector.instanceOf[ReportRepository]

  val isaManagerReferenceNumber = "Z1234"
  val returnId = "b4aba7b8-0d34-4936-923c-d9ef2747c099"

  protected def fakeRequest = FakeRequest()
}
