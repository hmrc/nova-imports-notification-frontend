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

import forms.behaviours.OptionFieldBehaviours
import models.VatReliefReason
import play.api.data.FormError

class VatReliefReasonFormProviderSpec extends OptionFieldBehaviours {

  private val fieldName = "value"

  ".value on the supplier journey" - {

    val requiredKey = "vatReliefReason.supplier.error.required"
    val form        = new VatReliefReasonFormProvider()(isSupplier = true)

    behave like optionsField[VatReliefReason](
      form,
      fieldName,
      validValues = VatReliefReason.supplierOptions,
      invalidError = FormError(fieldName, requiredKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must not bind a reason that is only valid on the import journey" in {
      val result = form.bind(Map(fieldName -> VatReliefReason.InheritedGoods.toString)).apply(fieldName)
      result.errors must contain only FormError(fieldName, requiredKey)
    }
  }

  ".value on the import journey" - {

    val requiredKey = "vatReliefReason.import.error.required"
    val form        = new VatReliefReasonFormProvider()(isSupplier = false)

    behave like optionsField[VatReliefReason](
      form,
      fieldName,
      validValues = VatReliefReason.importOptions,
      invalidError = FormError(fieldName, requiredKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
