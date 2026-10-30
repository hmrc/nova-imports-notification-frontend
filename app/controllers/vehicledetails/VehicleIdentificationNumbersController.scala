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
import forms.VehicleIdentificationNumbersFormProvider
import models.requests.DataRequest
import models.{ImportNumber, NormalMode, NovaUserType, SupplierNumber, VehicleIdentificationNumbersMethod, VehicleNumber}
import navigation.Navigator
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.vehicledetails.VehicleIdentificationNumbersPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.SessionRepository
import viewmodels.VehicleDetailsInset
import views.html.VehicleIdentificationNumbersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VehicleIdentificationNumbersController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: VehicleIdentificationNumbersFormProvider,
  view: VehicleIdentificationNumbersView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import VehicleIdentificationNumbersController.*

  val form: Form[VehicleIdentificationNumbersMethod] = formProvider()

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
      handlePageLoad(vehicleNumber, routes.VehicleIdentificationNumbersController.supplierOnSubmit(supplierNumber, vehicleNumber))
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
      handlePageLoad(vehicleNumber, routes.VehicleIdentificationNumbersController.importOnSubmit(importNumber, vehicleNumber))
    }

  def supplierOnSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      handleSubmit(vehicleNumber, routes.VehicleIdentificationNumbersController.supplierOnSubmit(supplierNumber, vehicleNumber))
    }

  def importOnSubmit(importNumber: ImportNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      handleSubmit(vehicleNumber, routes.VehicleIdentificationNumbersController.importOnSubmit(importNumber, vehicleNumber))
    }

  private def handlePageLoad(vehicleNumber: VehicleNumber, submitCall: Call)(implicit request: DataRequest[AnyContent]): Result =
    Ok(
      view(
        form.withDefault(request.userAnswers.get(VehicleIdentificationNumbersPage(vehicleNumber))),
        VehicleDetailsInset(request.userAnswers, vehicleNumber),
        submitCall
      )
    )

  private def handleSubmit(vehicleNumber: VehicleNumber, submitCall: Call)(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val page = VehicleIdentificationNumbersPage(vehicleNumber)
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, VehicleDetailsInset(request.userAnswers, vehicleNumber), submitCall))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(page, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(page, NormalMode, updatedAnswers, NovaUserType.fromRequest))
      )
  }
}

object VehicleIdentificationNumbersController {

  // TODO: add AVD10.0 = 'Multiple vehicles' is answered check when AVD10.0 is built
  def guardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(AmendSubmittedNotificationPage).contains(false)
}
