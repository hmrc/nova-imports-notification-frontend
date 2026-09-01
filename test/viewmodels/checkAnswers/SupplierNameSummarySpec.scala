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
import models.{CheckMode, NameDetails, SupplierNumber, UserAnswers}
import pages.sections.notifierdetails.NameDetailsPage
import pages.sections.purchaserdetails.PurchaserNamePage
import pages.sections.supplierdetails.SupplierNamePage
import play.api.Application
import play.api.i18n.Messages

class SupplierNameSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "SupplierNameSummary" - {

    //TODO: Add using client name details tests once AVD-S1.2 page is added

    "must return a summary with the name parts stacked on separate lines and a single change link using personal name details" in {
      val userAnswers =
        UserAnswers(userAnswersId).set(NameDetailsPage, NameDetails("Mr", "John", "Smith")).success.value

      val result = SupplierNameSummary.rowFromPersonalDetails(userAnswers, SupplierNumber(1)).value
      val value  = result.value.content.asHtml.toString

      result.key.content.asHtml.toString must include(msgs("supplierName.checkYourAnswersLabel"))
      value                              must (include("Mr") and include("John") and include("Smith") and include("<br>"))
      result.actions.value.items.head.href mustBe routes.UsePersonalDetailsAsSupplierController.onPageLoad(SupplierNumber(1), CheckMode).url
    }

    "must return a summary with the name parts stacked on separate lines and a single change link using purchaser name details" in {
      val userAnswers =
        UserAnswers(userAnswersId).set(PurchaserNamePage, NameDetails("Mr", "Adam", "Smith")).success.value

      val result = SupplierNameSummary.rowFromPurchaserDetails(userAnswers, SupplierNumber(2)).value
      val value  = result.value.content.asHtml.toString

      result.key.content.asHtml.toString must include(msgs("supplierName.checkYourAnswersLabel"))
      value                              must (include("Mr") and include("Adam") and include("Smith") and include("<br>"))
      result.actions.value.items.head.href mustBe routes.UsePurchaserDetailsAsSupplierController.onPageLoad(SupplierNumber(2), CheckMode).url
    }

    "must return a summary with the name parts stacked on separate lines and a single change link using supplier name details" in {
      val userAnswers =
        UserAnswers(userAnswersId).set(SupplierNamePage(SupplierNumber(3)), NameDetails("Mr", "Tom", "Smith")).success.value

      val result = SupplierNameSummary.rowFromSupplierDetails(userAnswers, SupplierNumber(3)).value
      val value  = result.value.content.asHtml.toString

      result.key.content.asHtml.toString must include(msgs("supplierName.checkYourAnswersLabel"))
      value                              must (include("Mr") and include("Tom") and include("Smith") and include("<br>"))
      result.actions.value.items.head.href mustBe routes.SupplierNameController.onPageLoad(SupplierNumber(3), CheckMode).url
    }

    "must return Not Provided when the answer is not present when using personal name details" in {
      val result = SupplierNameSummary.rowFromPersonalDetails(UserAnswers(userAnswersId), SupplierNumber(1)).value
      val value  = result.value.content.asHtml.toString
      result.key.content.asHtml.toString must include(msgs("supplierName.checkYourAnswersLabel"))
      value                              must include(msgs("supplierDetailsCheckYourAnswers.notProvided"))
    }

    "must return Not Provided when the answer is not present when using purchaser name details" in {
      val result = SupplierNameSummary.rowFromPurchaserDetails(UserAnswers(userAnswersId), SupplierNumber(2)).value
      val value  = result.value.content.asHtml.toString
      result.key.content.asHtml.toString must include(msgs("supplierName.checkYourAnswersLabel"))
      value                              must include(msgs("supplierDetailsCheckYourAnswers.notProvided"))
    }

    "must return Not Provided when the answer is not present when using supplier name details" in {
      val result = SupplierNameSummary.rowFromSupplierDetails(UserAnswers(userAnswersId), SupplierNumber(3)).value
      val value  = result.value.content.asHtml.toString
      result.key.content.asHtml.toString must include(msgs("supplierName.checkYourAnswersLabel"))
      value                              must include(msgs("supplierDetailsCheckYourAnswers.notProvided"))
    }
  }
}
