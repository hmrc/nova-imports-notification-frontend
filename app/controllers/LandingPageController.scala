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
import models.{NotificationSummary, NovaUserType}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.LandingPagePrivateView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class LandingPageController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  @Named("standard") identify: IdentifierAction,
  backendConnector: NovaImportsBackendConnector,
  privateView: LandingPagePrivateView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = identify.async { implicit request =>
    NovaUserType.from(request.affinityGroup, request.enrolments) match {
      case NovaUserType.PrivateIndividual =>
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

        backendConnector.getNotificationSummary().map { result =>
          val (traderName, hasDrafts) = result match {
            case Right(summary: NotificationSummary.IndividualOrOrganisation) =>
              (Some(summary.traderName), summary.hasDraftNotifications)
            case Right(_) =>
              (None, false)
            case Left(error) =>
              logger.warn(s"failed to fetch notification summary; defaulting hasDraftNotifications=false: $error")
              (None, false)
          }
          Ok(privateView(traderName = traderName, hasDraftNotifications = hasDrafts))
        }

      case _ =>
        // TODO: route Organisation / Agent users to LP2.0 / LP3.0 / LP3.1 when those stories land.
        Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
    }
  }
}
