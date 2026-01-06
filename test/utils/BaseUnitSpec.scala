/*
 * Copyright 2023 HM Revenue & Customs
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

package utils

import org.mockito.Mockito
import org.scalacheck.Gen
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Request, Result}
import play.api.test.DefaultAwaitTimeout
import uk.gov.hmrc.disareturnsstubs.controllers.action.AuthorizationFilter
import uk.gov.hmrc.disareturnsstubs.repositories.ReportRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

abstract class BaseUnitSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with EitherValues
    with ScalaFutures
    with MockitoSugar
    with DefaultAwaitTimeout
    with GuiceOneAppPerSuite {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()
  val stubAuthFilter                = new StubAuthorizationFilter(None)

  val mockReportRepository: ReportRepository = mock[ReportRepository]

  override def beforeEach(): Unit = Mockito.reset()

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .build()

  class StubAuthorizationFilter(result: Option[Result])(implicit ec: ExecutionContext) extends AuthorizationFilter {
    override def filter[A](request: Request[A]): Future[Option[Result]] = Future.successful(result)
  }

  val zReferenceGen: Gen[String] =
    Gen.listOfN(4, Gen.numChar).map(digits => s"Z${digits.mkString}")

  val validZReference: String = zReferenceGen.sample.get

}
