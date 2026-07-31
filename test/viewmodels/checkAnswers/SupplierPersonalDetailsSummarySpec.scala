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
import pages.sections.notifieraddress.{AddressPage, IsYourAddressInTheUkPage}
import pages.sections.notifierDetails.{BusinessNamePage, NameDetailsPage}
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
  private def keyOf(row: SummaryListRow): String   = row.key.content.asHtml.body

  "SupplierPersonalDetailsSummary" - {

    "must render a name row and a single comma-separated address row" in {
      val answers = emptyUserAnswers
        .unsafeSet(BusinessNamePage, "ABC Ltd")
        .unsafeSet(
          AddressPage,
          Address(Seq("23, North Road", "East London", "London", "Greater London"), Some("ER45 6UI"), Country("GB", "United Kingdom"))
        )

      val rows = SupplierPersonalDetailsSummary.rows(answers)

      rows.map(keyOf) mustBe Seq("Name", "Address")
      valueOf(rows.head) mustBe "ABC Ltd"
      valueOf(rows(1)) mustBe "23, North Road, East London, London, Greater London, ER45 6UI"
    }

    "must end a UK address with the postcode" in {
      val answers = emptyUserAnswers
        .unsafeSet(AddressPage, Address(Seq("1 High Street"), Some("AB1 2CD"), Country("GB", "United Kingdom")))

      valueOf(SupplierPersonalDetailsSummary.rows(answers)(msgs)(1)) mustBe "1 High Street, Not provided, AB1 2CD"
    }

    "must end a non-UK address with the country (never the postcode) even if a postcode is held" in {
      val answers = emptyUserAnswers
        .unsafeSet(AddressPage, Address(Seq("10 Rue de Paris"), Some("75000"), Country("FR", "France")))

      valueOf(SupplierPersonalDetailsSummary.rows(answers)(msgs)(1)) mustBe "10 Rue de Paris, Not provided, France"
    }

    "must fall back to the individual name when no business name is present" in {
      val answers = emptyUserAnswers
        .unsafeSet(NameDetailsPage, NameDetails("Mr", "John", "Smith"))
        .unsafeSet(AddressPage, Address(Seq("1 High Street"), Some("AB1 2CD"), Country("GB", "United Kingdom")))

      valueOf(SupplierPersonalDetailsSummary.rows(answers).head) mustBe "Mr John Smith"
    }

    "must show 'Not provided' for lines 1 & 2 but omit empty lines 3 & 4" in {
      val answers = emptyUserAnswers
        .unsafeSet(AddressPage, Address(Seq("1", "2"), None, Country("AF", "Afghanistan")))

      valueOf(SupplierPersonalDetailsSummary.rows(answers)(msgs)(1)) mustBe "1, 2, Afghanistan"
    }

    "must show 'Not provided' for missing lines 1 & 2 while still omitting empty lines 3 & 4" in {
      val answers = emptyUserAnswers
        .unsafeSet(AddressPage, Address(Seq.empty, None, Country("AF", "Afghanistan")))

      valueOf(SupplierPersonalDetailsSummary.rows(answers)(msgs)(1)) mustBe "Not provided, Not provided, Afghanistan"
    }

    "must use the 'Is your address in the UK?' answer over the stored country" in {
      val ukAnswerNonGbCountry = emptyUserAnswers
        .unsafeSet(IsYourAddressInTheUkPage, true)
        .unsafeSet(AddressPage, Address(Seq("1 High Street"), Some("AB1 2CD"), Country("FR", "France")))

      valueOf(SupplierPersonalDetailsSummary.rows(ukAnswerNonGbCountry)(msgs)(1)) mustBe "1 High Street, Not provided, AB1 2CD"

      val nonUkAnswerGbCountry = emptyUserAnswers
        .unsafeSet(IsYourAddressInTheUkPage, false)
        .unsafeSet(AddressPage, Address(Seq("1 High Street"), Some("AB1 2CD"), Country("GB", "United Kingdom")))

      valueOf(SupplierPersonalDetailsSummary.rows(nonUkAnswerGbCountry)(msgs)(1)) mustBe "1 High Street, Not provided, United Kingdom"
    }

    "must HTML-escape personal details" in {
      val answers = emptyUserAnswers
        .unsafeSet(BusinessNamePage, "A & B <Ltd>")
        .unsafeSet(AddressPage, Address(Seq("A & B"), None, Country("GB", "United Kingdom")))

      val rows = SupplierPersonalDetailsSummary.rows(answers)

      valueOf(rows.head) mustBe "A &amp; B &lt;Ltd&gt;"
      valueOf(rows(1)) mustBe "A &amp; B, Not provided, Not provided"
    }

    "must collapse an entirely empty address to a single 'Not provided'" in {
      val rows = SupplierPersonalDetailsSummary.rows(emptyUserAnswers)

      rows.map(keyOf) mustBe Seq("Name", "Address")
      valueOf(rows.head) mustBe "Not provided"
      valueOf(rows(1)) mustBe "Not provided"
    }

    "must collapse to a single 'Not provided' when an address exists but holds no values" in {
      val answers = emptyUserAnswers
        .unsafeSet(AddressPage, Address(Seq.empty, None, Country("GB", "United Kingdom")))

      valueOf(SupplierPersonalDetailsSummary.rows(answers)(msgs)(1)) mustBe "Not provided"
    }

    "must expose the rows as a SummaryList" in {
      val answers = emptyUserAnswers.unsafeSet(BusinessNamePage, "ABC Ltd")

      SupplierPersonalDetailsSummary.summaryList(answers).rows.size mustBe 2
    }
  }
}
