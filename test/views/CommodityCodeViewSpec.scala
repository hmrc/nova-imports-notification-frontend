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
import forms.CommodityCodeFormProvider
import models.{ImportNumber, NormalMode, VehicleNumber}
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
import views.html.CommodityCodeView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class CommodityCodeViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: CommodityCodeView = app.injector.instanceOf[CommodityCodeView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[String] = new CommodityCodeFormProvider()()

  val postAction: Call =
    controllers.vehicledetails.routes.CommodityCodeController.onSubmit(ImportNumber(1), VehicleNumber(1), NormalMode)

  val findingCommodityCodesUrl = "https://www.gov.uk/guidance/finding-commodity-codes-for-imports-or-exports#what-a-commodity-code-is"

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <b>Volvo FH16</b>")

  private def render(form: Form[String] = form, vehicleDetails: Option[InsetText] = Some(vehicleDetails)): String =
    view(form, vehicleDetails, postAction, findingCommodityCodesUrl)(request, msgs).toString

  "CommodityCodeView" - {

    "must render the AVD10.4 page title" in {
      Jsoup.parse(render()).title mustEqual "Commodity code - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the vehicle details inset above the caption" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-inset-text").text mustEqual "Vehicle details: Volvo FH16"
      document.select(".govuk-inset-text + .govuk-caption-l").isEmpty mustEqual false
    }

    "must not render an inset when there are no vehicle details" in {
      Jsoup.parse(render(vehicleDetails = None)).select(".govuk-inset-text").isEmpty mustEqual true
    }

    "must render the caption, heading and first paragraph" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-caption-l").text mustEqual "Add vehicle details"
      document.select("h1").text mustEqual "Commodity code"
      document.select("p.govuk-body").first.text mustEqual "You can find it on the vehicle’s import entry document."
    }

    "must render the help link opening the commodity code guidance in a new tab" in {
      val link = Jsoup.parse(render()).select("p.govuk-body a.govuk-link").first

      link.text mustEqual "Get more help to find the commodity code (opens in new tab)"
      link.attr("href") mustEqual findingCommodityCodesUrl
      link.attr("target") mustEqual "_blank"
      link.attr("rel") must include("noopener")
    }

    "must render the question as a medium label outside the h1" in {
      val document = Jsoup.parse(render())

      document.select("label.govuk-label--m").attr("for") mustEqual "value"
      document.select("label.govuk-label--m").text mustEqual "What is the commodity code?"
      document.select("h1 label").isEmpty mustEqual true
    }

    "must render the hint text against the input" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-hint").text mustEqual "Must be between 4 and 10 characters, for example 2134 or 8703123456"
      document.getElementById("value").attr("aria-describedby") must include("value-hint")
    }

    "must render a numeric input with autocomplete off" in {
      val input = Jsoup.parse(render()).getElementById("value")

      input.attr("inputmode") mustEqual "numeric"
      input.attr("autocomplete") mustEqual "off"
    }

    "must post to the given submit route" in {
      Jsoup.parse(render()).select("form").attr("action") mustEqual postAction.url
    }

    "must remove the top margin from the vehicle details inset" in {
      Jsoup.parse(render()).select(".govuk-inset-text").hasClass("govuk-!-margin-top-0") mustEqual true
    }

    "must add the larger bottom margin after the form" in {
      Jsoup.parse(render()).select("form").hasClass("govuk-!-margin-bottom-8") mustEqual true
    }

    "must render the back link" in {
      Jsoup.parse(render()).select(".govuk-back-link").text mustEqual msgs("site.back")
    }

    "must render the continue button" in {
      Jsoup.parse(render()).select(".govuk-button").text mustEqual msgs("site.continue")
    }

    "must show an error summary linking to the input for each error" in {
      List(
        ""           -> "Enter the commodity code",
        "123"        -> "The commodity code must be between 4 and 10 characters",
        "8703A23456" -> "Enter the commodity code in the correct format"
      ).foreach { case (value, message) =>
        val document = Jsoup.parse(render(form.bind(Map("value" -> value))))

        document.select(".govuk-error-summary").text must include(message)
        document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
        document.select(".govuk-error-message").text must include(message)
      }
    }

    "must fill the input with the saved commodity code" in {
      Jsoup.parse(render(form.fill("8703123456"))).getElementById("value").attr("value") mustEqual "8703123456"
    }
  }
}
