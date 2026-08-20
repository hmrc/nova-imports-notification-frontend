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

import forms.behaviours.CheckboxFieldBehaviours
import models.VehicleDates
import play.api.data.FormError

class VehicleDatesFormProviderSpec extends CheckboxFieldBehaviours {

  val requiredKey = "vehicleDates.error.required"
  val invalidKey  = "error.invalid"

  val form = new VehicleDatesFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like checkboxField[VehicleDates](
      form,
      fieldName,
      validValues = VehicleDates.values.toSeq,
      invalidError = FormError(s"$fieldName[0]", invalidKey)
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey
    )

    "must bind both date options together" in {

      val result = form.bind(
        Map(
          s"$fieldName[0]" -> VehicleDates.PurchaseInvoiceDate.toString,
          s"$fieldName[1]" -> VehicleDates.AvailabilityAndFirstRegistration.toString
        )
      )

      result.get mustEqual Set(VehicleDates.PurchaseInvoiceDate, VehicleDates.AvailabilityAndFirstRegistration)
      result.errors mustBe empty
    }

    "must fail to bind when a date is selected alongside no dates" in {

      val result = form.bind(
        Map(
          s"$fieldName[0]" -> VehicleDates.PurchaseInvoiceDate.toString,
          s"$fieldName[1]" -> VehicleDates.NoDates.toString
        )
      )

      result.errors must contain(FormError(fieldName, requiredKey))
    }
  }
}
