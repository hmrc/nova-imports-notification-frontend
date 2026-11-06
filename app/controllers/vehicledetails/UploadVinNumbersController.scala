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

import connectors.{GetFileUploadSummaryError, NovaImportsBackendConnector}
import controllers.BaseController
import controllers.actions.Actions
import controllers.utils.IsDraftIdDefined
import models.requests.DataRequest
import models.{DraftId, ImportNumber, OneMultiVehicle, SupplierNumber, VehicleNumber, VinUploadError}
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.vehicledetails.NumberOfVehiclesPage
import play.api.Logging
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.{ImportService, SupplierService, VehicleService}
import views.html.UploadVinNumbersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadVinNumbersController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  view: UploadVinNumbersView,
  connector: NovaImportsBackendConnector,
  supplierService: SupplierService,
  importService: ImportService,
  vehicleService: VehicleService
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import UploadVinNumbersController.*

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, restart: Boolean): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request =>
        handlePageLoad(
          restart,
          statusPage = routes.VinUploadStatusController.supplierOnPageLoad(supplierNumber, vehicleNumber),
          selfPage = routes.UploadVinNumbersController.supplierOnPageLoad(supplierNumber, vehicleNumber, restart = false)
        )
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber, restart: Boolean): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async {
      implicit request =>
        handlePageLoad(
          restart,
          statusPage = routes.VinUploadStatusController.importOnPageLoad(importNumber, vehicleNumber),
          selfPage = routes.UploadVinNumbersController.importOnPageLoad(importNumber, vehicleNumber, restart = false)
        )
    }

  private def handlePageLoad(restart: Boolean, statusPage: Call, selfPage: Call)(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val draftId = request.userAnswers.get(DraftIdPage).get

    val discardExisting =
      if (restart)
        connector.deleteVinUpload(draftId).map {
          case Right(_)    => ()
          case Left(error) => logger.warn(s"Could not delete the existing VIN upload before restarting: $error")
        }
      else Future.unit

    discardExisting.flatMap { _ =>
      connector.getFileUploadSummary(draftId).flatMap {
        case Right(summary) if summary.kind != "VIN" =>
          renderUploadForm(draftId, statusPage, selfPage)
        case Right(summary) if summary.fileStatus != "AWAITING_UPLOAD" =>
          Future.successful(Redirect(statusPage))
        case Right(_) =>
          renderUploadForm(draftId, statusPage, selfPage)
        case Left(GetFileUploadSummaryError.NotFound) =>
          renderUploadForm(draftId, statusPage, selfPage)
        case Left(GetFileUploadSummaryError.Forbidden) =>
          Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
        case Left(error) =>
          logger.warn(s"Could not check the existing VIN upload summary: $error")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
  }

  private def renderUploadForm(draftId: DraftId, statusPage: Call, selfPage: Call)(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val uploadError = request.getQueryString("errorCode").map(VinUploadError.fromUpscanErrorCode)

    connector
      .createVinUploadTracking(
        draftId,
        request.userAnswers.get(AmendSubmittedNotificationPage),
        successRedirect = statusPage.absoluteURL(),
        errorRedirect = selfPage.absoluteURL()
      )
      .map {
        case Right(uploadTracking) =>
          Ok(view(uploadTracking.uploadUrl, uploadTracking.fields, uploadError))
        case Left(error) =>
          logger.warn(s"Could not start a VIN upload: $error")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }
}

object UploadVinNumbersController {

  def supplierGuardPredicate(
    supplierService: SupplierService,
    vehicleService: VehicleService,
    supplierNumber: SupplierNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      !request.userAnswers.get(AmendSubmittedNotificationPage).contains(true) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      supplierService.numberHasValues(request.userAnswers, supplierNumber) &&
      vehicleService.belongsToSupplier(request.userAnswers, vehicleNumber, supplierNumber) &&
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber) &&
      request.userAnswers.get(NumberOfVehiclesPage(vehicleNumber)).exists(_.oneMultiVehicle == OneMultiVehicle.More)

  def importGuardPredicate(
    importService: ImportService,
    vehicleService: VehicleService,
    importNumber: ImportNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      !request.userAnswers.get(AmendSubmittedNotificationPage).contains(true) &&
      request.userAnswers.get(VehicleFromEuPage).contains(false) &&
      importService.numberHasValues(request.userAnswers, importNumber) &&
      vehicleService.belongsToImport(request.userAnswers, vehicleNumber, importNumber) &&
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber) &&
      request.userAnswers.get(NumberOfVehiclesPage(vehicleNumber)).exists(_.oneMultiVehicle == OneMultiVehicle.More)
}
