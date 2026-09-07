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

package forms.mappings

import java.time.LocalDate

import generators.Generators
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

class DateMappingsSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with Generators with OptionValues with Mappings {

  private implicit val messages: Messages = stubMessages()

  val form = Form(
    "value" -> localDate(
      requiredKey = "error.required",
      allRequiredKey = "error.required.all",
      twoRequiredKey = "error.required.two",
      invalidKey = "error.invalid",
      notARealDateKey = "error.notARealDate"
    )
  )

  val validData = datesBetween(
    min = LocalDate.of(2000, 1, 1),
    max = LocalDate.of(3000, 1, 1)
  )

  val invalidField: Gen[String] = Gen.alphaStr.suchThat(_.nonEmpty)

  val missingField: Gen[Option[String]] = Gen.option(Gen.const(""))

  "must bind valid dates with months provided as numbers" in {

    forAll(validData -> "valid date") { date =>

      val data = Map(
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonthValue.toString,
        "value.year"  -> date.getYear.toString
      )

      val result = form.bind(data)

      result.value.value mustEqual date
    }
  }

  "must bind valid dates with months provided as numbers with leading zeroes" in {

    forAll(validData -> "valid date") { date =>

      val data = Map(
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> s"0${date.getMonthValue.toString}",
        "value.year"  -> date.getYear.toString
      )

      val result = form.bind(data)

      result.value.value mustEqual date
    }
  }

  "must bind valid dates with months provided as full names in upper case" in {

    forAll(validData -> "valid date") { date =>

      val data = Map(
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonth.toString,
        "value.year"  -> date.getYear.toString
      )

      val result = form.bind(data)

      result.value.value mustEqual date
    }
  }

  "must bind valid dates with months provided as full names in lower case" in {

    forAll(validData -> "valid date") { date =>

      val data = Map(
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonth.toString.toLowerCase,
        "value.year"  -> date.getYear.toString
      )

      val result = form.bind(data)

      result.value.value mustEqual date
    }
  }

  "must bind valid dates with months provided as three characters in upper case" in {

    forAll(validData -> "valid date") { date =>

      val data = Map(
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonth.toString.take(3),
        "value.year"  -> date.getYear.toString
      )

      val result = form.bind(data)

      result.value.value mustEqual date
    }
  }

  "must bind valid dates with months provided as three characters in lower case" in {

    forAll(validData -> "valid date") { date =>

      val data = Map(
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonth.toString.take(3).toLowerCase,
        "value.year"  -> date.getYear.toString
      )

      val result = form.bind(data)

      result.value.value mustEqual date
    }
  }

  "must fail to bind an empty date" in {

    val result = form.bind(Map.empty[String, String])

    result.errors must contain only FormError("value", "error.required.all", List.empty)
  }

  "must fail to bind a date with a missing day" in {

    forAll(validData -> "valid date", missingField -> "missing field") { (date, field) =>

      val initialData = Map(
        "value.month" -> date.getMonthValue.toString,
        "value.year"  -> date.getYear.toString
      )

      val data = field.fold(initialData) { value =>
        initialData + ("value.day" -> value)
      }

      val result = form.bind(data)

      result.errors must contain only FormError("value", "error.required", List(messages("date.error.day")))
    }
  }

  "must fail to bind a date with an invalid day, naming the day so that it is the only field highlighted" in {

    forAll(validData -> "valid date", invalidField -> "invalid field") { (date, field) =>

      val data = Map(
        "value.day"   -> field,
        "value.month" -> date.getMonthValue.toString,
        "value.year"  -> date.getYear.toString
      )

      val result = form.bind(data)

      result.errors must contain(
        FormError("value", "error.invalid", List(messages("date.error.day")))
      )
    }
  }

  "must fail to bind a date with a missing month" in {

    forAll(validData -> "valid date", missingField -> "missing field") { (date, field) =>

      val initialData = Map(
        "value.day"  -> date.getDayOfMonth.toString,
        "value.year" -> date.getYear.toString
      )

      val data = field.fold(initialData) { value =>
        initialData + ("value.month" -> value)
      }

      val result = form.bind(data)

      result.errors must contain only FormError("value", "error.required", List(messages("date.error.month")))
    }
  }

