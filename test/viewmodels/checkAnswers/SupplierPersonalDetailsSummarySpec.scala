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
import pages.sections.notifieraddress.AddressPage
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

  "SupplierPersonalDetailsSummary" - {

    "must render the business name and a multi-line UK address (no country line) when both are present" in {
      val answers = emptyUserAnswers
        .unsafeSet(BusinessNamePage, "ABC Ltd")
        .unsafeSet(AddressPage, Address(Seq("23, North Road", "East London", "London"), Some("ER45 6UI"), Country("GB", "United Kingdom")))

      val rows = SupplierPersonalDetailsSummary.rows(answers)

      rows.size mustBe 2
      valueOf(rows.head) mustBe "ABC Ltd"
      valueOf(rows(1)) mustBe "23, North Road<br>East London<br>London<br>ER45 6UI"
    }

    "must fall back to the individual name when no business name is present" in {
      val answers = emptyUserAnswers
        .unsafeSet(NameDetailsPage, NameDetails("Mr", "John", "Smith"))
        .unsafeSet(AddressPage, Address(Seq("1 High Street"), Some("AB1 2CD"), Country("GB", "United Kingdom")))

      val rows = SupplierPersonalDetailsSummary.rows(answers)

      valueOf(rows.head) mustBe "Mr John Smith"
    }

    "must include the country line for a non-UK address" in {
      val answers = emptyUserAnswers
        .unsafeSet(BusinessNamePage, "ABC Ltd")
        .unsafeSet(AddressPage, Address(Seq("10 Rue de Paris"), None, Country("FR", "France")))

      val rows = SupplierPersonalDetailsSummary.rows(answers)

      valueOf(rows(1)) mustBe "10 Rue de Paris<br>France"
    }

    "must HTML-escape personal details" in {
      val answers = emptyUserAnswers.unsafeSet(BusinessNamePage, "A & B <Ltd>")

      val rows = SupplierPersonalDetailsSummary.rows(answers)

      valueOf(rows.head) mustBe "A &amp; B &lt;Ltd&gt;"
    }

    "must render both rows as 'Not provided' when the session holds no personal details" in {
      val rows = SupplierPersonalDetailsSummary.rows(emptyUserAnswers)

      rows.size mustBe 2
      valueOf(rows.head) mustBe "Not provided"
      valueOf(rows(1)) mustBe "Not provided"
    }

    "must render the name as 'Not provided' when only the address is present" in {
      val answers = emptyUserAnswers
        .unsafeSet(AddressPage, Address(Seq("1 High Street"), Some("AB1 2CD"), Country("GB", "United Kingdom")))

      val rows = SupplierPersonalDetailsSummary.rows(answers)

      valueOf(rows.head) mustBe "Not provided"
      valueOf(rows(1)) mustBe "1 High Street<br>AB1 2CD"
    }

    "must render the address as 'Not provided' when only the name is present" in {
      val answers = emptyUserAnswers.unsafeSet(BusinessNamePage, "ABC Ltd")

      val rows = SupplierPersonalDetailsSummary.rows(answers)

      valueOf(rows.head) mustBe "ABC Ltd"
      valueOf(rows(1)) mustBe "Not provided"
    }

    "must expose the rows as a SummaryList" in {
      val answers = emptyUserAnswers.unsafeSet(BusinessNamePage, "ABC Ltd")

      SupplierPersonalDetailsSummary.summaryList(answers).rows.size mustBe 2
    }
  }
}
