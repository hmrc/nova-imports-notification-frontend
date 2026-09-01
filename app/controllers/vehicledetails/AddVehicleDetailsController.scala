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
import models.{AddVehicleDetails, Mode, NormalMode, NovaUserType, PurchaserOrOnBehalf, SupplierNumber, UserAnswers}
import navigation.Navigator
import pages.sections.initialquestions.{NotifyingAsPurchaserPage, VehicleFromEuPage}
import pages.sections.vehicledetails.AddVehicleDetailsPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.SupplierService
import views.html.{AddVehicleDetailsBySupplierOnlyView, AddVehicleDetailsView}

import scala.concurrent.{ExecutionContext, Future}

class AddVehicleDetailsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: AddVehicleDetailsFormProvider,
  supplierService: SupplierService,
  view: AddVehicleDetailsView,
  bySupplierOnlyView: AddVehicleDetailsBySupplierOnlyView,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController {

  import AddVehicleDetailsController.*

  val form: Form[AddVehicleDetails] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
    if (isAgentOrVatRegOrg)
      Ok(view(form.withDefault(request.userAnswers.get(AddVehicleDetailsPage)), mode, appConfig.multipleVehiclesSpreadsheetsUrl))
    else
      Ok(bySupplierOnlyView(mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
    if (isAgentOrVatRegOrg)
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, appConfig.multipleVehiclesSpreadsheetsUrl))),
          {
            // choosing to add by supplier sets up a new supplier collection in the session ready for next question
            case AddVehicleDetails.BySupplier => addSupplierAndRedirect()
            case value                        =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(AddVehicleDetailsPage, value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(AddVehicleDetailsPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
              )
          }
        )
    else
      // the by-supplier-only view has no form, adding by supplier is the only option available
      addSupplierAndRedirect()
  }

  // VAT-registered organisations and agents acting for a selected client can also upload a spreadsheet;
  // everyone else only ever adds vehicles by supplier, so they don't need to be asked
  private def isAgentOrVatRegOrg(implicit request: DataRequest[?]): Boolean =
    request.userContext.isVatRegisteredOrganisation || request.userContext.isAgentWithClient

  private def addSupplierAndRedirect()(implicit request: DataRequest[?]): Future[Result] =
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(AddVehicleDetailsPage, AddVehicleDetails.BySupplier))
      supplierNumber <- supplierService.add(updatedAnswers)
    } yield redirectToSupplierDetails(supplierNumber, updatedAnswers)

  // non-VAT-registered users who bought on behalf of the purchaser (or an agent without a
  // selected client) supply the purchaser's details as the supplier; everyone else supplies their own
  private def redirectToSupplierDetails(supplierNumber: SupplierNumber, updatedAnswers: UserAnswers)(implicit
    request: DataRequest[?]
  ): Result =
    if (
      !request.userContext.isVatRegisteredOrganisation &&
      (updatedAnswers.get(NotifyingAsPurchaserPage).contains(PurchaserOrOnBehalf.OnBehalfOfPurchaser) ||
        request.userContext.isAgentWithoutClient)
    )
      Redirect(supplierdetails.routes.UsePurchaserDetailsAsSupplierController.onPageLoad(supplierNumber, NormalMode))
    else
      Redirect(supplierdetails.routes.UsePersonalDetailsAsSupplierController.onPageLoad(supplierNumber, NormalMode))
}

object AddVehicleDetailsController {

  // Allow user access if IQ1.0 = Yes. User types 7 & 8 (HMRC-NOVRN-AGNT) are rejected by StandardIdentifierAction.
  def guardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true)
}
