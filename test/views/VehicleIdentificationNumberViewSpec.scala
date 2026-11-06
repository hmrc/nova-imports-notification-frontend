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
import forms.VehicleIdentificationNumberFormProvider
import models.{NormalMode, SupplierNumber, VehicleNumber}
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
import views.html.VehicleIdentificationNumberView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VehicleIdentificationNumberViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: VehicleIdentificationNumberView = app.injector.instanceOf[VehicleIdentificationNumberView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[String] = new VehicleIdentificationNumberFormProvider()(_ => false)

  val postAction: Call =
    controllers.vehicledetails.routes.VehicleIdentificationNumberController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <b>Volvo FH16</b>")

  private def render(form: Form[String] = form, vehicleDetails: Option[InsetText] = Some(vehicleDetails)): String =
    view(form, vehicleDetails, postAction)(request, msgs).toString

  "VehicleIdentificationNumberView" - {

    "must render the AVD10.1 page title" in {
      Jsoup.parse(render()).title mustEqual msgs("vehicleIdentificationNumber.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the back link" in {
      Jsoup.parse(render()).select(".govuk-back-link").isEmpty mustEqual false
    }

    "must render the vehicle details inset above the caption" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-inset-text").text mustEqual "Vehicle details: Volvo FH16"
      document.select(".govuk-inset-text + .govuk-caption-l").isEmpty mustEqual false
    }

    "must not render an inset when there are no vehicle details" in {
      Jsoup.parse(render(vehicleDetails = None)).select(".govuk-inset-text").isEmpty mustEqual true
    }

    "must render the caption, heading and paragraph" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-caption-l").text mustEqual "Add vehicle details"
      document.select("h1").text mustEqual "Vehicle identification number"
      document.select("p.govuk-body").first.text mustEqual msgs("vehicleIdentificationNumber.paragraph")
    }

    "must render the bullet list" in {
      val items = Jsoup.parse(render()).select("ul.govuk-list--bullet li")

      items.size mustEqual 2
      items.get(0).text mustEqual "O or Q, replace them with 0 (zero)"
      items.get(1).text mustEqual "I, replace it with 1 (one)"
    }

    "must render the question as a medium label outside the h1" in {
      val document = Jsoup.parse(render())

      document.select("label.govuk-label--m").attr("for") mustEqual "value"
      document.select("label.govuk-label--m").text mustEqual "What is the vehicle identification number?"
      document.select("h1 label").isEmpty mustEqual true
    }

    "must render the hint text against the input" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-hint").text mustEqual "For example, 1ABCD2E03FG456789"
      document.getElementById("value").attr("aria-describedby") must include("value-hint")
    }

    "must turn autocomplete off on the input" in {
      Jsoup.parse(render()).getElementById("value").attr("autocomplete") mustEqual "off"
    }

    "must post to the given submit route" in {
      Jsoup.parse(render()).select("form").attr("action") mustEqual postAction.url
    }

    "must render the continue button" in {
      Jsoup.parse(render()).select("button.govuk-button").text mustEqual "Continue"
    }

    "must render the elements in a logical order for keyboard navigation" in {
      val tabbable = Jsoup.parse(render()).select("main a, main input, main button").eachAttr("class")

      tabbable.size must be >= 2
    }

    "must show an error summary linking to the input for each error" in {
      val errors = Map(
        ""                   -> "Enter the vehicle identification number",
        "1ABCD2E03FG4567890" -> "The vehicle identification number must be 17 characters or less",
        "1ABCD2E03FG-56789"  -> "Enter the vehicle identification number in the correct format"
      )

      errors.foreach { case (value, message) =>
        val document = Jsoup.parse(render(form.bind(Map("value" -> value))))

        document.select(".govuk-error-summary").text must include(message)
        document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
        document.select("#value-error").text must include(message)
        document.title                       must startWith("Error: ")
      }
    }

    "must show the duplicate error" in {
      val duplicateForm = new VehicleIdentificationNumberFormProvider()(_ => true).bind(Map("value" -> "1ABCD2E03FG456789"))

      Jsoup.parse(render(duplicateForm)).select(".govuk-error-summary").text must include(
        "Enter a different vehicle identification number. You have already used this one for another vehicle in this submission"
      )
    }

    "must fill the input with the saved VIN" in {
      Jsoup.parse(render(form.fill("1ABCD2E03FG456789"))).getElementById("value").attr("value") mustEqual "1ABCD2E03FG456789"
    }
  }
}
