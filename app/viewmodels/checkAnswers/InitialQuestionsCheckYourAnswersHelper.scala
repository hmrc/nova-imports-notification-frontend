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

import models.{NovaUserType, PurchaserOrOnBehalf, UserAnswers, UserContext}
import pages.sections.initialquestions.PurchaserOrOnBehalfPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.govuk.summarylist.*

object InitialQuestionsCheckYourAnswersHelper {

  def buildSummaryList(userContext: UserContext, answers: UserAnswers)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(rows = buildRows(userContext, answers))

  private def buildRows(userContext: UserContext, answers: UserAnswers)(implicit messages: Messages) =
    userContext.userType match {
      case NovaUserType.VatRegisteredOrganisation =>
        Seq(
          VehicleFromEuSummary.row(answers),
          VehicleBusinessUseSummary.row(answers)
        ).flatten

      case NovaUserType.Agent if userContext.isAgentWithClient =>
        Seq(
          VehicleFromEuSummary.row(answers),
          AgentVehicleBusinessUseSummary.row(answers)
        ).flatten

      case _ =>
        val conditionalRow = answers.get(PurchaserOrOnBehalfPage) match {
          case Some(PurchaserOrOnBehalf.OnBehalfOfPurchaser) => PurchaserBusinessOrIndividualSummary.row(answers)
          case _                                             => None
        }
        Seq(
          VehicleFromEuSummary.row(answers),
          BusinessPrivateSummary.row(answers),
          PurchaseOrOnBehalfSummary.row(answers),
          conditionalRow
        ).flatten
    }
}
