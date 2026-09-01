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

import controllers.supplierdetails.routes
import base.SpecBase
import models.{Address, CheckMode, Country, SupplierNumber, UserAnswers}
import pages.sections.notifieraddress.AddressPage
import pages.sections.purchaseraddress.PurchaserAddressPage
import pages.sections.supplieraddress.SupplierAddressPage
import play.api.Application
import play.api.i18n.Messages

class SupplierAddressSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "SupplierAddressSummary" - {

    //TODO: Add using client address details tests once AVD-S1.2 page is added

    "must end a UK address with the postcode and never show the country name when using personal address details" in {
      val address = Address(
        lines = Seq("12 High Street", "Reading"),
        postcode = Some("RE12 9GC"),
        country = Country("GB", "United Kingdom")
      )

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(AddressPage, address)

      val result = SupplierAddressSummary.rowFromPersonalDetails(userAnswers, SupplierNumber(1)).value
      val value  = result.value.content.asHtml.toString

      result.key.content.asHtml.toString must include(msgs("supplierAddressCheckYourAnswers.checkYourAnswersLabel"))
      value mustBe "12 High Street<br>Reading<br>RE12 9GC"
      result.actions.value.items.head.href mustBe routes.UsePersonalDetailsAsSupplierController.onPageLoad(SupplierNumber(1), CheckMode).url
    }

    "must end a UK address with the postcode and never show the country name when using purchaser address details" in {
      val address = Address(
        lines = Seq("13 High Street", "Reading"),
        postcode = Some("RE12 9GC"),
        country = Country("GB", "United Kingdom")
      )

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(PurchaserAddressPage, address)

      val result = SupplierAddressSummary.rowFromPurchaserDetails(userAnswers, SupplierNumber(2)).value
      val value  = result.value.content.asHtml.toString

      result.key.content.asHtml.toString must include(msgs("supplierAddressCheckYourAnswers.checkYourAnswersLabel"))
      value mustBe "13 High Street<br>Reading<br>RE12 9GC"
      result.actions.value.items.head.href mustBe routes.UsePurchaserDetailsAsSupplierController.onPageLoad(SupplierNumber(2), CheckMode).url
    }

    "must end a UK address with the postcode and never show the country name when using supplier address details" in {
      val address = Address(
        lines = Seq("14 High Street", "Reading"),
        postcode = Some("RE12 9GC"),
        country = Country("GB", "United Kingdom")
      )

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(SupplierAddressPage(SupplierNumber(3)), address)

      val result = SupplierAddressSummary.rowFromSupplierDetails(userAnswers, SupplierNumber(3)).value
      val value  = result.value.content.asHtml.toString

      result.key.content.asHtml.toString must include(msgs("supplierAddressCheckYourAnswers.checkYourAnswersLabel"))
      value mustBe "14 High Street<br>Reading<br>RE12 9GC"
      result.actions.value.items.head.href mustBe routes.SupplierDetailsCheckYourAnswersController.onChangeAddress(SupplierNumber(3)).url
    }

    "must format an overseas address with the postcode at end and then the country name afterwards when using personal address details" in {
      val address = Address(
        lines = Seq("Musterstrasse 12", "Mitte", "Berlin"),
        postcode = Some("10115"),
        country = Country("DE", "Germany")
      )

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(AddressPage, address)

      val value = SupplierAddressSummary.rowFromPersonalDetails(userAnswers, SupplierNumber(1)).value.value.content.asHtml.toString

      value mustBe "Musterstrasse 12<br>Mitte<br>Berlin<br>10115<br>Germany"
    }

    "must format an overseas address with the postcode at end and then the country name afterwards when using purchaser address details" in {
      val address = Address(
        lines = Seq("Musterstrasse 13", "Mitte", "Berlin"),
        postcode = Some("10115"),
        country = Country("DE", "Germany")
      )

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(PurchaserAddressPage, address)

      val value = SupplierAddressSummary.rowFromPurchaserDetails(userAnswers, SupplierNumber(2)).value.value.content.asHtml.toString

      value mustBe "Musterstrasse 13<br>Mitte<br>Berlin<br>10115<br>Germany"
    }

