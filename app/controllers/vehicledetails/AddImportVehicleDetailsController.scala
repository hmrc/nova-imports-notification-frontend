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
import controllers.utils.IsDraftIdDefined
import forms.AddImportVehicleDetailsFormProvider
import models.requests.DataRequest

import javax.inject.Inject
import models.{AddImportVehicleDetails, Mode, NovaUserType}
import navigation.Navigator
import pages.sections.vehicledetails.AddImportVehicleDetailsPage
import pages.sections.initialquestions.VehicleFromEuPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.AddImportVehicleDetailsView

import scala.concurrent.{ExecutionContext, Future}

class AddImportVehicleDetailsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: AddImportVehicleDetailsFormProvider,
  view: AddImportVehicleDetailsView,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController {

  import AddImportVehicleDetailsController.*

  val form: Form[AddImportVehicleDetails] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
    Ok(view(form.withDefault(request.userAnswers.get(AddImportVehicleDetailsPage)), mode, appConfig.multipleVehiclesSpreadsheetsUrl))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, appConfig.multipleVehiclesSpreadsheetsUrl))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(AddImportVehicleDetailsPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(AddImportVehicleDetailsPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
          )
      )
  }
}

object AddImportVehicleDetailsController {

  // AVD1.1 - Vehicles brought from outside the UK/EU (import journey).
  // Access requires IQ1.0 = No and a VAT-registered organisation or an agent (user types 3/4/5/6).
  // Private individuals and non-VAT organisations (types 1/2) are rejected here; OGD agents (types 7/8,
  // HMRC-NOVRN-AGNT) are rejected earlier by StandardIdentifierAction.
  def guardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(false) &&
      isVatOrganisationOrAgent(request)

  private def isVatOrganisationOrAgent(request: DataRequest[?]): Boolean =
    request.userContext.userType == NovaUserType.VatRegisteredOrganisation || request.userContext.isAgent
}
