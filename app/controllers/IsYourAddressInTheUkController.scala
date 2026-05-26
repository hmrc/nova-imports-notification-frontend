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

import controllers.actions.*
import forms.IsYourAddressInTheUkFormProvider
import javax.inject.Inject
import models.{Mode, NovaUserType}
import models.requests.DataRequest
import navigation.Navigator
import pages.IsYourAddressInTheUkPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.IsYourAddressInTheUkView

import scala.concurrent.{ExecutionContext, Future}

class IsYourAddressInTheUkController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: IsYourAddressInTheUkFormProvider,
  view: IsYourAddressInTheUkView
)(implicit ec: ExecutionContext)
    extends BaseController {

  val form: Form[Boolean] = formProvider()

  private val dataGuard: DataRequest[?] => Boolean = !_.userContext.isAgent

  def onPageLoad(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(dataGuard) { implicit request =>
    Ok(view(form.withDefault(request.userAnswers.get(IsYourAddressInTheUkPage)), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(dataGuard).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(IsYourAddressInTheUkPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(IsYourAddressInTheUkPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
          )
      )
  }
}
