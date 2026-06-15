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

sealed trait NotificationSummary

object NotificationSummary {

  final case class IndividualOrOrganisation(
    traderName: Option[String],
    vrn: Option[String],
    hasDraftNotifications: Boolean,
    isDeregistered: Boolean
  ) extends NotificationSummary

  final case class AgentWithoutClient(
    agentName: Option[String],
    hasDraftNotifications: Boolean
  ) extends NotificationSummary

  final case class AgentWithClient(
    agentName: Option[String],
    clientTraderName: Option[String],
    clientVrn: String,
    clientHasDraftNotifications: Boolean,
    clientIsDeregistered: Boolean
  ) extends NotificationSummary

  implicit val reads: Reads[NotificationSummary] = Reads { json =>
    if (json \ "clientVrn").asOpt[String].isDefined then
      ((__ \ "agentName").readNullable[String] and
        (__ \ "clientTraderName").readNullable[String] and
        (__ \ "clientVrn").read[String] and
        (__ \ "clientHasDraftNotifications").read[Boolean] and
        (__ \ "clientIsDeregistered").read[Boolean])(AgentWithClient.apply _).reads(json)
    else if (json \ "hasClients").toOption.isDefined then
      ((__ \ "agentName").readNullable[String] and
        (__ \ "hasDraftNotifications").read[Boolean])(AgentWithoutClient.apply _).reads(json)
    else
      ((__ \ "traderName").readNullable[String] and
        (__ \ "vrn").readNullable[String] and
        (__ \ "hasDraftNotifications").read[Boolean] and
        (__ \ "isDeregistered").read[Boolean])(IndividualOrOrganisation.apply _).reads(json)
  }
}
