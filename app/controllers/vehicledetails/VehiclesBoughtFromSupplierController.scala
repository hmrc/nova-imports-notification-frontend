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
import controllers.vehicledetails.VehiclesBoughtFromSupplierController.*
import models.requests.DataRequest
import models.{NormalMode, SupplierNumber}
import pages.sections.initialquestions.VehicleFromEuPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SupplierService, VehicleService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.VehiclesBoughtFromSupplierView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class VehiclesBoughtFromSupplierController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  view: VehiclesBoughtFromSupplierView,
  supplierService: SupplierService,
  vehicleService: VehicleService,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad(supplierNumber: SupplierNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      supplierService.supplierName(request.userAnswers, request.userContext, supplierNumber).map { name =>
        Ok(
          view(name, supplierNumber, appConfig.personalTransportUnitUrl)
        )
      }
    }

  // sets up a new vehicle collection in session, it carries the supplierNumber it was bought from in the current URL
  def onSubmit(supplierNumber: SupplierNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>
      vehicleService.addForSupplier(request.userAnswers, supplierNumber).map { vehicleNumber =>
        Redirect(routes.VehicleDatesController.onPageLoad(supplierNumber, vehicleNumber, NormalMode))
      }
    }
}

object VehiclesBoughtFromSupplierController {

  // The supplier number in the URL must be one of the suppliers the user has in session
  def guardPredicate(supplierService: SupplierService, supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      supplierService.numberExists(request.userAnswers, supplierNumber)
}
