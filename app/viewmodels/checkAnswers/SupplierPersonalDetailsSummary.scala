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

import models.{NameDetails, UserAnswers}
import pages.sections.notifierDetails.{BusinessNamePage, NameDetailsPage}
import pages.sections.notifieraddress.{AddressPage, IsYourAddressInTheUkPage}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

// TODO (F21): user types 4 & 5 should get name/address from the RDS DataCache GetTraderInformation
// lookup; until then all types render from the session. Swap the data source here when F21 is built.
object SupplierPersonalDetailsSummary {

  def summaryList(answers: UserAnswers)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(rows = rows(answers))

  def rows(answers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(nameRow(answers), addressRow(answers))

  private def nameRow(answers: UserAnswers)(implicit messages: Messages): SummaryListRow =
    row("usePersonalDetailsAsSupplier.name", name(answers).map(escape).getOrElse(notProvided))

  // The address collapses into a single comma-separated row. Lines 1 & 2 and the postcode/country show
  // "Not provided" when empty; lines 3 & 4 are dropped when empty. UK addresses end with the postcode,
  // non-UK addresses with the country. An entirely empty address collapses to a single "Not provided".
  private def addressRow(answers: UserAnswers)(implicit messages: Messages): SummaryListRow = {
    val address = answers.get(AddressPage)
    val lines   = address.map(_.lines).getOrElse(Seq.empty)

    def lineAt(i: Int): Option[String] = lines.lift(i).filter(_.nonEmpty).map(escape)

    val lastPart =
      (if (isUk(answers)) address.flatMap(_.postcode) else address.map(_.country.name)).filter(_.nonEmpty).map(escape)

    val providedParts = Seq(lineAt(0), lineAt(1), lineAt(2), lineAt(3), lastPart)

    val value =
      if (providedParts.forall(_.isEmpty)) notProvided
      else
        Seq(
          Some(lineAt(0).getOrElse(notProvided)),
          Some(lineAt(1).getOrElse(notProvided)),
          lineAt(2),
          lineAt(3),
          Some(lastPart.getOrElse(notProvided))
        ).flatten.mkString(", ")

    row("usePersonalDetailsAsSupplier.address", value)
  }

  // Prefer the "Is your address in the UK?" answer; fall back to the stored country (GB, or absent, is UK).
  private def isUk(answers: UserAnswers): Boolean =
    answers.get(IsYourAddressInTheUkPage).getOrElse(answers.get(AddressPage).forall(_.country.code == "GB"))

  private def row(key: String, valueHtml: String)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = key,
      value = ValueViewModel(HtmlContent(valueHtml))
    )

  private def escape(value: String): String = HtmlFormat.escape(value).body

  private def notProvided(implicit messages: Messages): String =
    escape(messages("usePersonalDetailsAsSupplier.notProvided"))

  private def name(answers: UserAnswers): Option[String] =
    answers.get(BusinessNamePage).orElse(answers.get(NameDetailsPage).map(formatName))

  private def formatName(name: NameDetails): String =
    Seq(name.title, name.firstName, name.lastName).filter(_.nonEmpty).mkString(" ")
}
