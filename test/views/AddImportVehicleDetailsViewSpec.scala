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
import forms.AddImportVehicleDetailsFormProvider
import models.NormalMode
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.AddImportVehicleDetailsView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AddImportVehicleDetailsViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: AddImportVehicleDetailsView = app.injector.instanceOf[AddImportVehicleDetailsView]

  private val spreadsheetUrl = "https://www.gov.uk/guidance/telling-hmrc-youre-importing-multiple-vehicles#spreadsheets-for-different-vehicle-types"

  private val formProvider = new AddImportVehicleDetailsFormProvider()
  private val form         = formProvider()

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "AddImportVehicleDetailsView" - {

    "must render the correct page title" in {
      view(form, NormalMode, spreadsheetUrl)(request, msgs).toString must include(msgs("addImportVehicleDetails.title"))
    }

    "must render the caption and heading" in {
      val html = view(form, NormalMode, spreadsheetUrl)(request, msgs).toString
      html must include("govuk-caption-l")
      html must include(msgs("addImportVehicleDetails.caption"))
      html must include(msgs("addImportVehicleDetails.heading"))
    }

    "must render the intro paragraph, both bullet points and the 100-vehicles paragraph" in {
      val html = view(form, NormalMode, spreadsheetUrl)(request, msgs).toString
      html must include(msgs("addImportVehicleDetails.paragraph.1"))
      html must include(msgs("addImportVehicleDetails.bullet.1"))
      html must include(msgs("addImportVehicleDetails.bullet.2"))
      html must include(msgs("addImportVehicleDetails.paragraph.2"))
    }

    "must render the method question heading" in {
      view(form, NormalMode, spreadsheetUrl)(request, msgs).toString must include(msgs("addImportVehicleDetails.method.heading"))
    }

    "must render both radio options with their hints" in {
      val html = view(form, NormalMode, spreadsheetUrl)(request, msgs).toString
      html must include(msgs("addImportVehicleDetails.radio.byImportEntryNumber"))
      html must include(msgs("addImportVehicleDetails.radio.byImportEntryNumber.hint"))
      html must include(msgs("addImportVehicleDetails.radio.bySpreadsheet"))
      html must include(msgs("addImportVehicleDetails.radio.bySpreadsheet.hint"))
    }

    "must render the inset text with the find-spreadsheet link opening in a new tab" in {
      val html = view(form, NormalMode, spreadsheetUrl)(request, msgs).toString
      html must include(msgs("addImportVehicleDetails.inset.findSpreadsheet.linkText"))
      html must include(spreadsheetUrl)
      html must include("(opens in new tab)")
    }

    "must render the Continue button" in {
      view(form, NormalMode, spreadsheetUrl)(request, msgs).toString must include(msgs("site.continue"))
    }

    "must post to the submit URL" in {
      view(form, NormalMode, spreadsheetUrl)(request, msgs).toString must include(
        s"""action="${controllers.vehicledetails.routes.AddImportVehicleDetailsController.onSubmit(NormalMode).url}""""
      )
    }

    "must render the error summary and required error when the form has errors" in {
      val boundForm = form.bind(Map("value" -> ""))
      val html      = view(boundForm, NormalMode, spreadsheetUrl)(request, msgs).toString
      html must include(msgs("error.summary.title"))
      html must include(msgs("addImportVehicleDetails.error.required"))
    }

    "must render the same content via the render method" in {
      view.render(form, NormalMode, spreadsheetUrl, request, msgs).toString must include(msgs("addImportVehicleDetails.heading"))
    }

    "must render the same content via the f method" in {
      view.f(form, NormalMode, spreadsheetUrl)(request, msgs).toString must include(msgs("addImportVehicleDetails.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
