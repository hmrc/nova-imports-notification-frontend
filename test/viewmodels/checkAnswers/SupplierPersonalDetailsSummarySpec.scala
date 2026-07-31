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

    "must render one row per field for a full UK address, ending with the postcode" in {
      val answers = emptyUserAnswers
        .unsafeSet(BusinessNamePage, "ABC Ltd")
        .unsafeSet(
          AddressPage,
          Address(Seq("23, North Road", "East London", "London", "Greater London"), Some("ER45 6UI"), Country("GB", "United Kingdom"))
        )

      val rows = SupplierPersonalDetailsSummary.rows(answers)

      rows.map(keyOf) mustBe Seq("Name", "Address line 1", "Address line 2", "Address line 3", "Address line 4", "Postcode")
      rows.map(valueOf) mustBe Seq("ABC Ltd", "23, North Road", "East London", "London", "Greater London", "ER45 6UI")
    }

    "must end a non-UK address with the country (and no postcode row) even if a postcode is held" in {
      val answers = emptyUserAnswers
        .unsafeSet(BusinessNamePage, "ABC Ltd")
        .unsafeSet(AddressPage, Address(Seq("10 Rue de Paris"), Some("75000"), Country("FR", "France")))

      val rows = SupplierPersonalDetailsSummary.rows(answers)

      rows.map(keyOf) mustBe Seq("Name", "Address line 1", "Address line 2", "Address line 3", "Address line 4", "Country")
      valueOf(rows.last) mustBe "France"
    }

    "must fall back to the individual name when no business name is present" in {
      val answers = emptyUserAnswers
        .unsafeSet(NameDetailsPage, NameDetails("Mr", "John", "Smith"))
        .unsafeSet(AddressPage, Address(Seq("1 High Street"), Some("AB1 2CD"), Country("GB", "United Kingdom")))

      val rows = SupplierPersonalDetailsSummary.rows(answers)

      valueOf(rows.head) mustBe "Mr John Smith"
    }

    "must render only the provided lines and mark the rest 'Not provided'" in {
      val answers = emptyUserAnswers
        .unsafeSet(AddressPage, Address(Seq("1", "2"), None, Country("AF", "Afghanistan")))

      val rows = SupplierPersonalDetailsSummary.rows(answers)

      rows.map(valueOf) mustBe Seq("Not provided", "1", "2", "Not provided", "Not provided", "Afghanistan")
    }

    "must use the 'Is your address in the UK?' answer over the stored country" in {
      val ukAnswerNonGbCountry = emptyUserAnswers
        .unsafeSet(IsYourAddressInTheUkPage, true)
        .unsafeSet(AddressPage, Address(Seq("1 High Street"), Some("AB1 2CD"), Country("FR", "France")))

      SupplierPersonalDetailsSummary.rows(ukAnswerNonGbCountry).map(keyOf).last mustBe "Postcode"

      val nonUkAnswerGbCountry = emptyUserAnswers
        .unsafeSet(IsYourAddressInTheUkPage, false)
        .unsafeSet(AddressPage, Address(Seq("1 High Street"), Some("AB1 2CD"), Country("GB", "United Kingdom")))

      SupplierPersonalDetailsSummary.rows(nonUkAnswerGbCountry).map(keyOf).last mustBe "Country"
    }

    "must HTML-escape personal details" in {
      val answers = emptyUserAnswers.unsafeSet(BusinessNamePage, "A & B <Ltd>")

      val rows = SupplierPersonalDetailsSummary.rows(answers)

      valueOf(rows.head) mustBe "A &amp; B &lt;Ltd&gt;"
    }

    "must render every row as 'Not provided' when the session holds no personal details" in {
      val rows = SupplierPersonalDetailsSummary.rows(emptyUserAnswers)

      rows.map(valueOf) mustBe Seq.fill(6)("Not provided")
    }

    "must expose the rows as a SummaryList" in {
      val answers = emptyUserAnswers.unsafeSet(BusinessNamePage, "ABC Ltd")

      SupplierPersonalDetailsSummary.summaryList(answers).rows.size mustBe 6
    }
  }
}
