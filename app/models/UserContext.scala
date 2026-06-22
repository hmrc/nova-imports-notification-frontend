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

import pages.{AgentSelectedClientPage, IsDeregisteredPage}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}

final case class UserContext(
  userType: NovaUserType,
  selectedClient: Option[AgentSelectedClient],
  isDeregistered: Boolean,
  isAgentWithNoEnrolments: Boolean
) {
  def isAgent: Boolean                     = userType == NovaUserType.Agent
  def isAgentWithClient: Boolean           = isAgent && selectedClient.isDefined
  def isAgentWithoutClient: Boolean        = isAgent && selectedClient.isEmpty
  def isVatRegisteredOrganisation: Boolean = userType == NovaUserType.VatRegisteredOrganisation
}

object UserContext {

  def from(affinityGroup: AffinityGroup, enrolments: Enrolments, userAnswers: UserAnswers): UserContext =
    UserContext(
      userType = NovaUserType.from(affinityGroup, enrolments),
      selectedClient = userAnswers.get(AgentSelectedClientPage),
      isDeregistered = userAnswers.get(IsDeregisteredPage).getOrElse(false),
      isAgentWithNoEnrolments = affinityGroup == AffinityGroup.Agent && !enrolments.enrolments.exists(_.isActivated)
    )

  val agentMustHaveClient: UserContext => Boolean =
    ctx => !ctx.isAgent || ctx.selectedClient.isDefined
}
