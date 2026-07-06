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

class PurchaserBusinessNameFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "purchaserBusinessName.error.required"
  val lengthKey   = "purchaserBusinessName.error.length"
  val invalidKey  = "purchaserBusinessName.error.invalid"

  val form = new PurchaserBusinessNameFormProvider()()

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
      maxLength = PurchaserBusinessNameFormProvider.MaxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(PurchaserBusinessNameFormProvider.MaxLength))
    )

    "must bind valid business names" in {

      val validNames = List(
        "Acme Ltd",
        "Smith & Sons (UK)",
        "O'Brien Motors",
        "A.B.C. Cars/Vans Ltd"
      )

      validNames.foreach { name =>
        val result = form.bind(Map(fieldName -> name))
        result.errors mustBe empty
        result.value mustBe Some(name)
      }
    }

    "must not bind names containing characters outside the allowed set" in {

      val invalidNames = List(
        "Acme#Ltd",
        "Café Motors",
        "naïve~co",
        "Ünder Ltd"
      )

      invalidNames.foreach { name =>
        val result = form.bind(Map(fieldName -> name)).apply(fieldName)
        result.errors must contain only FormError(fieldName, invalidKey, Seq(PurchaserBusinessNameFormProvider.PurchaserBusinessNameRegex))
      }
    }

    "must prefer the length error over the format error when the value exceeds 160 characters" in {

      val tooLong = "a" * 161

      val result = form.bind(Map(fieldName -> tooLong)).apply(fieldName)

      result.errors must contain only FormError(fieldName, lengthKey, Seq(PurchaserBusinessNameFormProvider.MaxLength))
    }

    "must return the format error for a valid-character name between 101 and 160 characters" in {

      val name = "a" * 120

      val result = form.bind(Map(fieldName -> name)).apply(fieldName)

      result.errors must contain only FormError(fieldName, invalidKey, Seq(PurchaserBusinessNameFormProvider.PurchaserBusinessNameRegex))
    }
  }
}
