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

package controllers.auth

import config.FrontendAppConfig
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  config: FrontendAppConfig,
  sessionRepository: SessionRepository,
  override val authConnector: AuthConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with AuthorisedFunctions {

  def signOut(): Action[AnyContent] = signOutAndRedirect(config.exitSurveyUrl)

  def signOutNoSurvey(): Action[AnyContent] = signOutAndRedirect(routes.SignedOutController.onPageLoad().url)

  private def signOutAndRedirect(continueUrl: String): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val redirect                   = Redirect(config.signOutUrl, Map("continue" -> Seq(continueUrl)))
    authorised()
      .retrieve(Retrievals.internalId) {
        case Some(internalId) => sessionRepository.clear(internalId).map(_ => redirect)
        case None             => Future.successful(redirect)
      }
      .recover { case _: AuthorisationException => redirect }
  }
}
