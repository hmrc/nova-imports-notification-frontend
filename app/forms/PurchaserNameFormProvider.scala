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

import javax.inject.Inject
import forms.mappings.Mappings
import models.NameDetails
import play.api.data.Form
import play.api.data.Forms.mapping

class PurchaserNameFormProvider @Inject() extends Mappings {

  private val titleRegex     = "^[A-Za-z\\-' ]{1,20}$"
  private val firstNameRegex = "^[A-Za-z0-9\\-' ]{1,100}$"
  private val lastNameRegex  = "^[A-Za-z0-9\\-' ]{1,100}$"

  def apply(): Form[NameDetails] = Form(
    mapping(
      "title" -> text("purchaserName.titleField.error.required")
        .verifying(
          firstError(
            maxLength(20, "purchaserName.titleField.error.length"),
            regexp(titleRegex, "purchaserName.titleField.error.format")
          )
        ),
      "firstName" -> text("purchaserName.firstName.error.required")
        .verifying(
          firstError(
            maxLength(100, "purchaserName.firstName.error.length"),
            regexp(firstNameRegex, "purchaserName.firstName.error.format")
          )
        ),
      "lastName" -> text("purchaserName.lastName.error.required")
        .verifying(
          firstError(
            maxLength(100, "purchaserName.lastName.error.length"),
            regexp(lastNameRegex, "purchaserName.lastName.error.format")
          )
        )
    )(NameDetails.apply)(name => Some((name.title, name.firstName, name.lastName)))
  )
}
