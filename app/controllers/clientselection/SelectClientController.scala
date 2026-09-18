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
import connectors.{GetNotificationSummaryError, NovaImportsBackendConnector}
import controllers.BaseController
import controllers.actions.*
import controllers.routes.{JourneyRecoveryController, LandingPageController, UnauthorisedController}
import models.{AgentSelectedClient, NotificationSummary, UserAnswers}
import pages.AgentSelectedClientPage
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class SelectClientController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  connector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  def select(vrn: String): Action[AnyContent] = actions.novaAgentAuthAndGetOptionalData().async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val answers = request.userAnswers.getOrElse(UserAnswers(request.userId))

    connector.getNotificationSummary(Some(vrn)).flatMap {
      case Right(summary: NotificationSummary.AgentWithClient) =>
        sessionRepository
          .setPage(answers, AgentSelectedClientPage, AgentSelectedClient(vrn, summary.clientTraderName))
          .map(_ => Redirect(LandingPageController.onPageLoad()))
      case Left(GetNotificationSummaryError.ClientNotFound) =>
        Future.successful(Redirect(UnauthorisedController.onPageLoad()))
      case Left(GetNotificationSummaryError.UpstreamError(status, _)) =>
        logger.warn(s"could not select client, backend returned $status")
        Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))
      case Right(_) =>
        logger.warn("could not select client, summary was not for an agent with a client")
        Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))
    }
  }
}
