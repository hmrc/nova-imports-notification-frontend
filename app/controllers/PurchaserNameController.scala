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
import forms.PurchaserNameFormProvider
import models.{CheckMode, Mode, NovaUserType, PurchaserBusinessOrIndividual, PurchaserOrOnBehalf}
import models.requests.DataRequest
import navigation.Navigator
import pages.sections.initialquestions.{PurchaserBusinessOrIndividualPage, PurchaserOrOnBehalfPage}
import pages.sections.purchaserDetails.PurchaserNamePage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.PurchaserNameView

import scala.concurrent.{ExecutionContext, Future}

class PurchaserNameController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: PurchaserNameFormProvider,
  view: PurchaserNameView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  private def guardPredicate(mode: Mode): DataRequest[?] => Boolean = request => {
    val answers = request.userAnswers

    IsDraftIdDefined(answers) && !request.userContext.isVatRegisteredOrganisation && {
      if (mode == CheckMode) {
        answers.get(PurchaserNamePage).isDefined && purchaserNameRequiredFor(request)
      } else {
        purchaserNameRequiredFor(request)
      }
    }
  }

  private def purchaserNameRequiredFor(request: DataRequest[?]): Boolean =
    request.userAnswers.get(PurchaserOrOnBehalfPage) match {
      case Some(PurchaserOrOnBehalf.Purchaser)           => true
      case Some(PurchaserOrOnBehalf.OnBehalfOfPurchaser) =>
        request.userAnswers
          .get(PurchaserBusinessOrIndividualPage)
          .contains(PurchaserBusinessOrIndividual.NonVatRegisteredPrivateIndividual)
      case _                                             => false
    }

  def onPageLoad(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate(mode)) { implicit request =>
    Ok(view(form.withDefault(request.userAnswers.get(PurchaserNamePage)), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate(mode)).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        purchaserName =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PurchaserNamePage, purchaserName))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(PurchaserNamePage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
          )
      )
  }
}
