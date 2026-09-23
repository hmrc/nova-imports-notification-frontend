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

class TotalAmountPaidFormProviderSpec extends StringFieldBehaviours {

  val requiredKey       = "totalAmountPaid.error.required"
  val lengthKey         = "totalAmountPaid.error.length"
  val commaOrDecimalKey = "totalAmountPaid.error.commaOrDecimalPoint"
  val invalidKey        = "totalAmountPaid.error.invalid"

  val form = new TotalAmountPaidFormProvider()()

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
      maxLength = TotalAmountPaidFormProvider.MaxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(TotalAmountPaidFormProvider.MaxLength))
    )

    "must bind whole numbers up to the maximum length" in {

      val validValues = List("1", "15000", "12345678901234")

      validValues.foreach { value =>
        val result = form.bind(Map(fieldName -> value))
        result.errors mustBe empty
        result.value mustBe Some(value)
      }
    }

    "must not bind values containing commas or decimal points" in {

      val invalidValues = List("15,000", "150.00", "1,234.56")

      invalidValues.foreach { value =>
        val result = form.bind(Map(fieldName -> value))
        result.errors must contain only FormError(fieldName, commaOrDecimalKey, Seq(TotalAmountPaidFormProvider.NoCommaOrDecimalPointRegex))
      }
    }

    "must not bind values containing other invalid characters" in {

      val invalidValues = List("15000abc", "£15000", "15 000", "-15000")

      invalidValues.foreach { value =>
        val result = form.bind(Map(fieldName -> value))
        result.errors must contain only FormError(fieldName, invalidKey, Seq(TotalAmountPaidFormProvider.WholeNumberRegex))
      }
    }

    "must report the length error rather than the comma/decimal error when an over long entry also contains a comma" in {

      val result = form.bind(Map(fieldName -> ("1,000" * 5)))

      result.errors must contain only FormError(fieldName, lengthKey, Seq(TotalAmountPaidFormProvider.MaxLength))
    }

    "must report the comma/decimal error rather than the general format error when a value has both" in {

      val result = form.bind(Map(fieldName -> "15,000abc"))

      result.errors must contain only FormError(fieldName, commaOrDecimalKey, Seq(TotalAmountPaidFormProvider.NoCommaOrDecimalPointRegex))
    }

    "must not bind an entry of only whitespace" in {

      val result = form.bind(Map(fieldName -> "   "))

      result.errors must contain only FormError(fieldName, requiredKey)
    }
  }
}
