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
import forms.ImportEntryDateFormProvider
import models.{CheckMode, ImportNumber, Mode, NormalMode}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import views.html.ImportEntryDateView

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ImportEntryDateViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: ImportEntryDateView = app.injector.instanceOf[ImportEntryDateView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[LocalDate] = app.injector.instanceOf[ImportEntryDateFormProvider].apply()

  val submitCall: Call =
    vehicledetails.routes.ImportEntryDateController.onSubmit(ImportNumber(1), NormalMode)

  private def render(form: Form[LocalDate] = form, mode: Mode = NormalMode): String =
    view(form, ImportNumber(1), mode)(request, msgs).toString

  private def bind(day: String, month: String, year: String): Form[LocalDate] =
    form.bind(Map("value.day" -> day, "value.month" -> month, "value.year" -> year))

  private val errorClass = "govuk-input--error"

  private def hasErrorClass(html: String, field: String): Boolean =
    Option(Jsoup.parse(html).getElementById(s"value.$field")).exists(_.hasClass(errorClass))

  "ImportEntryDateView" - {

    "must render the correct H1" in {
      val document = Jsoup.parse(render())

      document.select("h1").text mustEqual msgs("importEntryDate.heading.1")
      msgs("importEntryDate.heading.1") mustEqual "Import entry date"
    }

    "must render the correct page title" in {
      Jsoup.parse(render()).title mustEqual msgs("importEntryDate.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
      msgs("importEntryDate.title") mustEqual "Import entry date"
    }

    "must render the correct page caption" in {
      val caption = Jsoup.parse(render()).select("span.govuk-caption-l")

      caption.text mustEqual msgs("importEntryDate.caption")
      msgs("importEntryDate.caption") mustEqual "Add vehicle details"
    }

    "must render the introductory paragraph" in {
      Jsoup.parse(render()).select("p.govuk-body").text must include(msgs("importEntryDate.paragraph"))
      msgs("importEntryDate.paragraph") mustEqual
        "This is when the import entry was made. It may be earlier than the date it was accepted by customs."
    }

    "must render the question heading as an H2 inside the legend" in {
      val document = Jsoup.parse(render())

      document.select("legend.govuk-fieldset__legend--m h2.govuk-fieldset__heading").text mustEqual msgs(
        "importEntryDate.heading.2"
      )
      msgs("importEntryDate.heading.2") mustEqual "What is the import entry date?"
    }

    "must render the hint text" in {
      Jsoup.parse(render()).select(".govuk-hint").text mustEqual msgs("importEntryDate.hint")
      msgs("importEntryDate.hint") mustEqual "You can find this in the last 8 digits of the import entry number. For example, 27 03 2026"
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

    "must post the form to the import 1 submit call" in {
      val document = Jsoup.parse(render())

      document.select("form").attr("action") mustEqual submitCall.url
      document.select("form").attr("method") mustEqual "POST"
    }

    "must post the form to the import 1 in CheckMode submit call" in {
      val checkModeSubmitCall =
        vehicledetails.routes.ImportEntryDateController.onSubmit(ImportNumber(1), CheckMode)

      Jsoup.parse(render(mode = CheckMode)).select("form").attr("action") mustEqual checkModeSubmitCall.url
    }

    "must not set autocomplete on the form itself" in {
      Jsoup.parse(render()).select("form").hasAttr("autocomplete") mustBe false
    }

    "must show an error summary linking to the day field when the date is missing" in {
      val document = Jsoup.parse(render(bind("", "", "")))

      document.select(".govuk-error-summary").text must include(msgs("importEntryDate.error.required.all"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value.day"
      msgs("importEntryDate.error.required.all") mustEqual "Enter the import entry date"
    }

    "must highlight only the year field when only the year is missing" in {
      val html = render(bind("27", "03", ""))

      html must include(msgs("importEntryDate.error.required", msgs("date.error.year")))
      msgs("importEntryDate.error.required", msgs("date.error.year")) mustEqual
        "The import entry date must include a year"
      hasErrorClass(html, "year") mustEqual true
      hasErrorClass(html, "day") mustEqual false
      hasErrorClass(html, "month") mustEqual false
    }

    "must highlight only the month field when only the month is missing" in {
      val html = render(bind("27", "", "2026"))

      html must include(msgs("importEntryDate.error.required", msgs("date.error.month")))
      hasErrorClass(html, "month") mustEqual true
      hasErrorClass(html, "day") mustEqual false
      hasErrorClass(html, "year") mustEqual false
    }

    "must highlight only the day field when only the day is missing" in {
      val html = render(bind("", "03", "2026"))

      html must include(msgs("importEntryDate.error.required", msgs("date.error.day")))
      hasErrorClass(html, "day") mustEqual true
      hasErrorClass(html, "month") mustEqual false
      hasErrorClass(html, "year") mustEqual false
    }

    "must highlight the day and month fields when both are missing" in {
      val html = render(bind("", "", "2026"))

      html must include(msgs("importEntryDate.error.required.two", msgs("date.error.day"), msgs("date.error.month")))
      msgs("importEntryDate.error.required.two", msgs("date.error.day"), msgs("date.error.month")) mustEqual
        "The import entry date must include a day and month"
      hasErrorClass(html, "day") mustEqual true
      hasErrorClass(html, "month") mustEqual true
      hasErrorClass(html, "year") mustEqual false
    }

    "must highlight the day and year fields when both are missing" in {
      val html = render(bind("", "03", ""))

      html must include(msgs("importEntryDate.error.required.two", msgs("date.error.day"), msgs("date.error.year")))
      hasErrorClass(html, "day") mustEqual true
      hasErrorClass(html, "year") mustEqual true
      hasErrorClass(html, "month") mustEqual false
    }

    "must highlight the month and year fields when both are missing" in {
      val html = render(bind("27", "", ""))

      html must include(msgs("importEntryDate.error.required.two", msgs("date.error.month"), msgs("date.error.year")))
      hasErrorClass(html, "month") mustEqual true
      hasErrorClass(html, "year") mustEqual true
      hasErrorClass(html, "day") mustEqual false
    }

    "must highlight only the day field when the day cannot be a day of any month" in {
      val html = render(bind("32", "03", "2026"))

      html must include(msgs("importEntryDate.error.notARealDate"))
      hasErrorClass(html, "day") mustEqual true
      hasErrorClass(html, "month") mustEqual false
      hasErrorClass(html, "year") mustEqual false
    }

    "must highlight the whole date when the date does not exist but no single field is at fault" in {
      val html = render(bind("31", "02", "2026"))

      html must include(msgs("importEntryDate.error.notARealDate"))
      msgs("importEntryDate.error.notARealDate") mustEqual "The import entry date must be a real date"
      hasErrorClass(html, "day") mustEqual true
      hasErrorClass(html, "month") mustEqual true
      hasErrorClass(html, "year") mustEqual true
    }

    "must show the format error when the date is not made up of numbers" in {
      render(bind("aa", "03", "2026")) must include(msgs("importEntryDate.error.invalid"))
      msgs("importEntryDate.error.invalid") mustEqual "Enter the import entry date in the correct format"
    }

    "must highlight the whole date and show the future error when the date is in the future" in {
      val html = render(bind("01", "01", "2999"))

      html must include(msgs("importEntryDate.error.future"))
      msgs("importEntryDate.error.future") mustEqual "The import entry date must be today or in the past"
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
