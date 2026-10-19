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

package controllers.utils

import connectors.NovaImportsBackendConnector
import models.DraftNotification.SectionId
import models.{DraftNotification, NotificationSummary, PurchaserOrOnBehalf, UserContext}
import play.api.Logging
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

object PurchaserName extends Logging {

  // returns the purchaser's name for an agent without a client or when IQ3 is behalfOfPurchaser, otherwise the notifier's name
  def purchaserOrNotifierName(draft: DraftNotification, ctx: UserContext): Option[String] = {
    def sectionAnswers(sectionId: String): Option[JsObject] = draft.sections.get(sectionId).flatMap(_.data)

    val isOnBehalfOfPurchaser = sectionAnswers(SectionId.InitialQuestions)
      .flatMap(data => (data \ "notifyingAsPurchaser").asOpt[PurchaserOrOnBehalf])
      .contains(PurchaserOrOnBehalf.OnBehalfOfPurchaser)

    sectionAnswers(if (ctx.isAgentWithoutClient || isOnBehalfOfPurchaser) SectionId.PurchaserDetails else SectionId.NotifierDetails)
      .flatMap(businessOrPersonName)
  }

  def toDisplay(nameFromSections: Option[String], ctx: UserContext, summary: Option[NotificationSummary]): Option[String] =
    summary match {
      case Some(agentSummary: NotificationSummary.AgentWithClient) => agentSummary.clientTraderName.orElse(ctx.selectedClient.flatMap(_.name))
      case _ if ctx.isAgentWithClient                              => ctx.selectedClient.flatMap(_.name)
      case Some(traderSummary: NotificationSummary.IndividualOrOrganisation) if ctx.isVatRegisteredOrganisation =>
        nameFromSections.orElse(traderSummary.traderName)
      case _ => nameFromSections
    }

  def fetchTraderOrClientSummary(connector: NovaImportsBackendConnector, ctx: UserContext, clientVrn: Option[String])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[NotificationSummary]] =
    if (ctx.isVatRegisteredOrganisation || ctx.isAgentWithClient)
      connector.getNotificationSummary(clientVrn).map {
        case Right(summary) => Some(summary)
        case Left(error)    =>
          logger.warn(s"failed to fetch summary for the purchaser name: $error")
          None
      }
    else Future.successful(None)

  private def businessOrPersonName(data: JsObject): Option[String] = {
    def fieldValue(field: String): Option[String] = (data \ field).asOpt[String].map(_.trim).filter(_.nonEmpty)

    fieldValue("businessName").orElse {
      for {
        firstName <- fieldValue("firstName")
        lastName  <- fieldValue("lastName")
      } yield s"$firstName $lastName"
    }
  }
}
