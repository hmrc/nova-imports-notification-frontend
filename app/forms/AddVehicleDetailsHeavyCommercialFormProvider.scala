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
import models.AddVehicleDetailsHeavyCommercial
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class AddVehicleDetailsHeavyCommercialFormProvider @Inject() extends Mappings {

  import AddVehicleDetailsHeavyCommercialFormProvider.*

  def apply(): Form[AddVehicleDetailsHeavyCommercial] = Form(
    mapping(
      "make"    -> fieldMapping("make", MaxLength, StandardFieldRegex),
      "model"   -> fieldMapping("model", MaxLength, StandardFieldRegex),
      "hcvType" -> fieldMapping("hcvType", MaxLength, StandardFieldRegex),
      "cabType" -> fieldMapping("cabType", CabTypeMaxLength, CabTypeRegex)
    )(AddVehicleDetailsHeavyCommercial.apply)(addVehicleDetailsHeavyCommercial =>
      Some(
        (
          addVehicleDetailsHeavyCommercial.make,
          addVehicleDetailsHeavyCommercial.model,
          addVehicleDetailsHeavyCommercial.hcvType,
          addVehicleDetailsHeavyCommercial.cabType
        )
      )
    )
  )

  private def fieldMapping(field: String, maxLen: Int, regex: String) =
    text(s"addVehicleDetailsHeavyCommercial.$field.error.required")
      .verifying(
        firstError(
          maxLength(maxLen, s"addVehicleDetailsHeavyCommercial.$field.error.length"),
          regexp(regex, s"addVehicleDetailsHeavyCommercial.$field.error.format")
        )
      )
}

object AddVehicleDetailsHeavyCommercialFormProvider {
  val MaxLength: Int        = 50
  val CabTypeMaxLength: Int = 100

  val StandardFieldRegex: String = """^[A-Za-z0-9 .()/&'\-;!%*_+:@<>?=\[\],\\]{1,50}$"""
  val CabTypeRegex: String       = """^[A-Za-z0-9 .()/&'\-;!%*_+:@<>?=\[\],\\]{1,100}$"""
}
