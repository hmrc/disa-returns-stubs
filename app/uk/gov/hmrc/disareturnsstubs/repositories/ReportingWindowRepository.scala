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

import org.mongodb.scala.model.{Filters, ReplaceOptions}
import play.api.Logging
import uk.gov.hmrc.disareturnsstubs.models.ReportingWindowState
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportingWindowRepository @Inject() (mc: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ReportingWindowState](
      mongoComponent = mc,
      collectionName = "reportingWindow",
      domainFormat = ReportingWindowState.format,
      indexes = Seq.empty
    )
    with Logging {

  def setReportingWindowState(open: Boolean): Future[Unit] = {
    val doc = ReportingWindowState(reportingWindowOpen = open)
    collection
      .replaceOne(
        Filters.eq("_id", "test-scenario"),
        doc,
        new ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => logger.debug(s"Set reporting window state as: [$open]"))
  }

  def getReportingWindowState: Future[Option[Boolean]] =
    collection
      .find(Filters.eq("_id", "test-scenario"))
      .first()
      .toFutureOption()
      .map(_.map { state =>
        logger.debug(s"Retrieved reporting window state as: [$state]")
        state.reportingWindowOpen
      })
}
