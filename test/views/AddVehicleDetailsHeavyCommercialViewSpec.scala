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
import forms.AddVehicleDetailsHeavyCommercialFormProvider
import models.{AddVehicleDetailsHeavyCommercial, NormalMode, SupplierNumber, VehicleNumber}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import views.html.AddVehicleDetailsHeavyCommercialView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AddVehicleDetailsHeavyCommercialViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: AddVehicleDetailsHeavyCommercialView = app.injector.instanceOf[AddVehicleDetailsHeavyCommercialView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider = new AddVehicleDetailsHeavyCommercialFormProvider()
  val form         = formProvider()

  val submitCall: Call =
    controllers.vehicledetails.routes.AddVehicleDetailsHeavyCommercialController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  "AddVehicleDetailsHeavyCommercialView" - {

    "must render the page title" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsHeavyCommercial.title"))
    }

    "must render the caption" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("addVehicleDetailsHeavyCommercial.caption"))
    }

    "must render the heading" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsHeavyCommercial.heading"))
    }

    "must render the intro paragraph" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsHeavyCommercial.paragraph.1"))
    }

    "must render a label and hint for each field" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      Seq("make", "model", "bodyType", "cabType").foreach { field =>
        html must include(msgs(s"addVehicleDetailsHeavyCommercial.$field.label"))
        html must include(msgs(s"addVehicleDetailsHeavyCommercial.$field.hint"))
      }
    }

    "must render the Continue button" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(msgs("site.continue"))
    }

    "must post to the submit call given" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(submitCall.url)
    }

    "must render the error summary when the form has errors" in {
      val boundForm    = form.bind(Map.empty[String, String])
      val html: String = view(boundForm, submitCall)(request, msgs).toString

      html must include("govuk-error-summary")
      html must include(msgs("addVehicleDetailsHeavyCommercial.make.error.required"))
      html must include(msgs("addVehicleDetailsHeavyCommercial.model.error.required"))
      html must include(msgs("addVehicleDetailsHeavyCommercial.bodyType.error.required"))
      html must include(msgs("addVehicleDetailsHeavyCommercial.cabType.error.required"))
    }

    "must pre-populate the fields when the question has previously been answered" in {
      val answer =
        AddVehicleDetailsHeavyCommercial(make = "Renault Trucks", model = "Magnum", bodyType = "midlift axle tractor", cabType = "sleeper cab")
      val html: String = view(form.fill(answer), submitCall)(request, msgs).toString

      html must include("Renault Trucks")
      html must include("Magnum")
      html must include("midlift axle tractor")
      html must include("sleeper cab")
    }

    "must render the same content via the render method" in {
      val html: String = view.render(form, submitCall, request, msgs).toString

      html must include(msgs("addVehicleDetailsHeavyCommercial.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsHeavyCommercial.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
