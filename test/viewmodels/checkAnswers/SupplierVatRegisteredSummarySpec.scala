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
import models.{CheckMode, SupplierNumber, UserAnswers}
import pages.sections.supplierdetails.IsSupplierVatRegisteredPage
import play.api.Application
import play.api.i18n.Messages

class SupplierVatRegisteredSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "SupplierVatRegisteredSummary" - {

    "must return a summary row with the correct value when the answer is Yes" in {

      val userAnswers =
        UserAnswers(userAnswersId).unsafeSet(IsSupplierVatRegisteredPage(SupplierNumber(2)), true)

      val result = SupplierVatRegisteredSummary.row(userAnswers, SupplierNumber(2)).value

      result.key.content.asHtml.toString   must include(msgs("isSupplierVatRegistered.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include(msgs("site.yes"))
      result.actions.value.items.head.href mustBe routes.IsSupplierVatRegisteredController.onPageLoad(SupplierNumber(2), CheckMode).url
    }

    "must return a summary row with the correct value when the answer is No" in {

      val userAnswers =
        UserAnswers(userAnswersId)
          .unsafeSet(IsSupplierVatRegisteredPage(SupplierNumber(4)), false)

      val result = SupplierVatRegisteredSummary.row(userAnswers, SupplierNumber(4)).value

      result.key.content.asHtml.toString   must include(msgs("isSupplierVatRegistered.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include(msgs("site.no"))
      result.actions.value.items.head.href mustBe routes.IsSupplierVatRegisteredController.onPageLoad(SupplierNumber(4), CheckMode).url
    }

    "must return None when the answer is not present" in {

      val userAnswers = UserAnswers(userAnswersId)

      SupplierVatRegisteredSummary.row(userAnswers, SupplierNumber(1)) mustBe None
    }
  }
}
