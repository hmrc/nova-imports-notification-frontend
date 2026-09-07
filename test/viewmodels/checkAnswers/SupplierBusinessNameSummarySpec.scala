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
import controllers.supplierdetails.routes
import models.BusinessOrPrivateIndividual.{Business, PrivateIndividual}
import models.PurchaserBusinessOrIndividual.{NonVatRegisteredBusiness, NonVatRegisteredPrivateIndividual}
import models.{CheckMode, NormalMode, SupplierNumber, UserAnswers}
import pages.sections.initialquestions.{BusinessOrPrivatePage, PurchaserBusinessOrIndividualPage}
import pages.sections.notifierdetails.BusinessNamePage
import pages.sections.purchaserdetails.PurchaserBusinessNamePage
import pages.sections.supplierdetails.{SupplierBusinessNamePage, SupplierBusinessOrIndividualPage}
import play.api.Application
import play.api.i18n.Messages

class SupplierBusinessNameSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "SupplierBusinessNameSummary" - {

    "must return a summary row with the business name and a change link when using personal details for a Business" in {
      val userAnswers = UserAnswers(userAnswersId)
        .unsafeSet(BusinessNamePage, "Acme Trading Ltd")
        .unsafeSet(BusinessOrPrivatePage, Business)

      val result = SupplierBusinessNameSummary.rowFromPersonalDetails(userAnswers, SupplierNumber(1)).value

      result.key.content.asHtml.toString   must include(msgs("supplierBusinessName.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include("Acme Trading Ltd")
      result.actions.value.items.head.href mustBe routes.SupplierBusinessNameController.onPageLoad(SupplierNumber(1), CheckMode).url
    }

    "must return a summary row with the business name and a change link when using purchaser details for a Business" in {
      val userAnswers = UserAnswers(userAnswersId)
        .unsafeSet(PurchaserBusinessNamePage, "Acme Trading Ltd")
        .unsafeSet(PurchaserBusinessOrIndividualPage, NonVatRegisteredBusiness)

      val result = SupplierBusinessNameSummary.rowFromPurchaserDetails(userAnswers, SupplierNumber(1)).value

      result.key.content.asHtml.toString   must include(msgs("supplierBusinessName.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include("Acme Trading Ltd")
      result.actions.value.items.head.href mustBe routes.SupplierBusinessNameController.onPageLoad(SupplierNumber(1), CheckMode).url
    }

    "must return a summary row with the business name and a change link when using supplier details for a Business" in {
      val userAnswers = UserAnswers(userAnswersId)
        .unsafeSet(SupplierBusinessNamePage(SupplierNumber(1)), "Acme Trading Ltd")
        .unsafeSet(SupplierBusinessOrIndividualPage(SupplierNumber(1)), Business)

      val result = SupplierBusinessNameSummary.rowFromSupplierDetails(userAnswers, SupplierNumber(1)).value

      result.key.content.asHtml.toString   must include(msgs("supplierBusinessName.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include("Acme Trading Ltd")
      result.actions.value.items.head.href mustBe routes.SupplierBusinessNameController.onPageLoad(SupplierNumber(1), CheckMode).url
    }

    "must return None when the own details are not a Business" in {
      val userAnswers = UserAnswers(userAnswersId)
        .unsafeSet(BusinessNamePage, "Acme Trading Ltd")
        .unsafeSet(BusinessOrPrivatePage, PrivateIndividual)
      SupplierBusinessNameSummary.rowFromPersonalDetails(userAnswers, SupplierNumber(1)) mustBe None
    }

    "must return None when the purchaser is not a Business" in {
      val userAnswers = UserAnswers(userAnswersId)
        .unsafeSet(PurchaserBusinessNamePage, "Acme Trading Ltd")
        .unsafeSet(PurchaserBusinessOrIndividualPage, NonVatRegisteredPrivateIndividual)
      SupplierBusinessNameSummary.rowFromPurchaserDetails(userAnswers, SupplierNumber(1)) mustBe None
    }

    "must return None when the supplier is not a Business" in {
      val userAnswers = UserAnswers(userAnswersId)
        .unsafeSet(SupplierBusinessNamePage(SupplierNumber(1)), "Acme Trading Ltd")
        .unsafeSet(SupplierBusinessOrIndividualPage(SupplierNumber(1)), PrivateIndividual)
      SupplierBusinessNameSummary.rowFromSupplierDetails(userAnswers, SupplierNumber(1)) mustBe None
    }

    "must return Not Provided when the answer is not present when using own business name details" in {
      val userAnswers = UserAnswers(userAnswersId)
        .unsafeSet(BusinessOrPrivatePage, Business)

      val result = SupplierBusinessNameSummary.rowFromPersonalDetails(userAnswers, SupplierNumber(1)).value
      val value  = result.value.content.asHtml.toString
      result.key.content.asHtml.toString must include(msgs("supplierBusinessName.checkYourAnswersLabel"))
      value                              must include(msgs("supplierDetailsCheckYourAnswers.notProvided"))
    }

    "must return Not Provided when the answer is not present when using purchaser business name details" in {
      val userAnswers = UserAnswers(userAnswersId)
        .unsafeSet(PurchaserBusinessOrIndividualPage, NonVatRegisteredBusiness)

      val result = SupplierBusinessNameSummary.rowFromPurchaserDetails(userAnswers, SupplierNumber(1)).value
      val value  = result.value.content.asHtml.toString
      result.key.content.asHtml.toString must include(msgs("supplierBusinessName.checkYourAnswersLabel"))
      value                              must include(msgs("supplierDetailsCheckYourAnswers.notProvided"))
    }

    "must return Not Provided when the answer is not present when using supplier business name details" in {
      val userAnswers = UserAnswers(userAnswersId)
        .unsafeSet(SupplierBusinessOrIndividualPage(SupplierNumber(1)), Business)

      val result = SupplierBusinessNameSummary.rowFromSupplierDetails(userAnswers, SupplierNumber(1)).value
      val value  = result.value.content.asHtml.toString
      result.key.content.asHtml.toString must include(msgs("supplierBusinessName.checkYourAnswersLabel"))
      value                              must include(msgs("supplierDetailsCheckYourAnswers.notProvided"))
    }

  }
}
