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
import forms.PurchaseInvoiceDateFormProvider
import models.{NormalMode, SupplierNumber, VehicleNumber}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.PurchaseInvoiceDateView

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PurchaseInvoiceDateViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: PurchaseInvoiceDateView = app.injector.instanceOf[PurchaseInvoiceDateView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[LocalDate] = new PurchaseInvoiceDateFormProvider()()

  private def render(form: Form[LocalDate] = form): String =
    view(form, SupplierNumber(1), VehicleNumber(1), NormalMode)(request, msgs).toString

  private def bind(day: String, month: String, year: String): Form[LocalDate] =
    form.bind(Map("value.day" -> day, "value.month" -> month, "value.year" -> year))

  private val errorClass = "govuk-input--error"

  private def hasErrorClass(html: String, field: String): Boolean =
    Option(Jsoup.parse(html).getElementById(s"value.$field")).exists(_.hasClass(errorClass))

  "PurchaseInvoiceDateView" - {

    "must render the correct heading" in {
      render() must include(msgs("purchaseInvoiceDate.heading"))
    }

    "must render the correct page title" in {
      Jsoup.parse(render()).title mustEqual msgs("purchaseInvoiceDate.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the correct page caption" in {
      val html = render()

      html must include("govuk-caption-l")
      html must include(msgs("purchaseInvoiceDate.caption"))
    }

    "must render the hint text" in {
      render() must include(msgs("purchaseInvoiceDate.hint"))
    }

    "must render a day, month and year field" in {
      val document = Jsoup.parse(render())

      document.getElementById("value.day") must not be null
      document.getElementById("value.month") must not be null
      document.getElementById("value.year") must not be null
    }

    "must render the day, month and year labels" in {
      val html = render()

      html must include(msgs("date.day"))
      html must include(msgs("date.month"))
      html must include(msgs("date.year"))
    }

    "must render the continue button" in {
      render() must include(msgs("site.continue"))
    }

    "must show an error summary linking to the day field when the date is missing" in {
      val document = Jsoup.parse(render(bind("", "", "")))

      document.select(".govuk-error-summary").text must include(msgs("purchaseInvoiceDate.error.required.all"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value.day"
    }

    "must highlight only the year field when only the year is missing" in {
      val html = render(bind("27", "03", ""))

      html must include(msgs("purchaseInvoiceDate.error.required", msgs("date.error.year")))
      hasErrorClass(html, "year") mustEqual true
      hasErrorClass(html, "day") mustEqual false
      hasErrorClass(html, "month") mustEqual false
    }

    "must highlight the day and month fields when both are missing" in {
      val html = render(bind("", "", "2026"))

      html must include(msgs("purchaseInvoiceDate.error.required.two", msgs("date.error.day"), msgs("date.error.month")))
      hasErrorClass(html, "day") mustEqual true
      hasErrorClass(html, "month") mustEqual true
      hasErrorClass(html, "year") mustEqual false
    }

    "must highlight only the day field when the day cannot be a day of any month" in {
      val html = render(bind("32", "03", "2026"))

      html must include(msgs("purchaseInvoiceDate.error.notARealDate"))
      hasErrorClass(html, "day") mustEqual true
      hasErrorClass(html, "month") mustEqual false
      hasErrorClass(html, "year") mustEqual false
    }

    "must highlight the whole date when the date does not exist but no single field is at fault" in {
      val html = render(bind("31", "02", "2026"))

      html must include(msgs("purchaseInvoiceDate.error.notARealDate"))
      hasErrorClass(html, "day") mustEqual true
      hasErrorClass(html, "month") mustEqual true
      hasErrorClass(html, "year") mustEqual true
    }

    "must show the format error when the date is not made up of numbers" in {
      render(bind("aa", "03", "2026")) must include(msgs("purchaseInvoiceDate.error.invalid"))
    }

    "must pre-populate the fields with a previously entered date" in {
      val document = Jsoup.parse(render(form.fill(LocalDate.of(2026, 3, 27))))

      document.getElementById("value.day").attr("value") mustEqual "27"
      document.getElementById("value.month").attr("value") mustEqual "03"
      document.getElementById("value.year").attr("value") mustEqual "2026"
    }
  }
}
