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
import models.{NameDetails, NormalMode, SupplierNumber, UserAnswers}
import pages.QuestionPage
import pages.sections.initialquestions.{BusinessOrPrivatePage, PurchaserBusinessOrIndividualPage}
import pages.sections.notifierdetails.{BusinessNamePage, NameDetailsPage}
import pages.sections.purchaserdetails.{PurchaserBusinessNamePage, PurchaserNamePage}
import pages.sections.supplierdetails.{SupplierBusinessOrIndividualPage, SupplierNamePage}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object SupplierNameSummary {

  def rowFromPersonalDetails(answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages): Option[SummaryListRow] = {
    val nameValue = if (answers.get(BusinessOrPrivatePage).contains(Business)) {
      answers.get(BusinessNamePage).getOrElse("")
    } else {
      extractNameDetailsValue(answers, NameDetailsPage)
    }
    row(nameValue, routes.UsePersonalDetailsAsSupplierController.onPageLoad(supplierNumber, NormalMode).url)
  }

  def rowFromPurchaserDetails(answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages): Option[SummaryListRow] = {
    val nameValue = if (answers.get(PurchaserBusinessOrIndividualPage).contains(NonVatRegisteredBusiness)) {
      answers.get(PurchaserBusinessNamePage).getOrElse("")
    } else {
      extractNameDetailsValue(answers, PurchaserNamePage)
    }
    row(nameValue, routes.UsePurchaserDetailsAsSupplierController.onPageLoad(supplierNumber, NormalMode).url)
  }

  def rowFromSupplierDetails(answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages): Option[SummaryListRow] = {
    if (answers.get(SupplierBusinessOrIndividualPage(supplierNumber)).contains(PrivateIndividual)) {
      val nameValue = extractNameDetailsValue(answers, SupplierNamePage(supplierNumber))
      row(nameValue, routes.SupplierNameController.onPageLoad(supplierNumber, NormalMode).url)
    } else {
      None
    }
  }

  // TODO: Add rowFromClientDetails once AVD-S1.2 page is added

  private def row(name: String, redirectUrl: String)(implicit
    messages: Messages
  ): Option[SummaryListRow] = {
    Some(
      SummaryListRowViewModel(
        key = "supplierName.checkYourAnswersLabel",
        value = ValueViewModel(HtmlContent(name)),
        actions = Seq(
          ActionItemViewModel("site.change", redirectUrl)
            .withVisuallyHiddenText(messages("supplierName.change.hidden"))
        )
      )
    )
  }

  private def extractNameDetailsValue(answers: UserAnswers, nameDetailsPage: QuestionPage[NameDetails])(implicit messages: Messages) = {
    answers.get(nameDetailsPage) match {
      case Some(name) =>
        Seq(name.title, name.firstName, name.lastName)
          .map(part => HtmlFormat.escape(part).body)
          .mkString("<br>")
      case None =>
        Seq(messages("supplierDetailsCheckYourAnswers.notProvided"))
          .map(part => HtmlFormat.escape(part).body)
          .mkString("<br>")
    }
  }

}
