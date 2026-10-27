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
import pages.sections.vehicledetails.ImportEntryDatePage
import play.api.Application
import play.api.i18n.Messages

import java.time.LocalDate

class ImportEntryDateSummarySpec extends SpecBase {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  "ImportEntryDateSummary" - {

    "must return a summary row with the import entry date as 27/03/2026 and a change link to AVD-IE2.0" in {
      val userAnswers = UserAnswers(userAnswersId).set(ImportEntryDatePage(ImportNumber(1)), LocalDate.of(2026, 3, 27)).success.value

      val result = ImportEntryDateSummary.row(userAnswers, ImportNumber(1)).value

      result.key.content.asHtml.toString   must include(msgs("importEntryDate.checkYourAnswersLabel"))
      result.value.content.asHtml.toString must include("27/03/2026")
      result.actions.value.items.head.href mustBe routes.ImportEntryDateController.onPageLoad(ImportNumber(1), CheckMode).url
    }

    "must return None when import 1 has no import entry date" in {
      ImportEntryDateSummary.row(UserAnswers(userAnswersId), ImportNumber(1)) mustBe None
    }
  }
}
