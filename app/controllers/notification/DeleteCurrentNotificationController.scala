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

import config.FrontendAppConfig
import connectors.{DeleteDraftNotificationError, NovaImportsBackendConnector}
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import controllers.{BaseController, routes}
import forms.DeleteCurrentNotificationFormProvider
import models.DraftId
import models.requests.DataRequest
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, RequestHeader}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.{DeleteCurrentNotificationView, PageNotFoundView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteCurrentNotificationController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  backendConnector: NovaImportsBackendConnector,
  appConfig: FrontendAppConfig,
  actions: Actions,
  formProvider: DeleteCurrentNotificationFormProvider,
  view: DeleteCurrentNotificationView,
  pageNotFoundView: PageNotFoundView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import DeleteCurrentNotificationController.*

  val form: Form[Boolean] = formProvider()

  // TODO: Incoming navigation changes from NTL 1 and 3 (same page?)

  def onPageLoad(): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate()) { implicit request =>
      Ok(view(form.withDefault(None)))
    }

  def onSubmit(): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate()).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
          {
            case true =>
              request.userAnswers.get(DraftIdPage) match {
                case Some(draftId) =>
                  implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
                  backendConnector.deleteDraftNotification(draftId).flatMap {
                    case Right(states) =>
                      // Navigate back to ntl
                      Future.successful(Redirect(routes.NotificationTaskListController.onPageLoad()))
                    case Left(error) =>
                      handleDeleteFailure(draftId, error)
                  }
                case None =>
                  logger.warn(s"Failed to find draft id for delete current notification ")
                  // Show ERR3.0 View
                  Future.successful(Ok(pageNotFoundView(appConfig.technicalSupportUrl)))
              }

            case false =>
              // Navigate back to ntl
              Future.successful(Redirect(routes.NotificationTaskListController.onPageLoad()))
          }
        )
    }

  private def handleDeleteFailure(draftId: DraftId, error: DeleteDraftNotificationError)(implicit rh: RequestHeader, messages: Messages) = {
    logger.warn(s"Failed to delete draft notification ${draftId.value}: $error")
    error match {
      case DeleteDraftNotificationError.Forbidden =>
        // Redirect to ERR1.0 page
        Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
      case DeleteDraftNotificationError.NotFound =>
        // Show ERR3.0 View
        Future.successful(Ok(pageNotFoundView(appConfig.technicalSupportUrl)))
      case _ =>
        // Redirect to ERR2.0 page
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

}

object DeleteCurrentNotificationController {

  def guardPredicate()(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true)

}
