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

class AddVehicleDetailsAgriculturalTractorFormProviderSpec extends StringFieldBehaviours {

  val form = new AddVehicleDetailsAgriculturalTractorFormProvider()()

  private val invalidValues = Seq("John Deere #1", "SR$", "5090M^", "model`", "tractor{1}", "pipe|", "~tilde", "accented è")

  Seq("make", "model", "derivative").foreach { fieldName =>
    s".$fieldName" - {

      behave like mandatoryField(
        form,
        fieldName,
        requiredError = FormError(fieldName, s"addVehicleDetailsAgriculturalTractor.$fieldName.error.required")
      )

      behave like fieldWithMaxLength(
        form,
        fieldName,
        maxLength = AddVehicleDetailsAgriculturalTractorFormProvider.MaxLength,
        lengthError = FormError(
          fieldName,
          s"addVehicleDetailsAgriculturalTractor.$fieldName.error.length",
          Seq(AddVehicleDetailsAgriculturalTractorFormProvider.MaxLength)
        )
      )

      "must not bind strings with disallowed special characters or accented letters" in {
        invalidValues.foreach { value =>
          val result = form.bind(Map(fieldName -> value)).apply(fieldName)
          result.errors must contain only FormError(
            fieldName,
            s"addVehicleDetailsAgriculturalTractor.$fieldName.error.format",
            Seq(AddVehicleDetailsAgriculturalTractorFormProvider.StandardFieldRegex)
          )
        }
      }

      "must bind a valid value" in {
        val result = form.bind(Map(fieldName -> s"Valid $fieldName 1 & Co.")).apply(fieldName)
        result.errors mustBe empty
      }
    }
  }

  ".brakeHorsepower" - {

    val fieldName = "brakeHorsepower"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "addVehicleDetailsAgriculturalTractor.brakeHorsepower.error.required")
    )

    "must bind whole numbers of up to 4 digits" in {
      Seq("0", "90", "240", "9999").foreach { value =>
        form.bind(Map(fieldName -> value)).apply(fieldName).errors mustBe empty
      }
    }

    "must bind numbers of up to 4 digits with exactly 2 decimal places" in {
      Seq("0.00", "240.75", "9999.99").foreach { value =>
        form.bind(Map(fieldName -> value)).apply(fieldName).errors mustBe empty
      }
    }

    "must not bind values that are not numbers or do not have exactly 2 decimal places" in {
      Seq("abc", "90bhp", "abcdefg", "240.7", "240.755", "240.", ".75", "-90", "1,000", "90 ", "12a45.00").foreach { value =>
        form.bind(Map(fieldName -> value)).apply(fieldName).errors must contain only FormError(
          fieldName,
          "addVehicleDetailsAgriculturalTractor.brakeHorsepower.error.format",
          Seq(AddVehicleDetailsAgriculturalTractorFormProvider.BrakeHorsepowerRegex)
        )
      }
    }

    "must not bind values with more than 4 characters before the decimal point" in {
      Seq("10000", "12345.00", "99999.99").foreach { value =>
        form.bind(Map(fieldName -> value)).apply(fieldName).errors must contain only FormError(
          fieldName,
          "addVehicleDetailsAgriculturalTractor.brakeHorsepower.error.length",
          Seq(AddVehicleDetailsAgriculturalTractorFormProvider.BrakeHorsepowerMaxDigits)
        )
      }
    }
  }
}
