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
import models.{NormalMode, NovaUserType, UserAnswers}
import navigation.Navigator
import pages.{AboutYourDetailsPage, VehicleBusinessUsePage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.AboutYourDetailsView

class AboutYourDetailsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  navigator: Navigator,
  actions: Actions,
  view: AboutYourDetailsView
) extends BaseController {

  private val guardPredicate: UserAnswers => Boolean =
    _.get(VehicleBusinessUsePage).isDefined

  def onPageLoad: Action[AnyContent] = actions.vatTraderAuthAndGetDataWithGuard(guardPredicate) { implicit request =>
    Ok(view())
  }

  def onSubmit: Action[AnyContent] = actions.vatTraderAuthAndGetDataWithGuard(guardPredicate) { implicit request =>
    Redirect(
      navigator.nextPage(AboutYourDetailsPage, NormalMode, request.userAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
    )
  }
}
