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
import controllers.vehicledetails.UploadVinNumbersController.{importGuardPredicate, supplierGuardPredicate}
import models.requests.DataRequest
import models.{ImportNumber, NormalMode, SupplierNumber, VehicleNumber}
import pages.DraftIdPage
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.{ImportService, SupplierService, VehicleService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.*
import uk.gov.hmrc.play.bootstrap.binders.*
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.VinUploadStatusView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VinUploadStatusController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  connector: NovaImportsBackendConnector,
  appConfig: FrontendAppConfig,
  supplierService: SupplierService,
  importService: ImportService,
  vehicleService: VehicleService,
  view: VinUploadStatusView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import VinUploadStatusController.*

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, remove: Boolean): Action[AnyContent] =
    actions
      .authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber))
      .async { implicit request =>
        handlePageLoad(
          remove,
          cancelUrl = routes.UploadVinNumbersController.supplierOnPageLoad(supplierNumber, vehicleNumber, restart = true),
          removeUrl = routes.VinUploadStatusController.supplierOnPageLoad(supplierNumber, vehicleNumber, remove = true),
          continueUrl = routes.EnterVinNumbersController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode),
          errorUrls = errorUrlsFor(supplierNumber = Some(supplierNumber.value), importNumber = None, vehicleNumber = vehicleNumber.value)
        )
      }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber, remove: Boolean): Action[AnyContent] =
    actions
      .authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber))
      .async { implicit request =>
        handlePageLoad(
          remove,
          cancelUrl = routes.UploadVinNumbersController.importOnPageLoad(importNumber, vehicleNumber, restart = true),
          removeUrl = routes.VinUploadStatusController.importOnPageLoad(importNumber, vehicleNumber, remove = true),
          continueUrl = routes.EnterVinNumbersController.importOnPageLoad(importNumber, vehicleNumber, NormalMode),
          errorUrls = errorUrlsFor(supplierNumber = None, importNumber = Some(importNumber.value), vehicleNumber = vehicleNumber.value)
        )
      }

  def status(continueUrl: RedirectUrl): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard((request: DataRequest[?]) => IsDraftIdDefined(request.userAnswers)).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val lang = messagesApi.preferred(request)

      val safeContinueUrl = continueUrl.getEither(OnlyRelative) match {
        case Right(safeUrl) => safeUrl.url
        case Left(message)  =>
          logger.warn(s"Rejected unsafe continueUrl on VIN upload status poll: $message")
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }

      connector.getFileUploadSummary(request.userAnswers.get(DraftIdPage).get).map {
        case Right(summary) if summary.kind != "VIN" =>
          logger.warn(s"Expected a VIN upload in progress for draftId ${request.userAnswers.get(DraftIdPage).get.value} but found ${summary.kind}")
          InternalServerError
        case Right(summary) =>
          Ok(
            Json.obj(
              "statusText" -> lang(
                if (isFinal(summary.fileStatus)) "vinUploadStatus.status.uploaded" else "vinUploadStatus.status.uploading"
              ),
              "isFinal"     -> isFinal(summary.fileStatus),
              "fileName"    -> summary.fileName,
              "continueUrl" -> safeContinueUrl
            )
          )
        case Left(error) =>
          logger.warn(s"Could not retrieve the VIN upload summary: $error")
          InternalServerError
      }
    }

  private def handlePageLoad(
    remove: Boolean,
    cancelUrl: Call,
    removeUrl: Call,
    continueUrl: Call,
    errorUrls: ErrorUrls
  )(implicit request: DataRequest[AnyContent]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val draftId = request.userAnswers.get(DraftIdPage).get

    if (remove) {
      connector
        .deleteVinUpload(draftId)
        .map {
          case Right(_)    => ()
          case Left(error) => logger.warn(s"Could not delete the VIN upload: $error")
        }
        .map(_ => Redirect(controllers.routes.LandingPageController.onPageLoad())) // TODO: navigate to AMV1.0 when built
    } else {
      connector.getFileUploadSummary(draftId).map {
        case Right(summary) if summary.kind != "VIN" =>
          logger.warn(s"Expected a VIN upload in progress for draftId ${draftId.value} but found ${summary.kind}")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Right(summary) if summary.fileStatus == "VERIFICATION_FAILED" =>
          Redirect(errorUrls.forReason(summary.failureReason))
        case Right(summary) =>
          Ok(
            view(
              isFinal = isFinal(summary.fileStatus),
              fileName = summary.fileName,
              cancelUrl = cancelUrl,
              removeUrl = removeUrl,
              statusUrl = routes.VinUploadStatusController.status(RedirectUrl(continueUrl.url)).url,
              refreshIntervalSeconds = appConfig.uploadStatusRefreshIntervalSeconds,
              maxPollSeconds = appConfig.uploadStatusMaxPollSeconds,
              timeoutUrl = errorUrls.unknown,
              continueUrl = if (isFinal(summary.fileStatus)) Some(continueUrl) else None
            )
          )
        case Left(error) =>
          logger.warn(s"Could not retrieve the VIN upload summary: $error")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }
  }
}

object VinUploadStatusController {

  private val terminalStatuses = Set("UPLOADED")

  def isFinal(fileStatus: String): Boolean = terminalStatuses.contains(fileStatus)

  final case class ErrorUrls(quarantine: Call, rejected: Call, unknown: Call) {
    def forReason(failureReason: Option[String]): Call = failureReason match {
      case Some("QUARANTINE") => quarantine
      case Some("REJECTED")   => rejected
      case _                  => unknown
    }
  }

  def errorUrlsFor(supplierNumber: Option[Int], importNumber: Option[Int], vehicleNumber: Int): ErrorUrls =
    ErrorUrls(
      quarantine = routes.UploadSpreadsheetErrorQuarantineController
        .onPageLoad(from = "vin", supplierNumber = supplierNumber, importNumber = importNumber, vehicleNumber = Some(vehicleNumber)),
      rejected = routes.UploadSpreadsheetErrorRejectedController
        .onPageLoad(from = "vin", supplierNumber = supplierNumber, importNumber = importNumber, vehicleNumber = Some(vehicleNumber)),
      unknown = routes.UploadSpreadsheetErrorUnknownController
        .onPageLoad(from = "vin", supplierNumber = supplierNumber, importNumber = importNumber, vehicleNumber = Some(vehicleNumber))
    )
}
