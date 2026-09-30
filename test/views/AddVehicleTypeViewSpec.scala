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
import controllers.vehicledetails
import forms.AddVehicleTypeFormProvider
import models.{AddVehicleType, ImportNumber, NormalMode, SupplierNumber, VehicleNumber}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import views.html.AddVehicleTypeView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*

class AddVehicleTypeViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: AddVehicleTypeView = app.injector.instanceOf[AddVehicleTypeView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[AddVehicleType] = new AddVehicleTypeFormProvider()()

  val supplierSubmitCall: Call =
    vehicledetails.routes.AddVehicleTypeController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)
  val importSubmitCall: Call =
    vehicledetails.routes.AddVehicleTypeController.importOnSubmit(ImportNumber(1), VehicleNumber(1), NormalMode)

  private def render(form: Form[AddVehicleType] = form, submitCall: Call = supplierSubmitCall): String =
    view(form, submitCall)(request, msgs).toString

  "AddVehicleTypeView" - {

    "must render the heading as the legend inside the h1" in {
      val legend = Jsoup.parse(render()).select("legend.govuk-fieldset__legend--l h1.govuk-fieldset__heading")

      legend.text mustEqual msgs("addVehicleType.heading")
      msgs("addVehicleType.heading") mustEqual "Which type of vehicle are you notifying HMRC about?"
    }

    "must render the correct page title" in {
      Jsoup.parse(render()).title mustEqual
        msgs("addVehicleType.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
      msgs("addVehicleType.title") mustEqual "Which type of vehicle are you notifying HMRC about?"
    }

    "must render the correct page caption" in {
      Jsoup.parse(render()).select("span.govuk-caption-l").text mustEqual msgs("addVehicleType.caption")
      msgs("addVehicleType.caption") mustEqual "Add vehicle details"
    }

    "must render seven radio options" in {
      Jsoup.parse(render()).select("input[type=radio]").size mustEqual 7
    }

    "must render the seven radios in the order tractor, car, plant, hcv, lcv, motorcycle, caravan" in {
      val labels = Jsoup.parse(render()).select(".govuk-radios__label").asScala.map(_.text()).toSeq

      labels mustEqual Seq(
        msgs("addVehicleType.radio.tractor"),
        msgs("addVehicleType.radio.car"),
        msgs("addVehicleType.radio.plant"),
        msgs("addVehicleType.radio.hcv"),
        msgs("addVehicleType.radio.lcv"),
        msgs("addVehicleType.radio.motorcycle"),
        msgs("addVehicleType.radio.caravan")
      )
    }

    "must render a hint on the heavy commercial vehicle option only" in {
      val hints = Jsoup.parse(render()).select(".govuk-radios__hint")

      hints.size mustEqual 1
      hints.text() mustEqual msgs("addVehicleType.radio.hcv.hint")
    }

    "must render a back link" in {
      render() must include("govuk-back-link")
    }

    "must render the continue button" in {
      Jsoup.parse(render()).select("form button.govuk-button").text mustEqual msgs("site.continue")
    }

    "must set the supplier form to POST to /supplier/1/vehicle/1/vehicle-type" in {
      val form = Jsoup.parse(render(submitCall = supplierSubmitCall)).select("form")

      form.attr("action") mustEqual supplierSubmitCall.url
      form.attr("method") mustEqual "POST"
    }

    "must set the import form to POST to /import/1/vehicle/1/vehicle-type" in {
      val form = Jsoup.parse(render(submitCall = importSubmitCall)).select("form")

      form.attr("action") mustEqual importSubmitCall.url
      form.attr("method") mustEqual "POST"
    }

    "must show the required error when no vehicle type is selected" in {
      val document = Jsoup.parse(render(form.bind(Map("value" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("addVehicleType.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
      document.select(".govuk-form-group").hasClass("govuk-form-group--error") mustBe true
      document.select(".govuk-error-message").text                        must include(msgs("addVehicleType.error.required"))
      document.select(".govuk-error-message .govuk-visually-hidden").text must include("Error:")
      document.title                                                      must startWith(msgs("error.title.prefix"))
      msgs("addVehicleType.error.required") mustEqual "Select which type of vehicle you are notifying HMRC about"
    }

    "must pre-select the LCV radio when LCV was previously selected" in {
      val document = Jsoup.parse(render(form.fill(AddVehicleType.Lcv)))

      document.select("input[value=LCV]").hasAttr("checked") mustBe true
      document.select("input[type=radio][checked]").size mustEqual 1
    }
  }
}
