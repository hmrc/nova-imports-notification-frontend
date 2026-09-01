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
import models.NormalMode
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.AddVehicleDetailsBySupplierOnlyView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AddVehicleDetailsBySupplierOnlyViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: AddVehicleDetailsBySupplierOnlyView = app.injector.instanceOf[AddVehicleDetailsBySupplierOnlyView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private def render(): String = view(NormalMode).toString

  "AddVehicleDetailsBySupplierOnlyView" - {

    "must render the caption" in {
      render() must include(msgs("addVehicleDetails.caption"))
    }

    "must render the heading" in {
      render() must include("""<h1 class="govuk-heading-l">Vehicles brought from the EU</h1>""")
    }

    "must render both introductory paragraphs" in {
      val html = render()

      html must include(msgs("addVehicleDetails.bySupplierOnly.paragraph.1"))
      html must include(msgs("addVehicleDetails.bySupplierOnly.paragraph.2"))
    }

    "must render the Add supplier details button as a submit button" in {
      val html = render().replaceAll("\\s+", " ")

      html must include("""<button type="submit" class="govuk-button"""")
      html must include(msgs("addVehicleDetails.bySupplierOnly.button"))
    }

    "must post the form back to this page" in {
      val html = render()

      html must include(s"""action="${routes.AddVehicleDetailsController.onSubmit(NormalMode).url}"""")
    }

    "must set the page title" in {
      render() must include("<title>Vehicles brought from the EU - Notification of Vehicle Arrivals - GOV.UK")
    }
  }
}
