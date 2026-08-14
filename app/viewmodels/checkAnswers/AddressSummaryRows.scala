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

import models.Country
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import java.util.Locale

// Shared name/address summary-row rendering for the "use X's details as the supplier details" pages
// (e.g. usePersonalDetailsAsSupplier, usePurchaserDetailsAsSupplier), which differ only in their
// message-key prefix.
private[checkAnswers] object AddressSummaryRows {

  private val isoCountryCodes: Set[String] = Locale.getISOCountries.toSet

  def rows(nameKey: String, addressKey: String, notProvidedKey: String, name: Option[String], addressLines: Seq[String])(implicit
    messages: Messages
  ): Seq[SummaryListRow] =
    Seq(nameRow(nameKey, notProvidedKey, name), addressRow(addressKey, notProvidedKey, addressLines))

  private def nameRow(nameKey: String, notProvidedKey: String, name: Option[String])(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = nameKey,
      value = ValueViewModel(HtmlContent(name.map(HtmlFormat.escape(_).body).getOrElse(notProvided(notProvidedKey))))
    )

  private def addressRow(addressKey: String, notProvidedKey: String, addressLines: Seq[String])(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = addressKey,
      value = ValueViewModel(
        HtmlContent(
          if (addressLines.isEmpty) notProvided(notProvidedKey)
          else addressLines.map(line => HtmlFormat.escape(line).body).mkString("<br>")
        )
      )
    )

  // A UK address ends with the postcode; a non-UK address ends with the country name.
  def lastAddressPart(country: Country, postcode: Option[String]): Option[String] =
    if (country.code == "GB") postcode else Some(countryName(country))

  // Address lines render one per line. Lines 1 & 2 and the final part (postcode or country) show
  // "Not provided" when empty; lines 3 & 4 are dropped when empty. An address with no parts at all
  // yields an empty Seq, which addressRow renders as a single "Not provided".
  def addressDisplayLines(
    line1: Option[String],
    line2: Option[String],
    line3: Option[String],
    line4: Option[String],
    lastPart: Option[String],
    notProvidedKey: String
  )(implicit messages: Messages): Seq[String] = {
    val l1 = clean(line1)
    val l2 = clean(line2)
    val l3 = clean(line3)
    val l4 = clean(line4)
    val lp = clean(lastPart)

    if (Seq(l1, l2, l3, l4, lp).forall(_.isEmpty)) Seq.empty
    else
      Seq(
        Some(l1.getOrElse(messages(notProvidedKey))),
        Some(l2.getOrElse(messages(notProvidedKey))),
        l3,
        l4,
        Some(lp.getOrElse(messages(notProvidedKey)))
      ).flatten
  }

  private def clean(value: Option[String]): Option[String] = value.map(_.trim).filter(_.nonEmpty)

  private def notProvided(key: String)(implicit messages: Messages): String = HtmlFormat.escape(messages(key)).body

  // The stored address often carries only the ISO country code with an empty name, so resolve the
  // display name from the code. Fall back to the stored name (if any), then the raw code if unknown.
  private def countryName(country: Country): String = {
    val stored = country.name.trim
    if (stored.nonEmpty) stored
    else if (isoCountryCodes.contains(country.code)) new Locale("", country.code).getDisplayCountry(Locale.UK)
    else country.code
  }
}
