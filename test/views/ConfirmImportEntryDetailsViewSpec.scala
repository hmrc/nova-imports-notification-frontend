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

package views

import base.SpecBase
import models.{CheckMode, ImportNumber, UserAnswers}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import pages.sections.vehicledetails.{ImportEntryDatePage, ImportEntryNumberPage}
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.ConfirmImportEntryDetailsView

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ConfirmImportEntryDetailsViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val answers: UserAnswers = emptyUserAnswers
    .set(ImportEntryNumberPage(ImportNumber(1)), "12GB3456ACD789EF21")
    .success
    .value
    .set(ImportEntryDatePage(ImportNumber(1)), LocalDate.of(2026, 3, 27))
    .success
    .value

  val view: ConfirmImportEntryDetailsView = app.injector.instanceOf[ConfirmImportEntryDetailsView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "ConfirmImportEntryDetailsView" - {

    "must render the correct heading" in {
      val html: String = view(answers, ImportNumber(1))(request, msgs).toString

      html must include(msgs("confirmImportEntryDetails.heading"))
    }

    "must render the correct page title" in {
      val html: String = view(answers, ImportNumber(1))(request, msgs).toString

      html must include(msgs("confirmImportEntryDetails.title"))
    }

    "must render the caption" in {
      val html: String = view(answers, ImportNumber(1))(request, msgs).toString

      html must include(msgs("confirmImportEntryDetails.caption"))
      html must include("govuk-caption-l")
    }

    "must render the import entry number row" in {
      val html: String = view(answers, ImportNumber(1))(request, msgs).toString

      html must include(msgs("importEntryNumber.checkYourAnswersLabel"))
      html must include("12GB3456ACD789EF21")
    }

    "must render the import entry date row as 27/03/2026" in {
      val html: String = view(answers, ImportNumber(1))(request, msgs).toString

      html must include(msgs("importEntryDate.checkYourAnswersLabel"))
      html must include("27/03/2026")
    }

    "must render the summary list with govuk-!-margin-bottom-9" in {
      val html: String = view(answers, ImportNumber(1))(request, msgs).toString

      html must include("govuk-!-margin-bottom-9")
    }

    "must render the confirm details button" in {
      val html: String = view(answers, ImportNumber(1))(request, msgs).toString

      html must include(msgs("site.confirmDetails"))
    }

    "must post to AVD-IE2.1 onSubmit for import 1" in {
      val html: String = view(answers, ImportNumber(1))(request, msgs).toString

      html must include(s"""action="${controllers.vehicledetails.routes.ConfirmImportEntryDetailsController.onSubmit(ImportNumber(1)).url}"""")
    }

    "must render the change links to AVD-IE1.0 and AVD-IE2.0 for import 1" in {
      val html: String = view(answers, ImportNumber(1))(request, msgs).toString

      html must include(controllers.vehicledetails.routes.ImportEntryNumberController.onPageLoad(ImportNumber(1), CheckMode).url)
      html must include(controllers.vehicledetails.routes.ImportEntryDateController.onPageLoad(ImportNumber(1), CheckMode).url)
    }

    "must render the same content via the render method" in {
      val html: String = view.render(answers, ImportNumber(1), request, msgs).toString

      html must include(msgs("confirmImportEntryDetails.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(answers, ImportNumber(1))(request, msgs).toString

      html must include(msgs("confirmImportEntryDetails.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }

}
