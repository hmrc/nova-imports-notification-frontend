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
import models.{CheckMode, ContactNumbers, UserAnswers}
import pages.sections.notifierDetails.PhoneNumberPage
import play.api.Application
import play.api.i18n.Messages

class PhoneNumberSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "PhoneNumberSummary" - {

    "must return a summary row with both contact numbers separated by a line break" in {
      val userAnswers =
        UserAnswers(userAnswersId).set(PhoneNumberPage, ContactNumbers(Some("01632 960 001"), Some("07700 900 982"))).success.value

      val result = PhoneNumberSummary.row(userAnswers).value
      val value  = result.value.content.asHtml.toString

      result.key.content.asHtml.toString must include(msgs("phoneNumber.checkYourAnswersLabel"))
      value                              must include("01632 960 001")
      value                              must include("07700 900 982")
      value                              must include("<br>")
      result.actions.value.items.head.href mustBe routes.PhoneNumberController.onPageLoad(CheckMode).url
    }

    "must return a summary row with only the phone number when no mobile number is present" in {
      val userAnswers = UserAnswers(userAnswersId).set(PhoneNumberPage, ContactNumbers(Some("01632 960 001"), None)).success.value

      val result = PhoneNumberSummary.row(userAnswers).value
      val value  = result.value.content.asHtml.toString

      value must include("01632 960 001")
      value must not include "<br>"
    }

    "must return a summary row with only the mobile number when no phone number is present" in {
      val userAnswers = UserAnswers(userAnswersId).set(PhoneNumberPage, ContactNumbers(None, Some("07700 900 982"))).success.value

      val result = PhoneNumberSummary.row(userAnswers).value
      val value  = result.value.content.asHtml.toString

      value must include("07700 900 982")
      value must not include "<br>"
    }

    "must return None when the answer is not present" in {
      PhoneNumberSummary.row(UserAnswers(userAnswersId)) mustBe None
    }
  }
}
