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
import forms.AddVehicleDetailsFormProvider
import models.NormalMode
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.AddVehicleDetailsView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AddVehicleDetailsViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: AddVehicleDetailsView = app.injector.instanceOf[AddVehicleDetailsView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private val spreadsheetUrl = "https://example.com/spreadsheets"

  val formProvider = new AddVehicleDetailsFormProvider()
  val form         = formProvider()

  "AddVehicleDetailsView" - {

    "must render the correct page title" in {
      val html: String = view(form, NormalMode, spreadsheetUrl)(request, msgs).toString

      html must include(msgs("addVehicleDetails.title"))
    }

    "must render the correct heading" in {
      val html: String = view(form, NormalMode, spreadsheetUrl)(request, msgs).toString

      html must include(msgs("addVehicleDetails.heading"))
    }

    "must render the correct page caption" in {
      val html: String = view(form, NormalMode, spreadsheetUrl)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("addVehicleDetails.caption"))
    }

    "must render the introductory paragraphs" in {
      val html: String = view(form, NormalMode, spreadsheetUrl)(request, msgs).toString

      html must include(msgs("addVehicleDetails.paragraph.1"))
      html must include(msgs("addVehicleDetails.paragraph.2"))
    }

    "must render the method heading and both radio options with hints" in {
      val html: String = view(form, NormalMode, spreadsheetUrl)(request, msgs).toString

      html must include(msgs("addVehicleDetails.method.heading"))
      html must include(msgs("addVehicleDetails.radio.bySupplier"))
      html must include(msgs("addVehicleDetails.radio.bySupplier.hint"))
      html must include(msgs("addVehicleDetails.radio.bySpreadsheet"))
      html must include(msgs("addVehicleDetails.radio.bySpreadsheet.hint"))
    }

    "must render the spreadsheet inset text with the provided URL opening in a new tab" in {
      val html: String = view(form, NormalMode, spreadsheetUrl)(request, msgs).toString

      html must include("govuk-inset-text")
      html must include(msgs("addVehicleDetails.inset.paragraph.1"))
      html must include(msgs("addVehicleDetails.inset.findSpreadsheet.linkText"))
      html must include(spreadsheetUrl)
      html must include("target=\"_blank\"")
    }

    "must render the error summary when the form has errors" in {
      val boundForm    = form.bind(Map("value" -> ""))
      val html: String = view(boundForm, NormalMode, spreadsheetUrl)(request, msgs).toString

      html must include(msgs("addVehicleDetails.error.required"))
    }
  }

}
