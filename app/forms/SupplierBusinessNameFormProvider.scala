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

class SupplierBusinessNameFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("supplierBusinessName.error.required")
        .verifying(
          firstError(
            maxLength(SupplierBusinessNameFormProvider.MaxLength, "supplierBusinessName.error.length"),
            regexp(SupplierBusinessNameFormProvider.SupplierBusinessNameRegex, "supplierBusinessName.error.invalid")
          )
        )
    )
}

object SupplierBusinessNameFormProvider {
  val MaxLength: Int                    = 160
  val SupplierBusinessNameRegex: String = """^[A-Za-z0-9\-' ]{1,160}$"""
}
