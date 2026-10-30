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
import forms.VehicleIdentificationNumbersFormProvider
import models.{SupplierNumber, VehicleIdentificationNumbersMethod, VehicleNumber}
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
import views.html.VehicleIdentificationNumbersView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VehicleIdentificationNumbersViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: VehicleIdentificationNumbersView = app.injector.instanceOf[VehicleIdentificationNumbersView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[VehicleIdentificationNumbersMethod] = new VehicleIdentificationNumbersFormProvider()()

  val postAction: Call =
    controllers.vehicledetails.routes.VehicleIdentificationNumbersController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1))

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <b>Make Model BodyType</b>")

  private def render(form: Form[VehicleIdentificationNumbersMethod] = form, vehicleDetails: Option[InsetText] = Some(vehicleDetails)): String =
    view(form, vehicleDetails, postAction)(request, msgs).toString

  "VehicleIdentificationNumbersView" - {

    "must render the correct page title" in {
      Jsoup.parse(render()).title mustEqual msgs("vehicleIdentificationNumbers.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the vehicle details inset above the caption" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-inset-text").text mustEqual "Vehicle details: Make Model BodyType"
      document.select(".govuk-inset-text + .govuk-caption-l").isEmpty mustEqual false
    }

    "must not render an inset when there are no vehicle details" in {
      Jsoup.parse(render(vehicleDetails = None)).select(".govuk-inset-text").isEmpty mustEqual true
    }

    "must render the caption, heading, paragraphs and bullets" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-caption-l").text mustEqual msgs("vehicleIdentificationNumbers.caption")
      document.select("h1").text mustEqual msgs("vehicleIdentificationNumbers.heading.1")
      document.select("p.govuk-body").get(0).text mustEqual msgs("vehicleIdentificationNumbers.paragraph.1")
      document.select("ul.govuk-list--bullet li").get(0).text mustEqual msgs("vehicleIdentificationNumbers.list.1")
      document.select("ul.govuk-list--bullet li").get(1).text mustEqual msgs("vehicleIdentificationNumbers.list.2")
      document.select("p.govuk-body").get(1).text mustEqual msgs("vehicleIdentificationNumbers.paragraph.2")
    }

    "must render the question as a medium legend outside the h1" in {
      val document = Jsoup.parse(render())

      document.select("legend.govuk-fieldset__legend--m").text mustEqual msgs("vehicleIdentificationNumbers.heading.2")
      document.select("h1 legend").isEmpty mustEqual true
    }

    "must render the manual and upload radios with their hints" in {
      val document = Jsoup.parse(render())

      document.getElementById("value").attr("value") mustEqual "manual"
      document.getElementById("value-2").attr("value") mustEqual "upload"
      document.select("label[for=value]").text mustEqual msgs("vehicleIdentificationNumbers.radio.enterManually")
      document.select("label[for=value-2]").text mustEqual msgs("vehicleIdentificationNumbers.radio.uploadCsv")
      document.select(".govuk-radios__hint").get(0).text mustEqual msgs("vehicleIdentificationNumbers.radio.enterManually.hint")
      document.select(".govuk-radios__hint").get(1).text mustEqual msgs("vehicleIdentificationNumbers.radio.uploadCsv.hint")
    }

    "must post to the submit URL" in {
      Jsoup.parse(render()).select("form").attr("action") mustEqual postAction.url
    }

    "must render the Continue button" in {
      render() must include(msgs("site.continue"))
    }

    "must show an error summary message when nothing is selected" in {
      val document = Jsoup.parse(render(form.bind(Map("value" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("vehicleIdentificationNumbers.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
    }

    "must pre-populate the upload radio when upload was previously selected" in {
      val document = Jsoup.parse(render(form.fill(VehicleIdentificationNumbersMethod.UploadCsv)))

      document.getElementById("value-2").hasAttr("checked") mustEqual true
      document.getElementById("value").hasAttr("checked") mustEqual false
    }
  }
}
