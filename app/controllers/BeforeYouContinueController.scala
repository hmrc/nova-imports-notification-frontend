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
import models.{NormalMode, UserAnswers}
import models.NovaUserType
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import pages.{AgentSelectedClientPage, DraftIdPage}
import views.html.{BeforeYouContinueOrganisationView, BeforeYouContinueView}
import scala.concurrent.{ExecutionContext, Future}

class BeforeYouContinueController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  individualView: BeforeYouContinueView,
  organisationView: BeforeYouContinueOrganisationView,
  sessionRepository: SessionRepository,
  backendConnector: NovaImportsBackendConnector,
  actions: Actions
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad: Action[AnyContent] = actions.authAndGetData() { implicit request =>
    val ctx                   = request.userContext
    val showIndividualContent =
      ctx.userType == NovaUserType.PrivateIndividual || ctx.userType == NovaUserType.NonVatOrganisation || ctx.isAgentWithoutClient

    if showIndividualContent then Ok(individualView()) else Ok(organisationView())
  }

  def onSubmit(): Action[AnyContent] = actions.authAndGetData().async { implicit request =>

    val clientVrn = if (request.userContext.isAgentWithClient) request.userAnswers.get(AgentSelectedClientPage).map(_.vrn) else None

    backendConnector.createDraft(clientVrn).flatMap {
      case Right(draftId) =>
        Future
          .fromTry(UserAnswers(request.userId).set(DraftIdPage, draftId))
          .flatMap(sessionRepository.set)
          .map(_ => Redirect(routes.VehicleFromEuController.onPageLoad(NormalMode).url))
      case Left(_) =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
