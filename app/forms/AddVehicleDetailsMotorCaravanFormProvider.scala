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
import models.AddVehicleDetailsMotorCaravan
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}

import javax.inject.Inject

class AddVehicleDetailsMotorCaravanFormProvider @Inject() extends Mappings {

  import AddVehicleDetailsMotorCaravanFormProvider.*

  def apply(): Form[AddVehicleDetailsMotorCaravan] = Form(
    mapping(
      "make"              -> fieldMapping("make", MaxLength, StandardFieldRegex),
      "bodyType"           -> optional(
        text()
          .verifying(
            firstError(
              maxLength(BodyTypeMaxLength, "addVehicleDetailsMotorCaravan.bodyType.error.length"),
              regexp(BodyTypeRegex, "addVehicleDetailsMotorCaravan.bodyType.error.format")
            )
          )
      ),
      "model"              -> fieldMapping("model", MaxLength, StandardFieldRegex),
      "version"            -> fieldMapping("version", MaxLength, StandardFieldRegex),
      "makeOfBaseVehicle"  -> fieldMapping("makeOfBaseVehicle", MaxLength, StandardFieldRegex),
      "derivative"         -> fieldMapping("derivative", MaxLength, StandardFieldRegex)
    )(
      (make, bodyType, model, version, makeOfBaseVehicle, derivative) =>
        AddVehicleDetailsMotorCaravan(
          make = make,
          bodyType = bodyType.getOrElse(AddVehicleDetailsMotorCaravan.NoBodyType),
          model = model,
          version = version,
          makeOfBaseVehicle = makeOfBaseVehicle,
          derivative = derivative
        )
    )(addVehicleDetailsMotorCaravan =>
      Some(
        (
          addVehicleDetailsMotorCaravan.make,
          if (addVehicleDetailsMotorCaravan.bodyType == AddVehicleDetailsMotorCaravan.NoBodyType) None else Some(addVehicleDetailsMotorCaravan.bodyType),
          addVehicleDetailsMotorCaravan.model,
          addVehicleDetailsMotorCaravan.version,
          addVehicleDetailsMotorCaravan.makeOfBaseVehicle,
          addVehicleDetailsMotorCaravan.derivative
        )
      )
    )
  )

  private def fieldMapping(field: String, maxLen: Int, regex: String) =
    text(s"addVehicleDetailsMotorCaravan.$field.error.required")
      .verifying(
        firstError(
          maxLength(maxLen, s"addVehicleDetailsMotorCaravan.$field.error.length"),
          regexp(regex, s"addVehicleDetailsMotorCaravan.$field.error.format")
        )
      )
}

object AddVehicleDetailsMotorCaravanFormProvider {
  val MaxLength: Int         = 50
  val BodyTypeMaxLength: Int = 30

  val StandardFieldRegex: String = """^[A-Za-z0-9 .()/&'\-;!%*_+:@<>?=\[\],\\]{1,50}$"""
  val BodyTypeRegex: String      = """^[A-Za-z0-9 .()/&'\-;!%*_+:@<>?=\[\],\\]{1,30}$"""
}
