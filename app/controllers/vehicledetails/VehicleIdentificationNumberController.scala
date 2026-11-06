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
import forms.VehicleIdentificationNumberFormProvider
import models.requests.DataRequest
import models.{ImportNumber, Mode, NovaUserType, OneMultiVehicle, SupplierNumber, VehicleNumber}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.vehicledetails.{NumberOfVehiclesPage, VehicleIdentificationNumberPage}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{ImportService, SupplierService, VehicleService}
import viewmodels.VehicleDetailsInset
import views.html.VehicleIdentificationNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VehicleIdentificationNumberController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: VehicleIdentificationNumberFormProvider,
  supplierService: SupplierService,
  importService: ImportService,
  vehicleService: VehicleService,
  view: VehicleIdentificationNumberView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import VehicleIdentificationNumberController.*

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) {
      implicit request =>
        handlePageLoad(vehicleNumber, routes.VehicleIdentificationNumberController.supplierOnSubmit(supplierNumber, vehicleNumber, mode))
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)) { implicit request =>
      handlePageLoad(vehicleNumber, routes.VehicleIdentificationNumberController.importOnSubmit(importNumber, vehicleNumber, mode))
    }

  def supplierOnSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(vehicleNumber, routes.VehicleIdentificationNumberController.supplierOnSubmit(supplierNumber, vehicleNumber, mode), mode)
    }

  def importOnSubmit(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(vehicleNumber, routes.VehicleIdentificationNumberController.importOnSubmit(importNumber, vehicleNumber, mode), mode)
    }

  private def form(vehicleNumber: VehicleNumber)(implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(vin => vehicleService.vinUsedByAnotherVehicle(request.userAnswers, vehicleNumber, vin))

  private def handlePageLoad(vehicleNumber: VehicleNumber, submitCall: Call)(implicit request: DataRequest[AnyContent]): Result =
    Ok(
      view(
        form(vehicleNumber).withDefault(request.userAnswers.get(VehicleIdentificationNumberPage(vehicleNumber))),
        VehicleDetailsInset(request.userAnswers, vehicleNumber),
        submitCall
      )
    )

  private def handleSubmit(vehicleNumber: VehicleNumber, submitCall: Call, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    val page = VehicleIdentificationNumberPage(vehicleNumber)
    form(vehicleNumber)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, VehicleDetailsInset(request.userAnswers, vehicleNumber), submitCall))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(page, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, NovaUserType.fromRequest))
      )
  }
}

object VehicleIdentificationNumberController {

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
      isOneVehicleOrAmend(request, vehicleNumber)

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
      isOneVehicleOrAmend(request, vehicleNumber)

  // amend sessions skip AVD10.0 (C-AVD2.0 goes to AVD9.0), so the one-vehicle answer only applies to non-amend sessions
  private def isOneVehicleOrAmend(request: DataRequest[?], vehicleNumber: VehicleNumber): Boolean =
    request.userAnswers.get(AmendSubmittedNotificationPage).contains(true) ||
      request.userAnswers.get(NumberOfVehiclesPage(vehicleNumber)).exists(_.oneMultiVehicle == OneMultiVehicle.One)
}
