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
import forms.AddVehicleDetailsLightCommercialFormProvider
import models.requests.DataRequest
import models.{AddVehicleType, ImportNumber, Mode, NovaUserType, SupplierNumber, VehicleNumber}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{AddVehicleDetailsLightCommercialPage, AddVehicleTypePage}
import play.api.mvc.*
import repositories.SessionRepository
import services.{ImportService, SupplierService, VehicleService}
import views.html.AddVehicleDetailsLightCommercialView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddVehicleDetailsLightCommercialController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: AddVehicleDetailsLightCommercialFormProvider,
  supplierService: SupplierService,
  importService: ImportService,
  vehicleService: VehicleService,
  view: AddVehicleDetailsLightCommercialView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import AddVehicleDetailsLightCommercialController.*

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) {
      implicit request =>
        handlePageLoad(vehicleNumber, routes.AddVehicleDetailsLightCommercialController.supplierOnSubmit(supplierNumber, vehicleNumber, mode))
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)) { implicit request =>
      handlePageLoad(vehicleNumber, routes.AddVehicleDetailsLightCommercialController.importOnSubmit(importNumber, vehicleNumber, mode))
    }

  def supplierOnSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(vehicleNumber, routes.AddVehicleDetailsLightCommercialController.supplierOnSubmit(supplierNumber, vehicleNumber, mode), mode)
    }

  def importOnSubmit(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(vehicleNumber, routes.AddVehicleDetailsLightCommercialController.importOnSubmit(importNumber, vehicleNumber, mode), mode)
    }

  private def handlePageLoad(vehicleNumber: VehicleNumber, submitCall: Call)(implicit request: DataRequest[AnyContent]): Result =
    Ok(
      view(
        formProvider().withDefault(request.userAnswers.get(AddVehicleDetailsLightCommercialPage(vehicleNumber))),
        submitCall
      )
    )

  private def handleSubmit(vehicleNumber: VehicleNumber, submitCall: Call, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    val page = AddVehicleDetailsLightCommercialPage(vehicleNumber)
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, submitCall))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(page, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, NovaUserType.fromRequest))
      )
  }
}

object AddVehicleDetailsLightCommercialController {

  def supplierGuardPredicate(
    supplierService: SupplierService,
    vehicleService: VehicleService,
    supplierNumber: SupplierNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      supplierService.numberHasValues(request.userAnswers, supplierNumber) &&
      vehicleService.belongsToSupplier(request.userAnswers, vehicleNumber, supplierNumber) &&
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber) &&
      request.userAnswers.get(AddVehicleTypePage(vehicleNumber)).contains(AddVehicleType.Lcv)

  def importGuardPredicate(
    importService: ImportService,
    vehicleService: VehicleService,
    importNumber: ImportNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(false) &&
      importService.numberHasValues(request.userAnswers, importNumber) &&
      vehicleService.belongsToImport(request.userAnswers, vehicleNumber, importNumber) &&
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber) &&
      request.userAnswers.get(AddVehicleTypePage(vehicleNumber)).contains(AddVehicleType.Lcv)
}
