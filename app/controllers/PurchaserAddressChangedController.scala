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
import models.{NormalMode, PurchaserOrOnBehalf}
import models.draftsections.PurchaserAddress
import models.requests.DataRequest
import pages.sections.initialquestions.PurchaserOrOnBehalfPage
import pages.sections.purchaseraddress.{PurchaserAddressJourneyIdPage, PurchaserAddressPage}
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.PurchaserAddressChangedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PurchaserAddressChangedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  actions: Actions,
  view: PurchaserAddressChangedView,
  backendConnector: NovaImportsBackendConnector
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  private val dataGuard: DataRequest[?] => Boolean = request =>
    request.userAnswers.get(PurchaserAddressPage).isDefined && (request.userContext match {
      case ctx if ctx.isAgent => true
      case _                  => request.userAnswers.get(PurchaserOrOnBehalfPage).contains(PurchaserOrOnBehalf.OnBehalfOfPurchaser)
    })

  def onPageLoad(): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(dataGuard) { implicit request =>
    request.userAnswers.get(PurchaserAddressPage) match {
      case Some(address) => Ok(view(address))
      case None          => Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onChangeAddress(): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(dataGuard).async { implicit request =>
    for {
      cleared <- Future.fromTry(request.userAnswers.remove(PurchaserAddressPage).flatMap(_.remove(PurchaserAddressJourneyIdPage)))
      _       <- sessionRepository.set(cleared)
    } yield Redirect(routes.IsPurchaserAddressInTheUkController.onPageLoad(NormalMode))
  }

  def onSubmit(): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(dataGuard).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    lazy val versionId             = request.userAnswers.get(DraftVersionIdPage).getOrElse(0L)

    (request.userAnswers.get(PurchaserAddressPage), request.userAnswers.get(DraftIdPage)) match {
      case (Some(address), Some(draftId)) =>
        val body = Json.toJson(PurchaserAddress.fromAddress(address)).as[JsObject] + ("versionId", Json.toJson(versionId))
        backendConnector.updateDraftSection(draftId, "purchaser-address", body).map {
          case Right(vId) =>
            sessionRepository.setPage(request.userAnswers, DraftVersionIdPage, vId)
            Redirect(routes.NotificationTaskListController.onPageLoad())
          case Left(error) =>
            logger.warn(s"Failed to update purchaser-address section for draftId ${draftId.value}: $error")
            Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
      case _ =>
        logger.warn("Missing PurchaserAddressPage or DraftIdPage when submitting APA3.0")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
