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

import javax.inject.Inject
import controllers.actions.*
import forms.AddYourNameFormProvider
import models.{Mode, NovaUserType}
import navigation.Navigator
import pages.AddYourNamePage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.AddYourNameView

import scala.concurrent.{ExecutionContext, Future}

class AddYourNameController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: AddYourNameFormProvider,
  view: AddYourNameView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  // TODO: Replace actions.authAndGetData with a guard predicate once the correct user type
  //  and preceding answer requirements are confirmed and sorted.
  //  like actions.authAndGetDataWithGuard(guardPredicate) checking a prior page is answered - need to do.

  def onPageLoad(mode: Mode): Action[AnyContent] = actions.authAndGetData() { implicit request =>
    Ok(view(form.withDefault(request.userAnswers.get(AddYourNamePage)), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = actions.authAndGetData().async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        addYourName =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(AddYourNamePage, addYourName))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(AddYourNamePage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
          )
      )
  }
}
