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
import forms.VatReliefFormProvider
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
import views.html.VatReliefView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VatReliefViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: VatReliefView = app.injector.instanceOf[VatReliefView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[Boolean] = new VatReliefFormProvider()()

  val postAction: Call =
    controllers.vehicledetails.routes.VatReliefController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <strong>Make Model BodyType</strong>")

  val expectedLink = "https://www.gov.uk/guidance/vat-relief-on-adapted-motor-vehicles-for-disabled-people-and-charities-notice-1002"

  private def render(form: Form[Boolean] = form, vehicleDetails: Option[InsetText] = Some(vehicleDetails)): String =
    view(form, vehicleDetails, postAction, expectedLink)(request, msgs).toString

  "VatReliefView" - {

    "must render the correct page title" in {
      Jsoup.parse(render()).title mustEqual msgs("vatRelief.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the vehicle details inset above the caption" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-inset-text").text mustEqual "Vehicle details: Make Model BodyType"
      document.select(".govuk-inset-text + .govuk-caption-l").isEmpty mustEqual false
    }

    "must not render an inset when there are no vehicle details" in {
      Jsoup.parse(render(vehicleDetails = None)).select(".govuk-inset-text").isEmpty mustEqual true
    }

    "must render the caption, heading and paragraphs with link" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-caption-l").text mustEqual msgs("vatRelief.caption")
      document.select(".govuk-heading-l").text mustEqual msgs("vatRelief.heading.1")
      document.select(".govuk-body").get(0).text mustEqual msgs("vatRelief.paragraph.1")
      document.select(".govuk-body").get(1).text mustEqual
        msgs("vatRelief.paragraph.2").replace("{0}", msgs("vatRelief.paragraph.2.link") + " (opens in new tab)")
      document.select(".govuk-link").get(3).attr("href") mustEqual expectedLink
    }

    "must render the question" in {
      Jsoup.parse(render()).select(".govuk-fieldset__legend--m").text mustEqual msgs("vatRelief.heading.2")
    }

    "must render the yes no radios" in {
      val document = Jsoup.parse(render())

      document.getElementById("value").attr("value") mustEqual "true"
      document.getElementById("value-no").attr("value") mustEqual "false"
      document.select("label[for=value]").text mustEqual msgs("site.yes")
      document.select("label[for=value-no]").text mustEqual msgs("site.no")
    }

    "must post to the submit URL" in {
      Jsoup.parse(render()).select("form").attr("action") mustEqual postAction.url
    }

    "must turn autocomplete off on the form" in {
      Jsoup.parse(render()).select("form").attr("autocomplete") mustEqual "off"
    }

    "must render the Continue button" in {
      render() must include(msgs("site.continue"))
    }

    "must show an error summary message when nothing is selected" in {
      val document = Jsoup.parse(render(form.bind(Map("value" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("vatRelief.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
    }

    "must pre-fill the yes no radio when YES was previously selected" in {
      val document = Jsoup.parse(render(form.fill(true)))

      document.getElementById("value").hasAttr("checked") mustEqual true
      document.getElementById("value-no").hasAttr("checked") mustEqual false
    }

    "must pre-fill the yes no radio when NO was previously selected" in {
      val document = Jsoup.parse(render(form.fill(false)))

      document.getElementById("value").hasAttr("checked") mustEqual false
      document.getElementById("value-no").hasAttr("checked") mustEqual true
    }
  }
}
