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
import forms.EnterVinNumbersFormProvider
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
import views.html.EnterVinNumbersView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class EnterVinNumbersViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: EnterVinNumbersView = app.injector.instanceOf[EnterVinNumbersView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[String] = new EnterVinNumbersFormProvider()()

  val postAction: Call = controllers.vehicledetails.routes.EnterVinNumbersController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <strong>Land Rover Discovery 4</strong>")

  private def render(
    form: Form[String] = form,
    backendErrors: Seq[String] = Seq.empty,
    vehicleDetails: Option[InsetText] = Some(vehicleDetails),
    numberOfVehicles: Int = 5
  ): String =
    view(form, backendErrors, vehicleDetails, postAction, numberOfVehicles)(request, msgs).toString

  "EnterVinNumbersView" - {

    "must render the AMV3.0 page title with no error prefix when there are no errors" in {
      Jsoup.parse(render()).title mustEqual msgs("enterVinNumbers.title", 5) + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must prefix the page title with the error prefix when there are form errors" in {
      Jsoup.parse(render(form.bind(Map("enterVinNumbers" -> "")))).title must startWith(msgs("error.title.prefix"))
    }

    "must prefix the page title with the error prefix when there are backend errors even though the form itself is unbound" in {
      Jsoup.parse(render(backendErrors = Seq("bad format"))).title must startWith(msgs("error.title.prefix"))
    }

    "must render the vehicle details inset" in {
      Jsoup.parse(render()).select(".govuk-inset-text").text mustEqual "Vehicle details: Land Rover Discovery 4"
    }

    "must render the caption and heading, with the number of vehicles from AVD10.0 substituted in" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-caption-l").text mustEqual msgs("enterVinNumbers.caption")
      document.select("h1 label").text mustEqual msgs("enterVinNumbers.heading", 5)
      document.select("h1 label").text must include("5")
    }

    "must post to the given submit route" in {
      Jsoup.parse(render()).select("form").attr("action") mustEqual postAction.url
    }

    "must render the hint text against the textarea" in {
      Jsoup.parse(render()).select(".govuk-hint").text mustEqual msgs("enterVinNumbers.hint")
    }

    "must pre-fill the textarea with the given value" in {
      Jsoup.parse(render(form.fill("VIN1,VIN2"))).getElementById("enterVinNumbers").`val`() mustEqual "VIN1,VIN2"
    }

    "must show an error summary linking to the textarea for a form-level error" in {
      val document = Jsoup.parse(render(form.bind(Map("enterVinNumbers" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("enterVinNumbers.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#enterVinNumbers"
    }

    "must show each backend per-item error in the error summary" in {
      val document = Jsoup.parse(render(backendErrors = Seq("Item 2: not a valid VIN", "Item 5: duplicate VIN")))

      document.select(".govuk-error-summary").text must include("Item 2: not a valid VIN")
      document.select(".govuk-error-summary").text must include("Item 5: duplicate VIN")
    }

    "must render the continue button" in {
      render() must include(msgs("site.continue"))
    }
  }
}
