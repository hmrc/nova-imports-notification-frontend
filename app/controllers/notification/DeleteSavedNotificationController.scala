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

import connectors.{GetDraftNotificationError, NovaImportsBackendConnector}
import controllers.BaseController
import controllers.actions.*
import controllers.utils.{ClearedUserAnswers, PurchaserName}
import forms.DeleteSavedNotificationFormProvider
import models.requests.OptionalDataRequest
import models.{DraftId, UserAnswers, UserContext}
import play.api.Logging
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.DeleteSavedNotificationView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class DeleteSavedNotificationController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  connector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  actions: Actions,
  formProvider: DeleteSavedNotificationFormProvider,
  view: DeleteSavedNotificationView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(draftId: String): Action[AnyContent] =
    actions.authAndGetOptionalData().async { implicit request =>
      renderPage(draftId, form, Ok(_))
    }

  def onSubmit(draftId: String): Action[AnyContent] =
    actions.authAndGetOptionalData().async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => renderPage(draftId, formWithErrors, BadRequest(_)),
          {
            case true  => deleteAndRedirect(draftId)
            case false => Future.successful(Redirect(routes.ViewSavedNotificationsController.onPageLoad()))
          }
        )
    }

  private def deleteAndRedirect(draftId: String)(implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val (answers, ctx, clientVrn)  = userDetails(request)

    connector
      .deleteDraftNotification(DraftId(draftId), clientVrn)
      .flatMap {
        case Right(_) =>
          for {
            _      <- Future.fromTry(ClearedUserAnswers(request.userId, answers, ctx)).flatMap(sessionRepository.set)
            drafts <- connector.getDraftNotifications(clientVrn, 1, 1)
          } yield drafts match {
            case Right(notifications) if notifications.totalCount > 0 => Redirect(routes.ViewSavedNotificationsController.onPageLoad())
            case Right(_)                                             => Redirect(controllers.routes.LandingPageController.onPageLoad())
            case Left(error)                                          =>
              logger.warn(s"failed to fetch draft notifications after deleting $draftId: $error")
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
        case Left(error) =>
          logger.warn(s"failed to delete saved notification $draftId: $error")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
      .recover { case NonFatal(e) =>
        logger.warn(s"failed to delete saved notification $draftId", e)
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  private def renderPage(draftId: String, form: Form[Boolean], status: Html => Result)(implicit
    request: OptionalDataRequest[AnyContent]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val (_, ctx, clientVrn)        = userDetails(request)

    connector
      .getDraftNotification(DraftId(draftId))
      .flatMap {
        case Right(draft) =>
          PurchaserName.fetchTraderOrClientSummary(connector, ctx, clientVrn).map { summary =>
            val purchaserName = PurchaserName.toDisplay(PurchaserName.purchaserOrNotifierName(draft), ctx, summary)
            status(view(form, draftId, purchaserName, LocalDate.parse(draft.createdDate)))
          }
        case Left(error @ (GetDraftNotificationError.Forbidden | GetDraftNotificationError.NotFound)) =>
          logger.warn(s"no access to saved notification $draftId for delete: $error")
          Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
        case Left(error) =>
          logger.warn(s"failed to load saved notification $draftId for delete: $error")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
      .recover { case NonFatal(e) =>
        logger.warn(s"failed to load saved notification $draftId for delete", e)
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  private def userDetails(request: OptionalDataRequest[AnyContent]): (UserAnswers, UserContext, Option[String]) = {
    val answers = request.userAnswers.getOrElse(UserAnswers(request.userId))
    val ctx     = UserContext.from(request.affinityGroup, request.enrolments, answers)
    (answers, ctx, ctx.selectedClient.map(_.vrn))
  }
}
