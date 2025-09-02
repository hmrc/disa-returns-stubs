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
import uk.gov.hmrc.disareturnsstubs.models.ObligationStatus
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ObligationStatusRepository @Inject() (mc: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ObligationStatus](
      mongoComponent = mc,
      collectionName = "obligationStatus",
      domainFormat = ObligationStatus.format,
      indexes = Seq(IndexModel(Indexes.ascending("isaManagerReference"), IndexOptions().unique(true)))
    ) {

  def closeObligationStatus(isaManagerReference: String): Future[Unit] =
    collection
      .updateOne(
        Filters.eq("isaManagerReference", isaManagerReference),
        Updates.combine(
          Updates.set("obligationAlreadyMet", true),
          Updates.setOnInsert("isaManagerReference", isaManagerReference)
        ),
        UpdateOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())

  def openObligationStatus(isaManagerReference: String): Future[Unit] =
    collection
      .updateOne(
        Filters.eq("isaManagerReference", isaManagerReference),
        Updates.combine(
          Updates.set("obligationAlreadyMet", false),
          Updates.setOnInsert("isaManagerReference", isaManagerReference)
        ),
        UpdateOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())

  def getObligationStatus(isaManagerReference: String): Future[Option[Boolean]] =
    collection
      .find(Filters.eq("isaManagerReference", isaManagerReference))
      .first()
      .toFutureOption()
      .map(_.map(_.obligationAlreadyMet))

  def dropCollection(): Future[Unit] =
    collection.drop().toFuture().map(_ => ())
}
