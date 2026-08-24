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

import models.{Country, NameDetails, UserAnswers}
import pages.sections.purchaserdetails.{PurchaserBusinessNamePage, PurchaserNamePage}
import pages.sections.purchaseraddress.PurchaserAddressPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import java.util.Locale

object SupplierPurchaserDetailsSummary {

  def fromSession(answers: UserAnswers)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(rows = sessionRows(answers))

  def sessionRows(answers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    val address = answers.get(PurchaserAddressPage)
    val lines   = address.map(_.lines).getOrElse(Seq.empty)
    // A UK address ends with the postcode; a non-UK address ends with the country name.
    val lastPart = address.flatMap(a => if (a.country.code == "GB") a.postcode else Some(countryName(a.country)))

    rows(purchaserName(answers), addressDisplayLines(lines.lift(0), lines.lift(1), lines.lift(2), lines.lift(3), lastPart))
  }

  private def rows(name: Option[String], addressLines: Seq[String])(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(nameRow(name), addressRow(addressLines))

  private def nameRow(name: Option[String])(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = "usePurchaserDetailsAsSupplier.name",
      value = ValueViewModel(HtmlContent(name.map(HtmlFormat.escape(_).body).getOrElse(notProvided)))
    )

  private def addressRow(addressLines: Seq[String])(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = "usePurchaserDetailsAsSupplier.address",
      value = ValueViewModel(
        HtmlContent(
          if (addressLines.isEmpty) notProvided
          else addressLines.map(line => HtmlFormat.escape(line).body).mkString("<br>")
        )
      )
    )

  // Address lines render one per line. Lines 1 & 2 and the final part (postcode or country) show
  // "Not provided" when empty; lines 3 & 4 are dropped when empty. An address with no parts at all
  // yields an empty Seq, which addressRow renders as a single "Not provided".
  private def addressDisplayLines(
    line1: Option[String],
    line2: Option[String],
    line3: Option[String],
    line4: Option[String],
    lastPart: Option[String]
  )(implicit messages: Messages): Seq[String] = {
    val l1 = clean(line1)
    val l2 = clean(line2)
    val l3 = clean(line3)
    val l4 = clean(line4)
    val lp = clean(lastPart)

    if (Seq(l1, l2, l3, l4, lp).forall(_.isEmpty)) Seq.empty
    else Seq(Some(l1.getOrElse(notProvidedText)), Some(l2.getOrElse(notProvidedText)), l3, l4, Some(lp.getOrElse(notProvidedText))).flatten
  }

  private def clean(value: Option[String]): Option[String] = value.map(_.trim).filter(_.nonEmpty)

  private def notProvided(implicit messages: Messages): String = HtmlFormat.escape(notProvidedText).body

  private def notProvidedText(implicit messages: Messages): String = messages("usePurchaserDetailsAsSupplier.notProvided")

  private def purchaserName(answers: UserAnswers): Option[String] =
    answers.get(PurchaserBusinessNamePage).orElse(answers.get(PurchaserNamePage).map(formatName))

  private def formatName(name: NameDetails): String =
    Seq(name.title, name.firstName, name.lastName).filter(_.nonEmpty).mkString(" ")

  private val isoCountryCodes: Set[String] = Locale.getISOCountries.toSet

  // The stored address often carries only the ISO country code with an empty name, so resolve the
  // display name from the code. Fall back to the stored name (if any), then the raw code if unknown.
  private def countryName(country: Country): String = {
    val stored = country.name.trim
    if (stored.nonEmpty) stored
    else if (isoCountryCodes.contains(country.code)) new Locale("", country.code).getDisplayCountry(Locale.UK)
    else country.code
  }
}
