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
import views.html.AddImportVehicleDetailsNoSpreadsheetView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AddImportVehicleDetailsNoSpreadsheetViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: AddImportVehicleDetailsNoSpreadsheetView = app.injector.instanceOf[AddImportVehicleDetailsNoSpreadsheetView]

  private val formProvider = new AddImportVehicleDetailsFormProvider()
  private val form         = formProvider()

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "AddImportVehicleDetailsNoSpreadsheetView" - {

    "must render the correct page title" in {
      view(form, NormalMode)(request, msgs).toString must include(msgs("addImportVehicleDetails.title"))
    }

    "must render the caption and heading" in {
      val html = view(form, NormalMode)(request, msgs).toString
      html must include("govuk-caption-l")
      html must include(msgs("addImportVehicleDetails.caption"))
      html must include(msgs("addImportVehicleDetails.heading"))
    }

    "must render the intro paragraph, both bullet points and the 100-vehicles paragraph" in {
      val html = view(form, NormalMode)(request, msgs).toString
      html must include(msgs("addImportVehicleDetails.paragraph.1"))
      html must include(msgs("addImportVehicleDetails.bullet.1"))
      html must include(msgs("addImportVehicleDetails.bullet.2"))
      html must include(msgs("addImportVehicleDetails.noSpreadsheet.paragraph.2"))
    }

    "must render the 'Add import entry number' button" in {
      view(form, NormalMode)(request, msgs).toString must include(msgs("addImportVehicleDetails.noSpreadsheet.button"))
    }

    "must post to the submit URL" in {
      view(form, NormalMode)(request, msgs).toString must include(
        s"""action="${controllers.vehicledetails.routes.AddImportVehicleDetailsController.onSubmit(NormalMode).url}""""
      )
    }

    "must render the same content via the render method" in {
      view.render(form, NormalMode, request, msgs).toString must include(msgs("addImportVehicleDetails.heading"))
    }

    "must render the same content via the f method" in {
      view.f(form, NormalMode)(request, msgs).toString must include(msgs("addImportVehicleDetails.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
