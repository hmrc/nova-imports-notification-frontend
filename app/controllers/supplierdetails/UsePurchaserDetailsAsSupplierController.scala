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

import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import controllers.BaseController
import forms.UsePurchaserDetailsAsSupplierFormProvider
import models.requests.DataRequest
import models.{AddVehicleDetails, Mode, NovaUserType, PurchaserOrOnBehalf, SupplierNumber}
import navigation.Navigator
import pages.sections.initialquestions.{NotifyingAsPurchaserPage, VehicleFromEuPage}
import pages.sections.supplierdetails.UsePurchaserDetailsAsSupplierPage
import pages.sections.vehicledetails.AddVehicleDetailsPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.SupplierService
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
  supplierService: SupplierService,
  view: UsePurchaserDetailsAsSupplierView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import UsePurchaserDetailsAsSupplierController.*

  val form: Form[Boolean] = formProvider()

  private def authenticate(supplierNumber: SupplierNumber) =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber))

  def onPageLoad(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] = authenticate(supplierNumber) { implicit request =>
    Ok(
      view(
        form.withDefault(request.userAnswers.get(UsePurchaserDetailsAsSupplierPage(supplierNumber))),
        supplierNumber,
        mode,
        SupplierPurchaserDetailsSummary.fromSession(request.userAnswers)
      )
    )
  }

  def onSubmit(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] = authenticate(supplierNumber).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors, supplierNumber, mode, SupplierPurchaserDetailsSummary.fromSession(request.userAnswers)))
          ),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UsePurchaserDetailsAsSupplierPage(supplierNumber), value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(
              UsePurchaserDetailsAsSupplierPage(supplierNumber),
              mode,
              updatedAnswers,
              NovaUserType.from(request.affinityGroup, request.enrolments)
            )
          )
      )
  }
}

object UsePurchaserDetailsAsSupplierController {

  // The complement of UsePersonalDetailsAsSupplierController's guard: this page is for users who
  // bought on behalf of the purchaser (IQ3 = OnBehalfOfPurchaser), or an agent without a selected
  // client, and are not a VAT-registered organisation.
  def guardPredicate(supplierService: SupplierService, supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean = {
    val answers = request.userAnswers
    IsDraftIdDefined(answers) &&
    answers.get(AddVehicleDetailsPage).contains(AddVehicleDetails.BySupplier) &&
    answers.get(VehicleFromEuPage).contains(true) &&
    supplierService.numberExists(answers, supplierNumber) &&
    !request.userContext.isVatRegisteredOrganisation &&
    (answers.get(NotifyingAsPurchaserPage).contains(PurchaserOrOnBehalf.OnBehalfOfPurchaser) ||
      request.userContext.isAgentWithoutClient)
  }
}
