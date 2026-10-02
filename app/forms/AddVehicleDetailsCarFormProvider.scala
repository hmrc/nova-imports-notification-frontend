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
import models.AddVehicleDetailsCar
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class AddVehicleDetailsCarFormProvider @Inject() extends Mappings {

  import AddVehicleDetailsCarFormProvider.*

  def apply(): Form[AddVehicleDetailsCar] = Form(
    mapping(
      "make"       -> fieldMapping("make", MaxLength, StandardFieldRegex),
      "model"      -> fieldMapping("model", MaxLength, StandardFieldRegex),
      "derivative" -> fieldMapping("derivative", MaxLength, StandardFieldRegex),
      "trim"       -> fieldMapping("trim", MaxLength, StandardFieldRegex),
      "bodyType"   -> fieldMapping("bodyType", BodyTypeMaxLength, BodyTypeRegex)
    )(AddVehicleDetailsCar.apply)(addVehicleDetailsCar =>
      Some(
        (
          addVehicleDetailsCar.make,
          addVehicleDetailsCar.model,
          addVehicleDetailsCar.derivative,
          addVehicleDetailsCar.trim,
          addVehicleDetailsCar.bodyType
        )
      )
    )
  )

  private def fieldMapping(field: String, maxLen: Int, regex: String) =
    text(s"addVehicleDetailsCar.$field.error.required")
      .verifying(
        firstError(
          maxLength(maxLen, s"addVehicleDetailsCar.$field.error.length"),
          regexp(regex, s"addVehicleDetailsCar.$field.error.format")
        )
      )
}

object AddVehicleDetailsCarFormProvider {
  val MaxLength: Int         = 50
  val BodyTypeMaxLength: Int = 20

  val StandardFieldRegex: String = """^[A-Za-z0-9 .()/&'\-;!%*_+:@<>?=\[\],\\]{1,50}$"""
  val BodyTypeRegex: String      = """^[A-Za-z0-9 .()/&'\-;!%*_+:@<>?=\[\],\\]{1,20}$"""
}
