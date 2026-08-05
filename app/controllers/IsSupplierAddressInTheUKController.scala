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
import controllers.utils.{IsDraftIdDefined, IsSupplierNumberInSession}
import forms.IsSupplierAddressInTheUkFormProvider
import models.requests.DataRequest
import models.{Mode, NovaUserType, SupplierNumber}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.purchaseraddress.IsPurchaserAddressInTheUkPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.IsSupplierAddressInTheUkView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IsSupplierAddressInTheUKController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: IsSupplierAddressInTheUkFormProvider,
  view: IsSupplierAddressInTheUkView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import IsSupplierAddressInTheUKController.*

  val form: Form[Boolean] = formProvider()

  def onPageLoad(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierNumber)) { implicit request =>
      Ok(view(form.withDefault(request.userAnswers.get(IsPurchaserAddressInTheUkPage)), supplierNumber, mode))
    }

  def onSubmit(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierNumber)).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, supplierNumber, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(IsPurchaserAddressInTheUkPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(IsPurchaserAddressInTheUkPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
            )
        )
    }

}

object IsSupplierAddressInTheUKController {

  // The supplier number in the URL must be one of the suppliers the user has in session
  def guardPredicate(supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean =
    // For user types 1,2,3,4,5,6. Defined draftID = yes and IQ1 = yes
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      IsSupplierNumberInSession(request.userAnswers, supplierNumber)
}
