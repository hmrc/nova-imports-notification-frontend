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
import connectors.{GetEuMemberStatesError, NovaImportsBackendConnector}
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import controllers.{BaseController, routes, vehicledetails}
import forms.DateOfArrivalFormProvider
import forms.DateOfArrivalFormProvider.FirstRegistration
import models.requests.DataRequest
import models.{ImportNumber, Mode, NovaUserType, SupplierNumber, VehicleDates, VehicleNumber}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{CountryOfFirstRegistrationPage, DateOfArrivalPage, DateOfFirstRegistrationPage, VehicleDatesPage}
import play.api.Logging
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{ImportService, SupplierService, VehicleService}
import viewmodels.VehicleDetailsInset
import views.html.DateOfArrivalView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DateOfArrivalController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  appConfig: FrontendAppConfig,
  formProvider: DateOfArrivalFormProvider,
  backendConnector: NovaImportsBackendConnector,
  supplierService: SupplierService,
  importService: ImportService,
  vehicleService: VehicleService,
  view: DateOfArrivalView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import DateOfArrivalController.*

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) {
      implicit request =>
        handlePageLoad(
          vehicleNumber,
          formProvider(isRequired = true),
          vehicledetails.routes.DateOfArrivalController.supplierOnSubmit(supplierNumber, vehicleNumber, mode)
        )
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)) { implicit request =>
      handlePageLoad(
        vehicleNumber,
        formProvider(isRequired = false),
        vehicledetails.routes.DateOfArrivalController.importOnSubmit(importNumber, vehicleNumber, mode)
      )
    }

  def supplierOnSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request =>
        firstRegistration(supplierNumber, vehicleNumber).transformWith {
          case Success(Right(firstRegistrationRule)) =>
            handleSubmit(
              vehicleNumber,
              formProvider(isRequired = true, firstRegistrationRule),
              vehicledetails.routes.DateOfArrivalController.supplierOnSubmit(supplierNumber, vehicleNumber, mode),
              mode
            )
          case Success(Left(error)) =>
            logger.warn(s"Failed to retrieve EU member states for vehicle ${vehicleNumber.value}: $error")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          case Failure(e) =>
            logger.warn(s"Failed to retrieve EU member states for vehicle ${vehicleNumber.value}", e)
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
    }

  def importOnSubmit(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(
          vehicleNumber,
          formProvider(isRequired = false),
          vehicledetails.routes.DateOfArrivalController.importOnSubmit(importNumber, vehicleNumber, mode),
          mode
        )
    }

  private def handlePageLoad(vehicleNumber: VehicleNumber, form: Form[Option[LocalDate]], submitCall: Call)(implicit
    request: DataRequest[AnyContent]
  ): Result =
    Ok(renderView(vehicleNumber, form.withDefault(request.userAnswers.get(DateOfArrivalPage(vehicleNumber)).map(Some(_))), submitCall))

  private def handleSubmit(vehicleNumber: VehicleNumber, form: Form[Option[LocalDate]], submitCall: Call, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    val page = DateOfArrivalPage(vehicleNumber)
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(renderView(vehicleNumber, formWithErrors, submitCall))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(value.fold(request.userAnswers.remove(page))(request.userAnswers.set(page, _)))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, NovaUserType.fromRequest))
      )
  }

  private def renderView(vehicleNumber: VehicleNumber, form: Form[Option[LocalDate]], submitCall: Call)(implicit request: DataRequest[AnyContent]) =
    view(
      form,
      VehicleDetailsInset(request.userAnswers, vehicleNumber),
      submitCall,
      appConfig.secureRegistrationSchemeUrl,
      appConfig.registerAVehicleUrl
    )

  private def firstRegistration(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber)(implicit
    request: DataRequest[AnyContent]
  ): Future[Either[GetEuMemberStatesError, Option[FirstRegistration]]] = {
    val firstRegistrationAnswers = for {
      _           <- request.userAnswers.get(VehicleDatesPage(supplierNumber, vehicleNumber)).filter(_.contains(VehicleDates.FirstRegistration))
      date        <- request.userAnswers.get(DateOfFirstRegistrationPage(vehicleNumber))
      countryCode <- request.userAnswers.get(CountryOfFirstRegistrationPage(vehicleNumber))
    } yield (date, countryCode)

    firstRegistrationAnswers match {
      case Some((date, countryCode)) =>
        backendConnector.getEuMemberStates().map {
          _.map { euMemberStates =>
            Some(FirstRegistration(date, countryCode == UnitedKingdomCode || euMemberStates.countries.exists(_.code == countryCode)))
          }
        }
      case None => Future.successful(Right(None))
    }
  }
}

object DateOfArrivalController {

  private val UnitedKingdomCode = "GB"

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
      importService.numberHasValues(request.userAnswers, importNumber) &&
      vehicleService.belongsToImport(request.userAnswers, vehicleNumber, importNumber) &&
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber)
}
