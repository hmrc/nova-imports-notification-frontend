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

import base.SpecBase
import pages.sections.initialquestions.VehicleBusinessUsePage
import pages.sections.introduction.NotDeregisteredPage
import pages.AgentSelectedClientPage
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, EnrolmentIdentifier, Enrolments}

class UserContextSpec extends SpecBase {

  private val noEnrolments = Enrolments(Set.empty)

  private val vatEnrolment = Enrolments(
    Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123")), "Activated"))
  )

  private val activeAgentEnrolment = Enrolments(
    Set(Enrolment("HMCE-VAT-AGNT", Seq(EnrolmentIdentifier("AgentRefNo", "AB123")), "Activated"))
  )

  private val inactiveAgentEnrolment = Enrolments(
    Set(Enrolment("HMCE-VAT-AGNT", Seq(EnrolmentIdentifier("AgentRefNo", "AB123")), "NotYetActivated"))
  )

  private val sampleClient = AgentSelectedClient("GB123456789", Some("Acme Ltd"))

  private def answersWith(client: AgentSelectedClient): UserAnswers =
    emptyUserAnswers.set(AgentSelectedClientPage, client).success.value

  "UserContext.from" - {

    "classifies an Individual affinity group as PrivateIndividual with no client" in {
      val ctx = UserContext.from(AffinityGroup.Individual, noEnrolments, emptyUserAnswers)
      ctx.userType mustEqual NovaUserType.PrivateIndividual
      ctx.selectedClient mustBe empty
      ctx.isAgent mustBe false
      ctx.isAgentWithClient mustBe false
      ctx.isAgentWithoutClient mustBe false
    }

    "classifies an Organisation with VAT enrolment as VatRegisteredOrganisation" in {
      val ctx = UserContext.from(AffinityGroup.Organisation, vatEnrolment, emptyUserAnswers)
      ctx.userType mustEqual NovaUserType.VatRegisteredOrganisation
    }

    "classifies an Organisation without VAT enrolment as NonVatOrganisation" in {
      val ctx = UserContext.from(AffinityGroup.Organisation, noEnrolments, emptyUserAnswers)
      ctx.userType mustEqual NovaUserType.NonVatOrganisation
    }

    "classifies an Agent without a selected client as isAgentWithoutClient" in {
      val ctx = UserContext.from(AffinityGroup.Agent, noEnrolments, emptyUserAnswers)
      ctx.userType mustEqual NovaUserType.Agent
      ctx.isAgentWithoutClient mustBe true
      ctx.isAgentWithClient mustBe false
    }

    "classifies an Agent with a selected client as isAgentWithClient" in {
      val ctx = UserContext.from(AffinityGroup.Agent, noEnrolments, answersWith(sampleClient))
      ctx.isAgentWithClient mustBe true
      ctx.isAgentWithoutClient mustBe false
      ctx.selectedClient must contain(sampleClient)
    }

    "defaults notDeregistered to true when the answer is not present" in {
      val ctx = UserContext.from(AffinityGroup.Organisation, vatEnrolment, emptyUserAnswers)
      ctx.notDeregistered mustBe true
    }

    "reads notDeregistered as false when the answer is present and false" in {
      val answers = emptyUserAnswers.set(NotDeregisteredPage, false).success.value
      val ctx     = UserContext.from(AffinityGroup.Organisation, vatEnrolment, answers)
      ctx.notDeregistered mustBe false
    }

    "marks an Agent with a selected client and no enrolments as isAgentWithClientNoEnrolments" in {
      val ctx = UserContext.from(AffinityGroup.Agent, noEnrolments, answersWith(sampleClient))
      ctx.isAgentWithClientNoEnrolments mustBe true
    }

    "marks an Agent with a selected client that has enrolment but not activated as isAgentWithClientNoEnrolments" in {
      val ctx = UserContext.from(AffinityGroup.Agent, inactiveAgentEnrolment, answersWith(sampleClient))
      ctx.isAgentWithClientNoEnrolments mustBe true
    }

    "does not mark an Agent with a selected client and an active enrolment as isAgentWithClientNoEnrolments" in {
      val ctx = UserContext.from(AffinityGroup.Agent, activeAgentEnrolment, answersWith(sampleClient))
      ctx.isAgentWithClientNoEnrolments mustBe false
    }

    "does not mark an Agent with no enrolments and no selected client as isAgentWithClientNoEnrolments" in {
      val ctx = UserContext.from(AffinityGroup.Agent, noEnrolments, emptyUserAnswers)
      ctx.isAgentWithClientNoEnrolments mustBe false
    }

    "does not mark a non-Agent as isAgentWithClientNoEnrolments" in {
      val ctx = UserContext.from(AffinityGroup.Organisation, noEnrolments, emptyUserAnswers)
      ctx.isAgentWithClientNoEnrolments mustBe false
    }

    "marks an agent with an activated HMCE-VAT-AGNT enrolment and no client as isVatAgentWithoutClient" in {
      val ctx = UserContext.from(AffinityGroup.Agent, activeAgentEnrolment, emptyUserAnswers)
      ctx.isVatAgentWithoutClient mustBe true
      ctx.isNonVatAgentWithoutClient mustBe false
      ctx.agentHasVatAgentEnrolment mustBe true
    }

    "marks an agent with no enrolments and no client as isNonVatAgentWithoutClient" in {
      val ctx = UserContext.from(AffinityGroup.Agent, noEnrolments, emptyUserAnswers)
      ctx.isNonVatAgentWithoutClient mustBe true
      ctx.isVatAgentWithoutClient mustBe false
    }

    "does not treat an inactive HMCE-VAT-AGNT enrolment as a VAT agent" in {
      val ctx = UserContext.from(AffinityGroup.Agent, inactiveAgentEnrolment, emptyUserAnswers)
      ctx.isNonVatAgentWithoutClient mustBe true
      ctx.agentHasVatAgentEnrolment mustBe false
    }

    "does not mark a non-agent as agentHasVatAgentEnrolment" in {
      val ctx = UserContext.from(AffinityGroup.Organisation, vatEnrolment, emptyUserAnswers)
      ctx.agentHasVatAgentEnrolment mustBe false
    }

    "both VAT and non-VAT agent-without-client predicates are false when a client is selected" in {
      val ctx = UserContext.from(AffinityGroup.Agent, activeAgentEnrolment, answersWith(sampleClient))
      ctx.isVatAgentWithoutClient mustBe false
      ctx.isNonVatAgentWithoutClient mustBe false
    }
  }

