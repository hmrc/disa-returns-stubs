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

import play.api.test.Helpers.await
import uk.gov.hmrc.disareturnsstubs.models.ObligationStatus
import uk.gov.hmrc.disareturnsstubs.repositories.ObligationStatusRepository
import uk.gov.hmrc.mongo.MongoComponent
import utils.BaseUnitSpec

class ObligationStatusRepositorySpec extends BaseUnitSpec {

  protected val databaseName                           = "disa-returns-test"
  val isaManagerReference                              = "Z1110"
  protected val mongoUri: String                       = s"mongodb://127.0.0.1:27017/$databaseName"
  lazy val mongoComponentForTest: MongoComponent       = MongoComponent(mongoUri)
  protected val repository: ObligationStatusRepository =
    new ObligationStatusRepository(mongoComponentForTest)

  "closeObligationStatus" should {
    "insert a new document with obligationAlreadyMet = true if none exists" in new TestSetup {
      await(repository.closeObligationStatus(isaManagerReference))

      val stored: Option[ObligationStatus] =
        await(repository.collection.find().toFuture()).headOption
      stored shouldBe Some(ObligationStatus(isaManagerReference, obligationAlreadyMet = true))
    }

    "update an existing document to obligationAlreadyMet = true" in new TestSetup {
      await(repository.openObligationStatus(isaManagerReference))
      await(repository.closeObligationStatus(isaManagerReference))

      val stored: Option[ObligationStatus] =
        await(repository.collection.find().toFuture()).headOption
      stored shouldBe Some(ObligationStatus(isaManagerReference, obligationAlreadyMet = true))
    }
  }

  "openObligationStatus" should {
    "insert a new document with obligationAlreadyMet = false if none exists" in new TestSetup {
      await(repository.openObligationStatus(isaManagerReference))

      val stored: Option[ObligationStatus] =
        await(repository.collection.find().toFuture()).headOption
      stored shouldBe Some(ObligationStatus(isaManagerReference, obligationAlreadyMet = false))
    }

    "update an existing document to obligationAlreadyMet = false" in new TestSetup {
      await(repository.closeObligationStatus(isaManagerReference))
      await(repository.openObligationStatus(isaManagerReference))

      val stored: Option[ObligationStatus] =
        await(repository.collection.find().toFuture()).headOption
      stored shouldBe Some(ObligationStatus(isaManagerReference, obligationAlreadyMet = false))
    }
  }

  "getObligationStatus" should {
    "return Some(true) when obligationAlreadyMet is true" in new TestSetup {
      await(repository.closeObligationStatus(isaManagerReference))

      val result: Option[Boolean] = await(repository.getObligationStatus(isaManagerReference))
      result shouldBe Some(true)
    }

    "return Some(false) when obligationAlreadyMet is false" in new TestSetup {
      await(repository.openObligationStatus(isaManagerReference))

      val result: Option[Boolean] = await(repository.getObligationStatus(isaManagerReference))
      result shouldBe Some(false)
    }

    "return None when no record exists" in new TestSetup {
      val result: Option[Boolean] = await(repository.getObligationStatus(isaManagerReference))
      result shouldBe None
    }
  }

  class TestSetup {
    await(repository.dropCollection())
    await(repository.ensureIndexes())
  }
}
