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

import models.{SupplierNumber, UserAnswers, UserContext}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.govuk.summarylist.*

object SupplierDetailsCheckYourAnswersHelper {

  def buildSummaryList(userContext: UserContext, answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(rows = buildRows(answers, supplierNumber))

  private def buildRows(answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages) =
    Seq(
      SupplierBusinessOrIndividualSummary.row(answers, supplierNumber),
      SupplierBusinessNameSummary.row(answers, supplierNumber), // TODO: Unit tests, see other Summary examples
      SupplierNameSummary.row(answers, supplierNumber),
      SupplierAddressSummary.row(answers, supplierNumber), // TODO: Unit tests, see other Summary examples
      SupplierVatRegisteredSummary.row(answers, supplierNumber), // TODO: Unit tests, see other Summary examples
      SupplierVatRegistrationNumberSummary.row(answers, supplierNumber) // TODO: Unit tests, see other Summary examples
    ).flatten
}
