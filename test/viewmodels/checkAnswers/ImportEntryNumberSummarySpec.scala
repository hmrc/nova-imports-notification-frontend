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
import controllers.vehicledetails.routes
import models.{CheckMode, ImportNumber, UserAnswers}
import pages.sections.vehicledetails.ImportEntryNumberPage
import play.api.Application
import play.api.i18n.Messages

class ImportEntryNumberSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "ImportEntryNumberSummary" - {

    "must return a summary row with the import entry number and a change link to AVD-IE1.0" in {
      val userAnswers = UserAnswers(userAnswersId).set(ImportEntryNumberPage(ImportNumber(1)), "12GB3456ACD789EF21").success.value

      val result = ImportEntryNumberSummary.row(userAnswers, ImportNumber(1)).value

      result.key.content.asHtml.toString   must include(msgs("importEntryNumber.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include("12GB3456ACD789EF21")
      result.actions.value.items.head.href mustBe routes.ImportEntryNumberController.onPageLoad(ImportNumber(1), CheckMode).url
    }

    "must return None when import 1 has no import entry number" in {
      ImportEntryNumberSummary.row(UserAnswers(userAnswersId), ImportNumber(1)) mustBe None
    }
  }
}
