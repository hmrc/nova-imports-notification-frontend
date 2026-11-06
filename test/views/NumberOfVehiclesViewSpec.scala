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
import models.{NormalMode, NumberOfVehicles, OneMultiVehicle, SupplierNumber, VehicleNumber}
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

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <b>Land Rover Discovery 4</b>")

  private def render(form: Form[NumberOfVehicles] = form, vehicleDetails: Option[InsetText] = Some(vehicleDetails)): String =
    view(form, vehicleDetails, postAction)(request, msgs).toString

  "NumberOfVehiclesView" - {

    "must render the AVD10.0 page title" in {
      Jsoup.parse(render()).title mustEqual msgs("numberOfVehicles.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the vehicle details inset" in {
      Jsoup.parse(render()).select(".govuk-inset-text").text mustEqual "Vehicle details: Land Rover Discovery 4"
    }

    "must not render an inset when there are no vehicle details" in {
      Jsoup.parse(render(vehicleDetails = None)).select(".govuk-inset-text").isEmpty mustEqual true
    }

    "must render the caption, heading and paragraph" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-caption-l").text mustEqual msgs("numberOfVehicles.caption")
      document.select(".govuk-heading-l").text mustEqual msgs("numberOfVehicles.heading.1")
      document.select("p.govuk-body").first.text mustEqual msgs("numberOfVehicles.paragraph")
    }

    "must render both radio options with the count input nested under 'more than one'" in {
      val document = Jsoup.parse(render())
      val radios   = document.select("input[type=radio]")

      radios.size mustEqual 2
      radios.get(0).attr("value") mustEqual OneMultiVehicle.One.jsonValue
      radios.get(1).attr("value") mustEqual OneMultiVehicle.More.jsonValue
      document.select("#conditional-oneMultiVehicle-2 input#numberSameVehicles").isEmpty mustEqual false
    }

    "must post to the given submit route" in {
      Jsoup.parse(render()).select("form").attr("action") mustEqual postAction.url
    }

    "must render the continue button" in {
      render() must include(msgs("site.continue"))
    }

    "must show an error summary when nothing is selected" in {
      val document = Jsoup.parse(render(form.bind(Map.empty[String, String])))

      document.select(".govuk-error-summary").text must include(msgs("numberOfVehicles.error.required.selection"))
    }

    "must show an error summary when 'more than one' is selected with no count given" in {
      val document = Jsoup.parse(render(form.bind(Map("oneMultiVehicle" -> OneMultiVehicle.More.jsonValue, "numberSameVehicles" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("numberOfVehicles.error.required.count"))
    }

    "must pre-fill the count input when an answer has already been saved" in {
      val filled = form.fill(NumberOfVehicles(OneMultiVehicle.More, Some(7)))

      Jsoup.parse(render(filled)).getElementById("numberSameVehicles").attr("value") mustEqual "7"
    }
  }
}
