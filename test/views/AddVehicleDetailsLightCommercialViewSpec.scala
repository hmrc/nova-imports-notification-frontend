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
import forms.AddVehicleDetailsLightCommercialFormProvider
import models.{LightCommercialVehicleDetails, NormalMode, SupplierNumber, VehicleNumber}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import views.html.AddVehicleDetailsLightCommercialView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AddVehicleDetailsLightCommercialViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: AddVehicleDetailsLightCommercialView = app.injector.instanceOf[AddVehicleDetailsLightCommercialView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider                              = new AddVehicleDetailsLightCommercialFormProvider()
  val form: Form[LightCommercialVehicleDetails] = formProvider()

  def submitCall: Call =
    controllers.vehicledetails.routes.AddVehicleDetailsLightCommercialController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  "AddVehicleDetailsLightCommercialViewSpec" - {

    "must render the page title" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsLightCommercial.title"))
    }

    "must render the caption" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("addVehicleDetailsLightCommercial.caption"))
    }

    "must render the heading" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsLightCommercial.heading"))
    }

    "must render the intro paragraph" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsLightCommercial.paragraph.1"))
    }

    "must render a label and hint for each field" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      Seq("make", "model", "derivative", "trim", "bodyType").foreach { field =>
        html must include(msgs(s"addVehicleDetailsLightCommercial.label.$field"))
        html must include(msgs(s"addVehicleDetailsLightCommercial.hint.$field"))
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
      html must include(msgs("addVehicleDetailsLightCommercial.make.error.required"))
      html must include(msgs("addVehicleDetailsLightCommercial.model.error.required"))
      html must include(msgs("addVehicleDetailsLightCommercial.derivative.error.required"))
      html must include(msgs("addVehicleDetailsLightCommercial.trim.error.required"))
      html must include(msgs("addVehicleDetailsLightCommercial.bodyType.error.required"))
    }

    "must pre-populate the fields when the question has previously been answered" in {
      val answer = LightCommercialVehicleDetails(
        make = "LCV Make",
        model = "LCV Model",
        derivative = "LCV Derivative",
        trim = "LCV Trim",
        bodyType = "LCV BodyType"
      )
      val html: String = view(form.fill(answer), submitCall)(request, msgs).toString

      html must include("LCV Make")
      html must include("LCV Model")
      html must include("LCV Derivative")
      html must include("LCV Trim")
      html must include("LCV BodyType")
    }

    "must render the same content via the render method" in {
      val html: String = view.render(form, submitCall, request, msgs).toString

      html must include(msgs("addVehicleDetailsLightCommercial.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsLightCommercial.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
