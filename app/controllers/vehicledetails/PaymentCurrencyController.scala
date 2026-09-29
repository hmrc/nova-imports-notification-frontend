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

import config.FrontendAppConfig
import controllers.BaseController
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import controllers.vehicledetails.PaymentCurrencyController.*
import forms.PaymentCurrencyFormProvider
import models.requests.DataRequest
import models.{Currency, ImportNumber, Mode, NovaUserType, SupplierNumber, VehicleNumber}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.PaymentCurrencyPage
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{ImportService, SupplierService, VehicleService}
import views.html.PaymentCurrencyView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentCurrencyController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  appConfig: FrontendAppConfig,
  formProvider: PaymentCurrencyFormProvider,
  supplierService: SupplierService,
  importService: ImportService,
  vehicleService: VehicleService,
  view: PaymentCurrencyView
)(implicit ec: ExecutionContext)
    extends BaseController {

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) {
      implicit request =>
        handlePageLoad(
          vehicleNumber,
          appConfig.supplierCurrencies,
          SupplierHeadingKey,
          routes.PaymentCurrencyController.supplierOnSubmit(supplierNumber, vehicleNumber, mode)
        )
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)) { implicit request =>
      handlePageLoad(
        vehicleNumber,
        appConfig.currencies,
        ImportHeadingKey,
        routes.PaymentCurrencyController.importOnSubmit(importNumber, vehicleNumber, mode)
      )
    }

  def supplierOnSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(
          vehicleNumber,
          appConfig.supplierCurrencies,
          SupplierHeadingKey,
          routes.PaymentCurrencyController.supplierOnSubmit(supplierNumber, vehicleNumber, mode),
          mode
        )
    }

  def importOnSubmit(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(
          vehicleNumber,
          appConfig.currencies,
          ImportHeadingKey,
          routes.PaymentCurrencyController.importOnSubmit(importNumber, vehicleNumber, mode),
          mode
        )
    }

  private def handlePageLoad(vehicleNumber: VehicleNumber, currencies: List[Currency], headingKey: String, submitCall: Call)(implicit
    request: DataRequest[AnyContent]
  ): Result =
    Ok(view(currencies, formProvider(currencies).withDefault(request.userAnswers.get(PaymentCurrencyPage(vehicleNumber))), headingKey, submitCall))

  private def handleSubmit(vehicleNumber: VehicleNumber, currencies: List[Currency], headingKey: String, submitCall: Call, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    val page = PaymentCurrencyPage(vehicleNumber)
    formProvider(currencies)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(currencies, formWithErrors, headingKey, submitCall))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(page, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, NovaUserType.fromRequest))
      )
  }
}

object PaymentCurrencyController {

  val SupplierHeadingKey = "paymentCurrency.supplier.heading"

  val ImportHeadingKey = "paymentCurrency.import.heading"

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
