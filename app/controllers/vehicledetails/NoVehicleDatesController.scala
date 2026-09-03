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
import models.requests.DataRequest
import models.{SupplierNumber, VehicleDates, VehicleNumber}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.VehicleDatesPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SupplierService, VehicleService}
import views.html.NoVehicleDatesView

import javax.inject.Inject

class NoVehicleDatesController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  supplierService: SupplierService,
  vehicleService: VehicleService,
  view: NoVehicleDatesView,
  appConfig: FrontendAppConfig
) extends BaseController {

  import NoVehicleDatesController.*

  def onPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) { implicit request =>
      Ok(view(appConfig.personalTransportUnitUrl))
    }
}

object NoVehicleDatesController {

  def guardPredicate(
    supplierService: SupplierService,
    vehicleService: VehicleService,
    supplierNumber: SupplierNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      supplierService.numberExists(request.userAnswers, supplierNumber) &&
      vehicleService.belongsToSupplier(request.userAnswers, vehicleNumber, supplierNumber) &&
      request.userAnswers.get(VehicleDatesPage(supplierNumber, vehicleNumber)).exists(_.contains(VehicleDates.NoDates))
}