  "UserContext.usesTraderDetails" - {

    "is true for a VAT-registered organisation importing for business use" in {
      val answers = emptyUserAnswers.unsafeSet(VehicleBusinessUsePage, true)
      UserContext.from(AffinityGroup.Organisation, vatEnrolment, answers).usesTraderDetails mustBe true
    }

    "is false for a VAT-registered organisation not importing for business use" in {
      val answers = emptyUserAnswers.unsafeSet(VehicleBusinessUsePage, false)
      UserContext.from(AffinityGroup.Organisation, vatEnrolment, answers).usesTraderDetails mustBe false
    }

    "is false for a VAT-registered organisation that has not answered whether it is for business use" in {
      UserContext.from(AffinityGroup.Organisation, vatEnrolment, emptyUserAnswers).usesTraderDetails mustBe false
    }

    "is false for a private individual, even when importing for business use" in {
      val answers = emptyUserAnswers.unsafeSet(VehicleBusinessUsePage, true)
      UserContext.from(AffinityGroup.Individual, noEnrolments, answers).usesTraderDetails mustBe false
    }
  }

  "UserContext.agentMustHaveClient predicate" - {

    "allows a non-agent regardless of client" in {
      val individual = UserContext.from(AffinityGroup.Individual, noEnrolments, emptyUserAnswers)
      UserContext.agentMustHaveClient(individual) mustBe true
    }

    "rejects an agent without a selected client" in {
      val agent = UserContext.from(AffinityGroup.Agent, noEnrolments, emptyUserAnswers)
      UserContext.agentMustHaveClient(agent) mustBe false
    }

    "allows an agent with a selected client" in {
      val agent = UserContext.from(AffinityGroup.Agent, noEnrolments, answersWith(sampleClient))
      UserContext.agentMustHaveClient(agent) mustBe true
    }
  }
}
