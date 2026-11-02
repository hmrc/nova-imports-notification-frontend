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
import forms.NumberOfVehiclesFormProvider
import models.{NormalMode, NumberOfVehicles, OneOrMultipleVehicles, SupplierNumber, VehicleNumber}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText
import viewmodels.govuk.inset.*
import views.html.NumberOfVehiclesView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class NumberOfVehiclesViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: NumberOfVehiclesView = app.injector.instanceOf[NumberOfVehiclesView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[NumberOfVehicles] = new NumberOfVehiclesFormProvider()()

  val postAction: Call =
    controllers.vehicledetails.routes.NumberOfVehiclesController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <b>Make Model BodyType</b>")

  private def render(form: Form[NumberOfVehicles] = form, vehicleDetails: Option[InsetText] = Some(vehicleDetails)): String =
    view(form, vehicleDetails, postAction)(request, msgs).toString

  "NumberOfVehiclesView" - {

    "must render the correct page title" in {
      Jsoup.parse(render()).title mustEqual msgs("numberOfVehicles.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the vehicle details inset above the caption" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-inset-text").text mustEqual "Vehicle details: Make Model BodyType"
      document.select(".govuk-inset-text + .govuk-caption-l").isEmpty mustEqual false
    }

    "must render the vehicle details inset with govuk-!-margin-top-0" in {
      Jsoup.parse(render()).select(".govuk-inset-text").hasClass("govuk-!-margin-top-0") mustEqual true
    }

    "must not render an inset when there are no vehicle details" in {
      Jsoup.parse(render(vehicleDetails = None)).select(".govuk-inset-text").isEmpty mustEqual true
    }

    "must render the caption, heading and paragraph" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-caption-l").text mustEqual msgs("numberOfVehicles.caption")
      document.select("h1").text mustEqual msgs("numberOfVehicles.heading.1")
      document.select("p.govuk-body").first.text mustEqual msgs("numberOfVehicles.paragraph")
    }

    "must render the question as a medium legend outside the h1" in {
      val document = Jsoup.parse(render())

      document.select("legend.govuk-fieldset__legend--m").text mustEqual msgs("numberOfVehicles.heading.2")
      document.select("h1 legend").isEmpty mustEqual true
    }

    "must render the one vehicle and more than one vehicle radios" in {
      val document = Jsoup.parse(render())

      document.getElementById("oneOrMultipleVehicles").attr("value") mustEqual "One"
      document.getElementById("oneOrMultipleVehicles-2").attr("value") mustEqual "Multiple"
      document.select("label[for=oneOrMultipleVehicles]").text mustEqual msgs("numberOfVehicles.radio.one")
      document.select("label[for=oneOrMultipleVehicles-2]").text mustEqual msgs("numberOfVehicles.radio.many")
    }

    "must render the number input hidden under the more than one vehicle radio" in {
      val document    = Jsoup.parse(render())
      val conditional = document.getElementById("conditional-oneOrMultipleVehicles-2")
      val input       = conditional.getElementById("numberOfVehiclesToMultiply")

      document.getElementById("oneOrMultipleVehicles-2").attr("data-aria-controls") mustEqual "conditional-oneOrMultipleVehicles-2"
      conditional.hasClass("govuk-radios__conditional--hidden") mustEqual true
      conditional.select("label[for=numberOfVehiclesToMultiply]").text mustEqual msgs("numberOfVehicles.label.count")
      input.hasClass("govuk-!-width-one-third") mustEqual true
    }

    "must post to the submit URL" in {
      Jsoup.parse(render()).select("form").attr("action") mustEqual postAction.url
    }

    "must render the form with govuk-!-margin-bottom-8" in {
      Jsoup.parse(render()).select("form").hasClass("govuk-!-margin-bottom-8") mustEqual true
    }

    "must render the Continue button" in {
      render() must include(msgs("site.continue"))
    }

    "must show an error summary linking to the first radio when nothing is selected" in {
      val document = Jsoup.parse(render(form.bind(Map("oneOrMultipleVehicles" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("numberOfVehicles.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#oneOrMultipleVehicles"
    }

    "must show the number input with an error summary linking to it when the number is blank" in {
      val document = Jsoup.parse(render(form.bind(Map("oneOrMultipleVehicles" -> "Multiple", "numberOfVehiclesToMultiply" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("numberOfVehicles.error.input.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#numberOfVehiclesToMultiply"
      document.getElementById("conditional-oneOrMultipleVehicles-2").hasClass("govuk-radios__conditional--hidden") mustEqual false
    }

    "must pre-populate the radio and the number when more than one vehicle was previously selected" in {
      val document = Jsoup.parse(render(form.fill(NumberOfVehicles(OneOrMultipleVehicles.Multiple, Some(5)))))

      document.getElementById("oneOrMultipleVehicles-2").hasAttr("checked") mustEqual true
      document.getElementById("conditional-oneOrMultipleVehicles-2").hasClass("govuk-radios__conditional--hidden") mustEqual false
      document.getElementById("numberOfVehiclesToMultiply").attr("value") mustEqual "5"
    }

    "must pre-populate the one vehicle radio when one vehicle was previously selected" in {
      val document = Jsoup.parse(render(form.fill(NumberOfVehicles(OneOrMultipleVehicles.One, None))))

      document.getElementById("oneOrMultipleVehicles").hasAttr("checked") mustEqual true
      document.getElementById("oneOrMultipleVehicles-2").hasAttr("checked") mustEqual false
    }
  }
}
