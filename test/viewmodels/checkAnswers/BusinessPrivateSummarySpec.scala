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
import models.{BusinessOrPrivateIndividual, CheckMode, UserAnswers}
import pages.sections.initialquestions.BusinessOrPrivatePage
import play.api.Application
import play.api.i18n.Messages

class BusinessPrivateSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "BusinessPrivateSummary" - {

    "must return a summary row with the correct value when the answer is Business" in {

      val userAnswers = UserAnswers(userAnswersId).set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business).success.value

      val result = BusinessPrivateSummary.row(userAnswers).value

      result.key.content.asHtml.toString   must include(msgs("businessPrivate.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include(msgs("businessPrivate.business"))
      result.actions.value.items.head.href mustBe routes.BusinessPrivateController.onPageLoad(CheckMode).url
    }

    "must return a summary row with the correct value when the answer is PrivateIndividual" in {

      val userAnswers = UserAnswers(userAnswersId).set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual).success.value

      val result = BusinessPrivateSummary.row(userAnswers).value

      result.key.content.asHtml.toString   must include(msgs("businessPrivate.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include(msgs("businessPrivate.privateIndividual"))
      result.actions.value.items.head.href mustBe routes.BusinessPrivateController.onPageLoad(CheckMode).url
    }

    "must describe the change link with the business or private individual question in visually hidden text" in {

      val userAnswers = UserAnswers(userAnswersId).set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business).success.value

      val result = BusinessPrivateSummary.row(userAnswers).value

      result.actions.value.items.head.visuallyHiddenText.value mustBe "whether you are a business or private individual"
    }

    "must return None when the answer is not present" in {

      val userAnswers = UserAnswers(userAnswersId)

      BusinessPrivateSummary.row(userAnswers) mustBe None
    }
  }
}
