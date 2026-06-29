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
import models.ContactNumbers
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}

import javax.inject.Inject

class PhoneNumberFormProvider @Inject() extends Mappings {

  private val maxLengthChars = 20
  private val allowedChars   = "^[0-9 ]+$"

  def apply(): Form[ContactNumbers] =
    Form(
      mapping(
        "phoneNumber" -> optional(
          text()
            .verifying(
              firstError(
                maxLength(maxLengthChars, "phoneNumber.error.length"),
                regexp(allowedChars, "phoneNumber.error.invalid")
              )
            )
        ),
        "mobileNumber" -> optional(
          text()
            .verifying(
              firstError(
                maxLength(maxLengthChars, "phoneNumber.error.mobileLength"),
                regexp(allowedChars, "phoneNumber.error.mobileInvalid")
              )
            )
        )
      )(ContactNumbers.apply)(contactNumbers => Some((contactNumbers.phoneNumber, contactNumbers.mobileNumber)))
        .verifying(
          "phoneNumber.error.required",
          contactNumbers => contactNumbers.phoneNumber.isDefined || contactNumbers.mobileNumber.isDefined
        )
    )
}
