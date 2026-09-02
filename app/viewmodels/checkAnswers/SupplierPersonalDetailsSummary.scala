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

import models.{TraderInformation, UserAnswers}
import pages.sections.notifierdetails.{BusinessNamePage, NameDetailsPage}
import pages.sections.notifieraddress.AddressPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.{AddressDisplay, AddressLines}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object SupplierPersonalDetailsSummary {

  def fromSession(answers: UserAnswers)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(rows = sessionRows(answers))

  def fromTraderInformation(traderInformation: Option[TraderInformation])(implicit messages: Messages): SummaryList =
    SummaryListViewModel(rows = traderRows(traderInformation))

  def sessionRows(answers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    val address = answers.get(AddressPage)
    val lines   = address.map(_.lines).getOrElse(Seq.empty)

    rows(
      sessionName(answers),
      AddressDisplay.paddedLines(
        lines = AddressLines.from(lines),
        postcode = address.flatMap(_.postcode),
        country = address.map(_.country),
        notProvided = notProvidedText
      )
    )
  }

  def traderRows(traderInformation: Option[TraderInformation])(implicit messages: Messages): Seq[SummaryListRow] =
    rows(
      traderInformation.flatMap(_.name),
      AddressDisplay.paddedLines(
        lines = AddressLines(
          line1 = traderInformation.flatMap(_.addressLine1),
          line2 = traderInformation.flatMap(_.addressLine2),
          line3 = traderInformation.flatMap(_.addressLine3),
          line4 = traderInformation.flatMap(_.addressLine4)
        ),
        postcode = traderInformation.flatMap(_.postcode),
        country = None,
        notProvided = notProvidedText
      )
    )

  private def rows(name: Option[String], addressLines: Seq[String])(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(nameRow(name), addressRow(addressLines))

  private def nameRow(name: Option[String])(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = "usePersonalDetailsAsSupplier.name",
      value = ValueViewModel(HtmlContent(name.map(HtmlFormat.escape(_).body).getOrElse(notProvided)))
    )

  private def addressRow(addressLines: Seq[String])(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = "usePersonalDetailsAsSupplier.address",
      value = ValueViewModel(
        HtmlContent(
          if (addressLines.isEmpty) notProvided
          else addressLines.map(line => HtmlFormat.escape(line).body).mkString("<br>")
        )
      )
    )

  private def notProvided(implicit messages: Messages): String = HtmlFormat.escape(notProvidedText).body

  private def notProvidedText(implicit messages: Messages): String = messages("usePersonalDetailsAsSupplier.notProvided")

  private def sessionName(answers: UserAnswers): Option[String] =
    answers.get(BusinessNamePage).orElse(answers.get(NameDetailsPage).map(_.displayName))
}
