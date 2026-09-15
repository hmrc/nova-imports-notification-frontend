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
import controllers.{BaseController, routes}
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import forms.SupplierNameFormProvider
import models.requests.DataRequest

import javax.inject.Inject
import models.{AddressJourney, BusinessOrPrivateIndividual, Mode, NameDetails, SupplierNumber, UserAnswers}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplierdetails.{SupplierBusinessOrIndividualPage, SupplierEuMemberStatesPage, SupplierNamePage}
import play.api.Logging
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{AddressLookupService, SupplierService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.SupplierNameView

import scala.concurrent.{ExecutionContext, Future}

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
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)) { implicit request =>
      Ok(view(form.withDefault(request.userAnswers.get(SupplierNamePage(supplierNumber))), supplierNumber, mode))
    }

  def onSubmit(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, supplierNumber, mode))),
          supplierName =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(SupplierNamePage(supplierNumber), supplierName))
              _              <- sessionRepository.set(updatedAnswers)
              result         <- initialiseAlfJourney(supplierNumber, updatedAnswers)
            } yield result
        )
    }

  private def initialiseAlfJourney(supplierNumber: SupplierNumber, userAnswers: UserAnswers)(implicit
    request: DataRequest[?]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val journey                    = AddressJourney.Supplier(supplierNumber)

    backendConnector.getEuMemberStates().flatMap {
      case Right(states) =>
        addressLookupService.initJourney(journey, false, states.countries.map(_.code).toSeq).flatMap {
          case Right(journeyUrl) =>
            for {
              ua <- Future.fromTry(userAnswers.set(SupplierEuMemberStatesPage(supplierNumber), states.countries))
              _  <- sessionRepository.set(ua)
            } yield Redirect(journeyUrl)
          case Left(error) =>
            logger.warn(s"Failed to init supplier ALF journey : $error")
            Future successful Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
      case Left(error) =>
        logger.warn(s"Failed to init supplier ALF journey : $error")
        Future successful Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }

}

object SupplierNameController {

  // Only a private individual supplier has a name, and the supplier number in the URL
  // must be one of the suppliers the user has in session
  def guardPredicate(supplierService: SupplierService, supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      request.userAnswers.get(SupplierBusinessOrIndividualPage(supplierNumber)).contains(BusinessOrPrivateIndividual.PrivateIndividual) &&
      supplierService.numberExists(request.userAnswers, supplierNumber)
}
