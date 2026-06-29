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
import models.draftsections.InitialQuestions
import models.requests.DataRequest
import models.responses.CreateDraftResponse
import models.{DraftId, NovaUserType, PurchaserOrOnBehalf, UserAnswers, UserContext}
import pages.*
import pages.sections.initialquestions.{BusinessOrPrivatePage, PurchaserBusinessOrIndividualPage, PurchaserOrOnBehalfPage, VehicleBusinessUsePage, VehicleFromEuPage}
import pages.sections.introduction.{AmendSubmittedNotificationPage, IntroductionAcknowledgePage}
import play.api.libs.json.{JsObject, Json}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.InitialQuestionsCheckYourAnswersView
import play.api.mvc.Results.*

import scala.concurrent.{ExecutionContext, Future}

class InitialQuestionsCheckYourAnswersController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  backendConnector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  view: InitialQuestionsCheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import InitialQuestionsCheckYourAnswersController.*

  def onPageLoad: Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
    Ok(view(request.userContext, request.userAnswers))
  }

  def onSubmit: Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val ctx                        = UserContext.from(request.affinityGroup, request.enrolments, request.userAnswers)
    val clientVrn                  = if (ctx.isAgentWithClient) request.userAnswers.get(AgentSelectedClientPage).map(_.vrn) else None

    backendConnector.createDraft(clientVrn).flatMap {
      case Left(value)                                    => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      case Right(CreateDraftResponse(draftId, versionId)) =>
        for {
          answersWithDraft   <- sessionRepository.setPage(request.userAnswers, DraftIdPage, DraftId(draftId))
          answersWithVersion <- sessionRepository.setPage(answersWithDraft, DraftVersionIdPage, versionId)
          result             <- updateIntroductionSection(request, DraftId(draftId), answersWithVersion, backendConnector, sessionRepository)
        } yield result
    }
  }
}

object InitialQuestionsCheckYourAnswersController {

  private val logger = play.api.Logger(classOf[InitialQuestionsCheckYourAnswersController])

  // TODO: nav to nextPage set up remaining once downstream NTL screen set up for all user types inc agents
  def nextPage(userContext: UserContext): play.api.mvc.Call =
    userContext.userType match {
      case NovaUserType.VatRegisteredOrganisation => routes.NotificationTaskListController.onPageLoad()
      case _                                      => routes.LandingPageController.onPageLoad()
    }

  def guardPredicate(request: DataRequest[?]): Boolean =
    request.userContext.userType match {
      case NovaUserType.PrivateIndividual | NovaUserType.NonVatOrganisation =>
        standardUserAnswersComplete(request.userAnswers)
      case NovaUserType.Agent if request.userContext.isAgentWithoutClient =>
        agentWithoutClientAnswersComplete(request.userAnswers)
      case NovaUserType.VatRegisteredOrganisation =>
        vatRegisteredOrgAnswersComplete(request.userAnswers)
      case NovaUserType.Agent =>
        agentWithClientAnswersComplete(request.userAnswers)
    }

  private def standardUserAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(VehicleFromEuPage).contains(true) &&
      answers.get(BusinessOrPrivatePage).isDefined &&
      answers.get(PurchaserOrOnBehalfPage).exists {
        case PurchaserOrOnBehalf.Purchaser           => true
        case PurchaserOrOnBehalf.OnBehalfOfPurchaser => answers.get(PurchaserBusinessOrIndividualPage).isDefined
      }

  private def vatRegisteredOrgAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(VehicleFromEuPage).isDefined &&
      answers.get(VehicleBusinessUsePage).isDefined

  private def agentWithoutClientAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(VehicleFromEuPage).isDefined &&
      answers.get(BusinessOrPrivatePage).isDefined &&
      answers.get(PurchaserOrOnBehalfPage).exists {
        case PurchaserOrOnBehalf.Purchaser           => true
        case PurchaserOrOnBehalf.OnBehalfOfPurchaser => answers.get(PurchaserBusinessOrIndividualPage).isDefined
      }

