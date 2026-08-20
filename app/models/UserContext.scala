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

import pages.sections.initialquestions.VehicleBusinessUsePage
import pages.{AgentSelectedClientPage, IsDeregisteredPage}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}

final case class UserContext(
  userType: NovaUserType,
  selectedClient: Option[AgentSelectedClient],
  isDeregistered: Boolean,
  isAgentWithClientNoEnrolments: Boolean,
  agentHasVatAgentEnrolment: Boolean,
  isForBusinessUse: Boolean
) {
  def isAgent: Boolean                     = userType == NovaUserType.Agent
  def isAgentWithClient: Boolean           = isAgent && selectedClient.isDefined
  def isAgentWithoutClient: Boolean        = isAgent && selectedClient.isEmpty
  def isVatRegisteredOrganisation: Boolean = userType == NovaUserType.VatRegisteredOrganisation
  def isVatAgentWithoutClient: Boolean     = isAgentWithoutClient && agentHasVatAgentEnrolment
  def isNonVatAgentWithoutClient: Boolean  = isAgentWithoutClient && !agentHasVatAgentEnrolment

  def usesTraderDetails: Boolean = isVatRegisteredOrganisation && isForBusinessUse
}

object UserContext {

  def from(affinityGroup: AffinityGroup, enrolments: Enrolments, userAnswers: UserAnswers): UserContext = {
    val selectedClient = userAnswers.get(AgentSelectedClientPage)

    UserContext(
      userType = NovaUserType.from(affinityGroup, enrolments),
      selectedClient = selectedClient,
      isDeregistered = userAnswers.get(IsDeregisteredPage).getOrElse(false),
      isAgentWithClientNoEnrolments =
        affinityGroup == AffinityGroup.Agent && selectedClient.isDefined && !enrolments.enrolments.exists(_.isActivated),
      agentHasVatAgentEnrolment = affinityGroup == AffinityGroup.Agent && enrolments.getEnrolment("HMCE-VAT-AGNT").exists(_.isActivated),
      isForBusinessUse = userAnswers.get(VehicleBusinessUsePage).getOrElse(false)
    )
  }

  val agentMustHaveClient: UserContext => Boolean =
    ctx => !ctx.isAgent || ctx.selectedClient.isDefined
}
