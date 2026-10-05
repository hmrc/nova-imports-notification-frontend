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

import forms.mappings.Mappings
import models.{AddVehicleDetailsMotorcycle, MotorcycleFuelType, MotorcycleTransmissionType}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}

import javax.inject.Inject

class AddVehicleDetailsMotorcycleFormProvider @Inject() extends Mappings {

  import AddVehicleDetailsMotorcycleFormProvider.*

  def apply(): Form[AddVehicleDetailsMotorcycle] = Form(
    mapping(
      "make"              -> textFieldMapping("make", MaxLength, StandardFieldRegex),
      "model"             -> textFieldMapping("model", MaxLength, StandardFieldRegex),
      "derivative"        -> textFieldMapping("derivative", MaxLength, StandardFieldRegex),
      "motorcycleVersion" -> textFieldMapping("motorcycleVersion", MaxLength, StandardFieldRegex),
      "motorcycleType"    -> textFieldMapping("motorcycleType", MaxLength, StandardFieldRegex),
      "motorcycleStyle"   -> textFieldMapping("motorcycleStyle", StyleMaxLength, StandardFieldRegex),
      "transmission"      -> enumerable[MotorcycleTransmissionType](
        "addVehicleDetailsMotorcycle.transmission.error.required",
        "addVehicleDetailsMotorcycle.transmission.error.required"
      ),
      "fuelType" -> enumerable[MotorcycleFuelType](
        "addVehicleDetailsMotorcycle.fuelType.error.required",
        "addVehicleDetailsMotorcycle.fuelType.error.required"
      ),
      "engineSize" -> engineSizeMapping
    )(AddVehicleDetailsMotorcycle.apply)(addVehicleDetailsMotorcycle =>
      Some(
        (
          addVehicleDetailsMotorcycle.make,
          addVehicleDetailsMotorcycle.model,
          addVehicleDetailsMotorcycle.derivative,
          addVehicleDetailsMotorcycle.motorcycleVersion,
          addVehicleDetailsMotorcycle.motorcycleType,
          addVehicleDetailsMotorcycle.motorcycleStyle,
          addVehicleDetailsMotorcycle.transmission,
          addVehicleDetailsMotorcycle.fuelType,
          addVehicleDetailsMotorcycle.engineSize
        )
      )
    )
  )

  private def textFieldMapping(field: String, maxLen: Int, regex: String) =
    text(s"addVehicleDetailsMotorcycle.$field.error.required")
      .verifying(
        firstError(
          maxLength(maxLen, s"addVehicleDetailsMotorcycle.$field.error.length"),
          regexp(regex, s"addVehicleDetailsMotorcycle.$field.error.format")
        )
      )

  private def engineSizeMapping =
    text("addVehicleDetailsMotorcycle.engineSize.error.required")
      .verifying(
        firstError(
          engineSizeWholePartLength,
          regexp(EngineSizeRegex, "addVehicleDetailsMotorcycle.engineSize.error.format")
        )
      )

  private val engineSizeWholePartLength: Constraint[String] = Constraint[String] { input =>
    val wholePart = input.takeWhile(_ != '.')
    if (wholePart.nonEmpty && wholePart.forall(_.isDigit) && wholePart.length > EngineSizeMaxWholeDigits)
      Invalid("addVehicleDetailsMotorcycle.engineSize.error.length")
    else
      Valid
  }
}

object AddVehicleDetailsMotorcycleFormProvider {
  val MaxLength: Int      = 50
  val StyleMaxLength: Int = 20

  val EngineSizeMaxWholeDigits: Int = 5

  val StandardFieldRegex: String = """^[A-Za-z0-9 .()/&'\-;!%*_+:@<>?=\[\],\\]{1,50}$"""
  val EngineSizeRegex: String    = """^[0-9]{1,5}$|^([0-9]{1,5}\.[0-9]{2})$"""
}
