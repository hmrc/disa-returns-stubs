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

package uk.gov.hmrc.disareturnsstubs.repositories

import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes, ReplaceOptions}
import org.mongodb.scala.result.UpdateResult
import play.api.Logging
import uk.gov.hmrc.disareturnsstubs.models.MonthlyReport
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportRepository @Inject() (mc: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[MonthlyReport](
      mongoComponent = mc,
      collectionName = "monthlyReport",
      domainFormat = MonthlyReport.format,
      indexes = Seq(
        IndexModel(
          Indexes.compoundIndex(
            Indexes.ascending("zReference"),
            Indexes.ascending("year"),
            Indexes.ascending("month")
          ),
          IndexOptions().unique(true)
        ),
        IndexModel(
          Indexes.ascending("updatedAt"),
          IndexOptions()
            .name("updatedAt_ttl_index")
            .expireAfter(30L, TimeUnit.DAYS)
        )
      )
    )
    with Logging {

  def insertReport(monthlyReport: MonthlyReport): Future[UpdateResult] = {
    val filter = and(
      equal("month", monthlyReport.month),
      equal("year", monthlyReport.year),
      equal("zReference", monthlyReport.zReference)
    )
    logger.debug(s"Inserting monthly report into db: [$monthlyReport]")

    collection
      .replaceOne(
        filter = filter,
        replacement = monthlyReport,
        options = new ReplaceOptions().upsert(true)
      )
      .toFuture()
  }

  def getMonthlyReport(
    zReference: String,
    taxYear: String,
    month: String
  ): Future[Option[MonthlyReport]] = {
    val filter = and(
      equal("zReference", zReference),
      equal("year", taxYear),
      equal("month", month)
    )

    logger.debug(s"Retrieving monthly report from db")

    collection
      .find(filter)
      .headOption()
  }
}
