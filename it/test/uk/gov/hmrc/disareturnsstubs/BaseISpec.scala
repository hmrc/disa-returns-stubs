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

import org.scalacheck.Gen
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.DefaultAwaitTimeout
import uk.gov.hmrc.disareturnsstubs.config.AppConfig
import uk.gov.hmrc.disareturnsstubs.repositories.generatereport.{ReportEventRepository, ReportIssueRepository}
import uk.gov.hmrc.disareturnsstubs.repositories.{ObligationStatusRepository, ReportingWindowRepository}
import uk.gov.hmrc.http.test.WireMockSupport

import scala.reflect.ClassTag

trait BaseISpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with DefaultAwaitTimeout
    with ScalaFutures
    with IntegrationPatience
    with WireMockSupport {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.upscan-stub.host"     -> wireMockHost,
        "microservice.services.upscan-stub.port"     -> wireMockPort,
        "microservice.services.upscan-stub.protocol" -> "http",
        "http-verbs.retries.intervals"               -> Seq("1ms", "1ms", "1ms")
      )
      .build()

  protected def inject[T: ClassTag]: T =
    app.injector.instanceOf[T]

  protected lazy val wsClient: WSClient = inject[WSClient]

  lazy val reportingWindowRepository: ReportingWindowRepository = inject[ReportingWindowRepository]
  lazy val obligationStatusRepository: ObligationStatusRepository = inject[ObligationStatusRepository]
  lazy val reportEventRepository: ReportEventRepository = inject[ReportEventRepository]
  lazy val reportIssueRepository: ReportIssueRepository = inject[ReportIssueRepository]
  lazy val appConfig: AppConfig = inject[AppConfig]

  val zReferenceGen: Gen[String] =
    Gen.listOfN(4, Gen.numChar).map(digits => s"Z${digits.mkString}")

  val validZReference: String = zReferenceGen.sample.get

  protected def serviceUrl(path: String): String = s"http://localhost:$port$path"
}
