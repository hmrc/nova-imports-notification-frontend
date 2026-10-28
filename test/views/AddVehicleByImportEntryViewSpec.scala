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
import controllers.vehicledetails.routes
import models.ImportNumber
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.AddVehicleByImportEntryView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AddVehicleByImportEntryViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: AddVehicleByImportEntryView = app.injector.instanceOf[AddVehicleByImportEntryView]

  val importEntryNumber: String = "12GB3456ACD789EF21"

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private def render(): String =
    view(importEntryNumber, ImportNumber(1)).toString

  "AddVehicleByImportEntryView" - {

    "must set the page title" in {
      render() must include("<title>Vehicles brought as part of this import entry number - Notification of Vehicle Arrivals - GOV.UK")
    }

    "must render the Add vehicle details caption" in {
      render() must include(msgs("addVehicleByImportEntry.caption"))
    }

    "must render import entry 12GB3456ACD789EF21 in the heading" in {
      render() must include("""<h1 class="govuk-heading-l">Vehicles brought as part of import entry 12GB3456ACD789EF21</h1>""")
    }

    "must render both paragraphs" in {
      val html = render()

      html must include(msgs("addVehicleByImportEntry.paragraph.1"))
      html must include(msgs("addVehicleByImportEntry.paragraph.2"))
    }

    "must render both bullets in the list" in {
      val html = render()

      html must include("""<ul class="govuk-list govuk-list--bullet">""")
      html must include(msgs("addVehicleByImportEntry.list.1"))
      html must include(msgs("addVehicleByImportEntry.list.2"))
    }

    "must render the Add vehicle button as a submit button" in {
      val html = render().replaceAll("\\s+", " ")

      html must include("""<button type="submit" class="govuk-button"""")
      html must include(msgs("addVehicleByImportEntry.addVehicle"))
    }

    "must post to AVD-IE3.0 onSubmit for import 1" in {
      render() must include(s"""action="${routes.AddVehicleByImportEntryController.onSubmit(ImportNumber(1)).url}"""")
    }

    "must render the same content via the render method" in {
      val html: String = view.render(importEntryNumber, ImportNumber(1), request, msgs).toString

      html must include(msgs("addVehicleByImportEntry.heading", importEntryNumber))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(importEntryNumber, ImportNumber(1))(request, msgs).toString

      html must include(msgs("addVehicleByImportEntry.heading", importEntryNumber))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
