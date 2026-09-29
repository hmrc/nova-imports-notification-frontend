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
import models.Country
import play.api.data.FormError

class CountryOfFirstRegistrationFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "countryOfFirstRegistration.error.required"

  val form = new CountryOfFirstRegistrationFormProvider()(List(Country("FR", "France"), Country("DE", "Germany")))

  ".value" - {

    val fieldName = "value"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must bind a country code from the list" in {
      val result = form.bind(Map(fieldName -> "DE"))

      result.value.value mustEqual "DE"
      result.errors mustBe empty
    }

    "must not bind a country code that is not in the list" in {
      val result = form.bind(Map(fieldName -> "GB")).apply(fieldName)

      result.errors must contain only FormError(fieldName, requiredKey)
    }

    "must not bind a country name" in {
      val result = form.bind(Map(fieldName -> "France")).apply(fieldName)

      result.errors must contain only FormError(fieldName, requiredKey)
    }
  }
}
