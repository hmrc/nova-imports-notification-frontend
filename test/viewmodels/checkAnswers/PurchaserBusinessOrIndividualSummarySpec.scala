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
import models.{CheckMode, PurchaserBusinessOrIndividual, UserAnswers}
import pages.PurchaserBusinessOrIndividualPage
import play.api.Application
import play.api.i18n.Messages

class PurchaserBusinessOrIndividualSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "PurchaserBusinessOrIndividualSummary" - {

    "must return a summary row with the correct value when the answer is NonVatRegisteredBusiness" in {

      val userAnswers =
        UserAnswers(userAnswersId).set(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredBusiness).success.value

      val result = PurchaserBusinessOrIndividualSummary.row(userAnswers).value

      result.key.content.asHtml.toString   must include(msgs("purchaserBusinessOrIndividual.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include(msgs("purchaserBusinessOrIndividual.radio.nonVatRegisteredBusiness"))
      result.actions.value.items.head.href mustBe routes.PurchaserBusinessOrIndividualController.onPageLoad(CheckMode).url
    }

    "must return a summary row with the correct value when the answer is NonVatRegisteredPrivateIndividual" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredPrivateIndividual)
        .success
        .value

      val result = PurchaserBusinessOrIndividualSummary.row(userAnswers).value

      result.key.content.asHtml.toString   must include(msgs("purchaserBusinessOrIndividual.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include(msgs("purchaserBusinessOrIndividual.radio.nonVatRegisteredPrivateIndividual"))
      result.actions.value.items.head.href mustBe routes.PurchaserBusinessOrIndividualController.onPageLoad(CheckMode).url
    }

    "must return None when the answer is not present" in {

      val userAnswers = UserAnswers(userAnswersId)

      PurchaserBusinessOrIndividualSummary.row(userAnswers) mustBe None
    }
  }
}