  "must fail to bind a date with an invalid month, naming the month so that it is the only field highlighted" in {

    forAll(validData -> "valid data", invalidField -> "invalid field") { (date, field) =>

      val data = Map(
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> field,
        "value.year"  -> date.getYear.toString
      )

      val result = form.bind(data)

      result.errors must contain(
        FormError("value", "error.invalid", List(messages("date.error.month")))
      )
    }
  }

  "must fail to bind a date with a missing year" in {

    forAll(validData -> "valid date", missingField -> "missing field") { (date, field) =>

      val initialData = Map(
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonthValue.toString
      )

      val data = field.fold(initialData) { value =>
        initialData + ("value.year" -> value)
      }

      val result = form.bind(data)

      result.errors must contain only FormError("value", "error.required", List(messages("date.error.year")))
    }
  }

  "must fail to bind a date with an invalid year, naming the year so that it is the only field highlighted" in {

    forAll(validData -> "valid data", invalidField -> "invalid field") { (date, field) =>

      val data = Map(
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonthValue.toString,
        "value.year"  -> field
      )

      val result = form.bind(data)

      result.errors must contain(
        FormError("value", "error.invalid", List(messages("date.error.year")))
      )
    }
  }

  "must fail to bind a date with a year that is not four digits" in {

    val data = Map(
      "value.day"   -> "27",
      "value.month" -> "3",
      "value.year"  -> "26"
    )

    val result = form.bind(data)

    result.errors must contain only FormError("value", "error.invalid", List(messages("date.error.year")))
  }

  "must fail to bind a date with a missing day and month" in {

    forAll(validData -> "valid date", missingField -> "missing day", missingField -> "missing month") { (date, dayOpt, monthOpt) =>

      val day = dayOpt.fold(Map.empty[String, String]) { value =>
        Map("value.day" -> value)
      }

      val month = monthOpt.fold(Map.empty[String, String]) { value =>
        Map("value.month" -> value)
      }

      val data: Map[String, String] = Map(
        "value.year" -> date.getYear.toString
      ) ++ day ++ month

      val result = form.bind(data)

      result.errors must contain only FormError("value", "error.required.two", List(messages("date.error.day"), messages("date.error.month")))
    }
  }

  "must fail to bind a date with a missing day and year" in {

    forAll(validData -> "valid date", missingField -> "missing day", missingField -> "missing year") { (date, dayOpt, yearOpt) =>

      val day = dayOpt.fold(Map.empty[String, String]) { value =>
        Map("value.day" -> value)
      }

      val year = yearOpt.fold(Map.empty[String, String]) { value =>
        Map("value.year" -> value)
      }

      val data: Map[String, String] = Map(
        "value.month" -> date.getMonthValue.toString
      ) ++ day ++ year

      val result = form.bind(data)

      result.errors must contain only FormError("value", "error.required.two", List(messages("date.error.day"), messages("date.error.year")))
    }
  }

  "must fail to bind a date with a missing month and year" in {

    forAll(validData -> "valid date", missingField -> "missing month", missingField -> "missing year") { (date, monthOpt, yearOpt) =>

      val month = monthOpt.fold(Map.empty[String, String]) { value =>
        Map("value.month" -> value)
      }

      val year = yearOpt.fold(Map.empty[String, String]) { value =>
        Map("value.year" -> value)
      }

      val data: Map[String, String] = Map(
        "value.day" -> date.getDayOfMonth.toString
      ) ++ month ++ year

      val result = form.bind(data)

      result.errors must contain only FormError("value", "error.required.two", List(messages("date.error.month"), messages("date.error.year")))
    }
  }

  "must fail to bind an invalid day and month" in {

    forAll(validData -> "valid date", invalidField -> "invalid day", invalidField -> "invalid month") { (date, day, month) =>

      val data = Map(
        "value.day"   -> day,
        "value.month" -> month,
        "value.year"  -> date.getYear.toString
      )

      val result = form.bind(data)

      result.errors must contain only FormError("value", "error.invalid", List.empty)
    }
  }

  "must fail to bind an invalid day and year" in {

    forAll(validData -> "valid date", invalidField -> "invalid day", invalidField -> "invalid year") { (date, day, year) =>

      val data = Map(
        "value.day"   -> day,
        "value.month" -> date.getMonthValue.toString,
        "value.year"  -> year
      )

      val result = form.bind(data)

      result.errors must contain only FormError("value", "error.invalid", List.empty)
    }
  }

