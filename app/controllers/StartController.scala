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

import connectors.NovaImportsBackendConnector
import controllers.actions.IdentifierAction
import models.UserAnswers
import pages.DraftIdPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class StartController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  @Named("standard") identify: IdentifierAction,
  sessionRepository: SessionRepository,
  backendConnector: NovaImportsBackendConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController {

  def start(): Action[AnyContent] = identify.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    Future successful Redirect(routes.LandingPageController.onPageLoad())
  }
}
