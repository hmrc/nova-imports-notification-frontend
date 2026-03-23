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
import models.{CheckMode, PurchaserOrOnBehalf, UserAnswers}
import pages.PurchaserOrOnBehalfPage
import play.api.Application
import play.api.i18n.Messages

class PurchaseOrOnBehalfSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "PurchaseOrOnBehalfSummary" - {

    "must return a summary row with the correct value when the answer is Purchaser" in {

      val userAnswers = UserAnswers(userAnswersId).set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser).success.value

      val result = PurchaseOrOnBehalfSummary.row(userAnswers).value

      result.key.content.asHtml.toString   must include(msgs("purchaserOrOnBehalf.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include(msgs("purchaserOrOnBehalf.radio.purchaser"))
      result.actions.value.items.head.href mustBe routes.PurchaserOrOnBehalfController.onPageLoad(CheckMode).url
    }

    "must return a summary row with the correct value when the answer is OnBehalfOfPurchaser" in {

      val userAnswers = UserAnswers(userAnswersId).set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser).success.value

      val result = PurchaseOrOnBehalfSummary.row(userAnswers).value

      result.key.content.asHtml.toString   must include(msgs("purchaserOrOnBehalf.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include(msgs("purchaserOrOnBehalf.radio.behalfOfPurchaser"))
      result.actions.value.items.head.href mustBe routes.PurchaserOrOnBehalfController.onPageLoad(CheckMode).url
    }

    "must return None when the answer is not present" in {

      val userAnswers = UserAnswers(userAnswersId)

      PurchaseOrOnBehalfSummary.row(userAnswers) mustBe None
    }
  }
}
