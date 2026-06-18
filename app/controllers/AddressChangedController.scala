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
import models.draftsections.NotifierAddress
import models.requests.DataRequest
import pages.sections.notifieraddress.AddressPage
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.AddressChangedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressChangedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  view: AddressChangedView,
  backendConnector: NovaImportsBackendConnector
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  private val dataGuard: DataRequest[?] => Boolean =
    request => !request.userContext.isAgent && request.userAnswers.get(AddressPage).isDefined

  def onPageLoad(): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(dataGuard) { implicit request =>
    request.userAnswers.get(AddressPage) match {
      case Some(address) => Ok(view(address))
      case None          => Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(dataGuard).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    lazy val versionId             = request.userAnswers.get(DraftVersionIdPage).getOrElse(0L)

    (request.userAnswers.get(AddressPage), request.userAnswers.get(DraftIdPage)) match {
      case (Some(address), Some(draftId)) =>
        val body = Json.toJson(NotifierAddress.fromAddress(address)).as[JsObject] + ("versionId", Json.toJson(versionId))
        backendConnector.updateDraftSection(draftId, "notifier-address", body).map {
          case Right(_)    => Redirect(routes.NotificationTaskListController.onPageLoad()) // TODO: navigate to NTL3.0
          case Left(error) =>
            logger.warn(s"Failed to update notifier-address section for draftId ${draftId.value}: $error")
            Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
      case _ =>
        logger.warn("Missing AddressPage or DraftIdPage when submitting AYA3.0")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
