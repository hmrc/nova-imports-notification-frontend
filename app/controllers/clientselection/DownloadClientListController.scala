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

package controllers.clientselection

import com.google.inject.Inject
import connectors.NovaImportsBackendConnector
import controllers.BaseController
import controllers.actions.*
import controllers.routes.JourneyRecoveryController
import models.ClientListQuery
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.ClientListCsv

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class DownloadClientListController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  connector: NovaImportsBackendConnector
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  def download(): Action[AnyContent] = actions.novaAgentAuthAndGetOptionalData().async { implicit request =>
    connector
      .getClientList(ClientListQuery())
      .map {
        case Right(clientList) =>
          Ok(ClientListCsv(clientList))
            .as("text/csv; charset=utf-8")
            .withHeaders(CONTENT_DISPOSITION -> "attachment; filename=\"clients.csv\"")
        case Left(error) =>
          logger.warn(s"failed to download client list: $error")
          Redirect(JourneyRecoveryController.onPageLoad())
      }
      .recover { case NonFatal(e) =>
        logger.warn("failed to download client list", e)
        Redirect(JourneyRecoveryController.onPageLoad())
      }
  }
}
