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
import models.{Address, UserAnswers}
import models.draftsections.NotifierAddress
import models.requests.DataRequest
import pages.sections.notifieraddress.{AddressJourneyIdPage, AddressPage}
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.AddressSanitiser
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

  private val dataGuard: DataRequest[?] => Boolean = !_.userContext.isAgent

  def callback(id: Option[String]): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(dataGuard).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      id match {
        case None =>
          logger.warn("ALF callback called without an id query parameter")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

        case Some(journeyId) =>
          for {
            updatedAnswers <- sessionRepository.setPage(request.userAnswers, AddressJourneyIdPage, journeyId)
            result         <- confirmAddress(journeyId, updatedAnswers)
          } yield result
      }
    }

  private def confirmAddress(journeyId: String, userAnswers: UserAnswers)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    addressLookupConnector.confirmedAddress(journeyId).flatMap {
      case Left(error) =>
        logger.warn(s"Failed to retrieve confirmed address from ALF for journey $journeyId: $error")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

      case Right(address) =>
        val sanitised = AddressSanitiser.sanitise(address)

        if (!mandatoryFieldsPopulated(sanitised)) {
          logger.warn(s"Sanitiser stripped mandatory address fields to empty for journey $journeyId")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        } else {
          val toStore = if (sanitised == address) address else sanitised

          for {
            updatedAnswers <- Future.fromTry(userAnswers.set(AddressPage, toStore))
            _              <- sessionRepository.set(updatedAnswers)
            result         <-
              if (sanitised == address)
                saveViaF4(toStore, updatedAnswers)
              else
                Future.successful(Redirect(routes.AddressChangedController.onPageLoad()))
          } yield result
        }
    }
  }

  private def mandatoryFieldsPopulated(address: Address): Boolean =
    address.lines.lift(0).exists(_.trim.nonEmpty) && address.lines.lift(1).exists(_.trim.nonEmpty)

  private def saveViaF4(address: Address, userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Result] =
    val versionId = userAnswers.get(DraftVersionIdPage).getOrElse(0L)

    userAnswers.get(DraftIdPage) match {
      case None =>
        logger.warn("DraftId missing from UserAnswers — cannot persist notifier-address section")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

      case Some(draftId) =>
        val body = Json.toJson(NotifierAddress.fromAddress(address)).as[JsObject] + ("versionId", Json.toJson(versionId))
        backendConnector.updateDraftSection(draftId, "notifier-address", body).flatMap {
          case Right(versionId) =>
            for {
              _      <- sessionRepository.setPage(userAnswers, DraftVersionIdPage, versionId)
              result <- Future successful Redirect(routes.NotificationTaskListController.onPageLoad())
            } yield result
          case Left(error) =>
            logger.warn(s"Failed to update notifier-address section for draftId ${draftId.value}: $error")
            Future successful Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
    }
}
