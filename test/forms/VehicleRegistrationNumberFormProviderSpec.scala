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

class VehicleRegistrationNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "vehicleRegistrationNumber.error.required"
  val lengthKey   = "vehicleRegistrationNumber.error.length"
  val invalidKey  = "vehicleRegistrationNumber.error.invalid"

  val form = new VehicleRegistrationNumberFormProvider()()

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

    "must bind valid registration numbers including 1 and 12 characters, mixed case, spaces and hyphens" in {
      List("A", "ABC12DE", "BC12 DEF", "12-AB-34", "abc12de", "AbC12 de-34").foreach { vrn =>
        val result = form.bind(Map(fieldName -> vrn))
        result.errors mustBe empty
        result.value mustBe Some(vrn)
      }
    }

    "must not bind more than 12 characters and show the length error" in {
      List("ABCDEFGHIJKL1", "A" * 13, "A" * 100).foreach { vrn =>
        form.bind(Map(fieldName -> vrn)).apply(fieldName).errors must contain only
          FormError(fieldName, lengthKey, Seq(VehicleRegistrationNumberFormProvider.MaxLength))
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
        "$"
      )
        .foreach { char =>
          form.bind(Map(fieldName -> s"ABC12$char")).apply(fieldName).errors must contain only
            FormError(fieldName, invalidKey, Seq(VehicleRegistrationNumberFormProvider.VehicleRegistrationNumberRegex))
        }
    }

    "must show the length error rather than the format error when too long and invalid" in {
      form.bind(Map(fieldName -> ("£" * 13))).apply(fieldName).errors.map(_.message) must contain only lengthKey
    }
  }
}
