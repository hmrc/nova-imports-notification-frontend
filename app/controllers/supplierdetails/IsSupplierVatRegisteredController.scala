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
import controllers.utils.IsDraftIdDefined
import forms.IsSupplierVatRegisteredFormProvider
import models.requests.DataRequest
import models.{Mode, NovaUserType, SupplierNumber, UserAnswers}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplierdetails.{IsSupplierVatRegisteredPage, SupplierVatRegistrationNumberPage}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.SupplierService
import views.html.IsSupplierVatRegisteredView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class IsSupplierVatRegisteredController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: IsSupplierVatRegisteredFormProvider,
  supplierService: SupplierService,
  view: IsSupplierVatRegisteredView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import IsSupplierVatRegisteredController.*

  val form: Form[Boolean] = formProvider()

  def onPageLoad(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)) { implicit request =>
      Ok(view(form.withDefault(request.userAnswers.get(IsSupplierVatRegisteredPage(supplierNumber))), supplierNumber, mode))
    }

  def onSubmit(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, supplierNumber, mode))),
          value =>
            for {
              updatedAnswers  <- Future.fromTry(request.userAnswers.set(IsSupplierVatRegisteredPage(supplierNumber), value))
              updatedAnswers2 <- Future.fromTry(clearSessionDataOnAnswerChange(value, updatedAnswers, supplierNumber))
              _               <- sessionRepository.set(updatedAnswers2)
            } yield Redirect(
              navigator.nextPage(
                IsSupplierVatRegisteredPage(supplierNumber),
                mode,
                updatedAnswers2,
                NovaUserType.from(request.affinityGroup, request.enrolments)
              )
            )
        )
    }

}

object IsSupplierVatRegisteredController {

  private def clearSessionDataOnAnswerChange(
    isVatRegisteredValue: Boolean,
    userAnswers: UserAnswers,
    supplierNumber: SupplierNumber
  ): Try[UserAnswers] = {
    if (!isVatRegisteredValue) {
      // Clear vat registration details
      userAnswers.remove(SupplierVatRegistrationNumberPage(supplierNumber))
    } else {
      Try(userAnswers)
    }
  }

  // The supplier number in the URL must be one of the suppliers the user has in session
  def guardPredicate(supplierService: SupplierService, supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      supplierService.numberExists(request.userAnswers, supplierNumber)
}
