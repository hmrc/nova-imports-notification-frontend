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
import forms.PhoneNumberFormProvider
import models.requests.DataRequest
import models.{BusinessOrPrivateIndividual, CheckMode, ContactNumbers, Mode, NovaUserType}
import navigation.Navigator
import pages.{AboutYourDetailsPage, AgentClientVehicleBusinessUsePage}
import pages.sections.initialquestions.{BusinessOrPrivatePage, VehicleBusinessUsePage, VehicleFromEuPage}
import pages.sections.notifierDetails.{NameDetailsPage, PhoneNumberPage}
import play.api.data.{Form, FormError}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.PhoneNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PhoneNumberController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: PhoneNumberFormProvider,
  view: PhoneNumberView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import PhoneNumberController.*

  val form: Form[ContactNumbers] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate(mode)) { implicit request =>
    Ok(view(form.withDefault(request.userAnswers.get(PhoneNumberPage)), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate(mode)).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(addRequiredErrorsPerField(formWithErrors), mode))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PhoneNumberPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(PhoneNumberPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
          )
      )
  }
}

object PhoneNumberController {

  def addRequiredErrorsPerField(form: Form[ContactNumbers]): Form[ContactNumbers] =
    if (form.hasGlobalErrors) {
      form.copy(errors =
        form.errors ++ Seq(
          FormError("phoneNumber", "phoneNumber.error.phoneRequired"),
          FormError("mobileNumber", "phoneNumber.error.mobileRequired")
        )
      )
    } else {
      form
    }

  def guardPredicate(mode: Mode): DataRequest[?] => Boolean = request => {
    val ua = request.userAnswers

    IsDraftIdDefined(ua) && {
      if (mode == CheckMode) {
        YourDetailsCheckYourAnswersController.guardPredicate(request)
      } else {
        ua.get(NameDetailsPage).isDefined || nameNotRequiredFor(request)
      }
    }
  }

  private def nameNotRequiredFor(request: DataRequest[?]): Boolean = {
    val answers = request.userAnswers

    request.userContext match {
      case ctx if ctx.isAgentWithClientNoEnrolments => answers.get(AgentClientVehicleBusinessUsePage).isDefined
      case ctx if ctx.isVatRegisteredOrganisation   =>
        answers.get(AboutYourDetailsPage).contains(true) && answers.get(VehicleBusinessUsePage).contains(true)
      case ctx if ctx.isAgentWithClient => answers.get(AgentClientVehicleBusinessUsePage).contains(true)
      case _                            =>
        answers.get(VehicleFromEuPage).contains(true) &&
        answers.get(BusinessOrPrivatePage).exists(_ != BusinessOrPrivateIndividual.PrivateIndividual)
    }
  }
}
