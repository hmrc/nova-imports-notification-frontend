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

import connectors.{GetClientListError, NovaImportsBackendConnector, RemoveClientError}
import controllers.BaseController
import controllers.actions.*
import controllers.routes.{JourneyRecoveryController, LandingPageController, UnauthorisedController}
import forms.RemoveClientFormProvider
import models.{ClientListQuery, UserAnswers}
import models.requests.OptionalDataRequest
import pages.ClientToRemovePage
import play.api.Logging
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.RemoveClientView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RemoveClientController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  connector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  formProvider: RemoveClientFormProvider,
  view: RemoveClientView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  val form: Form[Boolean] = formProvider()

  def select(vrn: String): Action[AnyContent] =
    actions.novaAgentAuthAndGetOptionalData().async { implicit request =>
      sessionRepository
        .setPage(request.userAnswers.getOrElse(UserAnswers(request.userId)), ClientToRemovePage, vrn)
        .map(_ => Redirect(routes.RemoveClientController.onPageLoad()))
    }

  def onPageLoad(): Action[AnyContent] =
    actions.novaAgentAuthAndGetOptionalData().async { implicit request =>
      withClientToRemove(vrn => renderPage(vrn, form, Ok(_)))
    }

  def onSubmit(): Action[AnyContent] =
    actions.novaAgentAuthAndGetOptionalData().async { implicit request =>
      withClientToRemove { vrn =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => renderPage(vrn, formWithErrors, BadRequest(_)),
            {
              case true  => removeAndRedirect(vrn)
              case false => Future.successful(Redirect(routes.ViewClientsController.onPageLoad(None, None, 1)))
            }
          )
      }
    }

  private def withClientToRemove(block: String => Future[Result])(implicit request: OptionalDataRequest[AnyContent]): Future[Result] =
    request.userAnswers
      .flatMap(_.get(ClientToRemovePage))
      .fold(Future.successful(Redirect(UnauthorisedController.onPageLoad())))(block)

  private def removeAndRedirect(vrn: String)(implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    connector
      .removeClient(vrn)
      .flatMap {
        case Right(_) =>
          connector.getClientList(ClientListQuery(start = 0, count = 2)).map {
            case Right(list) if !list.clients.exists(_.vatRegistrationNumber != vrn) =>
              Redirect(LandingPageController.deselectClient())
            case Right(_) =>
              Redirect(routes.ViewClientsController.onPageLoad(None, None, 1))
            case Left(GetClientListError.UpstreamError(status, _)) =>
              logger.warn(s"failed to fetch client list after removing a client, backend returned $status")
              Redirect(routes.ViewClientsController.onPageLoad(None, None, 1))
          }
        case Left(RemoveClientError.UpstreamError(status, _)) =>
          logger.warn(s"failed to remove client, backend returned $status")
          Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))
      }
      .recover { case NonFatal(e) =>
        logger.warn("failed to remove client", e)
        Redirect(JourneyRecoveryController.onPageLoad())
      }
  }

  private def renderPage(vrn: String, form: Form[Boolean], status: Html => Result)(implicit
    request: OptionalDataRequest[AnyContent]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    connector
      .getClientList(ClientListQuery(vrn = Some(vrn)))
      .map {
        case Right(list) =>
          list.clients
            .find(_.vatRegistrationNumber == vrn)
            .fold(Redirect(UnauthorisedController.onPageLoad()))(client => status(view(form, client)))
        case Left(GetClientListError.UpstreamError(code, _)) =>
          logger.warn(s"failed to fetch client to remove, backend returned $code")
          Redirect(JourneyRecoveryController.onPageLoad())
      }
      .recover { case NonFatal(e) =>
        logger.warn("failed to fetch client to remove", e)
        Redirect(JourneyRecoveryController.onPageLoad())
      }
  }
}
