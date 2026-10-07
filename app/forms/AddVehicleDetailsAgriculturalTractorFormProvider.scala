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
import models.AddVehicleDetailsAgriculturalTractor
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}

import javax.inject.Inject

class AddVehicleDetailsAgriculturalTractorFormProvider @Inject() extends Mappings {

  import AddVehicleDetailsAgriculturalTractorFormProvider.*

  def apply(): Form[AddVehicleDetailsAgriculturalTractor] = Form(
    mapping(
      "make"            -> textFieldMapping("make"),
      "model"           -> textFieldMapping("model"),
      "derivative"      -> textFieldMapping("derivative"),
      "brakeHorsepower" -> text("addVehicleDetailsAgriculturalTractor.brakeHorsepower.error.required")
        .verifying(
          firstError(
            maxLengthBeforeDecimalPoint(BrakeHorsepowerMaxDigits, "addVehicleDetailsAgriculturalTractor.brakeHorsepower.error.length"),
            regexp(BrakeHorsepowerRegex, "addVehicleDetailsAgriculturalTractor.brakeHorsepower.error.format")
          )
        )
    )(AddVehicleDetailsAgriculturalTractor.apply)(addVehicleDetailsAgriculturalTractor =>
      Some(
        (
          addVehicleDetailsAgriculturalTractor.make,
          addVehicleDetailsAgriculturalTractor.model,
          addVehicleDetailsAgriculturalTractor.derivative,
          addVehicleDetailsAgriculturalTractor.brakeHorsepower
        )
      )
    )
  )

  private def textFieldMapping(field: String) =
    text(s"addVehicleDetailsAgriculturalTractor.$field.error.required")
      .verifying(
        firstError(
          maxLength(MaxLength, s"addVehicleDetailsAgriculturalTractor.$field.error.length"),
          regexp(StandardFieldRegex, s"addVehicleDetailsAgriculturalTractor.$field.error.format")
        )
      )

  private def maxLengthBeforeDecimalPoint(maximum: Int, errorKey: String): Constraint[String] =
    Constraint { str =>
      val wholePart = str.takeWhile(_ != '.')
      if (wholePart.forall(_.isDigit) && wholePart.length > maximum) Invalid(errorKey, maximum) else Valid
    }
}

object AddVehicleDetailsAgriculturalTractorFormProvider {
  val MaxLength: Int                = 50
  val BrakeHorsepowerMaxDigits: Int = 4

  val StandardFieldRegex: String   = """^[A-Za-z0-9 .()/&'\-;!%*_+:@<>?=\[\],\\]{1,50}$"""
  val BrakeHorsepowerRegex: String = """^[0-9]{1,4}$|^([0-9]{1,4}\.[0-9]{2})$"""
}
