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

package uk.gov.hmrc.disareturnsstubs.models

import play.api.libs.json.{Json, OFormat}

import play.api.libs.json._

sealed trait IssueIdentified {
  def code: String
}

case class IssueIdentifiedMessage(
                                   code: String,
                                   message: String
                                 ) extends IssueIdentified

case class IssueIdentifiedOverSubscribed(
                                          code: String,
                                          overSubscribedAmount: BigDecimal
                                        ) extends IssueIdentified

object IssueIdentified {
  implicit val messageFormat: OFormat[IssueIdentifiedMessage] =
    Json.format[IssueIdentifiedMessage]

  implicit val overSubscribedFormat: OFormat[IssueIdentifiedOverSubscribed] =
    Json.format[IssueIdentifiedOverSubscribed]

  implicit val format: Format[IssueIdentified] = new Format[IssueIdentified] {
    override def reads(json: JsValue): JsResult[IssueIdentified] = {
      (json \ "code").validate[String].flatMap {
        case "OVER_SUBSCRIBED" => json.validate[IssueIdentifiedOverSubscribed]
        case _                 => json.validate[IssueIdentifiedMessage]
      }
    }

    override def writes(issue: IssueIdentified): JsValue = issue match {
      case o: IssueIdentifiedOverSubscribed => overSubscribedFormat.writes(o)
      case m: IssueIdentifiedMessage        => messageFormat.writes(m)
    }
  }
}

