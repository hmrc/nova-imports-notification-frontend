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

package controllers.notification

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.NovaImportsBackendConnector
import controllers.BaseController
import controllers.actions.*
import models.{DraftId, DraftNotificationSummary, NotificationSummary, UserAnswers, UserContext}
import pages.{AgentSelectedClientPage, DraftIdPage}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.UserDataService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.PageOf
import views.html.ViewSavedNotificationsView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class ViewSavedNotificationsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  view: ViewSavedNotificationsView,
  actions: Actions,
  connector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  userDataService: UserDataService,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  def onPageLoad(page: Int): Action[AnyContent] =
    actions.authAndGetOptionalData().async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val answers     = request.userAnswers.getOrElse(UserAnswers(request.userId))
      val ctx         = UserContext.from(request.affinityGroup, request.enrolments, answers)
      val clientVrn   = ctx.selectedClient.map(_.vrn)
      val currentPage = math.max(1, page)
      val pageSize    = appConfig.savedNotificationsPageSize

      connector.getDraftNotifications(clientVrn, currentPage, pageSize).flatMap {
        case Right(notifications) if notifications.totalCount == 0 =>
          Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
        case Right(notifications) =>
          traderOrClientSummary(ctx, clientVrn).map { summary =>
            val notificationsWithNames =
              notifications.copy(drafts = notifications.drafts.map(draft => draft.copy(purchaserName = nameForPurchaser(draft, ctx, summary))))
            val results = PageOf(notificationsWithNames.drafts, currentPage, pageSize, notifications.totalCount)
            if (results.items.isEmpty && currentPage > results.totalPages)
              Redirect(routes.ViewSavedNotificationsController.onPageLoad(results.totalPages))
            else
              Ok(view(results))
          }
        case Left(error) =>
          logger.warn(s"failed to fetch draft notifications: $error")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onContinue(draftId: String): Action[AnyContent] =
    actions.authAndGetOptionalData().async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val answers = request.userAnswers.getOrElse(UserAnswers(request.userId))
      val ctx     = UserContext.from(request.affinityGroup, request.enrolments, answers)

      // clears the session, keeps the selected client for an agent with a client, writes the chosen draftId
      val selectedClient                   = if (ctx.isAgentWithClient) answers.get(AgentSelectedClientPage) else None
      val clearedAnswers: Try[UserAnswers] = selectedClient match {
        case Some(client) => UserAnswers(request.userId).set(AgentSelectedClientPage, client)
        case None         => Success(UserAnswers(request.userId))
      }

      for {
        answersWithDraftId <- Future.fromTry(clearedAnswers.flatMap(_.set(DraftIdPage, DraftId(draftId))))
        _                  <- sessionRepository.set(answersWithDraftId)
        retrievedDraft     <- userDataService.retrieveAndStoreDraftNotification(DraftId(draftId), answersWithDraftId, ctx)
      } yield retrievedDraft match {
        case Right(_)    => Redirect(controllers.routes.NotificationTaskListController.onPageLoad(draftLoaded = true))
        case Left(error) =>
          logger.warn(s"failed to load saved notification $draftId: $error")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

  private def traderOrClientSummary(ctx: UserContext, clientVrn: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[Option[NotificationSummary]] =
    if (ctx.isVatRegisteredOrganisation || ctx.isAgentWithClient)
      connector.getNotificationSummary(clientVrn).map {
        case Right(summary) => Some(summary)
        case Left(error)    =>
          logger.warn(s"failed to fetch summary for the saved notifications names: $error")
          None
      }
    else Future.successful(None)

  private def nameForPurchaser(draft: DraftNotificationSummary, ctx: UserContext, summary: Option[NotificationSummary]): Option[String] =
    summary match {
      case Some(s: NotificationSummary.AgentWithClient) => s.clientTraderName.orElse(ctx.selectedClient.flatMap(_.name))
      case _ if ctx.isAgentWithClient                   => ctx.selectedClient.flatMap(_.name)
      case Some(s: NotificationSummary.IndividualOrOrganisation) if ctx.isVatRegisteredOrganisation => draft.purchaserName.orElse(s.traderName)
      case _                                                                                        => draft.purchaserName
    }
}
