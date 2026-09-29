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
import controllers.vehicledetails
import controllers.vehicledetails.PaymentCurrencyController.{ImportHeadingKey, SupplierHeadingKey}
import forms.PaymentCurrencyFormProvider
import models.{Currency, ImportNumber, NormalMode, SupplierNumber, VehicleNumber}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import views.html.PaymentCurrencyView

import scala.jdk.CollectionConverters.*
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PaymentCurrencyViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: PaymentCurrencyView = app.injector.instanceOf[PaymentCurrencyView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val currencies: List[Currency] = List(Currency("GBP", "British pound sterling"), Currency("EUR", "Euro"))

  val form: Form[String] = new PaymentCurrencyFormProvider()(currencies)

  val supplierSubmitCall: Call =
    vehicledetails.routes.PaymentCurrencyController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)
  val importSubmitCall: Call =
    vehicledetails.routes.PaymentCurrencyController.importOnSubmit(ImportNumber(1), VehicleNumber(1), NormalMode)

  private def render(
    form: Form[String] = form,
    headingKey: String = SupplierHeadingKey,
    submitCall: Call = supplierSubmitCall,
    currencies: List[Currency] = currencies
  ): String =
    view(currencies, form, headingKey, submitCall)(request, msgs).toString

  "PaymentCurrencyView" - {

    "on the supplier journey" - {

      "must render the heading as a label inside the h1" in {
        val label = Jsoup.parse(render()).select("h1.govuk-label-wrapper label.govuk-label--l")

        label.text mustEqual msgs(SupplierHeadingKey)
        label.attr("for") mustEqual "value"
        msgs(SupplierHeadingKey) mustEqual "Enter the currency used to pay for the vehicle"
      }

      "must render the correct page title" in {
        Jsoup.parse(render()).title mustEqual
          msgs(SupplierHeadingKey) + " - " + msgs("service.name") + " - " + msgs("site.govuk")
      }
    }

    "on the import journey" - {

      "must render the heading as a label inside the h1" in {
        val label = Jsoup.parse(render(headingKey = ImportHeadingKey)).select("h1.govuk-label-wrapper label.govuk-label--l")

        label.text mustEqual msgs(ImportHeadingKey)
        msgs(ImportHeadingKey) mustEqual "Enter the currency used to pay for the vehicle"
      }

      "must render the correct page title" in {
        Jsoup.parse(render(headingKey = ImportHeadingKey)).title mustEqual
          msgs(ImportHeadingKey) + " - " + msgs("service.name") + " - " + msgs("site.govuk")
      }
    }

    "must render the correct page caption" in {
      Jsoup.parse(render()).select("span.govuk-caption-l").text mustEqual msgs("paymentCurrency.caption")
      msgs("paymentCurrency.caption") mustEqual "Add vehicle details"
    }

    "must render the select as an accessible autocomplete" in {
      val select = Jsoup.parse(render()).getElementById("value")

      select.tagName mustEqual "select"
      select.hasClass("govuk-select") mustBe true
      select.attr("data-module") mustEqual "hmrc-accessible-autocomplete"
      select.attr("name") mustEqual "value"
    }

    "must render a blank first option followed by every currency as its code and name" in {
      val document = Jsoup.parse(render())
      val options  = document.select("#value option")

      options.size mustEqual 3
      options.first.attr("value") mustEqual ""
      document.select("#value option[value=GBP]").text mustEqual "British pound sterling (GBP)"
      document.select("#value option[value=EUR]").text mustEqual "Euro (EUR)"
    }

    "must render the currencies in the order given" in {
      val options = Jsoup.parse(render()).select("#value option")

      options.asScala.drop(1).map(_.attr("value")).toList mustEqual List("GBP", "EUR")
    }

    "must render a back link" in {
      render() must include("govuk-back-link")
    }

    "must render the continue button" in {
      Jsoup.parse(render()).select("form button.govuk-button").text mustEqual msgs("site.continue")
    }

    "must post the form to the supplied supplier submit call" in {
      val form = Jsoup.parse(render(submitCall = supplierSubmitCall)).select("form")

      form.attr("action") mustEqual supplierSubmitCall.url
      form.attr("method") mustEqual "POST"
    }

    "must post the form to the supplied import submit call" in {
      val form = Jsoup.parse(render(submitCall = importSubmitCall)).select("form")

      form.attr("action") mustEqual importSubmitCall.url
      form.attr("method") mustEqual "POST"
    }

    "must show the required error in the summary and against the select when no currency is given" in {
      val document = Jsoup.parse(render(form.bind(Map("value" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("paymentCurrency.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
      document.getElementById("value").hasClass("govuk-select--error") mustBe true
      document.select(".govuk-error-message").text                        must include(msgs("paymentCurrency.error.required"))
      document.select(".govuk-error-message .govuk-visually-hidden").text must include("Error:")
      document.title                                                      must startWith(msgs("error.title.prefix"))
      msgs("paymentCurrency.error.required") mustEqual "Enter the currency used to pay for the vehicle"
    }

    "must pre-select a previously chosen currency" in {
      Jsoup.parse(render(form.fill("EUR"))).select("#value option[selected]").attr("value") mustEqual "EUR"
    }
  }
}
