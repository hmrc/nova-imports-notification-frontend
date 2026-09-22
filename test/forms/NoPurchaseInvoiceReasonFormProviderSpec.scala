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

class NoPurchaseInvoiceReasonFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "noPurchaseInvoiceReason.error.required"
  val lengthKey   = "noPurchaseInvoiceReason.error.length"
  val invalidKey  = "noPurchaseInvoiceReason.error.invalid"

  val form = new NoPurchaseInvoiceReasonFormProvider()()

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
      maxLength = NoPurchaseInvoiceReasonFormProvider.MaxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(NoPurchaseInvoiceReasonFormProvider.MaxLength))
    )

    "must bind reasons made up of the allowed characters" in {

      val validReasons = List(
        "The supplier did not issue an invoice",
        "Vehicle was a gift (family transfer)",
        "Invoice lost - see reference A/123",
        "Paid 50% up front; balance on delivery",
        "Bought from \"A & B Motors\"",
        "Contact: sales@example.com",
        "Was it ever issued? No_receipt <none>",
        "x",
        "A" * NoPurchaseInvoiceReasonFormProvider.MaxLength
      )

      validReasons.foreach { reason =>
        val result = form.bind(Map(fieldName -> reason))
        result.errors mustBe empty
        result.value mustBe Some(reason)
      }
    }

    "must not bind reasons containing characters outside the allowed set" in {

      val invalidReasons = List(
        "Invoice #123",
        "Cost $500",
        "Marked with a caret ^",
        "Quoted with a backtick `",
        "Wrapped in braces {like this}",
        "Split by a pipe |",
        "Approximately ~100",
        "Facture manquante è"
      )

      invalidReasons.foreach { reason =>
        val result = form.bind(Map(fieldName -> reason))
        result.errors must contain only FormError(fieldName, invalidKey, Seq(NoPurchaseInvoiceReasonFormProvider.NoPurchaseInvoiceReasonRegex))
      }
    }

    "must report the length error rather than the format error when a long reason also has invalid characters" in {

      val result = form.bind(Map(fieldName -> ("#" * 161)))

      result.errors must contain only FormError(fieldName, lengthKey, Seq(NoPurchaseInvoiceReasonFormProvider.MaxLength))
    }
  }
}
