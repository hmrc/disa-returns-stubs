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

import org.mongodb.scala.result.UpdateResult
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.await
import uk.gov.hmrc.disareturnsstubs.models.{IssueIdentifiedMessage, IssueIdentifiedOverSubscribed, MonthlyReport, ReturnResult}
import uk.gov.hmrc.disareturnsstubs.repositories.ReportRepository
import uk.gov.hmrc.mongo.MongoComponent
import utils.BaseUnitSpec

class MonthlyReportsRepositorySpec extends BaseUnitSpec {

  override lazy val app: Application = new GuiceApplicationBuilder().build()
  lazy val mongoComponent: MongoComponent = app.injector.instanceOf[MongoComponent]
  lazy val repo = new ReportRepository(mongoComponent)
  val report1: MonthlyReport = MonthlyReport(
    isaManagerReferenceNumber = "Z123",
    year = "2025-26",
    month = "JAN",
    returnResults = Seq(ReturnResult(
      accountNumber = "100000001",
      nino = "AB123457C",
      issueIdentified = IssueIdentifiedOverSubscribed(
        code = "OVER_SUBSCRIBED",
        overSubscribedAmount = 1000.00
      ))))

  val report2: MonthlyReport = report1.copy(returnResults = Seq(ReturnResult(
    accountNumber = "100000002",
    nino = "AB123456C",
    issueIdentified = IssueIdentifiedMessage(
      code = "UNABLE_TO_IDENTIFY_INVESTOR",
      message = "UNABLE_TO_IDENTIFY_INVESTOR"
    ))))

  "insert a new document when it doesn't exist" in {
    await(repo.collection.drop().toFuture())

    val result: UpdateResult = await(repo.insertReport(report1))
    result.wasAcknowledged() shouldBe true
    result.getUpsertedId should not be null

    val stored = await(repo.collection.find().headOption())
    stored shouldBe Some(report1)
  }

  "update the document if it already exists with same month + isaManagerReferenceNumber" in {
    await(repo.insertReport(report1))
    val result: UpdateResult = await(repo.insertReport(report2))

    result.wasAcknowledged() shouldBe true
    result.getUpsertedId shouldBe null
    result.getModifiedCount should be > 0L

    val stored = await(repo.collection.find().headOption())
    stored shouldBe Some(report2)
  }

  "allow inserting multiple documents for different months or managers" in {
    val otherReport = report1.copy(month = "JAN", isaManagerReferenceNumber = "Z456")

    await(repo.insertReport(report1))
    await(repo.insertReport(otherReport))

    val results = await(repo.collection.find().toFuture())
    results should contain theSameElementsAs Seq(report1, otherReport)
  }
}
