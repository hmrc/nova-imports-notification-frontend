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
import controllers.vehicledetails.VehiclesBoughtFromSupplierAvd12PlaceholderController.*
import models.requests.DataRequest
import models.{NormalMode, SupplierNumber, VehicleNumber}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import services.{SupplierService, VehicleService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.VehiclesBoughtFromSupplierAvd12PlaceholderView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

// TODO: DELETE THIS FILE when AVD12.0 is built.
// AVD12.0 is not a new screen. It is the AVD2.0 screen (VehiclesBoughtFromSupplierController) shown later in journey
// This screen exists only so we can test the vehicle and supplier numbering for what will be going into AVD2.0.
// Delete this controller, its view, its spec, its routes and its messages when AVD12.0 goes into in AVD2.0.
// This code can be used as part of the AVD12.0 section build out.
class VehiclesBoughtFromSupplierAvd12PlaceholderController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  view: VehiclesBoughtFromSupplierAvd12PlaceholderView,
  supplierService: SupplierService,
  vehicleService: VehicleService
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad(supplierNumber: SupplierNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>
      renderPage(supplierNumber, limitReached = false).map(Ok(_))
    }

  // adding a vehicle is blocked once the notification holds 100 vehicles
  def onSubmit(supplierNumber: SupplierNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>
      if (vehicleService.limitReached(request.userAnswers))
        renderPage(supplierNumber, limitReached = true).map(BadRequest(_))
      else
        vehicleService.addForSupplier(request.userAnswers, supplierNumber).map { vehicleNumber =>
          Redirect(routes.VehicleDatesController.onPageLoad(supplierNumber, vehicleNumber, NormalMode))
        }
    }

  def onDelete(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(deleteGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request =>
        vehicleService.deleteValues(request.userAnswers, vehicleNumber).map { _ =>
          Redirect(routes.VehiclesBoughtFromSupplierAvd12PlaceholderController.onPageLoad(supplierNumber))
        }
    }

  private def renderPage(supplierNumber: SupplierNumber, limitReached: Boolean)(implicit request: DataRequest[?]): Future[Html] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    // only gets vehicles from this supplier
    val vehicleNumbers = vehicleService.inOrder(request.userAnswers).collect {
      case (vehicleNumber, _) if vehicleService.belongsToSupplier(request.userAnswers, vehicleNumber, supplierNumber) => vehicleNumber
    }

    supplierService.supplierName(request.userAnswers, request.userContext, supplierNumber).map { name =>
      view(supplierNumber, name, vehicleNumbers, limitReached)
    }
  }
}

object VehiclesBoughtFromSupplierAvd12PlaceholderController {

  // the supplier number in the URL must be one of the suppliers the user has in session
  def guardPredicate(supplierService: SupplierService, supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      supplierService.numberHasValues(request.userAnswers, supplierNumber)

  // the vehicle being deleted must also belong to the supplier
  def deleteGuardPredicate(
    supplierService: SupplierService,
    vehicleService: VehicleService,
    supplierNumber: SupplierNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    guardPredicate(supplierService, supplierNumber)(request) &&
      vehicleService.belongsToSupplier(request.userAnswers, vehicleNumber, supplierNumber)
}
