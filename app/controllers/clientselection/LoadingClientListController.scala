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
import config.FrontendAppConfig
import connectors.NovaImportsBackendConnector
import controllers.BaseController
import controllers.actions.*
import models.ClientListStatus.*
import models.responses.ClientListRefresh
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.LoadingClientListView

import scala.concurrent.{ExecutionContext, Future}

class LoadingClientListController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  view: LoadingClientListView,
  actions: Actions,
  connector: NovaImportsBackendConnector,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  def onPageLoad(interval: Option[Int], attempt: Int): Action[AnyContent] = actions.novaAgentAuthAndGetOptionalData().async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val attempts         = math.max(0, attempt)
    val couldNotRetrieve = Redirect(routes.CouldNotRetrieveClientListController.onPageLoad())

    connector.getClientListStatus().flatMap {
      case Right(Succeeded) =>
        Future.successful(Redirect(routes.ViewClientsController.onPageLoad(None, None, 1)))
      case Right(Failed) =>
        Future.successful(couldNotRetrieve)
      case Right(InitiateDownload | InProgress) if attempts >= appConfig.clientListMaxRetries =>
        logger.warn(s"client list still not available after $attempts attempts")
        Future.successful(couldNotRetrieve)
      case Right(InitiateDownload) if attempts == 0 =>
        connector.refreshClientList().map {
          case Right(ClientListRefresh(true, Some(intervalMs))) => poll(intervalMs, 1)
          case other                                            =>
            logger.warn(s"client list refresh was not started: $other")
            couldNotRetrieve
        }
      case Right(InitiateDownload | InProgress) =>
        Future.successful(poll(interval.getOrElse(appConfig.clientListFallbackIntervalMs), attempts + 1))
      case Left(error) =>
        logger.warn(s"failed to fetch client list status: $error")
        Future.successful(couldNotRetrieve)
    }
  }

  private def poll(intervalMs: Int, attempt: Int)(implicit request: Request[?]): Result = {
    val seconds = math.max(1, math.ceil(intervalMs / 1000.0).toInt)
    Ok(view(routes.LoadingClientListController.onPageLoad(Some(intervalMs), attempt).url, seconds))
  }
}
