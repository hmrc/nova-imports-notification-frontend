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
import connectors.{GetFileUploadSummaryError, NovaImportsBackendConnector}
import controllers.BaseController
import controllers.actions.Actions
import controllers.utils.IsDraftIdDefined
import controllers.vehicledetails.UploadVehicleSpreadsheetController.{guardPredicate, spreadsheetValidationTypeFor}
import models.{DraftId, SpreadsheetUploadError, SpreadsheetValidationType, UserAnswers}
import models.requests.DataRequest
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import views.html.UploadVehicleSpreadsheetView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadVehicleSpreadsheetController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  view: UploadVehicleSpreadsheetView,
  connector: NovaImportsBackendConnector,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  def onPageLoad(restart: Boolean): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      val draftId = request.userAnswers.get(DraftIdPage).get

      val discardExisting =
        if (restart)
          connector.deleteFileUpload(draftId).map {
            case Right(_)    => ()
            case Left(error) =>
              logger.warn(s"Could not delete the existing vehicle spreadsheet upload before restarting: $error")
          }
        else Future.unit

      discardExisting.flatMap { _ =>
        connector.getFileUploadSummary(draftId).flatMap {
          case Right(summary) if summary.fileStatus != "AWAITING_UPLOAD" =>
            Future.successful(Redirect(controllers.vehicledetails.routes.VehicleSpreadsheetUploadController.onPageLoad()))
          case Right(_) =>
            renderUploadForm(draftId)
          case Left(GetFileUploadSummaryError.NotFound) =>
            renderUploadForm(draftId)
          case Left(GetFileUploadSummaryError.Forbidden) =>
            Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
          case Left(error) =>
            logger.warn(s"Could not check the existing upload summary: $error")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      }
    }

  private def renderUploadForm(draftId: DraftId)(implicit request: DataRequest[?]): Future[Result] = {
    val uploadError = request.getQueryString("errorCode").map(SpreadsheetUploadError.fromUpscanErrorCode)

    connector.createUploadTracking(draftId, request.userAnswers.get(AmendSubmittedNotificationPage)).map {
      case Right(uploadTracking) =>
        Ok(view(uploadTracking.uploadUrl, uploadTracking.fields, appConfig.multipleVehiclesSpreadsheetsUrl, uploadError))
      case Left(error) =>
        logger.warn(s"Could not start a vehicle spreadsheet upload: $error")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }
}

object UploadVehicleSpreadsheetController {

  def guardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).isDefined &&
      (request.userContext.isVatRegisteredOrganisation || request.userContext.isAgentWithClient)

  private def spreadsheetValidationTypeFor(answers: UserAnswers): SpreadsheetValidationType =
    if (answers.get(VehicleFromEuPage).contains(true)) SpreadsheetValidationType.CarsEu else SpreadsheetValidationType.CarsNonEu
}
