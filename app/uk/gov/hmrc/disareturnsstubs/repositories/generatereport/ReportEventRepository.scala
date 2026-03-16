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

package uk.gov.hmrc.disareturnsstubs.repositories.generatereport

import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes, ReplaceOptions}
import org.mongodb.scala.result.UpdateResult
import play.api.Logging
import uk.gov.hmrc.disareturnsstubs.models.generatereport.ReportEvent
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportEventRepository @Inject() (mc: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ReportEvent](
      mongoComponent = mc,
      collectionName = "reportEvents",
      domainFormat = ReportEvent.format,
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
          Indexes.ascending("createdAt"),
          IndexOptions()
            .name("createdAt_ttl_index")
            .expireAfter(3L, TimeUnit.DAYS)
        )
      )
    )
    with Logging {

  def upsert(event: ReportEvent): Future[UpdateResult] = {

    val filter = and(
      equal("zReference", event.zReference),
      equal("year", event.year),
      equal("month", event.month)
    )

    logger.debug(
      s"Upserting report event for zRef=${event.zReference}, year=${event.year}, month=${event.month}"
    )

    collection
      .replaceOne(
        filter = filter,
        replacement = event,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
  }

  def find(
    zReference: String,
    year: String,
    month: String
  ): Future[Option[ReportEvent]] = {

    val filter = and(
      equal("zReference", zReference),
      equal("year", year),
      equal("month", month)
    )

    collection
      .find(filter)
      .headOption()
  }
}
