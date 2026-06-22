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
import models.draftsections.{NotifierDetailsIndividual, NotifierDetailsOrganisation}
import models.requests.DataRequest
import models.{BusinessOrPrivateIndividual, NovaUserType, UserAnswers, UserContext}
import pages.*
import pages.sections.notifierDetails.*
import pages.sections.initialquestions.{BusinessOrPrivatePage, VehicleBusinessUsePage}
import play.api.libs.json.{JsObject, Json}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.YourDetailsCheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class YourDetailsCheckYourAnswersController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  backendConnector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  view: YourDetailsCheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import YourDetailsCheckYourAnswersController.*

  def onPageLoad: Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
    Ok(view(request.userContext, request.userAnswers))
  }

  def onSubmit: Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val submissionData = for {
      draftId     <- request.userAnswers.get(DraftIdPage)
      versionId   <- request.userAnswers.get(DraftVersionIdPage)
      sectionData <- buildSectionData(request.userContext, request.userAnswers)
    } yield (draftId, versionId, sectionData)

    submissionData match {
      case None =>
        logger.warn("Failed to submit 'your-details' — draftId, versionId or section data missing")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

      case Some((draftId, versionId, sectionData)) =>
        val sectionJsonBody = sectionData + ("versionId" -> Json.toJson(versionId))
        backendConnector.updateDraftSection(draftId, "notifier-details", sectionJsonBody).flatMap {
          case Right(newVersionId) =>
            sessionRepository
              .setPage(request.userAnswers, DraftVersionIdPage, newVersionId)
              .map(_ => Redirect(nextPage(request.userContext)))
          case Left(error) =>
            logger.warn(s"Failed to update 'notifier-details' section for draftId ${draftId.value}: $error")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
    }
  }
}

object YourDetailsCheckYourAnswersController {

  // TODO: route nav set up remaining for other user types once downstream task list pages are set up.
  def nextPage(userContext: UserContext): play.api.mvc.Call =
    userContext.userType match {
      case NovaUserType.VatRegisteredOrganisation => routes.NotificationTaskListController.onPageLoad()
      case _                                      => routes.LandingPageController.onPageLoad()
    }

  def guardPredicate(request: DataRequest[?]): Boolean = {
    val answers     = request.userAnswers
    val userContext = request.userContext

    IsDraftIdDefined(answers) && (userContext match {
      case ctx if ctx.isAgentWithClientNoEnrolments => agentWithClientNoEnrolmentsAnswersComplete(answers)
      case ctx if ctx.isVatRegisteredOrganisation   => vatRegisteredOrgAnswersComplete(answers)
      case ctx if ctx.isAgentWithoutClient          => agentWithoutClientAnswersComplete(answers)
      case ctx if ctx.isAgentWithClient             => agentWithClientAnswersComplete(answers)
      case _                                        => standardUserAnswersComplete(answers)
    })
  }

  private def agentWithClientNoEnrolmentsAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(AgentClientVehicleBusinessUsePage).isDefined &&
      answers.get(PhoneNumberPage).isDefined &&
      answers.get(EmailAddressPage).isDefined

  private def vatRegisteredOrgAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(PhoneNumberPage).isDefined &&
      answers.get(EmailAddressPage).isDefined &&
      (answers.get(NameDetailsPage).isDefined == answers.get(VehicleBusinessUsePage).contains(false))

  private def standardUserAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(PhoneNumberPage).isDefined &&
      answers.get(EmailAddressPage).isDefined &&
      (answers.get(NameDetailsPage).isDefined == answers.get(BusinessOrPrivatePage).contains(BusinessOrPrivateIndividual.PrivateIndividual))

  private def agentWithoutClientAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(PhoneNumberPage).isDefined &&
      answers.get(EmailAddressPage).isDefined &&
      (answers.get(NameDetailsPage).isDefined == answers.get(BusinessOrPrivatePage).contains(BusinessOrPrivateIndividual.PrivateIndividual))

  private def agentWithClientAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(PhoneNumberPage).isDefined &&
      answers.get(EmailAddressPage).isDefined &&
      (answers.get(NameDetailsPage).isDefined == answers.get(AgentClientVehicleBusinessUsePage).contains(false))

  def buildSectionData(userContext: UserContext, answers: UserAnswers): Option[JsObject] =
    for {
      phoneNumber  <- answers.get(PhoneNumberPage)
      emailAddress <- answers.get(EmailAddressPage)
    } yield answers.get(NameDetailsPage) match {
      case Some(name) =>
        Json.toJson(NotifierDetailsIndividual(name.title, name.firstName, name.lastName, emailAddress, phoneNumber)).as[JsObject]
      case None =>
        Json.toJson(NotifierDetailsOrganisation(emailAddress, phoneNumber)).as[JsObject]
    }
}
