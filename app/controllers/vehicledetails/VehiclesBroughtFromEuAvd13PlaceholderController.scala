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
import controllers.supplierdetails
import controllers.utils.IsDraftIdDefined
import controllers.vehicledetails.VehiclesBroughtFromEuAvd13PlaceholderController.*
import models.requests.DataRequest
import models.{NormalMode, SupplierNumber}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SupplierService, VehicleService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.VehiclesBroughtFromEuAvd13PlaceholderView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

// TODO: DELETE THIS FILE when AVD13.0 is built.
// AVD13.0 is not a new screen. It is the AVD1.0 screen (AddVehicleDetailsController) shown later in the
// journey, with the supplier list sitting above the question.
// This test screen exists only so we can test the supplier and vehicle numbering. It does not touch AVD1.0.
// Delete this controller, its view, its spec, its routes and its messages when AVD13.0 lands in AVD1.0.
// The code used here cna be used as part of the AVD13.0 build out.
class VehiclesBroughtFromEuAvd13PlaceholderController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  view: VehiclesBroughtFromEuAvd13PlaceholderView,
  supplierService: SupplierService,
  vehicleService: VehicleService
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad(): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      suppliersWithNames.map(suppliers => Ok(view(suppliers, limitReached = false)))
    }

  // adding a supplier is blocked once the notification holds 100 vehicles, same as the as-is
  def onSubmit(): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      if (vehicleService.limitReached(request.userAnswers))
        suppliersWithNames.map(suppliers => BadRequest(view(suppliers, limitReached = true)))
      else
        supplierService.add(request.userAnswers).map { supplierNumber =>
          Redirect(supplierdetails.routes.UsePersonalDetailsAsSupplierController.onPageLoad(supplierNumber, NormalMode))
        }
    }

  // deleting a supplier takes its vehicles with it
  def onDelete(supplierNumber: SupplierNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      supplierService.deleteValues(request.userAnswers, supplierNumber).map { _ =>
        Redirect(routes.VehiclesBroughtFromEuAvd13PlaceholderController.onPageLoad())
      }
    }

  private def suppliersWithNames(implicit request: DataRequest[?]): Future[Seq[(SupplierNumber, Option[String])]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    Future.sequence(
      supplierService.inOrder(request.userAnswers).map { case (supplierNumber, _) =>
        supplierService.supplierName(request.userAnswers, request.userContext, supplierNumber).map(name => supplierNumber -> name)
      }
    )
  }
}

object VehiclesBroughtFromEuAvd13PlaceholderController {

  def guardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers)
}
