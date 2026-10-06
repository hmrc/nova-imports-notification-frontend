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
import forms.AddVehicleDetailsAgriculturalTractorFormProvider
import models.{AddVehicleDetailsAgriculturalTractor, NormalMode, SupplierNumber, VehicleNumber}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import views.html.AddVehicleDetailsAgriculturalTractorView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AddVehicleDetailsAgriculturalTractorViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: AddVehicleDetailsAgriculturalTractorView = app.injector.instanceOf[AddVehicleDetailsAgriculturalTractorView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider = new AddVehicleDetailsAgriculturalTractorFormProvider()
  val form         = formProvider()

  val submitCall: Call =
    controllers.vehicledetails.routes.AddVehicleDetailsAgriculturalTractorController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  "AddVehicleDetailsAgriculturalTractorView" - {

    "must render the page title" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsAgriculturalTractor.title"))
    }

    "must render the caption" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("addVehicleDetailsAgriculturalTractor.caption"))
    }

    "must render the heading" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsAgriculturalTractor.heading"))
    }

    "must render the intro paragraph" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsAgriculturalTractor.paragraph.1"))
    }

    "must render a label and hint for each field" in {
      val html: String = view(form, submitCall)(request, msgs).toString

      Seq("make", "model", "derivative", "brakeHorsepower").foreach { field =>
        html must include(msgs(s"addVehicleDetailsAgriculturalTractor.$field.label"))
        html must include(msgs(s"addVehicleDetailsAgriculturalTractor.$field.hint"))
      }
    }

    "must render the brake horsepower input at width 10 and the other inputs at width 20" in {
      val doc = org.jsoup.Jsoup.parse(view(form, submitCall)(request, msgs).toString)

      doc.getElementById("brakeHorsepower").hasClass("govuk-input--width-10") mustBe true
      Seq("make", "model", "derivative").foreach { field =>
        doc.getElementById(field).hasClass("govuk-input--width-20") mustBe true
      }
    }

    "must render the error messages copy exactly as specified" in {
      msgs("addVehicleDetailsAgriculturalTractor.make.error.format") mustBe
        "The make must not include special characters such as #, $, ^, `, {, |, }, ~, or accented letters such as è"
      msgs("addVehicleDetailsAgriculturalTractor.model.error.format") mustBe
        "The model must not include special characters such as #, $, ^, `, {, |, }, ~, or accented letters such as è"
      msgs("addVehicleDetailsAgriculturalTractor.derivative.error.format") mustBe
        "The derivative must not include special characters such as #, $, ^, `, {, |, }, ~, or accented letters such as è"
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
      html must include(msgs("addVehicleDetailsAgriculturalTractor.make.error.required"))
      html must include(msgs("addVehicleDetailsAgriculturalTractor.model.error.required"))
      html must include(msgs("addVehicleDetailsAgriculturalTractor.derivative.error.required"))
      html must include(msgs("addVehicleDetailsAgriculturalTractor.brakeHorsepower.error.required"))
    }

    "must pre-populate the fields when the question has previously been answered" in {
      val answer       = AddVehicleDetailsAgriculturalTractor(make = "John Deere", model = "SR", derivative = "5090M", brakeHorsepower = "240.75")
      val html: String = view(form.fill(answer), submitCall)(request, msgs).toString

      html must include("John Deere")
      html must include("value=\"SR\"")
      html must include("5090M")
      html must include("240.75")
    }

    "must render the same content via the render method" in {
      val html: String = view.render(form, submitCall, request, msgs).toString

      html must include(msgs("addVehicleDetailsAgriculturalTractor.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(form, submitCall)(request, msgs).toString

      html must include(msgs("addVehicleDetailsAgriculturalTractor.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
