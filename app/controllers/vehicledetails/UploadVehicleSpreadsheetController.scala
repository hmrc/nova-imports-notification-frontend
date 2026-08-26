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
import connectors.NovaImportsBackendConnector
import controllers.BaseController
import controllers.actions.Actions
import controllers.routes
import controllers.utils.IsDraftIdDefined
import controllers.vehicledetails.UploadVehicleSpreadsheetController.guardPredicate
import models.SpreadsheetValidationType
import models.requests.DataRequest
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.UploadVehicleSpreadsheetView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class UploadVehicleSpreadsheetController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  view: UploadVehicleSpreadsheetView,
  connector: NovaImportsBackendConnector,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      val validationType =
        if (request.userAnswers.get(VehicleFromEuPage).contains(true)) SpreadsheetValidationType.CarsEu else SpreadsheetValidationType.CarsNonEu

      connector.createUploadTracking(request.userAnswers.get(DraftIdPage).get, validationType).map {
        case Right(uploadTracking) =>
          Ok(view(uploadTracking.uploadUrl, uploadTracking.fields, appConfig.multipleVehiclesSpreadsheetsUrl))
        case Left(error) =>
          logger.warn(s"Could not start a vehicle spreadsheet upload: $error")
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }
}

object UploadVehicleSpreadsheetController {

  def guardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).isDefined &&
      (request.userContext.isVatRegisteredOrganisation || request.userContext.isAgentWithClient)
}
