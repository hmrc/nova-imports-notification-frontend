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
import forms.CountryOfFirstRegistrationFormProvider
import models.{Country, ImportNumber, NormalMode, SupplierNumber, VehicleNumber}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import views.html.CountryOfFirstRegistrationView

import scala.jdk.CollectionConverters.*
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class CountryOfFirstRegistrationViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: CountryOfFirstRegistrationView = app.injector.instanceOf[CountryOfFirstRegistrationView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val countries: List[Country] = List(Country("FR", "France"), Country("DE", "Germany"))

  val form: Form[String] = new CountryOfFirstRegistrationFormProvider()(countries)

  val supplierSubmitCall: Call =
    vehicledetails.routes.CountryOfFirstRegistrationController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)
  val importSubmitCall: Call =
    vehicledetails.routes.CountryOfFirstRegistrationController.importOnSubmit(ImportNumber(1), VehicleNumber(1), NormalMode)

  private def render(form: Form[String] = form, submitCall: Call = supplierSubmitCall, countries: List[Country] = countries): String =
    view(countries, form, submitCall)(request, msgs).toString

  "CountryOfFirstRegistrationView" - {

    "must render the heading as a label inside the h1" in {
      val label = Jsoup.parse(render()).select("h1.govuk-label-wrapper label.govuk-label--l")

      label.text mustEqual msgs("countryOfFirstRegistration.heading")
      label.attr("for") mustEqual "value"
      msgs("countryOfFirstRegistration.heading") mustEqual "Enter the country where the vehicle was first registered"
    }

    "must render the correct page title" in {
      Jsoup.parse(render()).title mustEqual
        msgs("countryOfFirstRegistration.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
      msgs("countryOfFirstRegistration.title") mustEqual "Enter the country where the vehicle was first registered"
    }

    "must render the correct page caption" in {
      Jsoup.parse(render()).select("span.govuk-caption-l").text mustEqual msgs("countryOfFirstRegistration.caption")
      msgs("countryOfFirstRegistration.caption") mustEqual "Add vehicle details"
    }

    "must render the hint against the select" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-hint").text mustEqual msgs("countryOfFirstRegistration.hint")
      document.getElementById("value").attr("aria-describedby") must include("value-hint")
      msgs("countryOfFirstRegistration.hint") mustEqual "You can find this in the vehicle log book"
    }

    "must render the select as an accessible autocomplete" in {
      val select = Jsoup.parse(render()).getElementById("value")

      select.tagName mustEqual "select"
      select.hasClass("govuk-select") mustBe true
      select.attr("data-module") mustEqual "hmrc-accessible-autocomplete"
      select.attr("name") mustEqual "value"
    }

    "must render a blank first option followed by every country as a code and name" in {
      val document = Jsoup.parse(render())
      val options  = document.select("#value option")

      options.size mustEqual 3
      options.first.attr("value") mustEqual ""
      document.select("#value option[value=FR]").text mustEqual "France"
      document.select("#value option[value=DE]").text mustEqual "Germany"
    }

    "must render the countries in alphabetical order by name" in {
      val options = Jsoup.parse(render(countries = List(Country("DE", "Germany"), Country("FR", "France")))).select("#value option")

      options.asScala.drop(1).map(_.text).toList mustEqual List("France", "Germany")
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

    "must not set autocomplete on the form itself" in {
      Jsoup.parse(render()).select("form").hasAttr("autocomplete") mustBe false
    }

    "must show the required error in the summary and against the select when no country is given" in {
      val document = Jsoup.parse(render(form.bind(Map("value" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("countryOfFirstRegistration.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
      document.getElementById("value").hasClass("govuk-select--error") mustBe true
      document.select(".govuk-error-message").text                        must include(msgs("countryOfFirstRegistration.error.required"))
      document.select(".govuk-error-message .govuk-visually-hidden").text must include("Error:")
      document.title                                                      must startWith(msgs("error.title.prefix"))
      msgs("countryOfFirstRegistration.error.required") mustEqual "Enter the country where the vehicle was first registered"
    }

    "must pre-select a previously chosen country" in {
      Jsoup.parse(render(form.fill("DE"))).select("#value option[selected]").attr("value") mustEqual "DE"
    }
  }
}
