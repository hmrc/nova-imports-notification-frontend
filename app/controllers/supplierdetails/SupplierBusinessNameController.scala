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

package controllers.supplierdetails

import controllers.BaseController
import controllers.actions.*
import controllers.utils.{IsDraftIdDefined, IsSupplierNumberInSession}
import forms.SupplierBusinessNameFormProvider
import models.requests.DataRequest

import javax.inject.Inject
import models.{BusinessOrPrivateIndividual, Mode, NovaUserType, SupplierNumber}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplierdetails.{SupplierBusinessNamePage, SupplierBusinessOrIndividualPage}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.SupplierBusinessNameView

import scala.concurrent.{ExecutionContext, Future}

class SupplierBusinessNameController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: SupplierBusinessNameFormProvider,
  view: SupplierBusinessNameView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import SupplierBusinessNameController.*

  val form: Form[String] = formProvider()

  def onPageLoad(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierNumber)) { implicit request =>
      Ok(view(form.withDefault(request.userAnswers.get(SupplierBusinessNamePage)), supplierNumber, mode))
    }

  def onSubmit(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierNumber)).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, supplierNumber, mode))),
          supplierBusinessName =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(SupplierBusinessNamePage, supplierBusinessName))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(SupplierBusinessNamePage, mode, updatedAnswers, NovaUserType.fromRequest)
            )
        )
    }
}

object SupplierBusinessNameController {

  def guardPredicate(supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      request.userAnswers.get(SupplierBusinessOrIndividualPage).contains(BusinessOrPrivateIndividual.Business) &&
      IsSupplierNumberInSession(request.userAnswers, supplierNumber)
}
