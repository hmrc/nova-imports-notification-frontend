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
import models.{CheckMode, NameDetails, SupplierNumber, UserAnswers}
import pages.QuestionPage
import pages.sections.notifierdetails.NameDetailsPage
import pages.sections.purchaserdetails.PurchaserNamePage
import pages.sections.supplierdetails.SupplierNamePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object SupplierNameSummary {

  def rowFromPersonalDetails(answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages): Option[SummaryListRow] = {
    row(answers, NameDetailsPage, routes.UsePersonalDetailsAsSupplierController.onPageLoad(supplierNumber, CheckMode).url)
  }

  def rowFromPurchaserDetails(answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages): Option[SummaryListRow] = {
    row(answers, PurchaserNamePage, routes.UsePurchaserDetailsAsSupplierController.onPageLoad(supplierNumber, CheckMode).url)
  }

  def rowFromSupplierDetails(answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages): Option[SummaryListRow] = {
    row(answers, SupplierNamePage(supplierNumber), routes.SupplierNameController.onPageLoad(supplierNumber, CheckMode).url)
  }

  private def row(answers: UserAnswers, namePage: QuestionPage[NameDetails], redirectUrl: String)(implicit
    messages: Messages
  ): Option[SummaryListRow] = {
    answers.get(namePage).map { name =>

      val value = Seq(name.title, name.firstName, name.lastName)
        .map(part => HtmlFormat.escape(part).body)
        .mkString("<br>")

      SummaryListRowViewModel(
        key = "supplierName.checkYourAnswersLabel",
        value = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          ActionItemViewModel("site.change", redirectUrl)
            .withVisuallyHiddenText(messages("supplierName.change.hidden"))
        )
      )
    }
  }
}
