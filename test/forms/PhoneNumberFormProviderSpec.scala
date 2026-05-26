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

class PhoneNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey  = "phoneNumber.error.required"
  val lengthKey    = "phoneNumber.error.length"
  val invalidKey   = "phoneNumber.error.invalid"
  val maxLength    = 20
  val allowedChars = "^[0-9 ]+$"

  val form = new PhoneNumberFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    "bind valid phone numbers consisting only of digits and spaces" in {
      val validValues = Seq("0121 456 7898", "02079460123", "01632960001", "01632 960 001", "07700 900 982", "020 7946 0958")

      validValues.foreach { value =>
        val result = form.bind(Map(fieldName -> value)).apply(fieldName)
        withClue(s"bind '$value': ") {
          result.errors mustBe empty
          result.value.value mustBe value
        }
      }
    }

    "not bind values containing characters other than digits or spaces" in {
      val invalidValues = Seq(
        "+44 7700 900 982",
        "+1 555 123 4567",
        "+447700900982",
        "020-7946-0958",
        "(01632) 960001",
        "01632.960.001",
        "abc12345",
        "0123 ext 4"
      )

      invalidValues.foreach { value =>
        val result = form.bind(Map(fieldName -> value)).apply(fieldName)
        withClue(s"bind '$value': ") {
          result.errors must contain(FormError(fieldName, invalidKey, Seq(allowedChars)))
        }
      }
    }

    "prefer the length error over the invalid-characters error when input is too long" in {
      val tooLongAndInvalid = "020-7946-0958-extra-bits"

      val result = form.bind(Map(fieldName -> tooLongAndInvalid)).apply(fieldName)
      result.errors must contain only FormError(fieldName, lengthKey, Seq(maxLength))
    }

    "prefer the required error over other errors when input is blank" in {
      val result = form.bind(Map(fieldName -> "")).apply(fieldName)
      result.errors must contain only FormError(fieldName, requiredKey)
    }
  }
}
