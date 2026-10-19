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

class NotificationReferenceFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("notificationReference.error.required")
        .verifying(
          firstError(
            correctLength("notificationReference.error.length"),
            regexp(NotificationReferenceFormProvider.NotificationReferenceRegex, "notificationReference.error.format")
          )
        )
    )

  private def correctLength(errorKey: String): Constraint[String] =
    Constraint {
      case str if str.length == 13 => Valid
      case _                       => Invalid(errorKey)
    }
}

object NotificationReferenceFormProvider {
  val NotificationReferenceRegex: String = """^NOVA\d\dE[1-6]\d{5}$"""
}
