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
import controllers.utils.IsDraftIdDefined
import forms.EmailAddressFormProvider
import javax.inject.Inject
import models.{CheckMode, Mode, NovaUserType}
import models.requests.DataRequest
import navigation.Navigator
import pages.AgentClientVehicleBusinessUsePage
import pages.sections.notifierDetails.{EmailAddressPage, PhoneNumberPage}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.EmailAddressView

import scala.concurrent.{ExecutionContext, Future}

class EmailAddressController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: EmailAddressFormProvider,
  view: EmailAddressView
)(implicit ec: ExecutionContext)
    extends BaseController {

  val form: Form[String] = formProvider()

  private def guardPredicate(mode: Mode): DataRequest[?] => Boolean = request => {
    val answers = request.userAnswers

    IsDraftIdDefined(answers) && {
      if (mode == CheckMode) {
        YourDetailsCheckYourAnswersController.guardPredicate(request)
      } else {
        request.userContext match {
          case ctx if ctx.isAgentWithClientNoEnrolments =>
            answers.get(AgentClientVehicleBusinessUsePage).isDefined && answers.get(PhoneNumberPage).isDefined
          case _ =>
            answers.get(PhoneNumberPage).isDefined
        }
      }
    }
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate(mode)) { implicit request =>
    Ok(view(form.withDefault(request.userAnswers.get(EmailAddressPage)), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate(mode)).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(EmailAddressPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(EmailAddressPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
          )
      )
  }
}
