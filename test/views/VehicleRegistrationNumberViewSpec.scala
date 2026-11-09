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
import forms.VehicleRegistrationNumberFormProvider
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
import views.html.VehicleRegistrationNumberView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VehicleRegistrationNumberViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: VehicleRegistrationNumberView = app.injector.instanceOf[VehicleRegistrationNumberView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[String] = new VehicleRegistrationNumberFormProvider()()

  val postAction: Call =
    controllers.vehicledetails.routes.VehicleRegistrationNumberController.onSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <b>Volvo FH16</b>")

  private def render(form: Form[String] = form, vehicleDetails: Option[InsetText] = Some(vehicleDetails)): String =
    view(form, vehicleDetails, postAction)(request, msgs).toString

  "VehicleRegistrationNumberView" - {

    "must render the AVD10.2a page title" in {
      Jsoup.parse(render()).title mustEqual msgs("vehicleRegistrationNumber.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
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

    "must render the caption" in {
      Jsoup.parse(render()).select(".govuk-caption-l").text mustEqual "Add vehicle details"
    }

    "must render the question as the h1 page heading" in {
      val document = Jsoup.parse(render())

      document.select("h1 label").text mustEqual "What is the current vehicle registration number?"
      document.select("label").attr("for") mustEqual "value"
    }

    "must render the hint text against the input" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-hint").text mustEqual "For example, ABC12DE, BC12 DEF or 12-AB-34"
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

    "must show an error summary linking to the input for each error" in {
      val errors = Map(
        ""              -> "Enter the current vehicle registration number",
        "ABCDEFGHIJKL1" -> "The current vehicle registration number must be 12 characters or less",
        "ABC12£"        -> "Enter the current vehicle registration number in the correct format"
      )

      errors.foreach { case (value, message) =>
        val document = Jsoup.parse(render(form.bind(Map("value" -> value))))

        document.select(".govuk-error-summary").text must include(message)
        document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
        document.select("#value-error").text must include(message)
        document.title                       must startWith("Error: ")
      }
    }

    "must fill the input with the saved registration number" in {
      Jsoup.parse(render(form.fill("ABC12DE"))).getElementById("value").attr("value") mustEqual "ABC12DE"
    }
  }
}
