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

import config.FrontendAppConfig
import controllers.actions.*
import controllers.utils.{IsDraftIdDefined, IsSupplierNumberInSession}
import forms.SupplierVatRegistrationDetailsFormProvider
import models.requests.DataRequest
import models.{Mode, NovaUserType, SupplierNumber, VatNumberDetails}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplierDetails.{IsSupplierVatRegisteredPage, SupplierVatRegistrationNumberPage}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.SupplierVatRegistrationDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SupplierVatRegistrationDetailsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  appConfig: FrontendAppConfig,
  formProvider: SupplierVatRegistrationDetailsFormProvider,
  view: SupplierVatRegistrationDetailsView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import SupplierVatRegistrationDetailsController.*

  val form: Form[VatNumberDetails] = formProvider(appConfig.vrnValidationList)

  def onPageLoad(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierNumber)) { implicit request =>
      Ok(view(appConfig.vrnValidationList, form.withDefault(request.userAnswers.get(SupplierVatRegistrationNumberPage)), supplierNumber, mode))
    }

  def onSubmit(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierNumber)).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(appConfig.vrnValidationList, formWithErrors, supplierNumber, mode))),
          supplierVatNumberDetails =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(SupplierVatRegistrationNumberPage, supplierVatNumberDetails))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator
                .nextPage(SupplierVatRegistrationNumberPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
            )
        )
    }

}

object SupplierVatRegistrationDetailsController {
  def guardPredicate(supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      request.userAnswers.get(IsSupplierVatRegisteredPage).contains(true) &&
      IsSupplierNumberInSession(request.userAnswers, supplierNumber)
}