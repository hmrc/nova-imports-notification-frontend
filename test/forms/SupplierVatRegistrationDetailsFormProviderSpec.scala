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
import models.CountryVrnValidation
import play.api.data.FormError

class SupplierVatRegistrationDetailsFormProviderSpec extends StringFieldBehaviours {

  val requiredTitleKey = "supplierVatRegistrationDetails.country.error.required"

  val requiredFirstKey = "supplierVatRegistrationDetails.vatNumber.error.required"
  val formatFirstKey   = "supplierVatRegistrationDetails.vatNumber.error.format"

  val countryRegex = "[A-HJ-NP-Z0-9]{1}[A-HJ-NP-Z0-9]{1}[0-9]{9}"
  val countryCode = "FR"
  val testCountryValidation = CountryVrnValidation(countryCode, "France", "France", countryRegex)
  val form = new SupplierVatRegistrationDetailsFormProvider()(Seq(testCountryValidation))

  ".countryCode" - {
    val fieldName = "countryCode"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredTitleKey)
    )
  }

  ".vatNumber" - {
    val fieldName = "vatNumber"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredFirstKey)
    )

    "must not bind strings with invalid formats for given country" in {
      val invalid      = Seq("A123", "AA1234567891", "A123456789")
      val rootField = ""

      invalid.foreach { value =>
        val result = form.bind(Map("countryCode" -> countryCode, fieldName -> value)).apply(rootField)
        result.errors must contain only FormError(rootField, formatFirstKey, Seq(countryRegex))
      }
    }

    "must bind strings with valid formats for given country" in {
      val invalid      = Seq("AA123456789", "HJ123456789", "X9123456789")
      val rootField = ""

      invalid.foreach { value =>
        val result = form.bind(Map("countryCode" -> countryCode, fieldName -> value)).apply(rootField)
        result.errors mustBe empty
      }
    }

  }

}
