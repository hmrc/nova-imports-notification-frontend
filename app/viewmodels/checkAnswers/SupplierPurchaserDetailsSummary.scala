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
import pages.sections.purchaserDetails.{PurchaserBusinessNamePage, PurchaserNamePage}
import pages.sections.purchaseraddress.PurchaserAddressPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}

object SupplierPurchaserDetailsSummary {

  private val notProvidedKey = "usePurchaserDetailsAsSupplier.notProvided"

  def fromSession(answers: UserAnswers)(implicit messages: Messages): SummaryList =
    SummaryList(rows = sessionRows(answers))

  def sessionRows(answers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    val address  = answers.get(PurchaserAddressPage)
    val lines    = address.map(_.lines).getOrElse(Seq.empty)
    val lastPart = address.flatMap(a => AddressSummaryRows.lastAddressPart(a.country, a.postcode))

    AddressSummaryRows.rows(
      "usePurchaserDetailsAsSupplier.name",
      "usePurchaserDetailsAsSupplier.address",
      notProvidedKey,
      purchaserName(answers),
      AddressSummaryRows.addressDisplayLines(lines.lift(0), lines.lift(1), lines.lift(2), lines.lift(3), lastPart, notProvidedKey)
    )
  }

  private def purchaserName(answers: UserAnswers): Option[String] =
    answers.get(PurchaserBusinessNamePage).orElse(answers.get(PurchaserNamePage).map(formatName))

  private def formatName(name: NameDetails): String =
    Seq(name.title, name.firstName, name.lastName).filter(_.nonEmpty).mkString(" ")
}
