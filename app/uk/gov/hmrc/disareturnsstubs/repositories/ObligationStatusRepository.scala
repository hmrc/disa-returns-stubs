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

import org.mongodb.scala.model._
import play.api.Logging
import uk.gov.hmrc.disareturnsstubs.models.ObligationStatus
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ObligationStatusRepository @Inject() (mc: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ObligationStatus](
      mongoComponent = mc,
      collectionName = "obligationStatus",
      domainFormat = ObligationStatus.format,
      indexes = Seq(
        IndexModel(
          keys = Indexes.ascending("createdAt"),
          indexOptions = IndexOptions()
            .name("createdAtTtlIdx")
            .expireAfter(5, TimeUnit.DAYS)
        ),
        IndexModel(Indexes.ascending("zReference"), IndexOptions().unique(true))
      ),
      replaceIndexes = true
    )
    with Logging {

  def closeObligationStatus(zReference: String): Future[Unit] =
    collection
      .updateOne(
        Filters.eq("zReference", zReference),
        Updates.combine(
          Updates.set("obligationAlreadyMet", true),
          Updates.setOnInsert("zReference", zReference),
          Updates.setOnInsert("createdAt", Instant.now)
        ),
        UpdateOptions().upsert(true)
      )
      .toFuture()
      .map(_ => logger.debug(s"Closed obligation status for IM Ref: [$zReference]"))

  def openObligationStatus(zReference: String): Future[Unit] =
    collection
      .updateOne(
        Filters.eq("zReference", zReference),
        Updates.combine(
          Updates.set("obligationAlreadyMet", false),
          Updates.setOnInsert("zReference", zReference),
          Updates.setOnInsert("createdAt", Instant.now)
        ),
        UpdateOptions().upsert(true)
      )
      .toFuture()
      .map(_ => logger.debug(s"Opened obligation status for IM Ref: [$zReference]"))

  def getObligationStatus(zReference: String): Future[Option[Boolean]] =
    collection
      .find(Filters.eq("zReference", zReference))
      .first()
      .toFutureOption()
      .map(_.map { status =>
        logger.debug(s"Retrieved obligation status as: [${status.obligationAlreadyMet}]")
        status.obligationAlreadyMet
      })

  def dropCollection(): Future[Unit] =
    collection.drop().toFuture().map(_ => ())
}
