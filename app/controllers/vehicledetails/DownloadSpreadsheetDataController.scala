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

import com.google.inject.Inject
import connectors.NovaImportsBackendConnector
import controllers.BaseController
import controllers.actions.*
import controllers.routes.JourneyRecoveryController
import controllers.vehicledetails.VehicleSpreadsheetUploadController.guardPredicate
import pages.DraftIdPage
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.SpreadsheetDataCsv

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class DownloadSpreadsheetDataController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  connector: NovaImportsBackendConnector
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  def download(): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
    connector
      .getUploadResult(request.userAnswers.get(DraftIdPage).get)
      .map {
        case Right(result) if result.fileStatus == "VALIDATED" =>
          SpreadsheetDataCsv(result) match {
            case Some(csv) =>
              Ok(csv)
                .as("text/csv; charset=utf-8")
                .withHeaders(CONTENT_DISPOSITION -> "attachment; filename=\"nova-summary.csv\"")
            case None =>
              logger.warn(s"failed to download spreadsheet data: no CSV columns for validationType ${result.validationType.getOrElse("none")}")
              Redirect(JourneyRecoveryController.onPageLoad())
          }
        case Right(result) =>
          logger.warn(s"failed to download spreadsheet data: fileStatus ${result.fileStatus}")
          Redirect(JourneyRecoveryController.onPageLoad())
        case Left(error) =>
          logger.warn(s"failed to download spreadsheet data: $error")
          Redirect(JourneyRecoveryController.onPageLoad())
      }
      .recover { case NonFatal(e) =>
        logger.warn("failed to download spreadsheet data", e)
        Redirect(JourneyRecoveryController.onPageLoad())
      }
  }
}
