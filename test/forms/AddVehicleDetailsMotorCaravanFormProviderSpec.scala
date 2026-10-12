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
import models.AddVehicleDetailsMotorCaravan
import play.api.data.FormError

class AddVehicleDetailsMotorCaravanFormProviderSpec extends StringFieldBehaviours {

  val form = new AddVehicleDetailsMotorCaravanFormProvider()()

  private val invalidValues = Seq("Swift #1", "Voyager$", "685FB^", "Fiat`", "2.3TD{1}", "~tilde", "accented è")

  private val validFormData: Map[String, String] = Map(
    "make"              -> "Swift",
    "bodyType"          -> "a coachbuilt",
    "model"             -> "Voyager",
    "version"           -> "Voyager 685FB 4 berth",
    "makeOfBaseVehicle" -> "Fiat",
    "derivative"        -> "2.3TD"
  )

  Seq(
    ("make", 50),
    ("model", 50),
    ("version", 50),
    ("makeOfBaseVehicle", 50),
    ("derivative", 50)
  ).foreach { case (fieldName, maxLength) =>
    s".$fieldName" - {

      behave like mandatoryField(
        form,
        fieldName,
        requiredError = FormError(fieldName, s"addVehicleDetailsMotorCaravan.$fieldName.error.required")
      )

      behave like fieldWithMaxLength(
        form,
        fieldName,
        maxLength = maxLength,
        lengthError = FormError(fieldName, s"addVehicleDetailsMotorCaravan.$fieldName.error.length", Seq(maxLength))
      )

      "must not bind strings with disallowed special characters or accented letters" in {
        invalidValues.foreach { value =>
          val result = form.bind(validFormData.updated(fieldName, value)).apply(fieldName)
          result.errors must contain only FormError(
            fieldName,
            s"addVehicleDetailsMotorCaravan.$fieldName.error.format",
            Seq(AddVehicleDetailsMotorCaravanFormProvider.StandardFieldRegex)
          )
        }
      }
    }
  }

  ".bodyType" - {

    "must bind when the body type is missing and default to 'N/A'" in {
      val result = form.bind(validFormData - "bodyType")
      result.errors mustBe empty
      result.value.value.bodyType mustBe "N/A"
    }

    "must bind when the body type is blank and default to 'N/A'" in {
      val result = form.bind(validFormData.updated("bodyType", ""))
      result.errors mustBe empty
      result.value.value.bodyType mustBe "N/A"
    }

    "must bind a valid body type" in {
      val result = form.bind(validFormData.updated("bodyType", "a coachbuilt"))
      result.errors mustBe empty
      result.value.value.bodyType mustBe "a coachbuilt"
    }

    "must not bind a body type longer than 30 characters" in {
      val result = form.bind(validFormData.updated("bodyType", "a" * 31)).apply("bodyType")
      result.errors must contain only FormError("bodyType", "addVehicleDetailsMotorCaravan.bodyType.error.length", Seq(30))
    }

    "must not bind a body type with disallowed special characters or accented letters" in {
      invalidValues.foreach { value =>
        val result = form.bind(validFormData.updated("bodyType", value)).apply("bodyType")
        result.errors must contain only FormError(
          "bodyType",
          "addVehicleDetailsMotorCaravan.bodyType.error.format",
          Seq(AddVehicleDetailsMotorCaravanFormProvider.BodyTypeRegex)
        )
      }
    }
  }

  "must bind valid data and round-trip through fill/unbind" in {
    val result = form.bind(validFormData).value.value

    result mustBe AddVehicleDetailsMotorCaravan(
      make = "Swift",
      bodyType = "a coachbuilt",
      model = "Voyager",
      version = "Voyager 685FB 4 berth",
      makeOfBaseVehicle = "Fiat",
      derivative = "2.3TD"
    )

    form.fill(result).data mustBe validFormData
  }

  "must fill with no 'bodyType' field when the stored value is 'N/A'" in {
    val answer = AddVehicleDetailsMotorCaravan(
      make = "Swift",
      bodyType = "N/A",
      model = "Voyager",
      version = "Voyager 685FB 4 berth",
      makeOfBaseVehicle = "Fiat",
      derivative = "2.3TD"
    )

    form.fill(answer).data.get("bodyType") mustBe None
  }
}
