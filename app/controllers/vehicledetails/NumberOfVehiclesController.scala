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
import forms.NumberOfVehiclesFormProvider
import models.requests.DataRequest
import models.{ImportNumber, Mode, NovaUserType, NumberOfVehicles, OneMultiVehicle, SupplierNumber, UserAnswers, VehicleNumber}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.vehicledetails.NumberOfVehiclesPage
import play.api.Logging
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{ImportService, SupplierService, VehicleService}
import viewmodels.VehicleDetailsInset
import views.html.NumberOfVehiclesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NumberOfVehiclesController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: NumberOfVehiclesFormProvider,
  supplierService: SupplierService,
  importService: ImportService,
  vehicleService: VehicleService,
  view: NumberOfVehiclesView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import NumberOfVehiclesController.*

  val form: Form[NumberOfVehicles] = formProvider()

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) {
      implicit request =>
        handlePageLoad(vehicleNumber, routes.NumberOfVehiclesController.supplierOnSubmit(supplierNumber, vehicleNumber, mode))
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)) { implicit request =>
      handlePageLoad(vehicleNumber, routes.NumberOfVehiclesController.importOnSubmit(importNumber, vehicleNumber, mode))
    }

  def supplierOnSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(
          vehicleNumber,
          routes.NumberOfVehiclesController.supplierOnSubmit(supplierNumber, vehicleNumber, mode),
          routes.AddingMultipleVehiclesErrorController.supplierOnPageLoad(supplierNumber, vehicleNumber),
          mode
        )
    }

  def importOnSubmit(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(
          vehicleNumber,
          routes.NumberOfVehiclesController.importOnSubmit(importNumber, vehicleNumber, mode),
          routes.AddingMultipleVehiclesErrorController.importOnPageLoad(importNumber, vehicleNumber),
          mode
        )
    }

  private def handlePageLoad(vehicleNumber: VehicleNumber, submitCall: Call)(implicit request: DataRequest[AnyContent]): Result =
    Ok(
      view(
        form.withDefault(request.userAnswers.get(NumberOfVehiclesPage(vehicleNumber))),
        VehicleDetailsInset(request.userAnswers, vehicleNumber),
        submitCall
      )
    )

  private def handleSubmit(vehicleNumber: VehicleNumber, submitCall: Call, multipleNotAllowedCall: Call, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    val page = NumberOfVehiclesPage(vehicleNumber)
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, VehicleDetailsInset(request.userAnswers, vehicleNumber), submitCall))),
        value =>
          if (multipleNotAllowed(value, request.userAnswers))
            Future.successful(Redirect(multipleNotAllowedCall))
          else
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(page, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, NovaUserType.fromRequest))
      )
  }

  private def multipleNotAllowed(value: NumberOfVehicles, answers: UserAnswers): Boolean =
    value.oneMultiVehicle == OneMultiVehicle.More && vehicleService.moreThanOneVehicle(answers)
}

object NumberOfVehiclesController {

  def multiplicationSection(value: NumberOfVehicles): JsObject =
    value.numberSameVehicles.fold(Json.obj())(n => Json.obj("numberOfVehiclesToMultiply" -> n))

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
      ConfirmVehicleTypeDetailsController.vehicleDetailsSection(request.userAnswers, vehicleNumber).isDefined

  def importGuardPredicate(
    importService: ImportService,
    vehicleService: VehicleService,
    importNumber: ImportNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      !request.userAnswers.get(AmendSubmittedNotificationPage).contains(true) &&
      request.userAnswers.get(VehicleFromEuPage).contains(false) &&
      importService.numberHasValues(request.userAnswers, importNumber) &&
      vehicleService.belongsToImport(request.userAnswers, vehicleNumber, importNumber) &&
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber) &&
      ConfirmVehicleTypeDetailsController.vehicleDetailsSection(request.userAnswers, vehicleNumber).isDefined
}
