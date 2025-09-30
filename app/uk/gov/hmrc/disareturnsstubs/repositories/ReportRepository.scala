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
import org.mongodb.scala.model.ReplaceOptions
import org.mongodb.scala.result.UpdateResult
import uk.gov.hmrc.disareturnsstubs.models.MonthlyReport
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportRepository @Inject() (mc: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[MonthlyReport](
      mongoComponent = mc,
      collectionName = "monthlyReport",
      domainFormat = MonthlyReport.format,
      indexes = Seq.empty
    ) {

  def insertReport(monthlyReport: MonthlyReport): Future[UpdateResult] = {
    val filter = and(
      equal("month", monthlyReport.month),
      equal("year", monthlyReport.year),
      equal("isaManagerReferenceNumber", monthlyReport.isaManagerReferenceNumber)
    )
    collection
      .replaceOne(
        filter = filter,
        replacement = monthlyReport,
        options = new ReplaceOptions().upsert(true)
      )
      .toFuture()
  }
}
