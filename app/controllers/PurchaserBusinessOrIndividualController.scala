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
import forms.PurchaserBusinessOrIndividualFormProvider

import javax.inject.Inject
import models.{Mode, NovaUserType, PurchaserBusinessOrIndividual, PurchaserOrOnBehalf, UserAnswers}
import navigation.Navigator
import pages.{PurchaserBusinessOrIndividualPage, PurchaserOrOnBehalfPage}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.PurchaserBusinessOrIndividualView

import scala.concurrent.{ExecutionContext, Future}

class PurchaserBusinessOrIndividualController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: PurchaserBusinessOrIndividualFormProvider,
  view: PurchaserBusinessOrIndividualView
)(implicit ec: ExecutionContext)
    extends BaseController {

  val form: Form[PurchaserBusinessOrIndividual] = formProvider()

  private val guardPredicate: UserAnswers => Boolean =
    _.get(PurchaserOrOnBehalfPage).contains(PurchaserOrOnBehalf.OnBehalfOfPurchaser)

  def onPageLoad(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithGuard(guardPredicate) { implicit request =>
    Ok(view(form.withDefault(request.userAnswers.get(PurchaserBusinessOrIndividualPage)), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithGuard(guardPredicate).async { implicit request =>

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PurchaserBusinessOrIndividualPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(PurchaserBusinessOrIndividualPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
          )
      )
  }
}
