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
import play.api.data.Form
import play.api.data.validation.{Constraint, Invalid, Valid}

class ImportEntryNumberFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("importEntryNumber.error.required")
        .verifying(
          firstError(
            correctLength("importEntryNumber.error.length"),
            regexp(ImportEntryNumberFormProvider.ImportEntryNumberRegex, "importEntryNumber.error.format")
          )
        )
    )

  private def correctLength(errorKey: String): Constraint[String] =
    Constraint {
      case str if str.length == 11 || str.length == 18 => Valid
      case _                                           => Invalid(errorKey)
    }
}

object ImportEntryNumberFormProvider {
  val ImportEntryNumberRegex: String =
    """^([0-9]{3}-[0-9]{6}[A-Za-z]{1})|([0-9]{2}GB[A-Z0-9]{14})|([0-9]{2}XI[A-Z0-9]{14})$"""
}
