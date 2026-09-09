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
import forms.PurchaseInvoiceNumberFormProvider
import models.{NormalMode, SupplierNumber, VehicleNumber}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.PurchaseInvoiceNumberView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PurchaseInvoiceNumberViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: PurchaseInvoiceNumberView = app.injector.instanceOf[PurchaseInvoiceNumberView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[String] = new PurchaseInvoiceNumberFormProvider()()

  private def render(form: Form[String] = form): String =
    view(form, SupplierNumber(1), VehicleNumber(1), NormalMode)(request, msgs).toString

  "PurchaseInvoiceNumberView" - {

    "must render the correct heading" in {
      render() must include(msgs("purchaseInvoiceNumber.heading"))
    }

    "must render the correct page title" in {
      Jsoup.parse(render()).title mustEqual msgs("purchaseInvoiceNumber.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the heading as the label for the input" in {
      val document = Jsoup.parse(render())

      document.select("h1 label").attr("for") mustEqual "value"
      document.select("h1 label").text mustEqual msgs("purchaseInvoiceNumber.heading")
    }

    "must render the correct page caption" in {
      val html = render()

      html must include("govuk-caption-l")
      html must include(msgs("purchaseInvoiceNumber.caption"))
    }

    "must render the hint text against the input" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-hint").text mustEqual msgs("purchaseInvoiceNumber.hint")
      document.getElementById("value").attr("aria-describedby") must include("value-hint")
    }

    "must render a text input" in {
      Option(Jsoup.parse(render()).getElementById("value")) must not be None
    }

    "must render the continue button" in {
      render() must include(msgs("site.continue"))
    }

    "must post to the purchase invoice number submit route" in {
      Jsoup.parse(render()).select("form").attr("action") mustEqual
        controllers.vehicledetails.routes.PurchaseInvoiceNumberController.onSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode).url
    }

    "must show an error summary linking to the input when nothing is entered" in {
      val document = Jsoup.parse(render(form.bind(Map("value" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("purchaseInvoiceNumber.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
    }

    "must show the format error when invalid characters are entered" in {
      val html = render(form.bind(Map("value" -> "INV 123#")))

      html must include(msgs("purchaseInvoiceNumber.error.invalid"))
      Jsoup.parse(html).getElementById("value").hasClass("govuk-input--error") mustEqual true
    }

    "must show the length error when more than 20 characters are entered" in {
      render(form.bind(Map("value" -> "A" * 21))) must include(msgs("purchaseInvoiceNumber.error.length"))
    }

    "must pre-populate the input with a previously entered invoice number" in {
      Jsoup.parse(render(form.fill("INV-2026-001"))).getElementById("value").attr("value") mustEqual "INV-2026-001"
    }
  }
}
