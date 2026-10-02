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
import models.LightCommercialVehicleDetails
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class AddVehicleDetailsLightCommercialFormProvider @Inject() extends Mappings {

  private val makeRegex       = "^[A-Za-z0-9 .()/&'-;!%*_+:@<>?=\\\\[\\\\],\\\\\\\\]{1,50}$"
  private val modelNameRegex  = "^[A-Za-z0-9 .()/&'-;!%*_+:@<>?=\\\\[\\\\],\\\\\\\\]{1,50}$"
  private val derivativeRegex = "^[A-Za-z0-9 .()/&'-;!%*_+:@<>?=\\\\[\\\\],\\\\\\\\]{1,50}$"
  private val trimRegex       = "^[A-Za-z0-9 .()/&'-;!%*_+:@<>?=\\\\[\\\\],\\\\\\\\]{1,50}$"
  private val bodyTypeRegex   = "^[A-Za-z0-9 .()/&'-;!%*_+:@<>?=\\\\[\\\\],\\\\\\\\]{1,20}$"

  def apply(): Form[LightCommercialVehicleDetails] = Form(
    mapping(
      "make" -> text("addVehicleDetailsLightCommercial.make.error.required")
        .verifying(
          firstError(
            maxLength(50, "addVehicleDetailsLightCommercial.make.error.length"),
            regexp(makeRegex, "addVehicleDetailsLightCommercial.make.error.format")
          )
        ),
      "model" -> text("addVehicleDetailsLightCommercial.model.error.required")
        .verifying(
          firstError(
            maxLength(50, "addVehicleDetailsLightCommercial.model.error.length"),
            regexp(modelNameRegex, "addVehicleDetailsLightCommercial.model.error.format")
          )
        ),
      "derivative" -> text("addVehicleDetailsLightCommercial.derivative.error.required")
        .verifying(
          firstError(
            maxLength(50, "addVehicleDetailsLightCommercial.derivative.error.length"),
            regexp(derivativeRegex, "addVehicleDetailsLightCommercial.derivative.error.format")
          )
        ),
      "trim" -> text("addVehicleDetailsLightCommercial.trim.error.required")
        .verifying(
          firstError(
            maxLength(50, "addVehicleDetailsLightCommercial.trim.error.length"),
            regexp(trimRegex, "addVehicleDetailsLightCommercial.trim.error.format")
          )
        ),
      "bodyType" -> text("addVehicleDetailsLightCommercial.bodyType.error.required")
        .verifying(
          firstError(
            maxLength(20, "addVehicleDetailsLightCommercial.bodyType.error.length"),
            regexp(bodyTypeRegex, "addVehicleDetailsLightCommercial.bodyType.error.format")
          )
        )
    )(LightCommercialVehicleDetails.apply)(details => Some((details.make, details.model, details.derivative, details.trim, details.bodyType)))
  )
}
