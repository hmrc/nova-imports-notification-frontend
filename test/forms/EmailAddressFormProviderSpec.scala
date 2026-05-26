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

class EmailAddressFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "emailAddress.error.required"
  val lengthKey   = "emailAddress.error.length"
  val invalidKey  = "emailAddress.error.invalid"

  val form = new EmailAddressFormProvider()()

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
      maxLength = EmailAddressFormProvider.MaxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(EmailAddressFormProvider.MaxLength))
    )

    "must bind valid email addresses" in {

      val validEmails = Seq(
        "a@b",
        "name@example.com",
        "first.last@sub.example.com",
        "a-b_c@example-host.co.uk"
      )

      validEmails.foreach { email =>
        val result = form.bind(Map(fieldName -> email))
        result.errors mustBe empty
        result.value mustBe Some(email)
      }
    }

    "must not bind email addresses containing characters outside the allowed set" in {

      val invalidEmails = Seq(
        "with space@example.com",
        "name+plus@example.com",
        "name!boss@example.com",
        "name(test)@example.com"
      )

      invalidEmails.foreach { email =>
        val result = form.bind(Map(fieldName -> email)).apply(fieldName)
        result.errors must contain only FormError(fieldName, invalidKey, Seq(EmailAddressFormProvider.EmailRegex))
      }
    }

    "must prefer the length error over the regex error when the value exceeds 70 characters" in {

      val tooLong = "a" * 71 + "@b.com"

      val result = form.bind(Map(fieldName -> tooLong)).apply(fieldName)

      result.errors must contain only FormError(fieldName, lengthKey, Seq(EmailAddressFormProvider.MaxLength))
    }
  }
}
