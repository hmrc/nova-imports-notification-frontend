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
import models.{Mileage, MileageUnit}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}

import javax.inject.Inject

class MileageFormProvider @Inject() extends Mappings {

  import MileageFormProvider.*
  def apply(): Form[Mileage] =
    Form(
      mapping(
        "unit"  -> enumerable[MileageUnit]("mileage.error.required.unit"),
        "value" -> text("mileage.error.required.mileage")
          .verifying(
            firstError(
              correctLength("mileage.error.length"),
              regexp(inputRegex, "mileage.error.format")
            )
          )
      )(Mileage.apply)(mileage => Some((mileage.mileageUnits, mileage.mileage)))
    )

  private def correctLength(errorKey: String): Constraint[String] =
    Constraint {
      case value if value.length <= maxInputLength => Valid
      case _                                       => Invalid(errorKey)
    }
}

private object MileageFormProvider {
  private val maxInputLength = 6
  val inputRegex: String     = """^[0-9]{1,6}$"""
}
