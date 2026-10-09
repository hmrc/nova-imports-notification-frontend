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

package navigation

import controllers.vehicledetails.routes
import models.{AddVehicleType, CheckMode, ImportNumber, NormalMode, SupplierNumber, UserAnswers, VehicleDates, VehicleNumber}
import pages.sections.vehicledetails.*
import play.api.mvc.Call

object ConfirmVehicleDetailsJourney {

  def supplierFirstUnanswered(answers: UserAnswers, supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Option[Call] = {
    val dates = answers.get(VehicleDatesPage(supplierNumber, vehicleNumber)).getOrElse(Set.empty)

    val firstRegistration = dates.contains(VehicleDates.FirstRegistration)
    val madeAvailable     = dates.contains(VehicleDates.MadeAvailable)
    val invoiceDate       = dates.contains(VehicleDates.PurchaseInvoiceDate)

    val questions: Seq[(Boolean, Boolean, Call)] = Seq(
      (
        true,
        dates.nonEmpty && !dates.contains(VehicleDates.NoDates),
        routes.VehicleDatesController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)
      ),
      (
        firstRegistration,
        answers.get(DateOfFirstRegistrationPage(vehicleNumber)).isDefined,
        routes.DateOfFirstRegistrationController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
      ),
      (
        firstRegistration,
        answers.get(CountryOfFirstRegistrationPage(vehicleNumber)).isDefined,
        routes.CountryOfFirstRegistrationController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
      ),
      (
        madeAvailable,
        answers.get(DateOfAvailabilityPage(supplierNumber, vehicleNumber)).isDefined,
        routes.DateOfAvailabilityController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)
      ),
      (
        invoiceDate,
        answers.get(PurchaseInvoiceDatePage(supplierNumber, vehicleNumber)).isDefined,
        routes.PurchaseInvoiceDateController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)
      ),
      (
        invoiceDate,
        answers.get(PurchaseInvoiceNumberPage(supplierNumber, vehicleNumber)).isDefined,
        routes.PurchaseInvoiceNumberController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)
      ),
      (
        !invoiceDate,
        answers.get(NoPurchaseInvoiceReasonPage(supplierNumber, vehicleNumber)).isDefined,
        routes.NoPurchaseInvoiceReasonController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)
      ),
      (
        true,
        answers.get(TotalAmountPaidPage(vehicleNumber)).isDefined,
        routes.TotalAmountPaidController.onPageLoadSupplier(supplierNumber, vehicleNumber, CheckMode)
      ),
      (
        true,
        answers.get(PaymentCurrencyPage(vehicleNumber)).isDefined,
        routes.PaymentCurrencyController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
      ),
      (
        true,
        answers.get(AddVehicleTypePage(vehicleNumber)).isDefined,
        routes.AddVehicleTypeController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
      )
    )

    questions.collectFirst { case (required, answered, call) if required && !answered => call }
  }

  def importFirstUnanswered(answers: UserAnswers, importNumber: ImportNumber, vehicleNumber: VehicleNumber): Option[Call] = {
    val dateOfFirstRegistrationKnown = answers.get(DateOfFirstRegistrationKnownPage(vehicleNumber))

    val questions: Seq[(Boolean, Boolean, Call)] = Seq(
      (
        true,
        dateOfFirstRegistrationKnown.isDefined,
        dateOfFirstRegistrationKnownChange
      ),
      (
        dateOfFirstRegistrationKnown.contains(true),
        answers.get(DateOfFirstRegistrationPage(vehicleNumber)).isDefined,
        routes.DateOfFirstRegistrationController.importOnPageLoad(importNumber, vehicleNumber, CheckMode)
      ),
      (
        dateOfFirstRegistrationKnown.contains(true),
        answers.get(CountryOfFirstRegistrationPage(vehicleNumber)).isDefined,
        routes.CountryOfFirstRegistrationController.importOnPageLoad(importNumber, vehicleNumber, CheckMode)
      ),
      (
        true,
        answers.get(AddVehicleTypePage(vehicleNumber)).isDefined,
        routes.AddVehicleTypeController.importOnPageLoad(importNumber, vehicleNumber, CheckMode)
      )
    )

    questions.collectFirst { case (required, answered, call) if required && !answered => call }
  }

  def dateOfFirstRegistrationKnownChange: Call = controllers.routes.LandingPageController.onPageLoad()

  def checkModeRoute(answers: UserAnswers, vehicleNumber: VehicleNumber): Call =
    (answers.vehicleSupplierNumber(vehicleNumber), answers.vehicleImportNumber(vehicleNumber)) match {
      case (Some(supplierNumber), _) =>
        supplierFirstUnanswered(answers, supplierNumber, vehicleNumber)
          .getOrElse(routes.ConfirmVehicleDetailsController.supplierOnPageLoad(supplierNumber, vehicleNumber))
      case (None, Some(importNumber)) =>
        importFirstUnanswered(answers, importNumber, vehicleNumber)
          .getOrElse(routes.ConfirmVehicleDetailsController.importOnPageLoad(importNumber, vehicleNumber))
      case _ =>
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  def confirmedRoute(answers: UserAnswers, vehicleNumber: VehicleNumber): Call =
    (
      answers.get(AddVehicleTypePage(vehicleNumber)),
      answers.vehicleSupplierNumber(vehicleNumber),
      answers.vehicleImportNumber(vehicleNumber)
    ) match {
      case (Some(AddVehicleType.Car), Some(supplierNumber), _) =>
        routes.AddVehicleDetailsCarController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode)
      case (Some(AddVehicleType.Car), None, Some(importNumber)) =>
        routes.AddVehicleDetailsCarController.importOnPageLoad(importNumber, vehicleNumber, NormalMode)
      case (Some(AddVehicleType.Lcv), Some(supplierNumber), _) =>
        routes.AddVehicleDetailsLightCommercialController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode)
      case (Some(AddVehicleType.Lcv), None, Some(importNumber)) =>
        routes.AddVehicleDetailsLightCommercialController.importOnPageLoad(importNumber, vehicleNumber, NormalMode)
      case (Some(AddVehicleType.Hcv), Some(supplierNumber), _) =>
        routes.AddVehicleDetailsHeavyCommercialController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode)
      case (Some(AddVehicleType.Hcv), None, Some(importNumber)) =>
        routes.AddVehicleDetailsHeavyCommercialController.importOnPageLoad(importNumber, vehicleNumber, NormalMode)
      case (Some(AddVehicleType.AgriculturalTractor), Some(supplierNumber), _) =>
        routes.AddVehicleDetailsAgriculturalTractorController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode)
      case (Some(AddVehicleType.AgriculturalTractor), None, Some(importNumber)) =>
        routes.AddVehicleDetailsAgriculturalTractorController.importOnPageLoad(importNumber, vehicleNumber, NormalMode)
      case (Some(AddVehicleType.Motorcycle), Some(supplierNumber), _) =>
        routes.AddVehicleDetailsMotorcycleController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode)
      case (Some(AddVehicleType.Motorcycle), None, Some(importNumber)) =>
        routes.AddVehicleDetailsMotorcycleController.importOnPageLoad(importNumber, vehicleNumber, NormalMode)
      case (Some(AddVehicleType.MotorCaravan), _, _)     => controllers.routes.LandingPageController.onPageLoad()
      case (Some(AddVehicleType.ContractorsPlant), _, _) => controllers.routes.LandingPageController.onPageLoad()
      case _                                             => controllers.routes.JourneyRecoveryController.onPageLoad()
    }
}
