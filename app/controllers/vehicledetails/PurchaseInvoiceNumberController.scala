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
import forms.PurchaseInvoiceNumberFormProvider
import models.requests.DataRequest
import models.{Mode, NovaUserType, SupplierNumber, VehicleNumber}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.PurchaseInvoiceNumberPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{SupplierService, VehicleService}
import views.html.PurchaseInvoiceNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PurchaseInvoiceNumberController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: PurchaseInvoiceNumberFormProvider,
  supplierService: SupplierService,
  vehicleService: VehicleService,
  view: PurchaseInvoiceNumberView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import PurchaseInvoiceNumberController.*

  val form: Form[String] = formProvider()

  def onPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) { implicit request =>
      Ok(
        view(form.withDefault(request.userAnswers.get(PurchaseInvoiceNumberPage(supplierNumber, vehicleNumber))), supplierNumber, vehicleNumber, mode)
      )
    }

  def onSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, supplierNumber, vehicleNumber, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(PurchaseInvoiceNumberPage(supplierNumber, vehicleNumber), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                PurchaseInvoiceNumberPage(supplierNumber, vehicleNumber),
                mode,
                updatedAnswers,
                NovaUserType.from(request.affinityGroup, request.enrolments)
              )
            )
        )
    }
}

object PurchaseInvoiceNumberController {

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
