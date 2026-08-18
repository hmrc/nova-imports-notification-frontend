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
import models.{Address, Country, NameDetails, TraderInformation}
import org.scalatest.BeforeAndAfterAll
import pages.sections.notifieraddress.AddressPage
import pages.sections.notifierdetails.{BusinessNamePage, NameDetailsPage}
import play.api.Application
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SupplierPersonalDetailsSummarySpec extends SpecBase with BeforeAndAfterAll {

  val app: Application        = applicationBuilder().build()
  implicit val msgs: Messages = messages(app)

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private def valueOf(row: SummaryListRow): String = row.value.content.asHtml.body

  private val traderInformation: TraderInformation = TraderInformation(
    traderName = Some("ABC LTD"),
    tradingName = Some("ABC Trading"),
    addressLine1 = Some("1 High Street"),
    addressLine2 = Some("Testtown"),
    addressLine3 = None,
    addressLine4 = None,
    postcode = Some("TF3 4ER")
  )

  "SupplierPersonalDetailsSummary" - {

    "must render the business name and a multi-line UK address (no country line) when both are present" in {
      val answers = emptyUserAnswers
        .unsafeSet(BusinessNamePage, "ABC Ltd")
        .unsafeSet(AddressPage, Address(Seq("23, North Road", "East London", "London"), Some("ER45 6UI"), Country("GB", "United Kingdom")))

      val rows = SupplierPersonalDetailsSummary.sessionRows(answers)

      rows.size mustBe 2
      valueOf(rows.head) mustBe "ABC Ltd"
      valueOf(rows(1)) mustBe "23, North Road<br>East London<br>London<br>ER45 6UI"
    }

    "must fall back to the individual name when no business name is present" in {
      val answers = emptyUserAnswers
        .unsafeSet(NameDetailsPage, NameDetails("Mr", "John", "Smith"))
        .unsafeSet(AddressPage, Address(Seq("1 High Street"), Some("AB1 2CD"), Country("GB", "United Kingdom")))

      val rows = SupplierPersonalDetailsSummary.sessionRows(answers)

      valueOf(rows.head) mustBe "Mr John Smith"
    }

    "must end a non-UK address with the country (never the postcode) and mark empty lines 1 & 2 'Not provided'" in {
      val answers = emptyUserAnswers
        .unsafeSet(BusinessNamePage, "ABC Ltd")
        .unsafeSet(AddressPage, Address(Seq("10 Rue de Paris"), Some("75000"), Country("FR", "France")))

      val rows = SupplierPersonalDetailsSummary.sessionRows(answers)

      valueOf(rows(1)) mustBe "10 Rue de Paris<br>Not provided<br>France"
    }

    "must resolve a non-UK country name from the ISO code when the stored name is empty" in {
      val answers = emptyUserAnswers
        .unsafeSet(AddressPage, Address(Seq("Some Street", "Kabul"), None, Country("AF", "")))

      valueOf(SupplierPersonalDetailsSummary.sessionRows(answers)(msgs)(1)) mustBe "Some Street<br>Kabul<br>Afghanistan"
    }

    "must fall back to the raw country code when it is not a resolvable ISO code" in {
      val answers = emptyUserAnswers
        .unsafeSet(AddressPage, Address(Seq("10 Rue de Paris"), None, Country("ZZ", "")))

      valueOf(SupplierPersonalDetailsSummary.sessionRows(answers)(msgs)(1)) mustBe "10 Rue de Paris<br>Not provided<br>ZZ"
    }

    "must show 'Not provided' for empty lines 1 & 2 but omit empty lines 3 & 4" in {
      val answers = emptyUserAnswers
        .unsafeSet(AddressPage, Address(Seq("1", "2"), None, Country("AF", "Afghanistan")))

      valueOf(SupplierPersonalDetailsSummary.sessionRows(answers)(msgs)(1)) mustBe "1<br>2<br>Afghanistan"
    }

    "must HTML-escape personal details" in {
      val answers = emptyUserAnswers.unsafeSet(BusinessNamePage, "A & B <Ltd>")

      val rows = SupplierPersonalDetailsSummary.sessionRows(answers)

      valueOf(rows.head) mustBe "A &amp; B &lt;Ltd&gt;"
    }

    "must render both rows as 'Not provided' when the session holds no personal details" in {
      val rows = SupplierPersonalDetailsSummary.sessionRows(emptyUserAnswers)

      rows.size mustBe 2
      valueOf(rows.head) mustBe "Not provided"
      valueOf(rows(1)) mustBe "Not provided"
    }

    "must render the name as 'Not provided' when only the address is present" in {
      val answers = emptyUserAnswers
        .unsafeSet(AddressPage, Address(Seq("1 High Street"), Some("AB1 2CD"), Country("GB", "United Kingdom")))

      val rows = SupplierPersonalDetailsSummary.sessionRows(answers)

      valueOf(rows.head) mustBe "Not provided"
      valueOf(rows(1)) mustBe "1 High Street<br>Not provided<br>AB1 2CD"
    }

    "must render the address as 'Not provided' when only the name is present" in {
      val answers = emptyUserAnswers.unsafeSet(BusinessNamePage, "ABC Ltd")

      val rows = SupplierPersonalDetailsSummary.sessionRows(answers)

      valueOf(rows.head) mustBe "ABC Ltd"
      valueOf(rows(1)) mustBe "Not provided"
    }

    "must expose the rows as a SummaryList" in {
      val answers = emptyUserAnswers.unsafeSet(BusinessNamePage, "ABC Ltd")

      SupplierPersonalDetailsSummary.fromSession(answers).rows.size mustBe 2
    }

    "when rendering from the RDS trader record" - {

      "must render the trader name and address" in {
        val rows = SupplierPersonalDetailsSummary.traderRows(Some(traderInformation))

        rows.size mustBe 2
        valueOf(rows.head) mustBe "ABC LTD"
        valueOf(rows(1)) mustBe "1 High Street<br>Testtown<br>TF3 4ER"
      }

      "must fall back to the trading name when the record holds no trader name" in {
        val rows = SupplierPersonalDetailsSummary.traderRows(Some(traderInformation.copy(traderName = None)))

        valueOf(rows.head) mustBe "ABC Trading"
      }

      "must show 'Not provided' for an empty line 2 and postcode, while omitting empty lines 3 & 4" in {
        val sparse = traderInformation.copy(addressLine2 = None, postcode = None)

        valueOf(SupplierPersonalDetailsSummary.traderRows(Some(sparse))(msgs)(1)) mustBe "1 High Street<br>Not provided<br>Not provided"
      }

      "must render both rows as 'Not provided' when there is no trader record" in {
        val rows = SupplierPersonalDetailsSummary.traderRows(None)

        rows.size mustBe 2
        valueOf(rows.head) mustBe "Not provided"
        valueOf(rows(1)) mustBe "Not provided"
      }

      "must HTML-escape the trader name" in {
        val answers = traderInformation.copy(traderName = Some("A & B <Ltd>"))

        valueOf(SupplierPersonalDetailsSummary.traderRows(Some(answers)).head) mustBe "A &amp; B &lt;Ltd&gt;"
      }
    }
  }
}
