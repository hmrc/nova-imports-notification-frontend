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

import controllers.supplierdetails.routes
import models.BusinessOrPrivateIndividual.{Business, PrivateIndividual}
import models.PurchaserBusinessOrIndividual.NonVatRegisteredBusiness
import models.{CheckMode, SupplierNumber, UserAnswers}
import pages.QuestionPage
import pages.sections.initialquestions.{BusinessOrPrivatePage, PurchaserBusinessOrIndividualPage}
import pages.sections.notifierdetails.BusinessNamePage
import pages.sections.purchaserdetails.PurchaserBusinessNamePage
import pages.sections.supplierdetails.{SupplierBusinessNamePage, SupplierBusinessOrIndividualPage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object SupplierBusinessNameSummary {

  def rowFromPersonalDetails(answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages): Option[SummaryListRow] = {
    if (answers.get(BusinessOrPrivatePage).contains(Business)) {
      val nameValue = extractNameDetailsValue(answers, BusinessNamePage)
      Some(row(nameValue, supplierNumber))
    } else {
      None
    }
  }

  def rowFromPurchaserDetails(answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages): Option[SummaryListRow] = {
    if (answers.get(PurchaserBusinessOrIndividualPage).contains(NonVatRegisteredBusiness)) {
      val nameValue = extractNameDetailsValue(answers, PurchaserBusinessNamePage)
      Some(row(nameValue, supplierNumber))
    } else {
      None
    }
  }

  def rowFromSupplierDetails(answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages): Option[SummaryListRow] = {
    if (answers.get(SupplierBusinessOrIndividualPage(supplierNumber)).contains(Business)) {
      val nameValue = extractNameDetailsValue(answers, SupplierBusinessNamePage(supplierNumber))
      Some(row(nameValue, supplierNumber))
    } else {
      None
    }
  }

  // TODO: Add rowFromClientDetails once AVD-S1.2 page is added

  private def row(supplierBusinessName: String, supplierNumber: SupplierNumber)(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key = "supplierBusinessName.checkYourAnswersLabel",
      value = ValueViewModel(Text(supplierBusinessName)),
      actions = Seq(
        ActionItemViewModel("site.change", routes.SupplierBusinessNameController.onPageLoad(supplierNumber, CheckMode).url)
          .withVisuallyHiddenText(messages("supplierBusinessName.change.hidden"))
      )
    )
  }

  private def extractNameDetailsValue(answers: UserAnswers, businessNameDetailsPage: QuestionPage[String])(implicit messages: Messages) = {
    answers.get(businessNameDetailsPage) match {
      case Some(name) =>
        name
      case None =>
        messages("supplierDetailsCheckYourAnswers.notProvided")
    }
  }

}
