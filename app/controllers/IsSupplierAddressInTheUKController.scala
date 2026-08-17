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
import controllers.utils.{IsDraftIdDefined, IsSupplierNumberInSession}
import forms.IsSupplierAddressInTheUkFormProvider
import models.requests.DataRequest
import models.{AddressJourney, Mode, SupplierNumber}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplierDetails.IsSupplierAddressInTheUkPage
import play.api.Logging
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AddressLookupService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.IsSupplierAddressInTheUkView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IsSupplierAddressInTheUKController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  actions: Actions,
  formProvider: IsSupplierAddressInTheUkFormProvider,
  view: IsSupplierAddressInTheUkView,
  addressLookupService: AddressLookupService
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import IsSupplierAddressInTheUKController.*

  val form: Form[Boolean] = formProvider()

  def onPageLoad(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierNumber)) { implicit request =>
      Ok(view(form.withDefault(request.userAnswers.get(IsSupplierAddressInTheUkPage)), supplierNumber, mode))
    }

  def onSubmit(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierNumber)).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val journey = AddressJourney.Supplier(supplierNumber)

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, supplierNumber, mode))),
          ukMode =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(IsSupplierAddressInTheUkPage, ukMode))
              _              <- sessionRepository.set(updatedAnswers)
              initResult     <- addressLookupService.initJourney(journey, ukMode)
            } yield initResult match {
              case Right(journeyUrl) =>
                Redirect(journeyUrl)
              case Left(error) =>
                logger.warn(s"Failed to init supplier ALF journey (ukMode=$ukMode): $error")
                Redirect(routes.JourneyRecoveryController.onPageLoad())
            }
        )
    }

}

object IsSupplierAddressInTheUKController {

  // The supplier number in the URL must be one of the suppliers the user has in session
  def guardPredicate(supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      IsSupplierNumberInSession(request.userAnswers, supplierNumber)
}
