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
import forms.NoPurchaseInvoiceReasonFormProvider
import models.{NormalMode, SupplierNumber, VehicleNumber}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.NoPurchaseInvoiceReasonView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class NoPurchaseInvoiceReasonViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: NoPurchaseInvoiceReasonView = app.injector.instanceOf[NoPurchaseInvoiceReasonView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[String] = new NoPurchaseInvoiceReasonFormProvider()()

  private def render(form: Form[String] = form): String =
    view(form, SupplierNumber(1), VehicleNumber(1), NormalMode)(request, msgs).toString

  "NoPurchaseInvoiceReasonView" - {

    "must render the correct heading" in {
      render() must include(msgs("noPurchaseInvoiceReason.heading"))
    }

    "must render the correct page title" in {
      Jsoup.parse(render()).title mustEqual msgs("noPurchaseInvoiceReason.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the heading as the label for the text area" in {
      val document = Jsoup.parse(render())

      document.select("h1 label").attr("for") mustEqual "value"
      document.select("h1 label").text mustEqual msgs("noPurchaseInvoiceReason.heading")
    }

    "must render the correct page caption" in {
      val html = render()

      html must include("govuk-caption-l")
      html must include(msgs("noPurchaseInvoiceReason.caption"))
    }

    "must render a text area limited to 160 characters" in {
      val document = Jsoup.parse(render())

      document.select("textarea#value").size mustEqual 1
      document.select("textarea#value").attr("class") must include("govuk-js-character-count")
      document.select(".govuk-character-count").attr("data-maxlength") mustEqual NoPurchaseInvoiceReasonFormProvider.MaxLength.toString
    }

    "must tell the user the character limit before the count script runs" in {
      Jsoup.parse(render()).select("#value-info").text must include("160 characters")
    }

    "must wire the text area up to the character count module" in {
      val group = Jsoup.parse(render()).select(".govuk-character-count")

      group.attr("data-module") mustEqual "govuk-character-count"
      group.attr("data-maxlength") mustEqual NoPurchaseInvoiceReasonFormProvider.MaxLength.toString
      Jsoup.parse(render()).select("textarea#value").hasClass("govuk-js-character-count") mustEqual true
    }

    "must render the continue button" in {
      render() must include(msgs("site.continue"))
    }

    "must post to the reason submit route" in {
      Jsoup.parse(render()).select("form").attr("action") mustEqual
        controllers.vehicledetails.routes.NoPurchaseInvoiceReasonController.onSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode).url
    }

    "must show an error summary linking to the text area when nothing is entered" in {
      val document = Jsoup.parse(render(form.bind(Map("value" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("noPurchaseInvoiceReason.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
    }

    "must show the format error when invalid characters are entered" in {
      val html = render(form.bind(Map("value" -> "Invoice #123")))

      html must include(msgs("noPurchaseInvoiceReason.error.invalid"))
      Jsoup.parse(html).getElementById("value").hasClass("govuk-textarea--error") mustEqual true
    }

    "must list the disallowed characters in the format error" in {
      val summary = Jsoup.parse(render(form.bind(Map("value" -> "Invoice #123")))).select(".govuk-error-summary").text

      summary must include("#, $, ^, `, {, |, }, ~")
    }

    "must show the length error when more than 160 characters are entered" in {
      render(form.bind(Map("value" -> "A" * 161))) must include(msgs("noPurchaseInvoiceReason.error.length"))
    }

    "must pre-populate the text area with a previously entered reason" in {
      Jsoup.parse(render(form.fill("No invoice was issued"))).getElementById("value").text mustEqual "No invoice was issued"
    }
  }
}
