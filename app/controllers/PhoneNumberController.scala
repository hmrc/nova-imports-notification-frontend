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
import forms.PhoneNumberFormProvider
import models.requests.DataRequest
import models.{Mode, NovaUserType, PurchaserOrOnBehalf}
import navigation.Navigator
import pages.*
import pages.sections.initialquestions.{PurchaserBusinessOrIndividualPage, PurchaserOrOnBehalfPage, VehicleBusinessUsePage}
import pages.sections.notifierDetails.PhoneNumberPage
import play.api.data.Form
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

  val form: Form[String] = formProvider()

  // Checks the previous page per user type; earlier pages cascade via their own guards.
  private val guardPredicate: DataRequest[?] => Boolean = request => {
    val ua  = request.userAnswers
    val ctx = request.userContext
    ctx.userType match {
      case NovaUserType.VatRegisteredOrganisation =>
        ua.get(VehicleBusinessUsePage).isDefined
      case NovaUserType.Agent if ctx.selectedClient.isDefined =>
        ua.get(AgentClientVehicleBusinessUsePage).isDefined
      case _ =>
        ua.get(PurchaserOrOnBehalfPage) match {
          case Some(PurchaserOrOnBehalf.Purchaser)           => true
          case Some(PurchaserOrOnBehalf.OnBehalfOfPurchaser) =>
            ua.get(PurchaserBusinessOrIndividualPage).isDefined
          case None => false
        }
    }
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
    Ok(view(form.withDefault(request.userAnswers.get(PhoneNumberPage)), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
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
