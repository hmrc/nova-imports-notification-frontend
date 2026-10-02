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

package pages

import base.SpecBase
import models.{SupplierNumber, VehicleDates, VehicleNumber}
import pages.sections.vehicledetails.*

import java.time.LocalDate

class VehicleDatesPageSpec extends SpecBase {

  private val supplierNumber = SupplierNumber(1)
  private val vehicleNumber  = VehicleNumber(1)
  private val page           = VehicleDatesPage(supplierNumber, vehicleNumber)

  private val allDatesAnswered = emptyUserAnswers
    .unsafeSet(page, Set[VehicleDates](VehicleDates.FirstRegistration, VehicleDates.MadeAvailable, VehicleDates.PurchaseInvoiceDate))
    .unsafeSet(DateOfFirstRegistrationPage(vehicleNumber), LocalDate.of(2026, 3, 27))
    .unsafeSet(CountryOfFirstRegistrationPage(vehicleNumber), "FR")
    .unsafeSet(DateOfAvailabilityPage(supplierNumber, vehicleNumber), LocalDate.of(2026, 4, 1))
    .unsafeSet(PurchaseInvoiceDatePage(supplierNumber, vehicleNumber), LocalDate.of(2026, 2, 1))
    .unsafeSet(PurchaseInvoiceNumberPage(supplierNumber, vehicleNumber), "INV-001")

  "VehicleDatesPage" - {

    "must store the dates under the vehicle's details" in {
      (allDatesAnswered.data \ "vehicles" \ "1" \ "details" \ "vehicleDates").as[Set[VehicleDates]] mustBe
        Set(VehicleDates.FirstRegistration, VehicleDates.MadeAvailable, VehicleDates.PurchaseInvoiceDate)
    }

    "must keep the answers for the dates that are still selected" in {
      val answers = allDatesAnswered.unsafeSet(page, Set[VehicleDates](VehicleDates.FirstRegistration, VehicleDates.MadeAvailable))

      answers.get(DateOfFirstRegistrationPage(vehicleNumber)) mustBe Some(LocalDate.of(2026, 3, 27))
      answers.get(CountryOfFirstRegistrationPage(vehicleNumber)) mustBe Some("FR")
      answers.get(DateOfAvailabilityPage(supplierNumber, vehicleNumber)) mustBe Some(LocalDate.of(2026, 4, 1))
    }

    "must clear the date and country of first registration when that date is dropped" in {
      val answers = allDatesAnswered.unsafeSet(page, Set[VehicleDates](VehicleDates.MadeAvailable, VehicleDates.PurchaseInvoiceDate))

      answers.get(DateOfFirstRegistrationPage(vehicleNumber)) mustBe None
      answers.get(CountryOfFirstRegistrationPage(vehicleNumber)) mustBe None
    }

    "must clear the date of availability when that date is dropped" in {
      val answers = allDatesAnswered.unsafeSet(page, Set[VehicleDates](VehicleDates.FirstRegistration, VehicleDates.PurchaseInvoiceDate))

      answers.get(DateOfAvailabilityPage(supplierNumber, vehicleNumber)) mustBe None
    }

    "must clear the purchase invoice answers when that date is dropped" in {
      val answers = allDatesAnswered.unsafeSet(page, Set[VehicleDates](VehicleDates.FirstRegistration, VehicleDates.MadeAvailable))

      answers.get(PurchaseInvoiceDatePage(supplierNumber, vehicleNumber)) mustBe None
      answers.get(PurchaseInvoiceNumberPage(supplierNumber, vehicleNumber)) mustBe None
    }

    "must clear the reason for no purchase invoice once an invoice date is selected" in {
      val answers = emptyUserAnswers
        .unsafeSet(page, Set[VehicleDates](VehicleDates.FirstRegistration, VehicleDates.MadeAvailable))
        .unsafeSet(NoPurchaseInvoiceReasonPage(supplierNumber, vehicleNumber), "No invoice was issued")
        .unsafeSet(page, Set[VehicleDates](VehicleDates.FirstRegistration, VehicleDates.PurchaseInvoiceDate))

      answers.get(NoPurchaseInvoiceReasonPage(supplierNumber, vehicleNumber)) mustBe None
    }

    "must clear every date answer when the user says they have none of these dates" in {
      val answers = allDatesAnswered
        .unsafeSet(NoPurchaseInvoiceReasonPage(supplierNumber, vehicleNumber), "No invoice was issued")
        .unsafeSet(page, Set[VehicleDates](VehicleDates.NoDates))

      answers.get(DateOfFirstRegistrationPage(vehicleNumber)) mustBe None
      answers.get(CountryOfFirstRegistrationPage(vehicleNumber)) mustBe None
      answers.get(DateOfAvailabilityPage(supplierNumber, vehicleNumber)) mustBe None
      answers.get(PurchaseInvoiceDatePage(supplierNumber, vehicleNumber)) mustBe None
      answers.get(PurchaseInvoiceNumberPage(supplierNumber, vehicleNumber)) mustBe None
      answers.get(NoPurchaseInvoiceReasonPage(supplierNumber, vehicleNumber)) mustBe None
    }
  }
}
