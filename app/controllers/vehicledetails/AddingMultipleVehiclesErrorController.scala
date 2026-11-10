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

package controllers.vehicledetails

import controllers.BaseController
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import models.requests.DataRequest
import models.{ImportNumber, SupplierNumber, VehicleNumber}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import play.api.mvc.*
import services.{ImportService, SupplierService, VehicleService}
import views.html.AddingMultipleVehiclesErrorView

import javax.inject.Inject

class AddingMultipleVehiclesErrorController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  supplierService: SupplierService,
  importService: ImportService,
  vehicleService: VehicleService,
  view: AddingMultipleVehiclesErrorView
) extends BaseController {

  import AddingMultipleVehiclesErrorController.*

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) {
      implicit request =>
        // TODO: navigate to AVD12.0 when built
        Ok(view(routes.VehiclesBoughtFromSupplierAvd12PlaceholderController.onPageLoad(supplierNumber)))
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)) { implicit request =>
      // TODO: navigate to AVD-IE5.0 when built
      Ok(view(controllers.routes.LandingPageController.onPageLoad()))
    }
}

object AddingMultipleVehiclesErrorController {

  def supplierGuardPredicate(
    supplierService: SupplierService,
    vehicleService: VehicleService,
    supplierNumber: SupplierNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      !request.userAnswers.get(AmendSubmittedNotificationPage).contains(true) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      supplierService.numberHasValues(request.userAnswers, supplierNumber) &&
      vehicleService.belongsToSupplier(request.userAnswers, vehicleNumber, supplierNumber) &&
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber) &&
      ConfirmVehicleTypeDetailsController.vehicleDetailsSection(request.userAnswers, vehicleNumber).isDefined &&
      vehicleService.moreThanOneVehicle(request.userAnswers)

  def importGuardPredicate(
    importService: ImportService,
    vehicleService: VehicleService,
    importNumber: ImportNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      !request.userAnswers.get(AmendSubmittedNotificationPage).contains(true) &&
      request.userAnswers.get(VehicleFromEuPage).contains(false) &&
      (request.userContext.isVatRegisteredOrganisation || request.userContext.isAgent) &&
      importService.numberHasValues(request.userAnswers, importNumber) &&
      vehicleService.belongsToImport(request.userAnswers, vehicleNumber, importNumber) &&
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber) &&
      ConfirmVehicleTypeDetailsController.vehicleDetailsSection(request.userAnswers, vehicleNumber).isDefined &&
      vehicleService.moreThanOneVehicle(request.userAnswers)
}
