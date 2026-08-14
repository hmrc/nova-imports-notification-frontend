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
import models.{CountryVrnValidation, VatNumberDetails}
import play.api.Logging
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}

import javax.inject.Inject

class SupplierVatRegistrationDetailsFormProvider @Inject() extends Mappings with Logging {

  def apply(countryVrnValidationList: Seq[CountryVrnValidation]): Form[VatNumberDetails] = Form(
    mapping(
      "countryCode" -> text("supplierVatRegistrationDetails.country.error.required"),
      "vatNumber"   -> text("supplierVatRegistrationDetails.vatNumber.error.required")
    )(VatNumberDetails.apply)(v => Some((v.countryCode, v.vatNumber))).verifying(
      // Validate the Vat Number matches the regex for the country provided.
      // This error is applied to the root of the form and so need to be relocated in the controller to show on the vatNumber field.
      Constraint[VatNumberDetails]("euCountryVrnRegex") { data =>
        val countryVrnValidationOpt = countryVrnValidationList.find(countryVrnValidation => countryVrnValidation.code == data.countryCode)
        countryVrnValidationOpt match {
          case Some(countryVrnValidation) =>
            if (data.vatNumber.matches(countryVrnValidation.vrnValidationRegex)) {
              Valid
            } else {
              Invalid("supplierVatRegistrationDetails.vatNumber.error.format", countryVrnValidationOpt.map(_.vrnValidationRegex).getOrElse(""))
            }
          case None =>
            Invalid("supplierVatRegistrationDetails.country.error.required", countryVrnValidationOpt.map(_.vrnValidationRegex).getOrElse(""))
        }
      }
    )
  )

}
