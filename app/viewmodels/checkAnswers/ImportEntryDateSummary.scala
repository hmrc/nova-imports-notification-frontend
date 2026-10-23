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

import controllers.vehicledetails.routes
import models.{CheckMode, ImportNumber, UserAnswers}
import pages.sections.vehicledetails.ImportEntryDatePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import java.time.format.DateTimeFormatter

object ImportEntryDateSummary {

  private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  def row(answers: UserAnswers, importNumber: ImportNumber)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(ImportEntryDatePage(importNumber)).map { importEntryDate =>
      SummaryListRowViewModel(
        key = "importEntryDate.checkYourAnswersLabel",
        value = ValueViewModel(importEntryDate.format(dateFormat)),
        actions = Seq(
          ActionItemViewModel("site.change", routes.ImportEntryDateController.onPageLoad(importNumber, CheckMode).url)
            .withVisuallyHiddenText(messages("importEntryDate.change.hidden"))
        )
      )
    }
}