    "must format an overseas address with the postcode at end and then the country name afterwards when using supplier address details" in {
      val address = Address(
        lines = Seq("Musterstrasse 14", "Mitte", "Berlin"),
        postcode = Some("10115"),
        country = Country("DE", "Germany")
      )

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(SupplierAddressPage(SupplierNumber(3)), address)

      val value = SupplierAddressSummary.rowFromSupplierDetails(userAnswers, SupplierNumber(3)).value.value.content.asHtml.toString

      value mustBe "Musterstrasse 14<br>Mitte<br>Berlin<br>10115<br>Germany"
    }

    "must return a summary without a postcode line for an overseas address that has none saved as optional when using personal address details" in {
      val address = Address(
        lines = Seq("24 Rue de Rivoli", "Paris"),
        postcode = None,
        country = Country("FR", "France")
      )

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(AddressPage, address)

      val value = SupplierAddressSummary.rowFromPersonalDetails(userAnswers, SupplierNumber(1)).value.value.content.asHtml.toString

      value mustBe "24 Rue de Rivoli<br>Paris<br>France"
    }

    "must return a summary without a postcode line for an overseas address that has none saved as optional when using purchaser address details" in {
      val address = Address(
        lines = Seq("25 Rue de Rivoli", "Paris"),
        postcode = None,
        country = Country("FR", "France")
      )

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(PurchaserAddressPage, address)

      val value = SupplierAddressSummary.rowFromPurchaserDetails(userAnswers, SupplierNumber(2)).value.value.content.asHtml.toString

      value mustBe "25 Rue de Rivoli<br>Paris<br>France"
    }

    "must return a summary without a postcode line for an overseas address that has none saved as optional when using supplier address details" in {
      val address = Address(
        lines = Seq("24 Rue de Rivoli", "Paris"),
        postcode = None,
        country = Country("FR", "France")
      )

      val userAnswers = UserAnswers(userAnswersId).unsafeSet(SupplierAddressPage(SupplierNumber(3)), address)

      val value = SupplierAddressSummary.rowFromSupplierDetails(userAnswers, SupplierNumber(3)).value.value.content.asHtml.toString

      value mustBe "24 Rue de Rivoli<br>Paris<br>France"
    }

    "must return Not Provided when the answer is not present in personal address details" in {
      val result = SupplierAddressSummary.rowFromPersonalDetails(UserAnswers(userAnswersId), SupplierNumber(1)).value
      val value  = result.value.content.asHtml.toString
      result.key.content.asHtml.toString must include(msgs("supplierAddressCheckYourAnswers.checkYourAnswersLabel"))
      value                              must include(msgs("supplierDetailsCheckYourAnswers.notProvided"))
    }
    "must return Not Provided when the answer is not present in purchaser address details" in {
      val result = SupplierAddressSummary.rowFromPurchaserDetails(UserAnswers(userAnswersId), SupplierNumber(2)).value
      val value  = result.value.content.asHtml.toString
      result.key.content.asHtml.toString must include(msgs("supplierAddressCheckYourAnswers.checkYourAnswersLabel"))
      value                              must include(msgs("supplierDetailsCheckYourAnswers.notProvided"))
    }
    "must return Not Provided when the answer is not present in supplier address details" in {
      val result = SupplierAddressSummary.rowFromSupplierDetails(UserAnswers(userAnswersId), SupplierNumber(3)).value
      val value  = result.value.content.asHtml.toString
      result.key.content.asHtml.toString must include(msgs("supplierAddressCheckYourAnswers.checkYourAnswersLabel"))
      value                              must include(msgs("supplierDetailsCheckYourAnswers.notProvided"))
    }
  }
}
