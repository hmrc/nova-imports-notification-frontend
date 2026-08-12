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

import connectors.NovaImportsBackendConnector
import controllers.actions.*
import models.requests.DataRequest
import models.{AddressJourney, SupplierNumber}
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.AddressChangedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressChangedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  actions: Actions,
  view: AddressChangedView,
  backendConnector: NovaImportsBackendConnector
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  def onPageLoad(): Action[AnyContent]      = handlePageLoad(AddressJourney.Notifier)
  def onChangeAddress(): Action[AnyContent] = handleChangeAddress(AddressJourney.Notifier)
  def onSubmit(): Action[AnyContent]        = handleSubmit(AddressJourney.Notifier)

  def supplierOnPageLoad(supplierNumber: SupplierNumber): Action[AnyContent] =
    handlePageLoad(AddressJourney.Supplier(supplierNumber))

  def supplierOnChangeAddress(supplierNumber: SupplierNumber): Action[AnyContent] =
    handleChangeAddress(AddressJourney.Supplier(supplierNumber))

  def supplierOnSubmit(supplierNumber: SupplierNumber): Action[AnyContent] =
    handleSubmit(AddressJourney.Supplier(supplierNumber))

  private def dataGuard(binding: AddressJourneyBinding): DataRequest[?] => Boolean =
    request => binding.guard(request) && request.userAnswers.get(binding.addressPage).isDefined

  private def handlePageLoad(journey: AddressJourney): Action[AnyContent] = {
    val binding = AddressJourneyBinding(journey)

    actions.authAndGetDataWithUserTypeGuard(dataGuard(binding)) { implicit request =>
      request.userAnswers.get(binding.addressPage) match {
        case Some(address) =>
          Ok(view(address, binding.messageKeyPrefix, binding.changeAddressLink, binding.addressChangedSubmit))
        case None => Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }
  }

  private def handleChangeAddress(journey: AddressJourney): Action[AnyContent] = {
    val binding = AddressJourneyBinding(journey)

    actions.authAndGetDataWithUserTypeGuard(dataGuard(binding)).async { implicit request =>
      for {
        cleared <- Future.fromTry(request.userAnswers.remove(binding.addressPage).flatMap(_.remove(binding.journeyIdPage)))
        _       <- sessionRepository.set(cleared)
      } yield Redirect(binding.restartAt)
    }
  }

  private def handleSubmit(journey: AddressJourney): Action[AnyContent] = {
    val binding = AddressJourneyBinding(journey)

    actions.authAndGetDataWithUserTypeGuard(dataGuard(binding)).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      lazy val versionId             = request.userAnswers.get(DraftVersionIdPage).getOrElse(0L)

      (request.userAnswers.get(binding.addressPage), request.userAnswers.get(DraftIdPage)) match {
        case (Some(address), Some(draftId)) =>
          val body = binding.payload(address) + ("versionId", Json.toJson(versionId))
          backendConnector.updateDraftSection(draftId, binding.sectionId, body).map {
            case Right(vId) =>
              sessionRepository.setPage(request.userAnswers, DraftVersionIdPage, vId)
              Redirect(binding.onComplete)
            case Left(error) =>
              logger.warn(s"Failed to update ${binding.sectionId} section for draftId ${draftId.value}: $error")
              Redirect(routes.JourneyRecoveryController.onPageLoad())
          }
        case _ =>
          logger.warn(s"Missing ${binding.addressPage} or DraftIdPage when submitting the address-changed page")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }
  }
}
