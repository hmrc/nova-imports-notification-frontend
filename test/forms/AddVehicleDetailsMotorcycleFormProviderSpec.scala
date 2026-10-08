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

import forms.behaviours.StringFieldBehaviours
import models.{MotorcycleFuelType, MotorcycleTransmissionType}
import play.api.data.FormError

class AddVehicleDetailsMotorcycleFormProviderSpec extends StringFieldBehaviours {

  val form = new AddVehicleDetailsMotorcycleFormProvider()()

  private val invalidValues = Seq("Honda #1", "GL1200$", "1200^", "version`", "tourer{1}", "~tilde", "accented è")

  private def validValue(field: String): String = field match {
    case "motorcycleStyle" => "a tourer"
    case other             => s"Valid $other 1 & Co."
  }

  Seq(
    ("make", 50),
    ("model", 50),
    ("derivative", 50),
    ("motorcycleVersion", 50),
    ("motorcycleType", 50),
    ("motorcycleStyle", 20)
  ).foreach { case (fieldName, maxLength) =>
    s".$fieldName" - {

      behave like mandatoryField(
        form,
        fieldName,
        requiredError = FormError(fieldName, s"addVehicleDetailsMotorcycle.$fieldName.error.required")
      )

      behave like fieldWithMaxLength(
        form,
        fieldName,
        maxLength = maxLength,
        lengthError = FormError(fieldName, s"addVehicleDetailsMotorcycle.$fieldName.error.length", Seq(maxLength))
      )

      "must not bind strings with disallowed special characters or accented letters" in {
        invalidValues.foreach { value =>
          form.bind(Map(fieldName -> value)).apply(fieldName).errors.map(_.message) mustBe Seq(s"addVehicleDetailsMotorcycle.$fieldName.error.format")
        }
      }

      "must bind a valid value" in {
        form.bind(Map(fieldName -> validValue(fieldName))).apply(fieldName).errors mustBe empty
      }
    }
  }

  ".transmission" - {

    "must bind each valid option" in {
      MotorcycleTransmissionType.values.foreach { option =>
        form.bind(Map("transmission" -> option.jsonValue)).apply("transmission").errors mustBe empty
      }
    }

    "must return the required error when nothing is selected" in {
      form.bind(Map("transmission" -> "")).apply("transmission").errors.map(_.message) mustBe
        Seq("addVehicleDetailsMotorcycle.transmission.error.required")
    }

    "must return the required error when the value is not a known option" in {
      form.bind(Map("transmission" -> "cvt")).apply("transmission").errors.map(_.message) mustBe
        Seq("addVehicleDetailsMotorcycle.transmission.error.required")
    }
  }

  ".fuelType" - {

    "must bind each valid option" in {
      MotorcycleFuelType.values.foreach { option =>
        form.bind(Map("fuelType" -> option.jsonValue)).apply("fuelType").errors mustBe empty
      }
    }

    "must return the required error when nothing is selected" in {
      form.bind(Map("fuelType" -> "")).apply("fuelType").errors.map(_.message) mustBe
        Seq("addVehicleDetailsMotorcycle.fuelType.error.required")
    }
  }

  ".engineSize" - {

    "must bind whole numbers and numbers with exactly 2 decimal places" in {
      Seq("0", "1000", "99999", "0.00", "1000.12", "99999.99").foreach { value =>
        form.bind(Map("engineSize" -> value)).apply("engineSize").errors mustBe empty
      }
    }

    "must return the required error when empty" in {
      form.bind(Map("engineSize" -> "")).apply("engineSize").errors.map(_.message) mustBe
        Seq("addVehicleDetailsMotorcycle.engineSize.error.required")
    }

    "must return the format error for non-numeric or badly formatted values" in {
      Seq("abc", "1,000", "-100", "1000.1", "1000.123", "12.345", "1.2.3").foreach { value =>
        form.bind(Map("engineSize" -> value)).apply("engineSize").errors.map(_.message) mustBe
          Seq("addVehicleDetailsMotorcycle.engineSize.error.format")
      }
    }

    "must return the length error when there are more than 5 digits before the decimal point" in {
      Seq("123456", "123456.78").foreach { value =>
        form.bind(Map("engineSize" -> value)).apply("engineSize").errors.map(_.message) mustBe
          Seq("addVehicleDetailsMotorcycle.engineSize.error.length")
      }
    }

    "must return the format error, not the length error, for a non-numeric value longer than 5 characters before any decimal point" in {
      Seq("abcdefg", "abcdefg.12").foreach { value =>
        form.bind(Map("engineSize" -> value)).apply("engineSize").errors.map(_.message) mustBe
          Seq("addVehicleDetailsMotorcycle.engineSize.error.format")
      }
    }
  }
}
