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

package uk.gov.hmrc.disareturnsstubs.models.journeyData

import play.api.libs.json.{Format, Json, Reads, Writes}
import uk.gov.hmrc.disareturnsstubs.models.journeyData.isaProducts.IsaProducts
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class JourneyData(
  groupId: String,
  businessVerification: Option[BusinessVerification] = None,
  organisationDetails: Option[OrganisationDetails] = None,
  isaProducts: Option[IsaProducts] = None,
  certificatesOfAuthority: Option[CertificatesOfAuthority] = None,
  liaisonOfficers: Option[LiaisonOfficers] = None,
  signatories: Option[Signatories] = None,
  outsourcedAdministration: Option[OutsourcedAdministration] = None,
  feesCommissionsAndIncentives: Option[FeesCommissionsAndIncentives] = None,
  lastUpdated: Option[Instant] = None
)

object JourneyData {
  implicit val instantFormat: Format[Instant] =
    Format(MongoJavatimeFormats.instantReads, MongoJavatimeFormats.instantWrites)

  implicit val format: Format[JourneyData] = Json.format[JourneyData]

  case class TaskListJourney[A](reads: Reads[A], writes: Writes[A])

  val taskListJourneyHandlers: Map[String, TaskListJourney[_]] = Map(
    "businessVerification"         -> TaskListJourney(BusinessVerification.format, BusinessVerification.format),
    "organisationDetails"          -> TaskListJourney(OrganisationDetails.format, OrganisationDetails.format),
    "isaProducts"                  -> TaskListJourney(IsaProducts.format, IsaProducts.format),
    "certificatesOfAuthority"      -> TaskListJourney(CertificatesOfAuthority.format, CertificatesOfAuthority.format),
    "liaisonOfficers"              -> TaskListJourney(LiaisonOfficers.format, LiaisonOfficers.format),
    "signatories"                  -> TaskListJourney(Signatories.format, Signatories.format),
    "outsourcedAdministration"     -> TaskListJourney(OutsourcedAdministration.format, OutsourcedAdministration.format),
    "feesCommissionsAndIncentives" -> TaskListJourney(
      FeesCommissionsAndIncentives.format,
      FeesCommissionsAndIncentives.format
    )
  )
}
