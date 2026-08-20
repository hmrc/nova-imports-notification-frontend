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
import controllers.utils.{IsDraftIdDefined, IsSupplierNumberInSession, IsVehicleNumberInSession}
import forms.VehicleDatesFormProvider
import models.requests.DataRequest

import javax.inject.Inject
import models.{Mode, NovaUserType, SupplierNumber, VehicleDates, VehicleNumber}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.VehicleDatesPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.VehicleDatesView

import scala.concurrent.{ExecutionContext, Future}

class VehicleDatesController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: VehicleDatesFormProvider,
  view: VehicleDatesView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import VehicleDatesController.*

  val form: Form[Set[VehicleDates]] = formProvider()

  def onPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierNumber, vehicleNumber)) { implicit request =>
      Ok(view(form.withDefault(request.userAnswers.get(VehicleDatesPage)), supplierNumber, vehicleNumber, mode))
    }

  def onSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierNumber, vehicleNumber)).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, supplierNumber, vehicleNumber, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(VehicleDatesPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(VehicleDatesPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
            )
        )
    }
}

object VehicleDatesController {

  def guardPredicate(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      IsSupplierNumberInSession(request.userAnswers, supplierNumber) &&
      IsVehicleNumberInSession(request.userAnswers, vehicleNumber)
}
