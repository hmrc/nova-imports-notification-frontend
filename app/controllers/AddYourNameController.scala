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
import models.{BusinessOrPrivateIndividual, Mode, NovaUserType, UserAnswers}
import models.requests.DataRequest
import navigation.Navigator
import pages.*
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.AddYourNameView
import controllers.utils.IsDraftIdDefined
import pages.sections.initialquestions.{BusinessOrPrivatePage, VehicleBusinessUsePage, VehicleFromEuPage}
import pages.sections.notifierDetails.*

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

  import AddYourNameController.*

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
    Ok(view(form.withDefault(request.userAnswers.get(NameDetailsPage)), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
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

object AddYourNameController {

  def guardPredicate(request: DataRequest[?]): Boolean = {
    val answers     = request.userAnswers
    val userContext = request.userContext

    IsDraftIdDefined(answers) && {
      if (userContext.isDeregistered)
        deregisteredOrgAnswersComplete(answers)
      else
        userContext.userType match {
          case NovaUserType.PrivateIndividual | NovaUserType.NonVatOrganisation =>
            standardUserAnswersComplete(answers)
          case NovaUserType.Agent if userContext.isAgentWithoutClient =>
            standardUserAnswersComplete(answers)
          case NovaUserType.VatRegisteredOrganisation =>
            vatRegisteredOrgAnswersComplete(answers)
          case NovaUserType.Agent =>
            agentWithClientAnswersComplete(answers)
        }
    }
  }

  private def deregisteredOrgAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(VehicleFromEuPage).contains(true)

  private def standardUserAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(BusinessOrPrivatePage).contains(BusinessOrPrivateIndividual.PrivateIndividual) &&
      answers.get(VehicleFromEuPage).contains(true)

  private def vatRegisteredOrgAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(VehicleBusinessUsePage).contains(false)

  private def agentWithClientAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(AgentClientVehicleBusinessUsePage).contains(false)
}
