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
import controllers.notifieraddress.routes
import models.{Address, Country, UserAnswers}
import pages.sections.notifieraddress.AddressPage
import play.api.Application
import play.api.i18n.Messages

class AddressSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "AddressSummary" - {

    "must end a UK address with the postcode and never show the country name" in {
      val address = Address(
        lines = Seq("12 High Street", "Reading"),
        postcode = Some("RE12 9GC"),
        country = Country("GB", "United Kingdom")
      )

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(AddressPage, address)

      val result = AddressSummary.row(userAnswers).value
      val value  = result.value.content.asHtml.toString

      result.key.content.asHtml.toString must include(msgs("yourAddressCheckYourAnswers.checkYourAnswersLabel"))
      value mustBe "12 High Street<br>Reading<br>RE12 9GC"
      result.actions.value.items.head.href mustBe routes.YourAddressCheckYourAnswersController.onChangeAddress().url
    }

    "must format an overseas address with the postcode at end and then the country name afterwards" in {
      val address = Address(
        lines = Seq("Musterstrasse 12", "Mitte", "Berlin"),
        postcode = Some("10115"),
        country = Country("DE", "Germany")
      )

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(AddressPage, address)

      val value = AddressSummary.row(userAnswers).value.value.content.asHtml.toString

      value mustBe "Musterstrasse 12<br>Mitte<br>Berlin<br>10115<br>Germany"
    }

    "must return a summary without a postcode line for an overseas address that has none saved as optional" in {
      val address = Address(
        lines = Seq("24 Rue de Rivoli", "Paris"),
        postcode = None,
        country = Country("FR", "France")
      )

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(AddressPage, address)

      val value = AddressSummary.row(userAnswers).value.value.content.asHtml.toString

      value mustBe "24 Rue de Rivoli<br>Paris<br>France"
    }

    "must resolve an overseas country name from the ISO code when the stored name is empty" in {
      val address = Address(
        lines = Seq("Some Street", "Kabul"),
        postcode = None,
        country = Country("AF", "")
      )

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(AddressPage, address)

      val value = AddressSummary.row(userAnswers).value.value.content.asHtml.toString

      value mustBe "Some Street<br>Kabul<br>Afghanistan"
    }

    "must return None when the answer is not present" in {
      AddressSummary.row(UserAnswers(userAnswersId)) mustBe None
    }
  }
}
