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

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class AddYourNameFormProviderSpec extends StringFieldBehaviours {

  val requiredTitleKey = "addYourName.titleField.error.required"
  val lengthTitleKey   = "addYourName.titleField.error.length"
  val formatTitleKey   = "addYourName.titleField.error.format"

  val requiredFirstKey = "addYourName.firstName.error.required"
  val lengthFirstKey   = "addYourName.firstName.error.length"
  val formatFirstKey   = "addYourName.firstName.error.format"

  val requiredLastKey = "addYourName.lastName.error.required"
  val lengthLastKey   = "addYourName.lastName.error.length"
  val formatLastKey   = "addYourName.lastName.error.format"

  val form = new AddYourNameFormProvider()()

  ".title" - {
    val fieldName = "title"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredTitleKey)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = 20,
      lengthError = FormError(fieldName, lengthTitleKey, Seq(20))
    )

    "must not bind strings with invalid characters" in {
      val invalid    = Seq("John123", "Name!")
      val titleRegex = "^[A-Za-z\\-' ]{1,20}$"

      invalid.foreach { value =>
        val result = form.bind(Map(fieldName -> value)).apply(fieldName)
        result.errors must contain only FormError(fieldName, formatTitleKey, Seq(titleRegex))
      }
    }
  }

  ".firstName" - {
    val fieldName = "firstName"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredFirstKey)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = 100,
      lengthError = FormError(fieldName, lengthFirstKey, Seq(100))
    )

    "must not bind strings with invalid characters" in {
      val invalid        = Seq("Name!", "first~name")
      val firstNameRegex = "^[A-Za-z0-9\\-' ]{1,100}$"

      invalid.foreach { value =>
        val result = form.bind(Map(fieldName -> value)).apply(fieldName)
        result.errors must contain only FormError(fieldName, formatFirstKey, Seq(firstNameRegex))
      }
    }
  }

  ".lastName" - {
    val fieldName = "lastName"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredLastKey)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = 100,
      lengthError = FormError(fieldName, lengthLastKey, Seq(100))
    )

    "must not bind strings with invalid characters" in {
      val invalid       = Seq("Last!Name", "last~name")
      val lastNameRegex = "^[A-Za-z0-9\\-' ]{1,100}$"

      invalid.foreach { value =>
        val result = form.bind(Map(fieldName -> value)).apply(fieldName)
        result.errors must contain only FormError(fieldName, formatLastKey, Seq(lastNameRegex))
      }
    }
  }
}
