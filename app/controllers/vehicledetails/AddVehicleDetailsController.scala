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

package controllers.vehicledetails

import config.FrontendAppConfig
import controllers.BaseController
import controllers.actions.*
import controllers.supplierdetails
import controllers.utils.IsDraftIdDefined
import forms.AddVehicleDetailsFormProvider
import models.requests.DataRequest

import javax.inject.Inject
import models.{AddVehicleDetails, Mode, NormalMode, NovaUserType, PurchaserOrOnBehalf}
import navigation.Navigator
import pages.sections.initialquestions.{NotifyingAsPurchaserPage, VehicleFromEuPage}
import pages.sections.vehicledetails.AddVehicleDetailsPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.SupplierService
import views.html.AddVehicleDetailsView

import scala.concurrent.{ExecutionContext, Future}

class AddVehicleDetailsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: AddVehicleDetailsFormProvider,
  supplierService: SupplierService,
  view: AddVehicleDetailsView,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController {

  import AddVehicleDetailsController.*

  val form: Form[AddVehicleDetails] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
    Ok(view(form.withDefault(request.userAnswers.get(AddVehicleDetailsPage)), mode, appConfig.multipleVehiclesSpreadsheetsUrl))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, appConfig.multipleVehiclesSpreadsheetsUrl))),
        {
          // choosing to add by supplier sets up a new supplier collection in the session ready for next question
          case AddVehicleDetails.BySupplier =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(AddVehicleDetailsPage, AddVehicleDetails.BySupplier))
              supplierNumber <- supplierService.add(updatedAnswers)
            } yield
              // non-VAT-registered users who bought on behalf of the purchaser (or an agent without a
              // selected client) supply the purchaser's details as the supplier; everyone else supplies their own
              if (
                !request.userContext.isVatRegisteredOrganisation &&
                (updatedAnswers.get(NotifyingAsPurchaserPage).contains(PurchaserOrOnBehalf.OnBehalfOfPurchaser) ||
                  request.userContext.isAgentWithoutClient)
              )
                Redirect(supplierdetails.routes.UsePurchaserDetailsAsSupplierController.onPageLoad(supplierNumber, NormalMode))
              else
                Redirect(supplierdetails.routes.UsePersonalDetailsAsSupplierController.onPageLoad(supplierNumber, NormalMode))

          case value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(AddVehicleDetailsPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(AddVehicleDetailsPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
            )
        }
      )
  }
}

object AddVehicleDetailsController {

  // Allow user access if IQ1.0 = Yes. User types 7 & 8 (HMRC-NOVRN-AGNT) are rejected by StandardIdentifierAction.
  def guardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true)
}
