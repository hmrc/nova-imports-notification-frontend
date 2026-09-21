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
import forms.DateOfFirstRegistrationFormProvider
import models.{ImportNumber, NormalMode, SupplierNumber, VehicleNumber}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import views.html.DateOfFirstRegistrationView

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class DateOfFirstRegistrationViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: DateOfFirstRegistrationView = app.injector.instanceOf[DateOfFirstRegistrationView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[LocalDate] = app.injector.instanceOf[DateOfFirstRegistrationFormProvider].apply()

  val submitCall: Call =
    vehicledetails.routes.DateOfFirstRegistrationController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  private def render(form: Form[LocalDate] = form, submitCall: Call = submitCall): String =
    view(form, submitCall)(request, msgs).toString

  private def bind(day: String, month: String, year: String): Form[LocalDate] =
    form.bind(Map("value.day" -> day, "value.month" -> month, "value.year" -> year))

  private val errorClass = "govuk-input--error"

  private def hasErrorClass(html: String, field: String): Boolean =
    Option(Jsoup.parse(html).getElementById(s"value.$field")).exists(_.hasClass(errorClass))

  "DateOfFirstRegistrationView" - {

    "must render the correct H1" in {
      val document = Jsoup.parse(render())

      document.select("h1").text mustEqual msgs("dateOfFirstRegistration.heading")
      msgs("dateOfFirstRegistration.heading") mustEqual "Date of first registration"
    }

    "must render the correct page title" in {
      Jsoup.parse(render()).title mustEqual msgs("dateOfFirstRegistration.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
      msgs("dateOfFirstRegistration.title") mustEqual "Date of first registration"
    }

    "must render the correct page caption" in {
      val caption = Jsoup.parse(render()).select("span.govuk-caption-l")

      caption.text mustEqual msgs("dateOfFirstRegistration.caption")
      msgs("dateOfFirstRegistration.caption") mustEqual "Add vehicle details"
    }

    "must render the introductory paragraph" in {
      Jsoup.parse(render()).select("p.govuk-body").text must include(msgs("dateOfFirstRegistration.paragraph.1"))
      msgs("dateOfFirstRegistration.paragraph.1") mustEqual
        "You can usually find this information in the vehicle log book. If there is more than one date, you must enter the earliest one."
    }

    "must render the question heading as an H2 inside the legend" in {
      val document = Jsoup.parse(render())

      document.select("legend.govuk-fieldset__legend--m h2.govuk-fieldset__heading").text mustEqual msgs(
        "dateOfFirstRegistration.question.heading"
      )
      msgs("dateOfFirstRegistration.question.heading") mustEqual "When was the vehicle first registered for road use?"
    }

    "must render the hint text" in {
      Jsoup.parse(render()).select(".govuk-hint").text mustEqual msgs("dateOfFirstRegistration.hint")
      msgs("dateOfFirstRegistration.hint") mustEqual "For example, 27 03 2026"
    }

    "must not render any inset text" in {
      Jsoup.parse(render()).select(".govuk-inset-text").size mustEqual 0
    }

    "must render a back link" in {
      render() must include("govuk-back-link")
    }

    "must render a day, month and year field" in {
      val document = Jsoup.parse(render())

      document.getElementById("value.day")   must not be null
      document.getElementById("value.month") must not be null
      document.getElementById("value.year")  must not be null
    }

    "must render the day, month and year labels" in {
      val html = render()

      html must include(msgs("date.day"))
      html must include(msgs("date.month"))
      html must include(msgs("date.year"))
    }

    "must render the continue button" in {
      Jsoup.parse(render()).select("form button.govuk-button").text mustEqual msgs("site.continue")
    }

    "must post the form to the supplied supplier submit call" in {
      val document = Jsoup.parse(render())

      document.select("form").attr("action") mustEqual submitCall.url
      document.select("form").attr("method") mustEqual "POST"
    }

    "must post the form to the supplied import submit call" in {
      val importSubmitCall =
        vehicledetails.routes.DateOfFirstRegistrationController.importOnSubmit(ImportNumber(1), VehicleNumber(1), NormalMode)

      Jsoup.parse(render(submitCall = importSubmitCall)).select("form").attr("action") mustEqual importSubmitCall.url
    }

    "must not set autocomplete on the form itself" in {
      Jsoup.parse(render()).select("form").hasAttr("autocomplete") mustBe false
    }

    "must show an error summary linking to the day field when the date is missing" in {
      val document = Jsoup.parse(render(bind("", "", "")))

      document.select(".govuk-error-summary").text must include(msgs("dateOfFirstRegistration.error.required.all"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value.day"
      msgs("dateOfFirstRegistration.error.required.all") mustEqual "Enter the date the vehicle was first registered"
    }

    "must highlight only the year field when only the year is missing" in {
      val html = render(bind("27", "03", ""))

      html must include(msgs("dateOfFirstRegistration.error.required", msgs("date.error.year")))
      msgs("dateOfFirstRegistration.error.required", msgs("date.error.year")) mustEqual
        "The date the vehicle was first registered must include a year"
      hasErrorClass(html, "year") mustEqual true
      hasErrorClass(html, "day") mustEqual false
      hasErrorClass(html, "month") mustEqual false
    }

    "must highlight only the month field when only the month is missing" in {
      val html = render(bind("27", "", "2026"))

      html must include(msgs("dateOfFirstRegistration.error.required", msgs("date.error.month")))
      hasErrorClass(html, "month") mustEqual true
      hasErrorClass(html, "day") mustEqual false
      hasErrorClass(html, "year") mustEqual false
    }

    "must highlight only the day field when only the day is missing" in {
      val html = render(bind("", "03", "2026"))

      html must include(msgs("dateOfFirstRegistration.error.required", msgs("date.error.day")))
      hasErrorClass(html, "day") mustEqual true
      hasErrorClass(html, "month") mustEqual false
      hasErrorClass(html, "year") mustEqual false
    }

    "must highlight the day and month fields when both are missing" in {
      val html = render(bind("", "", "2026"))

      html must include(msgs("dateOfFirstRegistration.error.required.two", msgs("date.error.day"), msgs("date.error.month")))
      msgs("dateOfFirstRegistration.error.required.two", msgs("date.error.day"), msgs("date.error.month")) mustEqual
        "The date the vehicle was first registered must include a day and month"
      hasErrorClass(html, "day") mustEqual true
      hasErrorClass(html, "month") mustEqual true
      hasErrorClass(html, "year") mustEqual false
    }

    "must highlight the day and year fields when both are missing" in {
      val html = render(bind("", "03", ""))

      html must include(msgs("dateOfFirstRegistration.error.required.two", msgs("date.error.day"), msgs("date.error.year")))
      hasErrorClass(html, "day") mustEqual true
      hasErrorClass(html, "year") mustEqual true
      hasErrorClass(html, "month") mustEqual false
    }

    "must highlight the month and year fields when both are missing" in {
      val html = render(bind("27", "", ""))

      html must include(msgs("dateOfFirstRegistration.error.required.two", msgs("date.error.month"), msgs("date.error.year")))
      hasErrorClass(html, "month") mustEqual true
      hasErrorClass(html, "year") mustEqual true
      hasErrorClass(html, "day") mustEqual false
    }

    "must highlight only the day field when the day cannot be a day of any month" in {
      val html = render(bind("32", "03", "2026"))

      html must include(msgs("dateOfFirstRegistration.error.notARealDate"))
      hasErrorClass(html, "day") mustEqual true
      hasErrorClass(html, "month") mustEqual false
      hasErrorClass(html, "year") mustEqual false
    }

    "must highlight the whole date when the date does not exist but no single field is at fault" in {
      val html = render(bind("31", "02", "2026"))

      html must include(msgs("dateOfFirstRegistration.error.notARealDate"))
      msgs("dateOfFirstRegistration.error.notARealDate") mustEqual "The date the vehicle was first registered must be a real date"
      hasErrorClass(html, "day") mustEqual true
      hasErrorClass(html, "month") mustEqual true
      hasErrorClass(html, "year") mustEqual true
    }

    "must show the format error when the date is not made up of numbers" in {
      render(bind("aa", "03", "2026")) must include(msgs("dateOfFirstRegistration.error.invalid"))
      msgs("dateOfFirstRegistration.error.invalid") mustEqual "Enter the date the vehicle was first registered in the correct format"
    }

    "must highlight the whole date and show the future error when the date is in the future" in {
      val html = render(bind("01", "01", "2999"))

      html must include(msgs("dateOfFirstRegistration.error.future"))
      msgs("dateOfFirstRegistration.error.future") mustEqual "The date the vehicle was first registered must be today or in the past"
      hasErrorClass(html, "day") mustEqual true
      hasErrorClass(html, "month") mustEqual true
      hasErrorClass(html, "year") mustEqual true
    }

    "must pre-populate the fields with a previously entered date" in {
      val document = Jsoup.parse(render(form.fill(LocalDate.of(2026, 3, 27))))

      document.getElementById("value.day").attr("value") mustEqual "27"
      document.getElementById("value.month").attr("value") mustEqual "03"
      document.getElementById("value.year").attr("value") mustEqual "2026"
    }
  }
}
