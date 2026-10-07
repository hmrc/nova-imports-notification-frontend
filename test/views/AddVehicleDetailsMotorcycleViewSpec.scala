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
import forms.AddVehicleDetailsMotorcycleFormProvider
import models.{AddVehicleDetailsMotorcycle, MotorcycleFuelType, MotorcycleTransmissionType, NormalMode, SupplierNumber, VehicleNumber}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import views.html.AddVehicleDetailsMotorcycleView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AddVehicleDetailsMotorcycleViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: AddVehicleDetailsMotorcycleView = app.injector.instanceOf[AddVehicleDetailsMotorcycleView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider = new AddVehicleDetailsMotorcycleFormProvider()
  val form         = formProvider()

  val submitCall: Call =
    controllers.vehicledetails.routes.AddVehicleDetailsMotorcycleController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  "AddVehicleDetailsMotorcycleView" - {

    "must render the page title" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsMotorcycle.title"))
    }

    "must render the caption" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("addVehicleDetailsMotorcycle.caption"))
    }

    "must render the heading" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsMotorcycle.heading"))
    }

    "must render the intro paragraph" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsMotorcycle.paragraph.1"))
    }

    "must render a label and hint for each field" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      Seq("make", "model", "derivative", "motorcycleVersion", "motorcycleType", "motorcycleStyle", "engineSize").foreach { field =>
        html must include(msgs(s"addVehicleDetailsMotorcycle.$field.label"))
        html must include(msgs(s"addVehicleDetailsMotorcycle.$field.hint"))
      }
    }

    "must render the transmission type and fuel type dropdowns with their options" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsMotorcycle.transmission.label"))
      html must include(msgs("addVehicleDetailsMotorcycle.transmission.option.automatic"))
      html must include(msgs("addVehicleDetailsMotorcycle.transmission.option.manual"))
      html must include(msgs("addVehicleDetailsMotorcycle.transmission.option.semiAutomatic"))
      html must include(msgs("addVehicleDetailsMotorcycle.fuelType.label"))
      html must include(msgs("addVehicleDetailsMotorcycle.fuelType.option.electric"))
      html must include(msgs("addVehicleDetailsMotorcycle.fuelType.option.hybrid"))
      html must include(msgs("addVehicleDetailsMotorcycle.fuelType.option.petrol"))
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
      html must include(msgs("addVehicleDetailsMotorcycle.make.error.required"))
      html must include(msgs("addVehicleDetailsMotorcycle.model.error.required"))
      html must include(msgs("addVehicleDetailsMotorcycle.derivative.error.required"))
      html must include(msgs("addVehicleDetailsMotorcycle.motorcycleVersion.error.required"))
      html must include(msgs("addVehicleDetailsMotorcycle.motorcycleType.error.required"))
      html must include(msgs("addVehicleDetailsMotorcycle.motorcycleStyle.error.required"))
      html must include(msgs("addVehicleDetailsMotorcycle.transmission.error.required"))
      html must include(msgs("addVehicleDetailsMotorcycle.fuelType.error.required"))
      html must include(msgs("addVehicleDetailsMotorcycle.engineSize.error.required"))
    }

    "must pre-populate the fields when the question has previously been answered" in {
      val answer = AddVehicleDetailsMotorcycle(
        make = "Honda",
        model = "GL1200",
        derivative = "1200",
        motorcycleVersion = "Gold Wing Deluxe",
        motorcycleType = "a road motorcycle",
        motorcycleStyle = "a tourer",
        transmission = MotorcycleTransmissionType.Automatic,
        fuelType = MotorcycleFuelType.Petrol,
        engineSize = "1000"
      )
      val html: String = view(form.fill(answer), submitCall)(request, msgs).toString

      html must include("Honda")
      html must include("GL1200")
      html must include("1200")
      html must include("Gold Wing Deluxe")
      html must include("a road motorcycle")
      html must include("a tourer")
      html must include("1000")
    }

    "must render the same content via the render method" in {
      val html: String = view.render(form, submitCall, request, msgs).toString

      html must include(msgs("addVehicleDetailsMotorcycle.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsMotorcycle.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
