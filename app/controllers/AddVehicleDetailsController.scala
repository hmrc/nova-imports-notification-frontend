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
import forms.AddVehicleDetailsFormProvider
import models.requests.DataRequest

import javax.inject.Inject
import models.{AddVehicleDetails, Mode, NovaUserType}
import navigation.Navigator
import pages.AddVehicleDetailsPage
import pages.sections.initialquestions.VehicleFromEuPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.AddVehicleDetailsView

import scala.concurrent.{ExecutionContext, Future}

class AddVehicleDetailsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: AddVehicleDetailsFormProvider,
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
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(AddVehicleDetailsPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(AddVehicleDetailsPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
          )
      )
  }
}

object AddVehicleDetailsController {

  // Allow user types 1-6 with IQ1.0 = Yes. User types 7 & 8 (HMRC-NOVRN-AGNT) are
  // already rejected by StandardIdentifierAction. Reject user type 9 here:
  // organisation with HMCE-VATDEC-ORG present but not Activated (de-registered).
  def guardPredicate(request: DataRequest[?]): Boolean =
    !isDeregisteredOrganisation(request) && request.userAnswers.get(VehicleFromEuPage).contains(true)

  private def isDeregisteredOrganisation(request: DataRequest[?]): Boolean =
    request.affinityGroup == AffinityGroup.Organisation &&
      request.enrolments.getEnrolment(NovaEnrolments.vatDec).exists(!_.isActivated)
}
