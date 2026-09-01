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
import models.{Address, CheckMode, SupplierNumber, UserAnswers}
import pages.QuestionPage
import pages.sections.notifieraddress.AddressPage
import pages.sections.purchaseraddress.PurchaserAddressPage
import pages.sections.supplieraddress.SupplierAddressPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object SupplierAddressSummary {

  def rowFromPersonalDetails(answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages): Option[SummaryListRow] = {
    row(answers, AddressPage, routes.UsePersonalDetailsAsSupplierController.onPageLoad(supplierNumber, CheckMode).url)
  }

  def rowFromPurchaserDetails(answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages): Option[SummaryListRow] = {
    row(answers, PurchaserAddressPage, routes.UsePurchaserDetailsAsSupplierController.onPageLoad(supplierNumber, CheckMode).url)
  }

  def rowFromSupplierDetails(answers: UserAnswers, supplierNumber: SupplierNumber)(implicit messages: Messages): Option[SummaryListRow] = {
    row(answers, SupplierAddressPage(supplierNumber), routes.SupplierDetailsCheckYourAnswersController.onChangeAddress(supplierNumber).url)
  }
  
  //TODO: Add rowFromClientDetails once AVD-S1.2 page is added 

  private def row(answers: UserAnswers, addressPage: QuestionPage[Address], redirectUrl: String)(implicit
    messages: Messages
  ): Option[SummaryListRow] = {

    val value = answers.get(addressPage) match {
      case Some(address) =>
        val countryLine = if (address.country.code == "GB") None else Some(address.country.name)

        (address.lines ++ address.postcode.toSeq ++ countryLine)
          .filter(_.nonEmpty)
          .map(line => HtmlFormat.escape(line).body)
          .mkString("<br>")
      case None =>
        Seq(messages("supplierDetailsCheckYourAnswers.notProvided"))
          .map(part => HtmlFormat.escape(part).body)
          .mkString("<br>")
    }

    Some(
      SummaryListRowViewModel(
        key = "supplierAddressCheckYourAnswers.checkYourAnswersLabel",
        value = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          ActionItemViewModel("site.change", redirectUrl)
            .withVisuallyHiddenText(messages("supplierAddressCheckYourAnswers.change.hidden"))
        )
      )
    )
  }
}
