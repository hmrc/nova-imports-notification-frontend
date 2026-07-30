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
import controllers.utils.IsDraftIdDefined
import forms.UsePersonalDetailsAsSupplierFormProvider
import models.requests.DataRequest
import models.{AddVehicleDetails, Mode, NovaUserType, PurchaserOrOnBehalf}
import navigation.Navigator
import pages.AddVehicleDetailsPage
import pages.sections.initialquestions.{PurchaserOrOnBehalfPage, VehicleFromEuPage}
import pages.sections.supplierDetails.UsePersonalDetailsAsSupplierPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import viewmodels.checkAnswers.SupplierPersonalDetailsSummary
import views.html.UsePersonalDetailsAsSupplierView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UsePersonalDetailsAsSupplierController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: UsePersonalDetailsAsSupplierFormProvider,
  view: UsePersonalDetailsAsSupplierView,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController {

  import UsePersonalDetailsAsSupplierController.*

  val form: Form[Boolean] = formProvider()

  // The user can reach this page before completing "Add your details"/"Add your address". Any missing
  // personal details render as "Not provided"; the user can continue or choose "No" to add them later.
  private val authenticate = actions.authAndGetDataWithUserTypeGuard(guardPredicate)

  def onPageLoad(mode: Mode): Action[AnyContent] = authenticate { implicit request =>
    Ok(
      view(
        form.withDefault(request.userAnswers.get(UsePersonalDetailsAsSupplierPage)),
        mode,
        SupplierPersonalDetailsSummary.summaryList(request.userAnswers),
        appConfig.vatNotice728Url
      )
    )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = authenticate.async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(
                formWithErrors,
                mode,
                SupplierPersonalDetailsSummary.summaryList(request.userAnswers),
                appConfig.vatNotice728Url
              )
            )
          ),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UsePersonalDetailsAsSupplierPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(UsePersonalDetailsAsSupplierPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
          )
      )
  }
}

object UsePersonalDetailsAsSupplierController {

  // Access is limited to user types 1, 2, 4 & 5: agents (3 & 6) blocked here, OGD agents (7 & 8)
  // rejected upstream; types 1 & 2 also need IQ3 = As the purchaser, else they belong on AVD-S1.1.
  def guardPredicate(request: DataRequest[?]): Boolean = {
    val answers = request.userAnswers
    !request.userContext.isAgent &&
    IsDraftIdDefined(answers) &&
    answers.get(AddVehicleDetailsPage).contains(AddVehicleDetails.BySupplier) &&
    answers.get(VehicleFromEuPage).contains(true) &&
    (request.userContext.isVatRegisteredOrganisation ||
      answers.get(PurchaserOrOnBehalfPage).contains(PurchaserOrOnBehalf.Purchaser))
  }
}
