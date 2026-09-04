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

import forms.behaviours.DateBehaviours
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.LocalDate

class PurchaseInvoiceDateFormProviderSpec extends DateBehaviours {

  private implicit val messages: Messages = stubMessages()

  private val form = new PurchaseInvoiceDateFormProvider()()

  private val day   = messages("date.error.day")
  private val month = messages("date.error.month")
  private val year  = messages("date.error.year")

  private def bind(day: String, month: String, year: String) =
    form.bind(Map("value.day" -> day, "value.month" -> month, "value.year" -> year))

  ".value" - {

    behave like mandatoryDateField(form, "value", "purchaseInvoiceDate.error.required.all")

    "must bind a date written as dd MM yyyy" in {

      forAll(datesBetween(LocalDate.of(1900, 1, 1), LocalDate.of(2100, 1, 1)) -> "valid date") { date =>

        val result = bind(f"${date.getDayOfMonth}%02d", f"${date.getMonthValue}%02d", date.getYear.toString)

        result.value.value mustEqual date
        result.errors mustBe empty
      }
    }

    "must put a bound date back into the fields padded, so that it binds again unchanged" in {

      val filled = form.fill(LocalDate.of(2026, 3, 7))

      filled("value.day").value.value mustEqual "07"
      filled("value.month").value.value mustEqual "03"
      filled("value.year").value.value mustEqual "2026"
    }

    "must fail to bind a single digit day" in {
      bind("7", "03", "2026").errors must contain only FormError("value", "purchaseInvoiceDate.error.invalid", List(day))
    }

    "must fail to bind a single digit month" in {
      bind("27", "3", "2026").errors must contain only FormError("value", "purchaseInvoiceDate.error.invalid", List(month))
    }

    "must fail to bind a month typed as a name" in {
      bind("27", "March", "2026").errors must contain only FormError("value", "purchaseInvoiceDate.error.invalid", List(month))
    }

    "must fail to bind a single digit day and month, highlighting the date as a whole" in {
      bind("7", "3", "2026").errors must contain only FormError("value", "purchaseInvoiceDate.error.invalid", List.empty)
    }

    "must fail to bind when only the day is missing" in {
      bind("", "03", "2026").errors must contain only FormError("value", "purchaseInvoiceDate.error.required", List(day))
    }

    "must fail to bind when only the month is missing" in {
      bind("27", "", "2026").errors must contain only FormError("value", "purchaseInvoiceDate.error.required", List(month))
    }

    "must fail to bind when only the year is missing" in {
      bind("27", "03", "").errors must contain only FormError("value", "purchaseInvoiceDate.error.required", List(year))
    }

    "must fail to bind when the day and month are missing" in {
      bind("", "", "2026").errors must contain only FormError("value", "purchaseInvoiceDate.error.required.two", List(day, month))
    }

    "must fail to bind when the day and year are missing" in {
      bind("", "03", "").errors must contain only FormError("value", "purchaseInvoiceDate.error.required.two", List(day, year))
    }

    "must fail to bind when the month and year are missing" in {
      bind("27", "", "").errors must contain only FormError("value", "purchaseInvoiceDate.error.required.two", List(month, year))
    }

    "must fail to bind a date that is not made up of numbers" in {
      bind("aa", "03", "2026").errors must contain only FormError("value", "purchaseInvoiceDate.error.invalid", List(day))
    }

    "must fail to bind a year that is not four digits" in {
      bind("27", "03", "26").errors must contain only FormError("value", "purchaseInvoiceDate.error.invalid", List(year))
    }

    "must fail to bind a date that does not exist, highlighting the date as a whole" in {
      bind("31", "02", "2026").errors must contain only FormError("value", "purchaseInvoiceDate.error.notARealDate", List.empty)
    }

    "must fail to bind a day that is not a day of any month, highlighting only the day" in {
      bind("32", "03", "2026").errors must contain only FormError("value", "purchaseInvoiceDate.error.notARealDate", List(day))
    }

    "must fail to bind a month that is not a month of the year, highlighting only the month" in {
      bind("27", "13", "2026").errors must contain only FormError("value", "purchaseInvoiceDate.error.notARealDate", List(month))
    }
  }
}
