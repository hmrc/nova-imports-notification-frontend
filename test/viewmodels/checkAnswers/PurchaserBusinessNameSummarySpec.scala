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
import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.sections.purchaserDetails.PurchaserBusinessNamePage
import play.api.Application
import play.api.i18n.Messages

class PurchaserBusinessNameSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "PurchaserBusinessNameSummary" - {

    "must return a summary row with the business name and a change link" in {
      val userAnswers = UserAnswers(userAnswersId).set(PurchaserBusinessNamePage, "Acme Trading Ltd").success.value

      val result = PurchaserBusinessNameSummary.row(userAnswers).value

      result.key.content.asHtml.toString   must include(msgs("purchaserBusinessName.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include("Acme Trading Ltd")
      result.actions.value.items.head.href mustBe routes.PurchaserBusinessNameController.onPageLoad(CheckMode).url
    }

    "must return None when the answer is not present" in {
      PurchaserBusinessNameSummary.row(UserAnswers(userAnswersId)) mustBe None
    }
  }
}
