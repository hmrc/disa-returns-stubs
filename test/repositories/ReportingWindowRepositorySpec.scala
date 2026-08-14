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

import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.documentToUntypedDocument
import org.scalatest.OptionValues.convertOptionToValuable
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.await
import uk.gov.hmrc.disareturnsstubs.config.AppConfig
import uk.gov.hmrc.disareturnsstubs.repositories.ReportingWindowRepository
import uk.gov.hmrc.mongo.MongoComponent
import utils.BaseUnitSpec

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class ReportingWindowRepositorySpec extends BaseUnitSpec {

  override lazy val app: Application      = new GuiceApplicationBuilder().build()
  lazy val mongoComponent: MongoComponent = inject[MongoComponent]
  lazy val appConfig: AppConfig           = inject[AppConfig]
  lazy val repo                           = new ReportingWindowRepository(mongoComponent, appConfig)

  "indexes" should {
    "configure updatedAt to expire after the reporting window TTL" in {
      await(repo.ensureIndexes())

      val indexes  = await(repo.collection.listIndexes().toFuture())
      val ttlIndex = indexes.find(_.getString("name") == "updatedAtTtlIdx").value

      ttlIndex.get("key").value.asDocument().getInt32("updatedAt").getValue shouldBe 1
      ttlIndex.get("expireAfterSeconds").value.asNumber().longValue         shouldBe
        TimeUnit.DAYS.toSeconds(appConfig.reportingWindowTtlDays.toLong)
      appConfig.reportingWindowTtlDays                                      shouldBe 3
    }
  }

  "setReportingWindowState" should {
    "create the document when it doesn't exist" in {
      await(repo.collection.drop().toFuture())

      val startedAt   = Instant.now().truncatedTo(ChronoUnit.MILLIS)
      await(repo.setReportingWindowState(open = true))
      val completedAt = Instant.now().plusMillis(1).truncatedTo(ChronoUnit.MILLIS)

      val result = await(repo.getReportingWindowState)
      result shouldBe Some(true)

      val stored = await(repo.collection.find().headOption()).value
      stored.updatedAt should be >= startedAt
      stored.updatedAt should be <= completedAt
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
