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
import controllers.utils.IsDraftIdDefined
import controllers.vehicledetails.VehicleSpreadsheetUploadController.{continueUrlFor, errorRedirectFor, guardPredicate, isFinal, removeUrlFor}
import models.NormalMode
import models.requests.DataRequest
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.VehicleSpreadsheetUploadView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class VehicleSpreadsheetUploadController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  connector: NovaImportsBackendConnector,
  appConfig: FrontendAppConfig,
  view: VehicleSpreadsheetUploadView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  def onPageLoad(remove: Boolean): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val draftId = request.userAnswers.get(DraftIdPage).get

      if (remove) {
        val removeUrl = removeUrlFor(request.userAnswers.get(VehicleFromEuPage).contains(true))
        connector
          .deleteFileUpload(draftId)
          .map {
            case Right(_)    => ()
            case Left(error) =>
              logger.warn(s"Could not delete the vehicle spreadsheet upload: $error")
          }
          .map(_ => Redirect(removeUrl))
      } else {
        connector.getFileUploadSummary(draftId).map {
          case Right(summary) =>
            errorRedirectFor(summary.fileStatus, summary.failureReason) match {
              case Some(call) => Redirect(call)
              case None       =>
                Ok(
                  view(
                    isFinal = isFinal(summary.fileStatus),
                    fileName = summary.fileName,
                    removeUrl = routes.VehicleSpreadsheetUploadController.onPageLoad(remove = true),
                    statusUrl = routes.VehicleSpreadsheetUploadController.status().url,
                    refreshIntervalSeconds = appConfig.uploadStatusRefreshIntervalSeconds,
                    maxPollSeconds = appConfig.uploadStatusMaxPollSeconds,
                    continueUrl = continueUrlFor(summary.fileStatus)
                  )
                )
            }
          case Left(error) =>
            logger.warn(s"Could not retrieve the vehicle spreadsheet upload summary: $error")
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
      }
    }

  def status(): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val lang = messagesApi.preferred(request)

      connector.getFileUploadSummary(request.userAnswers.get(DraftIdPage).get).map {
        case Right(summary) =>
          errorRedirectFor(summary.fileStatus, summary.failureReason) match {
            case Some(call) =>
              Ok(Json.obj("redirectUrl" -> call.url))
            case None =>
              Ok(
                Json.obj(
                  "statusText" -> lang(
                    if (isFinal(summary.fileStatus)) "vehicleSpreadsheetUpload.status.uploaded" else "vehicleSpreadsheetUpload.status.uploading"
                  ),
                  "isFinal"     -> isFinal(summary.fileStatus),
                  "fileName"    -> summary.fileName,
                  "continueUrl" -> continueUrlFor(summary.fileStatus).map(_.url)
                )
              )
          }
        case Left(error) =>
          logger.warn(s"Could not retrieve the vehicle spreadsheet upload summary: $error")
          InternalServerError
      }
    }
}

object VehicleSpreadsheetUploadController {

  def guardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).isDefined &&
      (request.userContext.isVatRegisteredOrganisation || request.userContext.isAgentWithClient)

  private val terminalStatuses = Set("VALIDATED", "VALIDATION_FAILED")

  def isFinal(fileStatus: String): Boolean = terminalStatuses.contains(fileStatus)

  def continueUrlFor(fileStatus: String): Option[Call] = fileStatus match {
    case "VALIDATED"         => Some(routes.CheckVehicleSpreadsheetDetailsController.onPageLoad())
    case "VALIDATION_FAILED" => Some(routes.CheckVehicleSpreadsheetErrorsController.onPageLoad())
    case _                   => None
  }

  def errorRedirectFor(fileStatus: String, failureReason: Option[String]): Option[Call] =
    if (fileStatus != "VERIFICATION_FAILED") None
    else
      failureReason match {
        case Some("QUARANTINE") => Some(routes.UploadSpreadsheetErrorQuarantineController.onPageLoad())
        case Some("REJECTED")   => Some(routes.UploadSpreadsheetErrorRejectedController.onPageLoad())
        case _                  => Some(routes.UploadSpreadsheetErrorUnknownController.onPageLoad())
      }

  def removeUrlFor(vehicleFromEu: Boolean): Call =
    if (vehicleFromEu) routes.AddVehicleDetailsController.onPageLoad(NormalMode)
    else routes.AddImportVehicleDetailsController.onPageLoad(NormalMode)
}
