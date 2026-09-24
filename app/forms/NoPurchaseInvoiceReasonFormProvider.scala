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

import javax.inject.Inject

class NoPurchaseInvoiceReasonFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("noPurchaseInvoiceReason.error.required")
        .transform[String](collapseWhitespace, identity)
        .verifying(
          firstError(
            maxLength(NoPurchaseInvoiceReasonFormProvider.MaxLength, "noPurchaseInvoiceReason.error.length"),
            regexp(NoPurchaseInvoiceReasonFormProvider.NoPurchaseInvoiceReasonRegex, "noPurchaseInvoiceReason.error.invalid")
          )
        )
    )

  // the text area lets users press enter, but line breaks are not valid in the reason
  private def collapseWhitespace(value: String): String = value.replaceAll("\\s+", " ").trim
}

object NoPurchaseInvoiceReasonFormProvider {
  val MaxLength: Int                       = 160
  val NoPurchaseInvoiceReasonRegex: String = """^[A-Za-z0-9 ,.()/&'\-"!%*_+:@<>?=;]{1,160}$"""
}
