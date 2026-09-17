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
import forms.SupplierBusinessOrIndividualFormProvider
import models.requests.DataRequest
import models.{BusinessOrPrivateIndividual, CheckMode, Mode, SupplierNumber, UserAnswers}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplierdetails.SupplierBusinessOrIndividualPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.SupplierService
import views.html.SupplierBusinessOrIndividualView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SupplierBusinessOrIndividualController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: SupplierBusinessOrIndividualFormProvider,
  supplierService: SupplierService,
  view: SupplierBusinessOrIndividualView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import SupplierBusinessOrIndividualController.*

  val form: Form[BusinessOrPrivateIndividual] = formProvider()

  def onPageLoad(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)) { implicit request =>
      Ok(view(form.withDefault(request.userAnswers.get(SupplierBusinessOrIndividualPage(supplierNumber))), supplierNumber, mode))
    }

  def onSubmit(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, supplierNumber, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(saveSessionDataOnAnswerChange(value, request.userAnswers, supplierNumber, mode))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              value match {
                case BusinessOrPrivateIndividual.Business =>
                  controllers.supplierdetails.routes.SupplierBusinessNameController.onPageLoad(supplierNumber, CheckMode)
                case BusinessOrPrivateIndividual.PrivateIndividual =>
                  controllers.supplierdetails.routes.SupplierNameController.onPageLoad(supplierNumber, CheckMode)
                case _ =>
                  controllers.routes.JourneyRecoveryController.onPageLoad()
              }
            )
        )
    }
}

object SupplierBusinessOrIndividualController {

  private def saveSessionDataOnAnswerChange(
    businessOrPrivateIndividual: BusinessOrPrivateIndividual,
    userAnswers: UserAnswers,
    supplierNumber: SupplierNumber,
    mode: Mode
  ): Try[UserAnswers] = {
    // Do not save answer if in Check Mode and answer is yes.
    // We will save in the next screen before returning to CYA screen to prevent issues with back navigation.
    if (mode.equals(CheckMode)) {
      Try(userAnswers)
    } else {
      userAnswers.set(SupplierBusinessOrIndividualPage(supplierNumber), businessOrPrivateIndividual)
    }
  }

  // The supplier number in the URL must be one of the suppliers the user has in session
  def guardPredicate(supplierService: SupplierService, supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      supplierService.numberExists(request.userAnswers, supplierNumber)
}
