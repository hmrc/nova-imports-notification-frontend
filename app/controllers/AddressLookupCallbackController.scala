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

import com.google.inject.Inject
import connectors.{AddressLookupConnector, NovaImportsBackendConnector}
import controllers.actions.*
import models.{Address, AddressJourney, SupplierNumber, UserAnswers}
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{AddressSanitiser, AddressValidator}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AddressLookupCallbackController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  actions: Actions,
  addressLookupConnector: AddressLookupConnector,
  backendConnector: NovaImportsBackendConnector
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  def callback(id: Option[String]): Action[AnyContent] =
    handleCallback(AddressJourney.Notifier, id)

  def supplierCallback(supplierNumber: SupplierNumber, id: Option[String]): Action[AnyContent] =
    handleCallback(AddressJourney.Supplier(supplierNumber), id)

  def purchaserCallback(id: Option[String]): Action[AnyContent] =
    handleCallback(AddressJourney.Purchaser, id)

  private def handleCallback(journey: AddressJourney, id: Option[String]): Action[AnyContent] = {
    val binding = AddressJourneyBinding(journey)

    actions.authAndGetDataWithUserTypeGuard(binding.guard).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      id match {
        case None =>
          logger.warn(s"ALF callback called without an id query parameter for $journey")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

        case Some(journeyId) =>
          for {
            updatedAnswers <- sessionRepository.setPage(request.userAnswers, binding.journeyIdPage, journeyId)
            result         <- confirmAddress(binding, journeyId, updatedAnswers)
          } yield result
      }
    }
  }

  private def confirmAddress(binding: AddressJourneyBinding, journeyId: String, userAnswers: UserAnswers)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Result] = {
    addressLookupConnector.confirmedAddress(journeyId).flatMap {
      case Left(error) =>
        logger.warn(s"Failed to retrieve confirmed address from ALF for journey $journeyId: $error")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

      case Right(address) =>
        val sanitised = AddressSanitiser.sanitise(address)

        if (!AddressValidator.hasMandatoryFields(sanitised)) {
          logger.warn(s"Sanitiser stripped mandatory address fields to empty for journey $journeyId")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        } else {
          val toStore = if (sanitised == address) address else sanitised

          for {
            updatedAnswers <- Future.fromTry(userAnswers.set(binding.addressPage, toStore))
            _              <- sessionRepository.set(updatedAnswers)
            result         <-
              if (sanitised == address)
                saveViaF4(binding, toStore, updatedAnswers)
              else
                Future.successful(Redirect(binding.addressChangedPage))
          } yield result
        }
    }
  }

  private def saveViaF4(binding: AddressJourneyBinding, address: Address, userAnswers: UserAnswers)(implicit
    hc: HeaderCarrier
  ): Future[Result] =
    val versionId = userAnswers.get(DraftVersionIdPage).getOrElse(0L)

    userAnswers.get(DraftIdPage) match {
      case None =>
        logger.warn(s"DraftId missing from UserAnswers — cannot persist ${binding.sectionId} section")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

      case Some(draftId) =>
        val body = binding.payload(address) + ("versionId", Json.toJson(versionId))
        backendConnector.updateDraftSection(draftId, binding.sectionId, body).flatMap {
          case Right(versionId) =>
            for {
              _      <- sessionRepository.setPage(userAnswers, DraftVersionIdPage, versionId)
              result <- Future successful Redirect(binding.onComplete)
            } yield result
          case Left(error) =>
            logger.warn(s"Failed to update ${binding.sectionId} section for draftId ${draftId.value}: $error")
            Future successful Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
    }
}
