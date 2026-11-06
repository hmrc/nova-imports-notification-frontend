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
import play.api.data.Form
import play.api.data.validation.{Constraint, Invalid, Valid}

import javax.inject.Inject

class VehicleIdentificationNumberFormProvider @Inject() extends Mappings {

  def apply(isDuplicate: String => Boolean): Form[String] =
    Form(
      "value" -> text("vehicleIdentificationNumber.error.required")
        .verifying(
          firstError(
            maxLength(VehicleIdentificationNumberFormProvider.MaxLength, "vehicleIdentificationNumber.error.length"),
            regexp(VehicleIdentificationNumberFormProvider.VehicleIdentificationNumberRegex, "vehicleIdentificationNumber.error.invalid"),
            notDuplicate(isDuplicate, "vehicleIdentificationNumber.error.duplicate")
          )
        )
    )

  private def notDuplicate(isDuplicate: String => Boolean, errorKey: String): Constraint[String] =
    Constraint {
      case vin if isDuplicate(vin) => Invalid(errorKey)
      case _                       => Valid
    }
}

object VehicleIdentificationNumberFormProvider {
  val MaxLength: Int = 17
  // letters and digits, excluding I, O and Q in either case
  val VehicleIdentificationNumberRegex: String = "^[A-Za-z0-9]{1,17}$"
}
