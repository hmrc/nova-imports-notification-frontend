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
import pages.QuestionPage
import pages.sections.supplierdetails.{UsePersonalDetailsAsSupplierPage, UsePurchaserDetailsAsSupplierPage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.govuk.summarylist.*

object SupplierDetailsCheckYourAnswersHelper {

  def buildSummaryList(userContext: UserContext, answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(rows = buildRows(answers, supplierNumber))

  private def buildRows(answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages) = {

    if (isUsingPersonalDetails(answers, supplierNumber)) {
      Seq(
        SupplierNameSummary.rowFromPersonalDetails(answers, supplierNumber),
        SupplierAddressSummary.rowFromPersonalDetails(answers, supplierNumber),
      ).flatten
    } else if (isUsingPurchaserDetails(answers, supplierNumber)) {
      Seq(
        SupplierNameSummary.rowFromPurchaserDetails(answers, supplierNumber), //TODO: Can pass in the page / name details. Will also need to determine the change redirect page
        SupplierAddressSummary.rowFromPurchaserDetails(answers, supplierNumber),
      ).flatten
    } else {
      //TODO: Will the missing rows just be skipped avoiding displaying the extra fields or will that be an issue?
      Seq(
        SupplierBusinessOrIndividualSummary.row(answers, supplierNumber),
        SupplierBusinessNameSummary.row(answers, supplierNumber), // TODO: Unit tests, see other Summary examples
        SupplierNameSummary.rowFromSupplierDetails(answers, supplierNumber), // TODO: Update Unit tests. Already existed but will need to change with new row methods
        SupplierAddressSummary.rowFromSupplierDetails(answers, supplierNumber), // TODO: Unit tests, see other Summary examples. Also do all 3 row types
        SupplierVatRegisteredSummary.row(answers, supplierNumber), // TODO: Unit tests, see other Summary examples
        SupplierVatRegistrationNumberSummary.row(answers, supplierNumber) // TODO: Unit tests, see other Summary examples
      ).flatten
    }

  }


  private def isAnswerTrue(answers: UserAnswers, page: QuestionPage[Boolean]): Boolean = answers.get(page).contains(true)

  private def isUsingPersonalDetails(answers: UserAnswers, supplierNumber: SupplierNumber): Boolean = {
    isAnswerTrue(answers, UsePersonalDetailsAsSupplierPage(supplierNumber))
  }
  private def isUsingPurchaserDetails(answers: UserAnswers, supplierNumber: SupplierNumber): Boolean = {
    isAnswerTrue(answers, UsePurchaserDetailsAsSupplierPage(supplierNumber))
  }


}
