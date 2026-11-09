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
import forms.DateOfArrivalFormProvider
import forms.DateOfArrivalFormProvider.FirstRegistration
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText
import viewmodels.govuk.inset.*
import views.html.DateOfArrivalView

import java.time.{Clock, LocalDate, ZoneOffset}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*

class DateOfArrivalViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: DateOfArrivalView = app.injector.instanceOf[DateOfArrivalView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private val formProvider = new DateOfArrivalFormProvider(
    Clock.fixed(LocalDate.of(2026, 9, 18).atStartOfDay(ZoneOffset.UTC).toInstant, ZoneOffset.UTC)
  )

  val form: Form[Option[LocalDate]] = formProvider(isRequired = true)

  val submitCall: Call = Call("POST", "/nova-imports/supplier/1/vehicle/1/date-of-arrival")

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <strong>Volvo FH16</strong>")

  private val secureRegistrationSchemeUrl = "https://example.gov.uk/srs"
  private val registerAVehicleUrl         = "https://example.gov.uk/rav"

  private def doc(
    form: Form[Option[LocalDate]] = form,
    vehicleDetails: Option[InsetText] = Some(vehicleDetails),
    submitCall: Call = submitCall
  ): Document =
    Jsoup.parse(view(form, vehicleDetails, submitCall, secureRegistrationSchemeUrl, registerAVehicleUrl)(request, msgs).toString)

  private def bind(form: Form[Option[LocalDate]], day: String, month: String, year: String): Form[Option[LocalDate]] =
    form.bind(Map("value.day" -> day, "value.month" -> month, "value.year" -> year))

  private def bind(day: String, month: String, year: String): Form[Option[LocalDate]] =
    bind(form, day, month, year)

  private val errorClass = "govuk-input--error"

  private def hasErrorClass(document: Document, field: String): Boolean =
    Option(document.getElementById(s"value.$field")).exists(_.hasClass(errorClass))

  "DateOfArrivalView" - {

    "must render the page title" in {
      doc().title mustEqual msgs("dateOfArrival.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
      msgs("dateOfArrival.title") mustEqual "Date the vehicle arrived in the UK"
    }

    "must render the vehicle details inset above the caption" in {
      val document = doc()

      document.select(".govuk-inset-text").text mustEqual "Vehicle details: Volvo FH16"
      document.select(".govuk-inset-text + .govuk-caption-l").isEmpty mustEqual false
    }

    "must not render an inset when there are no vehicle details" in {
      doc(vehicleDetails = None).select(".govuk-inset-text").isEmpty mustEqual true
    }

    "must render the caption" in {
      doc().select("span.govuk-caption-l").text mustEqual "Add vehicle details"
    }

    "must render a single heading" in {
      val headings = doc().select("h1")

      headings.size mustEqual 1
      headings.hasClass("govuk-heading-l") mustEqual true
      headings.text mustEqual "Date the vehicle arrived in the UK"
    }

    "must render the brought from paragraph and its bullet list" in {
      val document = doc()

      document.select("p.govuk-body").get(0).text mustEqual "If you brought the vehicle from:"
      document.select("p.govuk-body + ul").get(0).className mustEqual "govuk-list govuk-list--bullet"
      document.select("ul.govuk-list--bullet").get(0).select("li").asScala.map(_.text).toList mustEqual List(
        "an EU country, enter the date the vehicle arrived in the UK",
        "outside the EU, enter the date HMRC accepted the import entry"
      )
    }

    "must render the change of intention paragraph and its bullet list" in {
      val document = doc()

      document.select("p.govuk-body").get(1).text mustEqual
        "If there is a change of intention for using the vehicle after arrival, enter the date of the change. A change of intention is when:"
      document.select("p.govuk-body + ul").get(1).className mustEqual "govuk-list govuk-list--bullet"
      document.select("ul.govuk-list--bullet").get(1).select("li").asScala.map(_.text).toList mustEqual List(
        "you bring a vehicle into the UK for a short period but decide to keep it longer",
        "you change your plans to register the vehicle under the secure registration scheme (SRS) (opens in new tab) or Register a Vehicle (RaV) service (opens in new tab) after arrival",
        "a dealer or customer informs you that the vehicle cannot be registered using the SRS or RaV service"
      )
    }

    "must link to the secure registration scheme in a new tab" in {
      val link = doc().select(s"""a.govuk-link[href="$secureRegistrationSchemeUrl"]""")

      link.size mustEqual 1
      link.attr("target") mustEqual "_blank"
      link.text mustEqual "secure registration scheme (SRS) (opens in new tab)"
    }

    "must link to the Register a Vehicle service in a new tab" in {
      val link = doc().select(s"""a.govuk-link[href="$registerAVehicleUrl"]""")

      link.size mustEqual 1
      link.attr("target") mustEqual "_blank"
      link.text mustEqual "Register a Vehicle (RaV) service (opens in new tab)"
    }

    "must render the question heading as an H2 inside a medium legend" in {
      doc().select("legend.govuk-fieldset__legend--m h2.govuk-fieldset__heading").text mustEqual "When did the vehicle arrive in the UK?"
    }

    "must render the hint text" in {
      doc().select(".govuk-hint").text mustEqual "For example, 27 03 2026"
    }

    "must render the day, month and year inputs with their labels" in {
      val document = doc()

      document.getElementById("value.day").attr("name") mustEqual "value.day"
      document.getElementById("value.month").attr("name") mustEqual "value.month"
      document.getElementById("value.year").attr("name") mustEqual "value.year"
      document.select(".govuk-date-input__label").asScala.map(_.text).toList mustEqual List(msgs("date.day"), msgs("date.month"), msgs("date.year"))
    }

    "must set autocomplete off on each input and not on the form" in {
      val document = doc()

      List("day", "month", "year").foreach { field =>
        document.getElementById(s"value.$field").attr("autocomplete") mustEqual "off"
      }
      document.select("form").hasAttr("autocomplete") mustEqual false
    }

    "must render the continue button" in {
      doc().select("form button.govuk-button").text mustEqual msgs("site.continue")
    }

    "must post the form to the supplied supplier submit call" in {
      val form = doc().select("form")

      form.attr("action") mustEqual submitCall.url
      form.attr("method") mustEqual "POST"
    }

    "must post the form to the supplied import submit call" in {
      val importSubmitCall = Call("POST", "/nova-imports/import/1/vehicle/1/date-of-arrival")

      doc(submitCall = importSubmitCall).select("form").attr("action") mustEqual importSubmitCall.url
    }

    "must render a back link" in {
      doc().select("a.govuk-back-link").size mustEqual 1
    }

    "must show an error summary linking to the day field when the date is missing" in {
      val document = doc(bind("", "", ""))

      document.select(".govuk-error-summary").text must include("Enter the date the vehicle arrived in the UK")
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value.day"
    }

    "must highlight only the year field and show the year message when only the year is missing" in {
      val document = doc(bind("27", "03", ""))

      document.select(".govuk-error-message").text must include("The date the vehicle arrived in the UK must include a year")
      hasErrorClass(document, "year") mustEqual true
      hasErrorClass(document, "day") mustEqual false
      hasErrorClass(document, "month") mustEqual false
    }

    "must show the missing day message" in {
      doc(bind("", "03", "2026")).select(".govuk-error-message").text must include("The date the vehicle arrived in the UK must include a day")
    }

    "must show the missing month and year message" in {
      doc(bind("27", "", "")).select(".govuk-error-message").text must include(
        "The date the vehicle arrived in the UK must include a month and year"
      )
    }

    "must show the format error when the date is not in the correct format" in {
      doc(bind("7", "03", "2026")).select(".govuk-error-message").text must include(
        "Enter the date the vehicle arrived in the UK in the correct format"
      )
    }

    "must show the real date error when the date does not exist" in {
      doc(bind("31", "02", "2026")).select(".govuk-error-message").text must include(
        "The date the vehicle arrived in the UK must be a real date"
      )
    }

    "must highlight the whole date and show the future error when the date is in the future" in {
      val document = doc(bind("01", "01", "2999"))

      document.select(".govuk-error-message").text must include("The date the vehicle arrived in the UK must be today or in the past")
      List("day", "month", "year").foreach(field => hasErrorClass(document, field) mustEqual true)
    }

    "must highlight the whole date and show the after first registration error" in {
      val firstRegistrationForm = formProvider(isRequired = true, Some(FirstRegistration(LocalDate.of(2026, 3, 10), countryIsEuOrUk = true)))
      val document              = doc(bind(firstRegistrationForm, "10", "03", "2026"))

      document.select(".govuk-error-message").text must include(
        "The date the vehicle arrived in the UK must be after the date the vehicle was first registered"
      )
      List("day", "month", "year").foreach(field => hasErrorClass(document, field) mustEqual true)
    }

    "must show the same as or after first registration error" in {
      val firstRegistrationForm = formProvider(isRequired = true, Some(FirstRegistration(LocalDate.of(2026, 3, 10), countryIsEuOrUk = false)))

      doc(bind(firstRegistrationForm, "09", "03", "2026")).select(".govuk-error-message").text must include(
        "The date the vehicle arrived in the UK must be the same as or after the date the vehicle was first registered"
      )
    }

    "must fill the fields from a saved date" in {
      val document = doc(form.fill(Some(LocalDate.of(2026, 3, 7))))

      document.getElementById("value.day").attr("value") mustEqual "07"
      document.getElementById("value.month").attr("value") mustEqual "03"
      document.getElementById("value.year").attr("value") mustEqual "2026"
    }
  }
}
