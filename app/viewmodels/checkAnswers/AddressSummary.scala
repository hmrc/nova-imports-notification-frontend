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

import controllers.notifieraddress.routes
import models.UserAnswers
import pages.sections.notifieraddress.AddressPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object AddressSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AddressPage).map { address =>

      val countryLine = if (address.country.code == "GB") None else Some(address.country.name)

      val value = (address.lines ++ address.postcode.toSeq ++ countryLine)
        .filter(_.nonEmpty)
        .map(line => HtmlFormat.escape(line).body)
        .mkString("<br>")

      SummaryListRowViewModel(
        key = "yourAddressCheckYourAnswers.checkYourAnswersLabel",
        value = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          ActionItemViewModel("site.change", routes.YourAddressCheckYourAnswersController.onChangeAddress().url)
            .withVisuallyHiddenText(messages("yourAddressCheckYourAnswers.change.hidden"))
        )
      )
    }
}
