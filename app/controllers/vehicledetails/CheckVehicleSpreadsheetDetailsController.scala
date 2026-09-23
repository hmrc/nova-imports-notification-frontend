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

import connectors.NovaImportsBackendConnector
import controllers.BaseController
import controllers.actions.Actions
import controllers.vehicledetails.VehicleSpreadsheetUploadController.guardPredicate
import pages.DraftIdPage
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.Pager
import views.html.CheckVehicleSpreadsheetDetailsView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CheckVehicleSpreadsheetDetailsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  connector: NovaImportsBackendConnector,
  view: CheckVehicleSpreadsheetDetailsView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  def onPageLoad(page: Int): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      connector.getUploadResult(request.userAnswers.get(DraftIdPage).get).map {
        case Right(result) if result.fileStatus == "VALIDATED" =>
          Ok(view(Pager.page(result.vehicles, page), p => routes.CheckVehicleSpreadsheetDetailsController.onPageLoad(p).url))
        case Right(result) if result.fileStatus == "VALIDATION_FAILED" => Redirect(routes.CheckVehicleSpreadsheetErrorsController.onPageLoad())
        case Right(_)                                                  => Redirect(routes.VehicleSpreadsheetUploadController.onPageLoad())
        case Left(error)                                               =>
          logger.warn(s"Could not retrieve the vehicle spreadsheet details: $error")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

}
