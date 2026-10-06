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

class AddVehicleDetailsHeavyCommercialFormProviderSpec extends StringFieldBehaviours {

  val form = new AddVehicleDetailsHeavyCommercialFormProvider()()

  private val invalidValues = Seq("Renault Trucks #1", "Magnum$", "tractor^", "cab`", "axle{1}", "~tilde", "accented è")

  Seq(
    ("make", 50),
    ("model", 50),
    ("bodyType", 50),
    ("cabType", 100)
  ).foreach { case (fieldName, maxLength) =>
    s".$fieldName" - {

      behave like mandatoryField(
        form,
        fieldName,
        requiredError = FormError(fieldName, s"addVehicleDetailsHeavyCommercial.$fieldName.error.required")
      )

      behave like fieldWithMaxLength(
        form,
        fieldName,
        maxLength = maxLength,
        lengthError = FormError(fieldName, s"addVehicleDetailsHeavyCommercial.$fieldName.error.length", Seq(maxLength))
      )

      "must not bind strings with disallowed special characters or accented letters" in {
        val expectedRegex =
          if (fieldName == "cabType") AddVehicleDetailsHeavyCommercialFormProvider.CabTypeRegex
          else AddVehicleDetailsHeavyCommercialFormProvider.StandardFieldRegex

        invalidValues.foreach { value =>
          val result = form.bind(Map(fieldName -> value)).apply(fieldName)
          result.errors must contain only FormError(fieldName, s"addVehicleDetailsHeavyCommercial.$fieldName.error.format", Seq(expectedRegex))
        }
      }

      "must bind a valid value" in {
        val result = form.bind(Map(fieldName -> s"Valid $fieldName 1 & Co.")).apply(fieldName)
        result.errors mustBe empty
      }
    }
  }

  ".cabType" - {

    "must bind a value longer than the other fields allow" in {
      val result = form.bind(Map("cabType" -> "a" * 100)).apply("cabType")
      result.errors mustBe empty
    }
  }
}
