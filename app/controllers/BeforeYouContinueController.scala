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

package controllers

import com.google.inject.Inject
import connectors.NovaImportsBackendConnector
import controllers.actions.*
import models.{NormalMode, UserAnswers, UserContext}
import models.NovaUserType
import pages.sections.introduction.{AmendSubmittedNotificationPage, IntroductionAcknowledgePage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import pages.{AgentSelectedClientPage, DraftIdPage}
import views.html.{BeforeYouContinueOrganisationView, BeforeYouContinueView}
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.Result
import scala.util.{Success, Try}

class BeforeYouContinueController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  individualView: BeforeYouContinueView,
  organisationView: BeforeYouContinueOrganisationView,
  sessionRepository: SessionRepository,
  backendConnector: NovaImportsBackendConnector,
  actions: Actions
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad: Action[AnyContent] = actions.authAndGetOptionalData().async { implicit request =>
    renderPage(isAmendment = false)
  }

  def onPageLoadAmend: Action[AnyContent] = actions.authAndGetOptionalData().async { implicit request =>
    renderPage(isAmendment = true)
  }

  private def renderPage(isAmendment: Boolean)(implicit request: models.requests.OptionalDataRequest[AnyContent]): Future[Result] = {
    val existingAnswers       = request.userAnswers.getOrElse(UserAnswers(request.userId))
    val ctx                   = UserContext.from(request.affinityGroup, request.enrolments, existingAnswers)
    val showIndividualContent =
      ctx.userType == NovaUserType.PrivateIndividual || ctx.userType == NovaUserType.NonVatOrganisation || ctx.isAgentWithoutClient

    sessionRepository.setPage(existingAnswers, AmendSubmittedNotificationPage, isAmendment).map { _ =>
      if showIndividualContent then Ok(individualView()) else Ok(organisationView())
    }
  }

  def onSubmit(): Action[AnyContent] = actions.authAndGetOptionalData().async { implicit request =>
    val existingAnswers                    = request.userAnswers.getOrElse(UserAnswers(request.userId))
    val ctx                                = UserContext.from(request.affinityGroup, request.enrolments, existingAnswers)
    val selectedClient                     = if (ctx.isAgentWithClient) existingAnswers.get(AgentSelectedClientPage) else None
    val freshUserAnswers: Try[UserAnswers] = selectedClient match {
      case Some(client) => UserAnswers(request.userId).set(AgentSelectedClientPage, client)
      case None         => Success(UserAnswers(request.userId))
    }

    backendConnector.createDraft(selectedClient.map(_.vrn)).flatMap {
      case Right(draftId) =>
        for {
          answers <- Future.fromTry(freshUserAnswers)
          _       <- sessionRepository.setPage(answers, DraftIdPage, draftId)
        } yield Redirect(routes.VehicleFromEuController.onPageLoad(NormalMode).url)
      case Left(_) =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
    for {
      _ <- sessionRepository.setPage(existingAnswers, IntroductionAcknowledgePage, true)
    } yield Redirect(routes.VehicleFromEuController.onPageLoad(NormalMode).url)

  }
}
