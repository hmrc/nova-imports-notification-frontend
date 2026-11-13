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
import forms.VatReliefReasonFormProvider
import models.requests.DataRequest
import models.{ImportNumber, Mode, NovaUserType, SupplierNumber, VatReliefReason, VehicleNumber}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{VatReliefPage, VatReliefReasonPage}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{ImportService, SupplierService, VehicleService}
import viewmodels.VehicleDetailsInset
import views.html.VatReliefReasonView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatReliefReasonController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: VatReliefReasonFormProvider,
  supplierService: SupplierService,
  importService: ImportService,
  vehicleService: VehicleService,
  view: VatReliefReasonView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import VatReliefReasonController.*

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) {
      implicit request =>
        handlePageLoad(vehicleNumber, isSupplier = true, routes.VatReliefReasonController.supplierOnSubmit(supplierNumber, vehicleNumber, mode))
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)) { implicit request =>
      handlePageLoad(vehicleNumber, isSupplier = false, routes.VatReliefReasonController.importOnSubmit(importNumber, vehicleNumber, mode))
    }

  def supplierOnSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(vehicleNumber, isSupplier = true, routes.VatReliefReasonController.supplierOnSubmit(supplierNumber, vehicleNumber, mode), mode)
    }

  def importOnSubmit(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(vehicleNumber, isSupplier = false, routes.VatReliefReasonController.importOnSubmit(importNumber, vehicleNumber, mode), mode)
    }

  private def handlePageLoad(vehicleNumber: VehicleNumber, isSupplier: Boolean, submitCall: Call)(implicit
    request: DataRequest[AnyContent]
  ): Result =
    Ok(
      view(
        formProvider(isSupplier).withDefault(request.userAnswers.get(VatReliefReasonPage(vehicleNumber))),
        optionsFor(isSupplier),
        VehicleDetailsInset(request.userAnswers, vehicleNumber),
        submitCall
      )
    )

  private def handleSubmit(vehicleNumber: VehicleNumber, isSupplier: Boolean, submitCall: Call, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    val page = VatReliefReasonPage(vehicleNumber)
    formProvider(isSupplier)
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors, optionsFor(isSupplier), VehicleDetailsInset(request.userAnswers, vehicleNumber), submitCall))
          ),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(page, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, NovaUserType.fromRequest))
      )
  }

  private def optionsFor(isSupplier: Boolean): Seq[VatReliefReason] =
    if (isSupplier) VatReliefReason.supplierOptions else VatReliefReason.importOptions
}

object VatReliefReasonController {

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
      request.userAnswers.get(VatReliefPage(vehicleNumber)).contains(true)

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
      request.userAnswers.get(VatReliefPage(vehicleNumber)).contains(true)
}
