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

import connectors.NovaImportsBackendConnector
import controllers.BaseController
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import controllers.utils.SupplierAlfUtil.initialiseAlfJourney
import forms.SupplierBusinessNameFormProvider
import models.BusinessOrPrivateIndividual.Business
import models.requests.DataRequest

import javax.inject.Inject
import models.{BusinessOrPrivateIndividual, CheckMode, Mode, SupplierNumber, UserAnswers}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplieraddress.SupplierAddressPage
import pages.sections.supplierdetails.{SupplierBusinessNamePage, SupplierBusinessOrIndividualPage}
import play.api.Logging
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{AddressLookupService, SupplierService}
import views.html.SupplierBusinessNameView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SupplierBusinessNameController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  actions: Actions,
  formProvider: SupplierBusinessNameFormProvider,
  supplierService: SupplierService,
  addressLookupService: AddressLookupService,
  backendConnector: NovaImportsBackendConnector,
  view: SupplierBusinessNameView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import SupplierBusinessNameController.*

  val form: Form[String] = formProvider()

  def onPageLoad(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber, mode)) { implicit request =>
      Ok(view(form.withDefault(request.userAnswers.get(SupplierBusinessNamePage(supplierNumber))), supplierNumber, mode))
    }

  def onSubmit(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber, mode)).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, supplierNumber, mode))),
          supplierBusinessName =>
            for {
              updatedAnswers  <- Future.fromTry(request.userAnswers.set(SupplierBusinessNamePage(supplierNumber), supplierBusinessName))
              updatedAnswers2 <- Future.fromTry(saveBusinessOrIndividual(updatedAnswers, supplierNumber, mode))
              _               <- sessionRepository.set(updatedAnswers2)
              result          <- (mode, updatedAnswers2.get(SupplierAddressPage(supplierNumber))) match {
                          case (CheckMode, Some(_)) =>
                            Future.successful(
                              Redirect(controllers.supplierdetails.routes.SupplierDetailsCheckYourAnswersController.onPageLoad(supplierNumber))
                            )
                          case _ => initialiseAlfJourney(backendConnector, addressLookupService, sessionRepository, supplierNumber, updatedAnswers2)
                        }
            } yield result
        )
    }
}

object SupplierBusinessNameController {

  def guardPredicate(supplierService: SupplierService, supplierNumber: SupplierNumber, mode: Mode)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      (mode.equals(CheckMode) || request.userAnswers
        .get(SupplierBusinessOrIndividualPage(supplierNumber))
        .contains(BusinessOrPrivateIndividual.Business)) &&
      supplierService.numberExists(request.userAnswers, supplierNumber)

  private def saveBusinessOrIndividual(userAnswers: UserAnswers, supplierNumber: SupplierNumber, mode: Mode): Try[UserAnswers] = {
    // If in Check Mode and then also save IsSupplierVatRegisteredPage.
    // We save both the VAT details and is supplier VAT registered details on the last page in case the user navigates with the back button to the CYA.
    if (mode.equals(CheckMode)) {
      userAnswers.set(SupplierBusinessOrIndividualPage(supplierNumber), Business)
    } else {
      Try(userAnswers)
    }
  }
}
