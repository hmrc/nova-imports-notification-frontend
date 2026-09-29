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
import models.{CheckMode, ImportNumber, SupplierNumber, UserAnswers, VehicleDates, VehicleNumber}
import pages.sections.vehicledetails.*
import play.api.mvc.Call

object ConfirmVehicleDetailsJourney {

  def supplierFirstUnanswered(answers: UserAnswers, supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Option[Call] = {
    val dates = answers.get(VehicleDatesPage(supplierNumber, vehicleNumber)).getOrElse(Set.empty)

    val invoiceDate  = dates.contains(VehicleDates.PurchaseInvoiceDate)
    val availability = dates.contains(VehicleDates.AvailabilityAndFirstRegistration)

    val questions: Seq[(Boolean, Boolean, Call)] = Seq(
      (
        true,
        dates.nonEmpty && !dates.contains(VehicleDates.NoDates),
        routes.VehicleDatesController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)
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
        availability,
        answers.get(DateOfAvailabilityPage(supplierNumber, vehicleNumber)).isDefined,
        routes.DateOfAvailabilityController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)
      ),
      (
        availability,
        answers.get(DateOfFirstRegistrationPage(vehicleNumber)).isDefined,
        routes.DateOfFirstRegistrationController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
      ),
      (
        availability,
        answers.get(CountryOfFirstRegistrationPage(vehicleNumber)).isDefined,
        routes.CountryOfFirstRegistrationController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
      ),
      (
        availability && !invoiceDate,
        answers.get(NoPurchaseInvoiceReasonPage(supplierNumber, vehicleNumber)).isDefined,
        routes.NoPurchaseInvoiceReasonController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)
      ),
      (
        true,
        answers.get(TotalAmountPaidPage(vehicleNumber)).isDefined,
        routes.TotalAmountPaidController.onPageLoadSupplier(supplierNumber, vehicleNumber, CheckMode)
      )
    )

    questions.collectFirst { case (required, answered, call) if required && !answered => call }
  }

  def importFirstUnanswered(answers: UserAnswers, importNumber: ImportNumber, vehicleNumber: VehicleNumber): Option[Call] =
    Seq(
      answers.get(DateOfFirstRegistrationPage(vehicleNumber)).isDefined ->
        routes.DateOfFirstRegistrationController.importOnPageLoad(importNumber, vehicleNumber, CheckMode),
      answers.get(CountryOfFirstRegistrationPage(vehicleNumber)).isDefined ->
        routes.CountryOfFirstRegistrationController.importOnPageLoad(importNumber, vehicleNumber, CheckMode)
    ).collectFirst { case (answered, call) if !answered => call }

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
}
