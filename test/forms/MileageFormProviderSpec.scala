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

import forms.behaviours.{OptionFieldBehaviours, StringFieldBehaviours}
import models.{Mileage, MileageUnit}
import play.api.data.FormError

class MileageFormProviderSpec extends OptionFieldBehaviours, StringFieldBehaviours {

  val requiredKey     = "mileage.error.required.mileage"
  val lengthKey       = "mileage.error.length"
  val formatKey       = "mileage.error.format"
  val requiredUnitKey = "mileage.error.required.unit"

  val invalidKey = "error.invalid"

  val form = new MileageFormProvider()()

  ".value" - {

    val fieldName   = "value"
    val unitMapping = "unit" -> MileageUnit.MILES.toString

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must bind valid mileage values" in {
      val validMileage = List(
        "1",
        "123",
        "999999",
        "0",
        "000000"
      )

      validMileage.foreach { reference =>
        val result = form.bind(Map(fieldName -> reference, unitMapping))
        result.errors mustBe empty
        result.value mustBe Some(Mileage(MileageUnit.MILES, reference))
      }
    }

    "must not bind mileage values in the wrong format" in {
      val invalidReferences = List(
        "-50",
        "inval",
        "10.2"
      )

      invalidReferences.foreach { reference =>
        val result = form.bind(Map(fieldName -> reference, unitMapping)).apply(fieldName)
        result.errors must contain only FormError(fieldName, formatKey, Seq(MileageFormProvider.inputRegex))
      }
    }

    "must not bind references that are over 6 characters" in {
      val wrongLengths = List(
        "1234567",
        "0000000"
      )

      wrongLengths.foreach { reference =>
        val result = form.bind(Map(fieldName -> reference, unitMapping)).apply(fieldName)
        result.errors must contain only FormError(fieldName, lengthKey)
      }
    }

    "must give the length error over the format error when the value is the wrong length" in {
      val result = form.bind(Map(fieldName -> "ABCDEFG", unitMapping)).apply(fieldName)
      result.errors must contain only FormError(fieldName, lengthKey)
    }

    "must not bind an entry of only whitespace" in {
      val result = form.bind(Map(fieldName -> "   ", unitMapping))
      result.errors must contain only FormError(fieldName, requiredKey)
    }
  }

  ".unit" - {

    val fieldName = "unit"

    behave like optionsField[MileageUnit](
      form,
      fieldName,
      validValues = MileageUnit.values.toSeq,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredUnitKey)
    )
  }
}
