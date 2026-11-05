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

class TotalValueOfOptionsFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "totalValueOfOptions.error.required"
  val lengthKey   = "totalValueOfOptions.error.length"
  val invalidKey  = "totalValueOfOptions.error.invalid"

  val form = new TotalValueOfOptionsFormProvider()()

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
      maxLength = TotalValueOfOptionsFormProvider.MaxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(TotalValueOfOptionsFormProvider.MaxLength))
    )

    "must bind whole numbers up to the maximum length, including 0" in {

      val validValues = List("0", "1", "12000", "12345678901234")

      validValues.foreach { value =>
        val result = form.bind(Map(fieldName -> value))
        result.errors mustBe empty
        result.value mustBe Some(value)
      }
    }

    "must not bind values that are not whole numbers" in {

      val invalidValues = List("12,000", "1200.50", "£12000", "12000abc", "12 000", "-12000")

      invalidValues.foreach { value =>
        val result = form.bind(Map(fieldName -> value))
        result.errors must contain only FormError(fieldName, invalidKey, Seq(TotalValueOfOptionsFormProvider.WholeNumberRegex))
      }
    }

    "must give the length error over the format error when an over long value is also invalid" in {

      val result = form.bind(Map(fieldName -> ("1,000" * 5)))

      result.errors must contain only FormError(fieldName, lengthKey, Seq(TotalValueOfOptionsFormProvider.MaxLength))
    }

    "must not bind an entry of only whitespace" in {

      val result = form.bind(Map(fieldName -> "   "))

      result.errors must contain only FormError(fieldName, requiredKey)
    }
  }
}
