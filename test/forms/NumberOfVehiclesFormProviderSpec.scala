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

import forms.behaviours.OptionFieldBehaviours
import models.{NumberOfVehicles, OneOrMultipleVehicles}
import play.api.data.FormError

class NumberOfVehiclesFormProviderSpec extends OptionFieldBehaviours {

  val requiredKey       = "numberOfVehicles.error.required"
  val invalidKey        = "error.invalid"
  val numberRequiredKey = "numberOfVehicles.error.input.required"
  val numberFormatKey   = "numberOfVehicles.error.input.format"

  val radioField  = "oneOrMultipleVehicles"
  val numberField = "numberOfVehiclesToMultiply"

  val form = new NumberOfVehiclesFormProvider()()

  private def bindMultiple(number: String) =
    form.bind(Map(radioField -> "Multiple", numberField -> number))

  ".oneOrMultipleVehicles" - {

    behave like optionsField[OneOrMultipleVehicles](
      form,
      radioField,
      validValues = OneOrMultipleVehicles.values.toSeq,
      invalidError = FormError(radioField, invalidKey)
    )

    behave like mandatoryField(
      form,
      radioField,
      requiredError = FormError(radioField, requiredKey)
    )
  }

  ".numberOfVehiclesToMultiply" - {

    "must bind 2 when more than one vehicle is selected" in {
      bindMultiple("2").value.value mustEqual NumberOfVehicles(OneOrMultipleVehicles.Multiple, Some(2))
    }

    "must bind 100 when more than one vehicle is selected" in {
      bindMultiple("100").value.value mustEqual NumberOfVehicles(OneOrMultipleVehicles.Multiple, Some(100))
    }

    "must return the blank error when more than one vehicle is selected and the number is empty" in {
      form.bind(Map(radioField -> "Multiple")).errors mustEqual Seq(FormError(numberField, numberRequiredKey))
      bindMultiple("").errors mustEqual Seq(FormError(numberField, numberRequiredKey))
      bindMultiple("   ").errors mustEqual Seq(FormError(numberField, numberRequiredKey))
    }

    "must return the format error when the number is below 2 or above 100" in {
      Seq("0", "1", "101", "1000").foreach { number =>
        bindMultiple(number).errors mustEqual Seq(FormError(numberField, numberFormatKey))
      }
    }

    "must return the format error when the entry is not a whole number" in {
      Seq("abc", "2.5", "-5", "02", " 5").foreach { number =>
        bindMultiple(number).errors mustEqual Seq(FormError(numberField, numberFormatKey))
      }
    }

    "must ignore the number when one vehicle is selected" in {
      form.bind(Map(radioField -> "One", numberField -> "abc")).value.value mustEqual NumberOfVehicles(OneOrMultipleVehicles.One, None)
    }

    "must ignore the number when nothing is selected" in {
      form.bind(Map(numberField -> "abc")).errors mustEqual Seq(FormError(radioField, requiredKey))
    }
  }

  "must fill both fields" in {
    form.fill(NumberOfVehicles(OneOrMultipleVehicles.Multiple, Some(5))).data mustEqual Map(radioField -> "Multiple", numberField -> "5")
  }
}
