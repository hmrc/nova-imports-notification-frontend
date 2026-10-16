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

package viewmodels.checkAnswers

import base.SpecBase
import models.{AgentSelectedClient, NovaUserType, SupplierNumber, TraderInformation, UserContext}
import pages.sections.supplierdetails.UseClientDetailsAsSupplierPage
import play.api.Application
import play.api.i18n.Messages

class SupplierDetailsCheckYourAnswersHelperSpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  private val supplierNumber = SupplierNumber(1)

  private val clientDetails = TraderInformation(
    traderName = Some("RDS Client Ltd"),
    tradingName = None,
    addressLine1 = Some("1 Arundel Mews"),
    addressLine2 = None,
    addressLine3 = None,
    addressLine4 = None,
    postcode = Some("BN11 5RG")
  )

  private def agentContext(cachedName: Option[String]): UserContext =
    UserContext(
      userType = NovaUserType.Agent,
      selectedClient = Some(AgentSelectedClient("100000001", cachedName)),
      notDeregistered = true,
      isAgentWithClientNoEnrolments = false,
      agentHasVatAgentEnrolment = true,
      isForBusinessUse = false
    )

  private val usingClientDetails = emptyUserAnswers.unsafeSet(UseClientDetailsAsSupplierPage(supplierNumber), true)

  private def rowValues(userContext: UserContext, traderDetails: Option[TraderInformation]): Seq[String] =
    SupplierDetailsCheckYourAnswersHelper
      .buildSummaryList(userContext, usingClientDetails, supplierNumber, traderDetails)
      .rows
      .map(_.value.content.asHtml.toString)

  "SupplierDetailsCheckYourAnswersHelper when using client details" - {

    "must show the client name and address from the data cache" in {
      val values = rowValues(agentContext(Some("Stale Name Ltd")), Some(clientDetails))

      values.head          must include("RDS Client Ltd")
      values.mkString(" ") must include("1 Arundel Mews")
    }

    "must show the client name from the data cache when none was cached at client selection" in {
      rowValues(agentContext(None), Some(clientDetails)).head must include("RDS Client Ltd")
    }

    "must fall back to the cached client name when the lookup fails" in {
      rowValues(agentContext(Some("ABC Ltd")), None).head must include("ABC Ltd")
    }

    "must show not provided when there is no name from either source" in {
      rowValues(agentContext(None), None).head must include(msgs("supplierDetailsCheckYourAnswers.notProvided"))
    }
  }
}
