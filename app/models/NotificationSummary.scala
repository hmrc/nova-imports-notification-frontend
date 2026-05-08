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

package models

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

sealed trait NotificationSummary {
  def traderName: String
  def vrn: String
}

object NotificationSummary {

  final case class IndividualOrOrganisation(
    traderName: String,
    vrn: String,
    hasDraftNotifications: Boolean
  ) extends NotificationSummary

  final case class AgentWithoutClient(
    traderName: String,
    vrn: String,
    hasDraftNotifications: Boolean,
    hasClients: Boolean
  ) extends NotificationSummary

  final case class AgentWithClient(
    traderName: String,
    vrn: String,
    clientTraderName: String,
    clientVrn: String,
    clientHasDraftNotifications: Boolean,
    hasClients: Boolean
  ) extends NotificationSummary

  implicit val reads: Reads[NotificationSummary] = Reads { json =>
    val clientVrn  = (json \ "clientVrn").asOpt[String]
    val hasClients = (json \ "hasClients").asOpt[Boolean]

    (clientVrn, hasClients) match {
      case (Some(_), _) =>
        ((__ \ "traderName").read[String] and
          (__ \ "vrn").read[String] and
          (__ \ "clientTraderName").read[String] and
          (__ \ "clientVrn").read[String] and
          (__ \ "clientHasDraftNotifications").read[Boolean] and
          (__ \ "hasClients").read[Boolean])(AgentWithClient.apply _).reads(json)

      case (None, Some(_)) =>
        ((__ \ "traderName").read[String] and
          (__ \ "vrn").read[String] and
          (__ \ "hasDraftNotifications").read[Boolean] and
          (__ \ "hasClients").read[Boolean])(AgentWithoutClient.apply _).reads(json)

      case (None, None) =>
        ((__ \ "traderName").read[String] and
          (__ \ "vrn").read[String] and
          (__ \ "hasDraftNotifications").read[Boolean])(IndividualOrOrganisation.apply _).reads(json)
    }
  }
}
