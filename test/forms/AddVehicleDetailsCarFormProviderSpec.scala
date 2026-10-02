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

class AddVehicleDetailsCarFormProviderSpec extends StringFieldBehaviours {

  val form = new AddVehicleDetailsCarFormProvider()()

  private val invalidValues = Seq("Land Rover #1", "Discovery$", "3.0TD^", "trim`", "wagon{1}", "~tilde", "accented è")

  private def validValue(field: String): String = field match {
    case "bodyType" => "station wagon"
    case other      => s"Valid $other 1 & Co."
  }

  Seq(
    ("make", 50),
    ("model", 50),
    ("derivative", 50),
    ("trim", 50),
    ("bodyType", 20)
  ).foreach { case (fieldName, maxLength) =>
    s".$fieldName" - {

      behave like mandatoryField(
        form,
        fieldName,
        requiredError = FormError(fieldName, s"addVehicleDetailsCar.$fieldName.error.required")
      )

      behave like fieldWithMaxLength(
        form,
        fieldName,
        maxLength = maxLength,
        lengthError = FormError(fieldName, s"addVehicleDetailsCar.$fieldName.error.length", Seq(maxLength))
      )

      "must not bind strings with disallowed special characters or accented letters" in {
        val expectedRegex =
          if (fieldName == "bodyType") AddVehicleDetailsCarFormProvider.BodyTypeRegex else AddVehicleDetailsCarFormProvider.StandardFieldRegex

        invalidValues.foreach { value =>
          val result = form.bind(Map(fieldName -> value)).apply(fieldName)
          result.errors must contain only FormError(fieldName, s"addVehicleDetailsCar.$fieldName.error.format", Seq(expectedRegex))
        }
      }

      "must bind a valid value" in {
        val result = form.bind(Map(fieldName -> validValue(fieldName))).apply(fieldName)
        result.errors mustBe empty
      }
    }
  }
}
