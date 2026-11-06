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
import controllers.actions.Actions
import controllers.utils.IsDraftIdDefined
import models.requests.DataRequest
import models.{ImportNumber, SupplierNumber, VehicleNumber}
import pages.sections.initialquestions.VehicleFromEuPage
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import views.html.UploadSpreadsheetErrorRejectedView

import javax.inject.Inject

class UploadSpreadsheetErrorRejectedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  view: UploadSpreadsheetErrorRejectedView
) extends BaseController {

  import UploadSpreadsheetErrorRejectedController.*

  def onPageLoad(
    from: String = "spreadsheet",
    supplierNumber: Option[Int] = None,
    importNumber: Option[Int] = None,
    vehicleNumber: Option[Int] = None
  ): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicateFor(from)) { implicit request =>
    Ok(view(tryAgainUrlFor(from, supplierNumber, importNumber, vehicleNumber)))
  }
}

object UploadSpreadsheetErrorRejectedController {

  def guardPredicateFor(from: String)(request: DataRequest[?]): Boolean =
    if (from == "vin") vinGuardPredicate(request) else spreadsheetGuardPredicate(request)

  private def spreadsheetGuardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).isDefined &&
      (request.userContext.isVatRegisteredOrganisation || request.userContext.isAgentWithClient)

  private def vinGuardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).isDefined

  def tryAgainUrlFor(from: String, supplierNumber: Option[Int], importNumber: Option[Int], vehicleNumber: Option[Int]): Call =
    if (from != "vin") routes.UploadVehicleSpreadsheetController.onPageLoad(restart = true)
    else
      (supplierNumber, importNumber, vehicleNumber) match {
        case (Some(s), _, Some(v)) =>
          routes.UploadVinNumbersController.supplierOnPageLoad(SupplierNumber(s), VehicleNumber(v), restart = true)
        case (_, Some(i), Some(v)) =>
          routes.UploadVinNumbersController.importOnPageLoad(ImportNumber(i), VehicleNumber(v), restart = true)
        case _ =>
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }
}
