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

  // Every field renders on its own row so partial addresses are visible; the UK flow ends with the
  // postcode, a non-UK flow with the country. Missing fields fall back to "Not provided".
  def rows(answers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] =
    nameRow(answers) +: addressRows(answers)

  private def nameRow(answers: UserAnswers)(implicit messages: Messages): SummaryListRow =
    row("usePersonalDetailsAsSupplier.name", name(answers))

  private def addressRows(answers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    val address = answers.get(AddressPage)
    val lines   = address.map(_.lines).getOrElse(Seq.empty)

    val lineRows = (1 to 4).map(i => row(s"usePersonalDetailsAsSupplier.addressLine.$i", lines.lift(i - 1)))

    val lastRow =
      if (isUk(answers)) row("usePersonalDetailsAsSupplier.postcode", address.flatMap(_.postcode))
      else row("usePersonalDetailsAsSupplier.country", address.map(_.country.name))

    lineRows :+ lastRow
  }

  // Prefer the "Is your address in the UK?" answer; fall back to the stored country (GB, or absent, is UK).
  private def isUk(answers: UserAnswers): Boolean =
    answers.get(IsYourAddressInTheUkPage).getOrElse(answers.get(AddressPage).forall(_.country.code == "GB"))

  private def row(key: String, value: Option[String])(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = key,
      value = ValueViewModel(HtmlContent(value.filter(_.nonEmpty).map(HtmlFormat.escape(_).body).getOrElse(notProvided)))
    )

  private def notProvided(implicit messages: Messages): String =
    HtmlFormat.escape(messages("usePersonalDetailsAsSupplier.notProvided")).body

  private def name(answers: UserAnswers): Option[String] =
    answers.get(BusinessNamePage).orElse(answers.get(NameDetailsPage).map(formatName))

  private def formatName(name: NameDetails): String =
    Seq(name.title, name.firstName, name.lastName).filter(_.nonEmpty).mkString(" ")
}
