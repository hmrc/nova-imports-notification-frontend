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
import pages.SelectedClientPage
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, EnrolmentIdentifier, Enrolments}

class UserContextSpec extends SpecBase {

  private val noEnrolments = Enrolments(Set.empty)

  private val vatEnrolment = Enrolments(
    Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123")), "Activated"))
  )

  private val sampleClient = SelectedClient("GB123456789", Some("Acme Ltd"))

  private def answersWith(client: SelectedClient): UserAnswers =
    emptyUserAnswers.set(SelectedClientPage, client).success.value

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
