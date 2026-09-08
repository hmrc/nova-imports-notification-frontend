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

class PurchaseInvoiceNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "purchaseInvoiceNumber.error.required"
  val lengthKey   = "purchaseInvoiceNumber.error.length"
  val invalidKey  = "purchaseInvoiceNumber.error.invalid"

  val form = new PurchaseInvoiceNumberFormProvider()()

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
      maxLength = PurchaseInvoiceNumberFormProvider.MaxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(PurchaseInvoiceNumberFormProvider.MaxLength))
    )

    "must bind invoice numbers made up of the allowed characters" in {

      val validNumbers = List(
        "INV12345",
        "inv-2026-001",
        "12345678",
        "INV/2026/001",
        "O'BRIEN01",
        "INV\\2026",
        "A",
        "12345678901234567890"
      )

      validNumbers.foreach { number =>
        val result = form.bind(Map(fieldName -> number))
        result.errors mustBe empty
        result.value mustBe Some(number)
      }
    }

    "must not bind invoice numbers containing characters outside the allowed set" in {

      val invalidNumbers = List(
        "INV 12345",
        "INV_12345",
        "INV#12345",
        "INV.12345",
        "INV(1)",
        "INV+123",
        "INVÉ123"
      )

      invalidNumbers.foreach { number =>
        val result = form.bind(Map(fieldName -> number))
        result.errors must contain only FormError(
          fieldName,
          invalidKey,
          Seq(PurchaseInvoiceNumberFormProvider.PurchaseInvoiceNumberRegex)
        )
      }
    }

    "must report the length error rather than the format error when an over long entry is also badly formatted" in {

      val result = form.bind(Map(fieldName -> ("INV 12345" * 5)))

      result.errors must contain only FormError(fieldName, lengthKey, Seq(PurchaseInvoiceNumberFormProvider.MaxLength))
    }

    "must not bind an entry of only whitespace" in {

      val result = form.bind(Map(fieldName -> "   "))

      result.errors must contain only FormError(fieldName, requiredKey)
    }
  }
}
