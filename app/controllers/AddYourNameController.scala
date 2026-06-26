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
import controllers.utils.IsDraftIdDefined
import forms.AddYourNameFormProvider
import models.{BusinessOrPrivateIndividual, CheckMode, Mode, NovaUserType}
import models.requests.DataRequest
import navigation.Navigator
import pages.{AboutYourDetailsPage, AgentClientVehicleBusinessUsePage}
import pages.sections.initialquestions.{BusinessOrPrivatePage, VehicleBusinessUsePage, VehicleFromEuPage}
import pages.sections.notifierDetails.NameDetailsPage
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

  private def guardPredicate(mode: Mode): DataRequest[?] => Boolean = request => {
    val answers = request.userAnswers

    IsDraftIdDefined(answers) && {
      if (mode == CheckMode) {
        answers.get(NameDetailsPage).isDefined && YourDetailsCheckYourAnswersController.guardPredicate(request)
      } else {
        answers.get(AboutYourDetailsPage).isDefined && nameRequiredFor(request)
      }
    }
  }

  private def nameRequiredFor(request: DataRequest[?]): Boolean = {
    val answers = request.userAnswers

    request.userContext match {
      case ctx if ctx.isAgentWithClientNoEnrolments => false
      case ctx if ctx.isVatRegisteredOrganisation   => answers.get(VehicleBusinessUsePage).contains(false)
      case ctx if ctx.isAgentWithClient             => answers.get(AgentClientVehicleBusinessUsePage).contains(false)
      case _                                        =>
        answers.get(VehicleFromEuPage).contains(true) &&
        answers.get(BusinessOrPrivatePage).contains(BusinessOrPrivateIndividual.PrivateIndividual)
    }
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate(mode)) { implicit request =>
    Ok(view(form.withDefault(request.userAnswers.get(NameDetailsPage)), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate(mode)).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        addYourName =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(NameDetailsPage, addYourName))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(NameDetailsPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
          )
      )
  }
}
