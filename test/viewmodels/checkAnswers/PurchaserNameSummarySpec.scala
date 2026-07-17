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
import models.{CheckMode, NameDetails, UserAnswers}
import pages.sections.purchaserDetails.PurchaserNamePage
import play.api.Application
import play.api.i18n.Messages

class PurchaserNameSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "PurchaserNameSummary" - {

    "must return a summary with the name parts stacked on separate lines and a single change link" in {
      val userAnswers = UserAnswers(userAnswersId).set(PurchaserNamePage, NameDetails("Mr", "John", "Smith")).success.value

      val result = PurchaserNameSummary.row(userAnswers).value
      val value  = result.value.content.asHtml.toString

      result.key.content.asHtml.toString must include(msgs("purchaserName.checkYourAnswersLabel"))
      value                              must (include("Mr") and include("John") and include("Smith") and include("<br>"))
      result.actions.value.items.head.href mustBe routes.PurchaserNameController.onPageLoad(CheckMode).url
    }

    "must return None when the answer is not present" in {
      PurchaserNameSummary.row(UserAnswers(userAnswersId)) mustBe None
    }
  }
}
