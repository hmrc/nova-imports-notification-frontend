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
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}

import javax.inject.Inject

class SupplierVatRegistrationDetailsFormProvider @Inject() extends Mappings {

  private val countryRegex   = "^[A-Za-z\\-' ]{1,100}$" //TODO: Still use?
  private val vatNumberRegex = "^[A-Za-z0-9\\-' ]{1,100}$" // TODO: Make dependant on the country

  // TODO: Validate the country is one of the valid values in the list
  // TODO: Use the entry in the list to get the regex

  def apply(countryVrnValidationList: Seq[CountryVrnValidation]): Form[VatNumberDetails] = Form(
    mapping(
      "countryName" -> text("supplierVatRegistrationDetails.country.error.required"),
      "vatNumber"   -> text("supplierName.firstName.error.required")
    )(VatNumberDetails.apply)(v => Some((v.countryName, v.vatNumber))).verifying(
      Constraint[VatNumberDetails]("euCountryVrnRegex") { data =>
        val countryVrnValidationOpt = countryVrnValidationList.find(countryVrnValidation =>
          countryVrnValidation.nameEN == data.countryName || countryVrnValidation.nameCY == data.countryName
        )

        countryVrnValidationOpt match {
          case Some(countryVrnValidation) =>
            if (data.vatNumber.matches(countryVrnValidation.vrnValidationRegex)) {
              Valid
            } else {
              Invalid("supplierVatRegistrationDetails.vatNumber.error.format", countryVrnValidationOpt.map(_.vrnValidationRegex).getOrElse(""))
            }

          // tODO: Also validate for vatNumber missing:  supplierVatRegistrationDetails.country.error.required

          case None =>
            Invalid("supplierVatRegistrationDetails.country.error.required", countryVrnValidationOpt.map(_.vrnValidationRegex).getOrElse(""))
        }
      }
    )
  )

}
