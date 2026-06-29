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
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import models.{NormalMode, NovaUserType}
import models.requests.DataRequest
import navigation.Navigator
import pages.{AboutYourDetailsPage, NotificationTaskListPage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.AboutYourDetailsView

import scala.concurrent.{ExecutionContext, Future}

class AboutYourDetailsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  navigator: Navigator,
  actions: Actions,
  sessionRepository: SessionRepository,
  view: AboutYourDetailsView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val guardPredicate: DataRequest[?] => Boolean = request =>
    IsDraftIdDefined(request.userAnswers) && request.userAnswers.get(NotificationTaskListPage).isDefined

  def onPageLoad: Action[AnyContent] = actions.vatTraderAuthAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
    Ok(view())
  }

  def onSubmit: Action[AnyContent] = actions.vatTraderAuthAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(AboutYourDetailsPage, true))
      _              <- sessionRepository.set(updatedAnswers)
    } yield Redirect(
      navigator.nextPage(AboutYourDetailsPage, NormalMode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
    )
  }
}
