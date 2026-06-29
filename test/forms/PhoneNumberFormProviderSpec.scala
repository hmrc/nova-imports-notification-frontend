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
import models.ContactNumbers
import play.api.data.FormError

class PhoneNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey      = "phoneNumber.error.required"
  val lengthKey        = "phoneNumber.error.length"
  val invalidKey       = "phoneNumber.error.invalid"
  val mobileLengthKey  = "phoneNumber.error.mobileLength"
  val mobileInvalidKey = "phoneNumber.error.mobileInvalid"
  val maxLength        = 20
  val allowedChars     = "^[0-9 ]+$"

  val form = new PhoneNumberFormProvider()()

  "PhoneNumberFormProvider" - {

    "must bind both a phone number and a mobile number" in {
      val result = form.bind(Map("phoneNumber" -> "01632 960 001", "mobileNumber" -> "07700 900 982"))
      result.errors mustBe empty
      result.value.value mustBe ContactNumbers(Some("01632 960 001"), Some("07700 900 982"))
    }

    "must bind when only the phone number is provided" in {
      val result = form.bind(Map("phoneNumber" -> "01632 960 001", "mobileNumber" -> ""))
      result.errors mustBe empty
      result.value.value mustBe ContactNumbers(Some("01632 960 001"), None)
    }

    "must bind when only the mobile number is provided" in {
      val result = form.bind(Map("phoneNumber" -> "", "mobileNumber" -> "07700 900 982"))
      result.errors mustBe empty
      result.value.value mustBe ContactNumbers(None, Some("07700 900 982"))
    }

    "must fail with error when both numbers are missing" in {
      val result = form.bind(Map("phoneNumber" -> "", "mobileNumber" -> ""))
      result.errors must contain only FormError("", requiredKey)
    }

    "must bind valid phone numbers in correct format" in {
      val validValues = Seq("0121 456 7898", "02079460123", "01632960001", "01632 960 001", "07700 900 982", "020 7946 0958")

      validValues.foreach { value =>
        val result = form.bind(Map("phoneNumber" -> value))
        withClue(s"bind '$value': ") {
          result.errors mustBe empty
          result.value.value mustBe ContactNumbers(Some(value), None)
        }
      }
    }

    ".phoneNumber" - {
      val fieldName = "phoneNumber"

      "must not bind a phone number longer than 20 characters" in {
        val result = form.bind(Map(fieldName -> "012345678901234567890")).apply(fieldName)
        result.errors must contain(FormError(fieldName, lengthKey, Seq(maxLength)))
      }

      "must not bind a phone number containing characters other than digits or spaces" in {
        val invalidValues = Seq("+44 7700 900 982", "020-7946-0958", "(01632) 960001", "abc12345")

        invalidValues.foreach { value =>
          val result = form.bind(Map(fieldName -> value)).apply(fieldName)
          withClue(s"bind '$value': ") {
            result.errors must contain(FormError(fieldName, invalidKey, Seq(allowedChars)))
          }
        }
      }

      "must give the length error first over the invalid-chars error when input is too long" in {
        val result = form.bind(Map(fieldName -> "020-7946-0958-extra-stuff")).apply(fieldName)
        result.errors must contain only FormError(fieldName, lengthKey, Seq(maxLength))
      }
    }

    ".mobileNumber" - {
      val fieldName = "mobileNumber"

      "must not bind a mobile number longer than 20 characters" in {
        val result = form.bind(Map(fieldName -> "012345678901234567890")).apply(fieldName)
        result.errors must contain(FormError(fieldName, mobileLengthKey, Seq(maxLength)))
      }

      "must not bind a mobile number containing invalid characters" in {
        val invalidValues = Seq("+44 7700 900 982", "070-0000-0000", "(0770) 0900982", "abc12345")

        invalidValues.foreach { value =>
          val result = form.bind(Map(fieldName -> value)).apply(fieldName)
          withClue(s"bind '$value': ") {
            result.errors must contain(FormError(fieldName, mobileInvalidKey, Seq(allowedChars)))
          }
        }
      }
    }
  }
}
