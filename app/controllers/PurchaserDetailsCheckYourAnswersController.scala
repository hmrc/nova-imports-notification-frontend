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

import javax.inject.Inject
import connectors.NovaImportsBackendConnector
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import models.draftsections.{PurchaserDetailsBusiness, PurchaserDetailsIndividual}
import models.requests.DataRequest
import models.{NovaUserType, UserAnswers, UserContext}
import pages.*
import pages.sections.purchaserDetails.{PurchaserBusinessNamePage, PurchaserNamePage}
import play.api.libs.json.{JsObject, Json}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.PurchaserDetailsCheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class PurchaserDetailsCheckYourAnswersController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  backendConnector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  view: PurchaserDetailsCheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import PurchaserDetailsCheckYourAnswersController.*

  def onPageLoad: Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
    Ok(view(request.userContext, request.userAnswers))
  }

  def onSubmit: Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val submissionData = for {
      draftId     <- request.userAnswers.get(DraftIdPage)
      versionId   <- request.userAnswers.get(DraftVersionIdPage)
      sectionData <- buildSectionData(request.userAnswers)
    } yield (draftId, versionId, sectionData)

    submissionData match {
      case None =>
        logger.warn("Failed to submit 'purchaser-details' — draftId, versionId or section data missing")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

      case Some((draftId, versionId, sectionData)) =>
        val sectionJsonBody = sectionData + ("versionId" -> Json.toJson(versionId))
        backendConnector.updateDraftSection(draftId, "purchaser-details", sectionJsonBody).flatMap {
          case Right(newVersionId) =>
            sessionRepository
              .setPage(request.userAnswers, DraftVersionIdPage, newVersionId)
              .map(_ => Redirect(nextPage(request.userContext)))
          case Left(error) =>
            logger.warn(s"Failed to update 'purchaser-details' section for draftId ${draftId.value}: $error")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
    }
  }
}

object PurchaserDetailsCheckYourAnswersController {

  // TODO: nav to nextPage set up remaining once downstream NTL screen set up for all user types inc agents
  def nextPage(userContext: UserContext): play.api.mvc.Call =
    userContext.userType match {
      case NovaUserType.VatRegisteredOrganisation => routes.NotificationTaskListController.onPageLoad()
      case _                                      => routes.LandingPageController.onPageLoad()
    }

  // Purchaser details only apply when notifying on behalf of a purchaser, so a VAT-registered
  // organisation never reaches this screen. The data guard checks only the immediately preceding
  // page (APD1.0 purchaser name or APD2.0 purchaser business name).
  def guardPredicate(request: DataRequest[?]): Boolean = {
    val answers = request.userAnswers

    IsDraftIdDefined(answers) && !request.userContext.isVatRegisteredOrganisation &&
    (answers.get(PurchaserNamePage).isDefined || answers.get(PurchaserBusinessNamePage).isDefined)
  }

  def buildSectionData(answers: UserAnswers): Option[JsObject] =
    answers.get(PurchaserBusinessNamePage) match {
      case Some(businessName) =>
        Some(Json.toJson(PurchaserDetailsBusiness(businessName)).as[JsObject])
      case None =>
        answers.get(PurchaserNamePage).map { name =>
          Json.toJson(PurchaserDetailsIndividual(name.title, name.firstName, name.lastName)).as[JsObject]
        }
    }
}
