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
import play.api.data.FormError

class VehicleIdentificationNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey  = "vehicleIdentificationNumber.error.required"
  val lengthKey    = "vehicleIdentificationNumber.error.length"
  val invalidKey   = "vehicleIdentificationNumber.error.invalid"
  val duplicateKey = "vehicleIdentificationNumber.error.duplicate"

  val form = new VehicleIdentificationNumberFormProvider()(_ => false)

  ".value" - {

    val fieldName = "value"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must not bind a blank value" in {
      form.bind(Map(fieldName -> "   ")).errors must contain only FormError(fieldName, requiredKey)
    }

    "must bind valid VINs including 1 and 17 characters and mixed case" in {
      List("A", "1ABCD2E03FG456789", "1abcd2e03fg456789", "1aBcD2e03Fg456789", "12345678901234567").foreach { vin =>
        val result = form.bind(Map(fieldName -> vin))
        result.errors mustBe empty
        result.value mustBe Some(vin)
      }
    }

    "must not bind more than 17 characters and show the length error" in {
      List("1ABCD2E03FG4567890", "A" * 18, "A" * 100).foreach { vin =>
        form.bind(Map(fieldName -> vin)).apply(fieldName).errors must contain only
          FormError(fieldName, lengthKey, Seq(VehicleIdentificationNumberFormProvider.MaxLength))
      }
    }

    "must bind the letters I, O and Q in either case" in {
      List("I", "O", "Q", "i", "o", "q", "1ABCD2E03FG45678I", "1ABCD2E03FG45678o", "Q1ABCD2E03FG45678").foreach { vin =>
        val result = form.bind(Map(fieldName -> vin))
        result.errors mustBe empty
        result.value mustBe Some(vin)
      }
    }

    "must not bind disallowed characters and show the format error" in {
      List(
        "~",
        "!",
        "@",
        "%",
        "&",
        "'",
        "(",
        ")",
        "*",
        "+",
        ",",
        "-",
        ".",
        "/",
        ":",
        "=",
        "?",
        "\\",
        "[",
        "]",
        "^",
        "_",
        "{",
        "}",
        ";",
        "£",
        "€",
        "#",
        "`",
        "$",
        "1ABC D2E03FG45678",
        "1ABCD2E03FG4567-"
      ).foreach { vin =>
        form.bind(Map(fieldName -> vin)).apply(fieldName).errors.map(_.message) must contain only invalidKey
      }
    }

    "must show the length error rather than the format error when too long and invalid" in {
      form.bind(Map(fieldName -> ("O" * 18))).apply(fieldName).errors.map(_.message) must contain only lengthKey
    }

    "must not bind a duplicate VIN and show the duplicate error" in {
      val duplicateForm = new VehicleIdentificationNumberFormProvider()(_ => true)

      duplicateForm.bind(Map(fieldName -> "1ABCD2E03FG456789")).apply(fieldName).errors must contain only FormError(fieldName, duplicateKey)
    }

    "must pass the entered value to the duplicate check" in {
      val checkedForm = new VehicleIdentificationNumberFormProvider()(_ == "1ABCD2E03FG456789")

      checkedForm.bind(Map(fieldName -> "1ABCD2E03FG456789")).errors.map(_.message) mustBe List(duplicateKey)
      checkedForm.bind(Map(fieldName -> "1ABCD2E03FG456788")).errors mustBe empty
    }

    "must show the format error rather than the duplicate error when invalid" in {
      val duplicateForm = new VehicleIdentificationNumberFormProvider()(_ => true)

      duplicateForm.bind(Map(fieldName -> "-")).apply(fieldName).errors.map(_.message) must contain only invalidKey
    }
  }
}
