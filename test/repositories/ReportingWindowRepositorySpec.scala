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

package repositories

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.await
import uk.gov.hmrc.disareturnsstubs.repositories.ReportingWindowRepository
import uk.gov.hmrc.mongo.MongoComponent
import utils.BaseUnitSpec

class ReportingWindowRepositorySpec extends BaseUnitSpec {

  override lazy val app: Application = new GuiceApplicationBuilder().build()
  lazy val mongoComponent: MongoComponent = app.injector.instanceOf[MongoComponent]
  lazy val repo = new ReportingWindowRepository(mongoComponent)

  "setReportingWindowState" should {
    "create the document when it doesn't exist" in {
      await(repo.collection.drop().toFuture())

      await(repo.setReportingWindowState(open = true))

      val result = await(repo.getReportingWindowState)
      result shouldBe Some(true)
    }

    "update the document if it already exists" in {
      await(repo.setReportingWindowState(open = true))
      await(repo.setReportingWindowState(open = false))

      val result = await(repo.getReportingWindowState)
      result shouldBe Some(false)
    }
  }

  "getReportingWindowState" should {
    "return None when no document exists" in {
      await(repo.collection.drop().toFuture())

      val result = await(repo.getReportingWindowState)
      result shouldBe None
    }
  }
}
