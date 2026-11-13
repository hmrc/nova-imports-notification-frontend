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
import forms.VatMarginSchemeFormProvider
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
import views.html.VatMarginSchemeView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VatMarginSchemeViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: VatMarginSchemeView = app.injector.instanceOf[VatMarginSchemeView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[Boolean] = new VatMarginSchemeFormProvider()()

  val postAction: Call =
    controllers.vehicledetails.routes.VatMarginSchemeController.onSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <strong>Volvo FH16</strong>")

  val expectedLink = "https://www.gov.uk/guidance/check-if-you-can-use-a-vat-margin-scheme-if-you-import-from-or-export-to-countries-outside-the-uk"

  private def render(form: Form[Boolean] = form, vehicleDetails: Option[InsetText] = Some(vehicleDetails)): String =
    view(form, vehicleDetails, postAction, expectedLink)(request, msgs).toString

  "VatMarginSchemeView" - {

    "must render the AVD10.9 page title" in {
      Jsoup.parse(render()).title mustEqual msgs("vatMarginScheme.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the vehicle details inset above the caption" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-inset-text").text mustEqual "Vehicle details: Volvo FH16"
      document.select(".govuk-inset-text").hasClass("govuk-!-margin-top-0") mustEqual true
      document.select(".govuk-inset-text + .govuk-caption-l").isEmpty mustEqual false
    }

    "must not render an inset when there are no vehicle details" in {
      Jsoup.parse(render(vehicleDetails = None)).select(".govuk-inset-text").isEmpty mustEqual true
    }

    "must render the caption, heading and explanatory paragraph with the external link" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-caption-l").text mustEqual msgs("vatMarginScheme.caption")
      document.select("h1").text mustEqual msgs("vatMarginScheme.heading.1")

      val link = document.select("h1 ~ p.govuk-body a.govuk-link")
      link.text mustEqual msgs("vatMarginScheme.link.text")
      link.attr("href") mustEqual expectedLink
      link.attr("target") mustEqual "_blank"
      link.attr("rel") mustEqual "noopener noreferrer"
    }

    "must render the radios with the question as a medium legend" in {
      val document = Jsoup.parse(render())

      document.select("fieldset legend").text mustEqual msgs("vatMarginScheme.heading.2")
      document.select("fieldset legend").hasClass("govuk-fieldset__legend--m") mustEqual true
      document.select(".govuk-radios__item input[type=radio]").size mustEqual 2
    }

    "must post to the given submit route" in {
      val form = Jsoup.parse(render()).select("form")

      form.attr("action") mustEqual postAction.url
      form.hasClass("govuk-!-margin-bottom-8") mustEqual true
    }

    "must render a back link" in {
      render() must include("govuk-back-link")
    }

    "must show an error summary linking to the input when nothing is selected" in {
      val document = Jsoup.parse(render(form.bind(Map("value" -> ""))))

      document.title                               must startWith(msgs("error.title.prefix"))
      document.select(".govuk-error-summary").text must include(msgs("vatMarginScheme.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
    }

    "must select the saved value" in {
      Jsoup.parse(render(form.fill(true))).getElementById("value").hasAttr("checked") mustEqual true
    }
  }
}
