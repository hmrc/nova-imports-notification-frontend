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

import base.SpecBase
import models.{Address, Country, NameDetails}
import org.scalatest.BeforeAndAfterAll
import pages.sections.purchaserDetails.{PurchaserBusinessNamePage, PurchaserNamePage}
import pages.sections.purchaseraddress.PurchaserAddressPage
import play.api.Application
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SupplierPurchaserDetailsSummarySpec extends SpecBase with BeforeAndAfterAll {

  val app: Application        = applicationBuilder().build()
  implicit val msgs: Messages = messages(app)

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private def valueOf(row: SummaryListRow): String = row.value.content.asHtml.body

  "SupplierPurchaserDetailsSummary" - {

    "must render the business name and a multi-line UK address (no country line) when both are present" in {
      val answers = emptyUserAnswers
        .unsafeSet(PurchaserBusinessNamePage, "ABC Ltd")
        .unsafeSet(
          PurchaserAddressPage,
          Address(Seq("1 Arundel Mews", "Sunnymede", "Worthing", "West Sussex"), Some("BN11 5RG"), Country("GB", "United Kingdom"))
        )

      val rows = SupplierPurchaserDetailsSummary.sessionRows(answers)

      rows.size mustBe 2
      valueOf(rows.head) mustBe "ABC Ltd"
      valueOf(rows(1)) mustBe "1 Arundel Mews<br>Sunnymede<br>Worthing<br>West Sussex<br>BN11 5RG"
    }

    "must fall back to the individual name when no business name is present" in {
      val answers = emptyUserAnswers
        .unsafeSet(PurchaserNamePage, NameDetails("Mr", "John", "Smith"))
        .unsafeSet(PurchaserAddressPage, Address(Seq("1 High Street"), Some("AB1 2CD"), Country("GB", "United Kingdom")))

      val rows = SupplierPurchaserDetailsSummary.sessionRows(answers)

      valueOf(rows.head) mustBe "Mr John Smith"
    }

    "must end a non-UK address with the country (never the postcode)" in {
      val answers = emptyUserAnswers
        .unsafeSet(PurchaserBusinessNamePage, "ABC Ltd")
        .unsafeSet(PurchaserAddressPage, Address(Seq("10 Rue de Paris"), Some("75000"), Country("FR", "France")))

      val rows = SupplierPurchaserDetailsSummary.sessionRows(answers)

      valueOf(rows(1)) mustBe "10 Rue de Paris<br>Not provided<br>France"
    }

    "must render both rows as 'Not provided' when the session holds no purchaser details" in {
      val rows = SupplierPurchaserDetailsSummary.sessionRows(emptyUserAnswers)

      rows.size mustBe 2
      valueOf(rows.head) mustBe "Not provided"
      valueOf(rows(1)) mustBe "Not provided"
    }

    "must HTML-escape purchaser details" in {
      val answers = emptyUserAnswers.unsafeSet(PurchaserBusinessNamePage, "A & B <Ltd>")

      val rows = SupplierPurchaserDetailsSummary.sessionRows(answers)

      valueOf(rows.head) mustBe "A &amp; B &lt;Ltd&gt;"
    }

    "must expose the rows as a SummaryList" in {
      val answers = emptyUserAnswers.unsafeSet(PurchaserBusinessNamePage, "ABC Ltd")

      SupplierPurchaserDetailsSummary.fromSession(answers).rows.size mustBe 2
    }
  }
}
