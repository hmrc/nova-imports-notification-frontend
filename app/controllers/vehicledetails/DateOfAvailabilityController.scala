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
import forms.DateOfAvailabilityFormProvider
import models.requests.DataRequest
import models.{Mode, NovaUserType, SupplierNumber, VehicleNumber}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.DateOfAvailabilityPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{SupplierService, VehicleService}
import views.html.DateOfAvailabilityView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DateOfAvailabilityController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: DateOfAvailabilityFormProvider,
  supplierService: SupplierService,
  vehicleService: VehicleService,
  appConfig: FrontendAppConfig,
  view: DateOfAvailabilityView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import DateOfAvailabilityController.*

  def onPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) { implicit request =>
      val form = formProvider()
      Ok(
        view(
          form.withDefault(request.userAnswers.get(DateOfAvailabilityPage(supplierNumber, vehicleNumber))),
          supplierNumber,
          vehicleNumber,
          mode,
          appConfig.importingVehiclesIntoTheUKUrl
        )
      )
    }

  def onSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async { implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, supplierNumber, vehicleNumber, mode, appConfig.importingVehiclesIntoTheUKUrl))
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(DateOfAvailabilityPage(supplierNumber, vehicleNumber), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                DateOfAvailabilityPage(supplierNumber, vehicleNumber),
                mode,
                updatedAnswers,
                NovaUserType.from(request.affinityGroup, request.enrolments)
              )
            )
        )
    }
}

object DateOfAvailabilityController {

  def guardPredicate(
    supplierService: SupplierService,
    vehicleService: VehicleService,
    supplierNumber: SupplierNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      supplierService.numberExists(request.userAnswers, supplierNumber) &&
      vehicleService.belongsToSupplier(request.userAnswers, vehicleNumber, supplierNumber)
}
