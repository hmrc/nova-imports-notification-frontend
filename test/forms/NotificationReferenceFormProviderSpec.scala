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

class NotificationReferenceFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "notificationReference.error.required"
  val lengthKey   = "notificationReference.error.length"
  val formatKey   = "notificationReference.error.format"

  val form = new NotificationReferenceFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must bind valid notification references" in {

      val validReferences = List(
        "NOVA26E123456",
        "NOVA00E100000",
        "NOVA99E699999"
      )

      validReferences.foreach { reference =>
        val result = form.bind(Map(fieldName -> reference))
        result.errors mustBe empty
        result.value mustBe Some(reference)
      }
    }

    "must not bind 13 character references in the wrong format" in {

      val invalidReferences = List(
        "nova26E123456",
        "NOVA26e123456",
        "NOVB26E123456",
        "NOVA2AE123456",
        "NOVA26F123456",
        "NOVA26E023456",
        "NOVA26E723456",
        "NOVA26E12345A",
        "NOVA26E 23456"
      )

      invalidReferences.foreach { reference =>
        val result = form.bind(Map(fieldName -> reference)).apply(fieldName)
        result.errors must contain only FormError(fieldName, formatKey, Seq(NotificationReferenceFormProvider.NotificationReferenceRegex))
      }
    }

    "must not bind references that are not 13 characters" in {

      val wrongLengths = List(
        "NOVA26E12345",
        "NOVA26E1234567",
        "NOVA",
        "NOVA26E123456 "
      )

      wrongLengths.foreach { reference =>
        val result = form.bind(Map(fieldName -> reference)).apply(fieldName)
        result.errors must contain only FormError(fieldName, lengthKey)
      }
    }

    "must give the length error over the format error when the value is the wrong length" in {

      val result = form.bind(Map(fieldName -> "ABC")).apply(fieldName)

      result.errors must contain only FormError(fieldName, lengthKey)
    }

    "must not bind an entry of only whitespace" in {

      val result = form.bind(Map(fieldName -> "   "))

      result.errors must contain only FormError(fieldName, requiredKey)
    }
  }
}
