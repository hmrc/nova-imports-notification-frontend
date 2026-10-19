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

class ImportEntryNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "importEntryNumber.error.required"
  val lengthKey   = "importEntryNumber.error.length"
  val formatKey   = "importEntryNumber.error.format"

  val form = new ImportEntryNumberFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must bind valid import entry numbers" in {

      val validNumbers = List(
        "123-456789A",
        "123-456789a",
        "12GB3456ACD789EF21",
        "12XI3456ACD789EF21"
      )

      validNumbers.foreach { number =>
        val result = form.bind(Map(fieldName -> number))
        result.errors mustBe empty
        result.value mustBe Some(number)
      }
    }

    "must not bind import entry numbers in the wrong format" in {

      val invalidNumbers = List(
        "123456789AB",
        "123-45678 A",
        "12FR3456ACD789EF21",
        "12gb3456ACD789EF21"
      )

      invalidNumbers.foreach { number =>
        val result = form.bind(Map(fieldName -> number)).apply(fieldName)
        result.errors must contain only FormError(fieldName, formatKey, Seq(ImportEntryNumberFormProvider.ImportEntryNumberRegex))
      }
    }

    "must not bind import entry numbers that are not 11 or 18 characters" in {

      val wrongLengths = List(
        "123-456789",
        "123-456789AB",
        "12GB3456ACD789EF2",
        "12GB3456ACD789EF213"
      )

      wrongLengths.foreach { number =>
        val result = form.bind(Map(fieldName -> number)).apply(fieldName)
        result.errors must contain only FormError(fieldName, lengthKey)
      }
    }

    "must give the length error over the format error when the value is incorrect length" in {

      val result = form.bind(Map(fieldName -> "123-456789A ")).apply(fieldName)

      result.errors must contain only FormError(fieldName, lengthKey)
    }

    "must not bind an entry of only whitespace" in {

      val result = form.bind(Map(fieldName -> "   "))

      result.errors must contain only FormError(fieldName, requiredKey)
    }
  }
}
