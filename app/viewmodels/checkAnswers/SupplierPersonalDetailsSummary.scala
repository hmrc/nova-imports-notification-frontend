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

import models.{Address, NameDetails, UserAnswers}
import pages.sections.notifierDetails.{BusinessNamePage, NameDetailsPage}
import pages.sections.notifieraddress.AddressPage
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
    Seq(nameRow(answers), addressRow(answers)).flatten

  def hasPersonalDetails(answers: UserAnswers): Boolean =
    name(answers).isDefined && answers.get(AddressPage).isDefined

  private def nameRow(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    name(answers).map { value =>
      SummaryListRowViewModel(
        key = "usePersonalDetailsAsSupplier.name",
        value = ValueViewModel(HtmlContent(HtmlFormat.escape(value).body))
      )
    }

  private def addressRow(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AddressPage).map { address =>
      SummaryListRowViewModel(
        key = "usePersonalDetailsAsSupplier.address",
        value = ValueViewModel(HtmlContent(formatAddress(address)))
      )
    }

  private def name(answers: UserAnswers): Option[String] =
    answers.get(BusinessNamePage).orElse(answers.get(NameDetailsPage).map(formatName))

  private def formatName(name: NameDetails): String =
    Seq(name.title, name.firstName, name.lastName).filter(_.nonEmpty).mkString(" ")

  private def formatAddress(address: Address): String = {
    val countryLine = if (address.country.code == "GB") Seq.empty else Seq(address.country.name)
    (address.lines ++ address.postcode.toSeq ++ countryLine)
      .filter(_.nonEmpty)
      .map(part => HtmlFormat.escape(part).body)
      .mkString("<br>")
  }
}
