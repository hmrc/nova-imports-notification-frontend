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
import forms.SupplierNameFormProvider
import models.BusinessOrPrivateIndividual.PrivateIndividual
import models.requests.DataRequest

import javax.inject.Inject
import models.{BusinessOrPrivateIndividual, CheckMode, Mode, NameDetails, SupplierNumber, UserAnswers}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplieraddress.SupplierAddressPage
import pages.sections.supplierdetails.{SupplierBusinessOrIndividualPage, SupplierNamePage}
import play.api.Logging
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{AddressLookupService, SupplierService}
import views.html.SupplierNameView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SupplierNameController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  actions: Actions,
  formProvider: SupplierNameFormProvider,
  supplierService: SupplierService,
  addressLookupService: AddressLookupService,
  backendConnector: NovaImportsBackendConnector,
  view: SupplierNameView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import SupplierNameController.*

  val form: Form[NameDetails] = formProvider()

  def onPageLoad(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber, mode)) { implicit request =>
      Ok(view(form.withDefault(request.userAnswers.get(SupplierNamePage(supplierNumber))), supplierNumber, mode))
    }

  def onSubmit(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber, mode)).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, supplierNumber, mode))),
          supplierName =>
            for {
              updatedAnswers  <- Future.fromTry(request.userAnswers.set(SupplierNamePage(supplierNumber), supplierName))
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

object SupplierNameController {

  // Only a private individual supplier has a name, and the supplier number in the URL
  // must be one of the suppliers the user has in session
  def guardPredicate(supplierService: SupplierService, supplierNumber: SupplierNumber, mode: Mode)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      (mode.equals(CheckMode) || request.userAnswers
        .get(SupplierBusinessOrIndividualPage(supplierNumber))
        .contains(BusinessOrPrivateIndividual.PrivateIndividual)) &&
      supplierService.numberHasValues(request.userAnswers, supplierNumber)

  private def saveBusinessOrIndividual(userAnswers: UserAnswers, supplierNumber: SupplierNumber, mode: Mode): Try[UserAnswers] = {
    // If in Check Mode and then also save IsSupplierVatRegisteredPage.
    // We save both the VAT details and is supplier VAT registered details on the last page in case the user navigates with the back button to the CYA.
    if (mode.equals(CheckMode)) {
      userAnswers.set(SupplierBusinessOrIndividualPage(supplierNumber), PrivateIndividual)
    } else {
      Try(userAnswers)
    }
  }
}
