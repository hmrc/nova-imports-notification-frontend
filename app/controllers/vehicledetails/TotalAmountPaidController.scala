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
import forms.TotalAmountPaidFormProvider
import models.requests.DataRequest
import models.{ImportNumber, Mode, NovaUserType, SupplierNumber, VehicleNumber}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.TotalAmountPaidPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{ImportService, SupplierService, VehicleService}
import views.html.TotalAmountPaidView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TotalAmountPaidController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: TotalAmountPaidFormProvider,
  supplierService: SupplierService,
  vehicleService: VehicleService,
  importService: ImportService,
  view: TotalAmountPaidView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import TotalAmountPaidController.*

  val form: Form[String] = formProvider()

  def onPageLoadSupplier(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) {
      implicit request =>
        onPageLoad(vehicleNumber, routes.TotalAmountPaidController.onSubmitSupplier(supplierNumber, vehicleNumber, mode))
    }

  def onPageLoadImport(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)) { implicit request =>
      onPageLoad(vehicleNumber, routes.TotalAmountPaidController.onSubmitImport(importNumber, vehicleNumber, mode))
    }

  def onSubmitSupplier(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request =>
        onSubmit(vehicleNumber, mode, routes.TotalAmountPaidController.onSubmitSupplier(supplierNumber, vehicleNumber, mode))
    }

  def onSubmitImport(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async {
      implicit request =>
        onSubmit(vehicleNumber, mode, routes.TotalAmountPaidController.onSubmitImport(importNumber, vehicleNumber, mode))
    }

  private def onPageLoad(vehicleNumber: VehicleNumber, postAction: Call)(implicit request: DataRequest[AnyContent]): Result =
    Ok(view(form.withDefault(request.userAnswers.get(TotalAmountPaidPage(vehicleNumber))), postAction))

  private def onSubmit(vehicleNumber: VehicleNumber, mode: Mode, postAction: Call)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] =
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, postAction))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(TotalAmountPaidPage(vehicleNumber), value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(TotalAmountPaidPage(vehicleNumber), mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
          )
      )
}

object TotalAmountPaidController {

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
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber)

  def importGuardPredicate(
    importService: ImportService,
    vehicleService: VehicleService,
    importNumber: ImportNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(false) &&
      isVatOrganisationOrAgent(request) &&
      importService.numberHasValues(request.userAnswers, importNumber) &&
      vehicleService.belongsToImport(request.userAnswers, vehicleNumber, importNumber) &&
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber)

  private def isVatOrganisationOrAgent(request: DataRequest[?]): Boolean =
    request.userContext.userType == NovaUserType.VatRegisteredOrganisation || request.userContext.isAgent
}
