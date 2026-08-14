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
import forms.UsePurchaserDetailsAsSupplierFormProvider
import models.requests.DataRequest
import models.{AddVehicleDetails, Mode, NovaUserType, PurchaserOrOnBehalf}
import navigation.Navigator
import pages.AddVehicleDetailsPage
import pages.sections.initialquestions.{PurchaserOrOnBehalfPage, VehicleFromEuPage}
import pages.sections.supplierDetails.UsePurchaserDetailsAsSupplierPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import viewmodels.checkAnswers.SupplierPurchaserDetailsSummary
import views.html.UsePurchaserDetailsAsSupplierView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UsePurchaserDetailsAsSupplierController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: UsePurchaserDetailsAsSupplierFormProvider,
  view: UsePurchaserDetailsAsSupplierView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import UsePurchaserDetailsAsSupplierController.*

  val form: Form[Boolean] = formProvider()

  private val authenticate = actions.authAndGetDataWithUserTypeGuard(guardPredicate)

  def onPageLoad(mode: Mode): Action[AnyContent] = authenticate { implicit request =>
    Ok(
      view(
        form.withDefault(request.userAnswers.get(UsePurchaserDetailsAsSupplierPage)),
        mode,
        SupplierPurchaserDetailsSummary.fromSession(request.userAnswers)
      )
    )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = authenticate.async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors, mode, SupplierPurchaserDetailsSummary.fromSession(request.userAnswers)))
          ),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UsePurchaserDetailsAsSupplierPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(UsePurchaserDetailsAsSupplierPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
          )
      )
  }
}

object UsePurchaserDetailsAsSupplierController {

  // The complement of UsePersonalDetailsAsSupplierController's guard: this page is for users who
  // bought on behalf of the purchaser (IQ3 = OnBehalfOfPurchaser), or an agent without a selected
  // client, and are not a VAT-registered organisation.
  def guardPredicate(request: DataRequest[?]): Boolean = {
    val answers = request.userAnswers
    IsDraftIdDefined(answers) &&
    answers.get(AddVehicleDetailsPage).contains(AddVehicleDetails.BySupplier) &&
    answers.get(VehicleFromEuPage).contains(true) &&
    !request.userContext.isVatRegisteredOrganisation &&
    (answers.get(PurchaserOrOnBehalfPage).contains(PurchaserOrOnBehalf.OnBehalfOfPurchaser) ||
      request.userContext.isAgentWithoutClient)
  }
}
