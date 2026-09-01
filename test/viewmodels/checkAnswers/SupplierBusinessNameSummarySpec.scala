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
import pages.sections.supplierdetails.SupplierBusinessNamePage
import play.api.Application
import play.api.i18n.Messages

class SupplierBusinessNameSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "SupplierBusinessNameSummary" - {

    "must return a summary row with the business name and a change link" in {
      val userAnswers = UserAnswers(userAnswersId).set(SupplierBusinessNamePage(SupplierNumber(1)), "Acme Trading Ltd").success.value

      val result = SupplierBusinessNameSummary.row(userAnswers, SupplierNumber(1)).value

      result.key.content.asHtml.toString   must include(msgs("supplierBusinessName.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include("Acme Trading Ltd")
      result.actions.value.items.head.href mustBe routes.SupplierBusinessNameController.onPageLoad(SupplierNumber(1), CheckMode).url
    }

    "must return None when the answer is not present" in {
      SupplierBusinessNameSummary.row(UserAnswers(userAnswersId), SupplierNumber(1)) mustBe None
    }
  }
}