  "must fail to bind an invalid month and year" in {

    forAll(validData -> "valid date", invalidField -> "invalid month", invalidField -> "invalid year") { (date, month, year) =>

      val data = Map(
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> month,
        "value.year"  -> year
      )

      val result = form.bind(data)

      result.errors must contain only FormError("value", "error.invalid", List.empty)
    }
  }

  "must fail to bind an invalid day, month and year" in {

    forAll(invalidField -> "valid day", invalidField -> "invalid month", invalidField -> "invalid year") { (day, month, year) =>

      val data = Map(
        "value.day"   -> day,
        "value.month" -> month,
        "value.year"  -> year
      )

      val result = form.bind(data)

      result.errors must contain only FormError("value", "error.invalid", List.empty)
    }
  }

  "must fail to bind a date that does not exist, highlighting the date as a whole" in {

    val data = Map(
      "value.day"   -> "30",
      "value.month" -> "2",
      "value.year"  -> "2018"
    )

    val result = form.bind(data)

    result.errors must contain only FormError("value", "error.notARealDate", List.empty)
  }

  "must fail to bind a day outside of the days in a month, naming the day so that it is the only field highlighted" in {

    val data = Map(
      "value.day"   -> "32",
      "value.month" -> "3",
      "value.year"  -> "2026"
    )

    val result = form.bind(data)

    result.errors must contain only FormError("value", "error.notARealDate", List(messages("date.error.day")))
  }

  "must fail to bind a month outside of the months in a year, naming the month so that it is the only field highlighted" in {

    val data = Map(
      "value.day"   -> "27",
      "value.month" -> "13",
      "value.year"  -> "2026"
    )

    val result = form.bind(data)

    result.errors must contain only FormError("value", "error.notARealDate", List(messages("date.error.month")))
  }

  "must fail to bind a day and a month that are both out of range, highlighting the date as a whole" in {

    val data = Map(
      "value.day"   -> "32",
      "value.month" -> "13",
      "value.year"  -> "2026"
    )

    val result = form.bind(data)

    result.errors must contain only FormError("value", "error.notARealDate", List.empty)
  }

  "must treat a field of only whitespace as missing rather than badly formatted" in {

    val data = Map(
      "value.day"   -> "  ",
      "value.month" -> "3",
      "value.year"  -> "2026"
    )

    val result = form.bind(data)

    result.errors must contain only FormError("value", "error.required", List(messages("date.error.day")))
  }

  "must unbind a date" in {

    forAll(validData -> "valid date") { date =>

      val filledForm = form.fill(date)

      filledForm("value.day").value.value mustEqual date.getDayOfMonth.toString
      filledForm("value.month").value.value mustEqual date.getMonthValue.toString
      filledForm("value.year").value.value mustEqual date.getYear.toString
    }
  }

  "when the date is held as dd/MM/yyyy" - {

    val ddMmYyyyForm = Form(
      "value" -> localDate(
        requiredKey = "error.required",
        allRequiredKey = "error.required.all",
        twoRequiredKey = "error.required.two",
        invalidKey = "error.invalid",
        notARealDateKey = "error.notARealDate",
        requireDdMmYyyy = true
      )
    )

    def bind(day: String, month: String, year: String) =
      ddMmYyyyForm.bind(Map("value.day" -> day, "value.month" -> month, "value.year" -> year))

    "must bind a day and month padded to two digits" in {
      bind("07", "03", "2026").value.value mustEqual LocalDate.of(2026, 3, 7)
    }

    "must fail to bind a single digit day" in {
      bind("7", "03", "2026").errors must contain only FormError("value", "error.invalid", List(messages("date.error.day")))
    }

    "must fail to bind a single digit month" in {
      bind("27", "3", "2026").errors must contain only FormError("value", "error.invalid", List(messages("date.error.month")))
    }

    "must fail to bind a month written as a name, which the lenient mapping accepts" in {
      bind("27", "March", "2026").errors must contain only FormError("value", "error.invalid", List(messages("date.error.month")))

      form.bind(Map("value.day" -> "27", "value.month" -> "March", "value.year" -> "2026")).value.value mustEqual LocalDate.of(2026, 3, 27)
    }

    "must unbind a date with the day and month padded, so that it binds again unchanged" in {

      val filledForm = ddMmYyyyForm.fill(LocalDate.of(2026, 3, 7))

      filledForm("value.day").value.value mustEqual "07"
      filledForm("value.month").value.value mustEqual "03"
      filledForm("value.year").value.value mustEqual "2026"
    }
  }
}