  private def agentWithClientAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(VehicleFromEuPage).isDefined &&
      answers.get(AgentClientVehicleBusinessUsePage).isDefined

  def buildSectionData(userContext: UserContext, answers: UserAnswers): Option[InitialQuestions] =
    answers.get(VehicleFromEuPage).map { vehicleFromEuToNi =>
      userContext.userType match {
        case NovaUserType.VatRegisteredOrganisation =>
          InitialQuestions(
            vehicleFromEuToNi = vehicleFromEuToNi,
            isForBusinessUse = answers.get(VehicleBusinessUsePage),
            areYouBusinessOrPrivate = None,
            notifyingAsPurchaserOrOnBehalf = None,
            isPurchaserBusinessOrPrivateIndividual = None,
            agentClientVehicleBusinessUse = None
          )

        case NovaUserType.Agent if userContext.isAgentWithClient =>
          InitialQuestions(
            vehicleFromEuToNi = vehicleFromEuToNi,
            isForBusinessUse = None,
            areYouBusinessOrPrivate = None,
            notifyingAsPurchaserOrOnBehalf = None,
            isPurchaserBusinessOrPrivateIndividual = None,
            agentClientVehicleBusinessUse = answers.get(AgentClientVehicleBusinessUsePage)
          )

        case _ =>
          InitialQuestions(
            vehicleFromEuToNi = vehicleFromEuToNi,
            isForBusinessUse = None,
            areYouBusinessOrPrivate = answers.get(BusinessOrPrivatePage),
            notifyingAsPurchaserOrOnBehalf = answers.get(PurchaserOrOnBehalfPage),
            isPurchaserBusinessOrPrivateIndividual = answers.get(PurchaserBusinessOrIndividualPage),
            agentClientVehicleBusinessUse = None
          )
      }
    }

  private def updateIntroductionSection(
    request: DataRequest[AnyContent],
    draftId: DraftId,
    answers: UserAnswers,
    backendConnector: NovaImportsBackendConnector,
    sessionRepository: SessionRepository
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    val versionId = answers.get(DraftVersionIdPage).getOrElse(0L)
    val body      = Json.obj(
      "acknowledged"               -> answers.get(IntroductionAcknowledgePage).getOrElse(false),
      "amendSubmittedNotification" -> answers.get(AmendSubmittedNotificationPage).getOrElse(false),
      "versionId"                  -> versionId
    )
    backendConnector.updateDraftSection(draftId, "introduction", body).flatMap {
      case Left(error) =>
        logger.warn(s"Failed to update 'introduction' section for draftId ${draftId.value}: $error")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      case Right(newVersionId) =>
        sessionRepository.setPage(answers, DraftVersionIdPage, newVersionId).flatMap { answersWithNewVersion =>
          buildSectionDataAndUpdate(request, draftId, answersWithNewVersion, backendConnector, sessionRepository)
        }
    }
  }

  private def buildSectionDataAndUpdate(
    request: DataRequest[AnyContent],
    draftId: DraftId,
    updatedAnswers: UserAnswers,
    backendConnector: NovaImportsBackendConnector,
    sessionRepository: SessionRepository
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    buildSectionData(request.userContext, updatedAnswers) match {
      case None =>
        logger.warn("Failed to submit 'initial-questions' — draftId or section data missing from UserAnswers")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      case Some(sectionData) =>
        val versionId       = updatedAnswers.get(DraftVersionIdPage).getOrElse(0L)
        val sectionJsonBody = Json.toJson(sectionData).as[JsObject] + ("versionId" -> Json.toJson(versionId))
        backendConnector.updateDraftSection(draftId, "initial-questions", sectionJsonBody).flatMap {
          case Right(newVersionId) =>
            sessionRepository.setPage(updatedAnswers, DraftVersionIdPage, newVersionId).map(_ => Redirect(nextPage(request.userContext)))
          case Left(error) =>
            logger.warn(s"Failed to update 'initial-questions' section for draftId ${draftId.value}: $error")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
    }
  }
}
