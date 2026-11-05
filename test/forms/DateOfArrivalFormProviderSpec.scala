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

package forms

import forms.DateOfArrivalFormProvider.FirstRegistration
import forms.behaviours.DateBehaviours
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.{Clock, Instant, LocalDate, ZoneOffset}

class DateOfArrivalFormProviderSpec extends DateBehaviours {

  private implicit val messages: Messages = stubMessages()

  private val today: LocalDate = LocalDate.of(2026, 9, 18)

  private val formProvider = new DateOfArrivalFormProvider(Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant, ZoneOffset.UTC))

  private val requiredForm = formProvider(isRequired = true)
  private val optionalForm = formProvider(isRequired = false)

  private val day   = messages("date.error.day")
  private val month = messages("date.error.month")
  private val year  = messages("date.error.year")

  private val firstRegistrationDate = LocalDate.of(2026, 3, 10)

  private def bind(form: Form[Option[LocalDate]], day: String, month: String, year: String) =
    form.bind(Map("value.day" -> day, "value.month" -> month, "value.year" -> year))

  private def bind(day: String, month: String, year: String): Form[Option[LocalDate]] =
    bind(requiredForm, day, month, year)

  private def bindDuringBritishSummerTimeMidnight(day: String, month: String, year: String) = {
    val halfPastMidnightBst = Clock.fixed(Instant.parse("2026-09-18T23:30:00Z"), ZoneOffset.UTC)
    bind(new DateOfArrivalFormProvider(halfPastMidnightBst)(isRequired = true), day, month, year)
  }

  "when the date is required" - {

    behave like mandatoryDateField(requiredForm, "value", "dateOfArrival.error.required.all")

    "must bind a date written as dd MM yyyy" in {

      forAll(datesBetween(LocalDate.of(1900, 1, 1), today) -> "valid date") { date =>

        val result = bind(f"${date.getDayOfMonth}%02d", f"${date.getMonthValue}%02d", date.getYear.toString)

        result.value.value mustEqual Some(date)
        result.errors mustBe empty
      }
    }

    "must put a bound date back into the fields padded, so that it binds again unchanged" in {

      val filled = requiredForm.fill(Some(LocalDate.of(2026, 3, 7)))

      filled("value.day").value.value mustEqual "07"
      filled("value.month").value.value mustEqual "03"
      filled("value.year").value.value mustEqual "2026"
    }

    "must fail to bind when only the day is missing" in {
      bind("", "03", "2026").errors must contain only FormError("value", "dateOfArrival.error.required", List(day))
    }

    "must fail to bind when only the month is missing" in {
      bind("27", "", "2026").errors must contain only FormError("value", "dateOfArrival.error.required", List(month))
    }

    "must fail to bind when only the year is missing, using the year's own message" in {
      bind("27", "03", "").errors must contain only FormError("value", "dateOfArrival.error.required.year", List(year))
    }

    "must fail to bind when the day and month are missing" in {
      bind("", "", "2026").errors must contain only FormError("value", "dateOfArrival.error.required.two", List(day, month))
    }

    "must fail to bind when the day and year are missing" in {
      bind("", "03", "").errors must contain only FormError("value", "dateOfArrival.error.required.two", List(day, year))
    }

    "must fail to bind when the month and year are missing" in {
      bind("27", "", "").errors must contain only FormError("value", "dateOfArrival.error.required.two", List(month, year))
    }

    "must fail to bind a single digit day" in {
      bind("7", "03", "2026").errors must contain only FormError("value", "dateOfArrival.error.invalid", List(day))
    }

    "must fail to bind a single digit month" in {
      bind("27", "3", "2026").errors must contain only FormError("value", "dateOfArrival.error.invalid", List(month))
    }

    "must fail to bind a month typed as a name" in {
      bind("27", "March", "2026").errors must contain only FormError("value", "dateOfArrival.error.invalid", List(month))
    }

    "must fail to bind a year that is not four digits" in {
      bind("27", "03", "26").errors must contain only FormError("value", "dateOfArrival.error.invalid", List(year))
    }

    "must fail to bind a single digit day and month, highlighting the date as a whole" in {
      bind("7", "3", "2026").errors must contain only FormError("value", "dateOfArrival.error.invalid", List.empty)
    }

    "must fail to bind a date that does not exist, highlighting the date as a whole" in {
      bind("31", "02", "2026").errors must contain only FormError("value", "dateOfArrival.error.notARealDate", List.empty)
    }

    "must fail to bind a day that is not a day of any month, highlighting only the day" in {
      bind("32", "03", "2026").errors must contain only FormError("value", "dateOfArrival.error.notARealDate", List(day))
    }

    "must fail to bind a month that is not a month of the year, highlighting only the month" in {
      bind("27", "13", "2026").errors must contain only FormError("value", "dateOfArrival.error.notARealDate", List(month))
    }

    "must bind today's date" in {
      val result = bind("18", "09", "2026")

      result.value.value mustEqual Some(today)
      result.errors mustBe empty
    }

    "must fail to bind tomorrow's date, highlighting the date as a whole" in {
      bind("19", "09", "2026").errors must contain only FormError("value", "dateOfArrival.error.future", List.empty)
    }

    "must accept today's UK date when UTC is still on the previous day" in {

      val result = bindDuringBritishSummerTimeMidnight("19", "09", "2026")

      result.value.value mustEqual Some(LocalDate.of(2026, 9, 19))
      result.errors mustBe empty
    }

    "must reject the day after today's UK date when UTC is still on the previous day" in {
      bindDuringBritishSummerTimeMidnight("20", "09", "2026").errors must contain only FormError("value", "dateOfArrival.error.future", List.empty)
    }
  }

  "when the date is not required" - {

    "must bind an empty date to no answer" in {

      val result = optionalForm.bind(Map.empty[String, String])

      result.value.value mustEqual None
      result.errors mustBe empty
    }

    "must bind a date that is entered" in {
      bind(optionalForm, "07", "03", "2026").value.value mustEqual Some(LocalDate.of(2026, 3, 7))
    }

    "must still reject a partly entered date" in {
      bind(optionalForm, "27", "03", "").errors must contain only FormError("value", "dateOfArrival.error.required.year", List(year))
    }

    "must still reject a future date" in {
      bind(optionalForm, "19", "09", "2026").errors must contain only FormError("value", "dateOfArrival.error.future", List.empty)
    }
  }

  "when the vehicle was first registered in an EU country or the UK" - {

    val form = formProvider(isRequired = true, Some(FirstRegistration(firstRegistrationDate, countryIsEuOrUk = true)))

    "must fail to bind an arrival on the first registration date" in {
      bind(form, "10", "03", "2026").errors must contain only FormError("value", "dateOfArrival.error.afterFirstRegistration")
    }

    "must fail to bind an arrival before the first registration date" in {
      bind(form, "09", "03", "2026").errors must contain only FormError("value", "dateOfArrival.error.afterFirstRegistration")
    }

    "must bind an arrival the day after the first registration date" in {
      bind(form, "11", "03", "2026").value.value mustEqual Some(LocalDate.of(2026, 3, 11))
    }

    "must report only the future error when the date is in the future and before the first registration date" in {

      val laterRegistrationForm = formProvider(isRequired = true, Some(FirstRegistration(LocalDate.of(2026, 12, 1), countryIsEuOrUk = true)))

      bind(laterRegistrationForm, "19", "09", "2026").errors must contain only FormError("value", "dateOfArrival.error.future")
    }
  }

  "when the vehicle was first registered outside the EU and the UK" - {

    val form = formProvider(isRequired = true, Some(FirstRegistration(firstRegistrationDate, countryIsEuOrUk = false)))

    "must bind an arrival on the first registration date" in {
      bind(form, "10", "03", "2026").value.value mustEqual Some(firstRegistrationDate)
    }

    "must fail to bind an arrival before the first registration date" in {
      bind(form, "09", "03", "2026").errors must contain only FormError("value", "dateOfArrival.error.sameAsOrAfterFirstRegistration")
    }
  }
}
