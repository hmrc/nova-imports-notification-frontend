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
import forms.PurchaseInvoiceDateFormProvider
import models.requests.DataRequest
import models.{Mode, NovaUserType, SupplierNumber, VehicleNumber}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.PurchaseInvoiceDatePage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{SupplierService, VehicleService}
import views.html.PurchaseInvoiceDateView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PurchaseInvoiceDateController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: PurchaseInvoiceDateFormProvider,
  supplierService: SupplierService,
  vehicleService: VehicleService,
  view: PurchaseInvoiceDateView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import PurchaseInvoiceDateController.*

  def onPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) { implicit request =>
      val form = formProvider()
      Ok(view(form.withDefault(request.userAnswers.get(PurchaseInvoiceDatePage(vehicleNumber))), supplierNumber, vehicleNumber, mode))
    }

  def onSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async { implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, supplierNumber, vehicleNumber, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(PurchaseInvoiceDatePage(vehicleNumber), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                PurchaseInvoiceDatePage(vehicleNumber),
                mode,
                updatedAnswers,
                NovaUserType.from(request.affinityGroup, request.enrolments)
              )
            )
        )
    }
}

object PurchaseInvoiceDateController {

  // The numbers in the URL must match a session supplier and a vehicle that belongs to that supplier
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
