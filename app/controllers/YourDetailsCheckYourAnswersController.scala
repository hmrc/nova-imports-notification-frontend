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
import models.draftsections.YourDetails
import models.requests.DataRequest
import models.{BusinessOrPrivateIndividual, NovaUserType, UserAnswers, UserContext}
import pages.*
import play.api.libs.json.{JsObject, Json}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.YourDetailsCheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class YourDetailsCheckYourAnswersController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  backendConnector: NovaImportsBackendConnector,
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
      sectionData <- buildSectionData(request.userContext, request.userAnswers)
    } yield (draftId, sectionData)

    submissionData match {
      case None =>
        logger.warn("Failed to submit 'your-details' — draftId or section data missing from UserAnswers")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

      case Some((draftId, sectionData)) =>
        val sectionJsonBody = Json.toJson(sectionData).as[JsObject]
        backendConnector.updateDraftSection(draftId, "your-details", sectionJsonBody).map {
          case Right(_)    => Redirect(nextPage(request.userContext))
          case Left(error) =>
            logger.warn(s"Failed to update 'your-details' section for draftId ${draftId.value}: $error")
            Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
    }
  }
}

object YourDetailsCheckYourAnswersController {

  // TODO: currently set up for all users to go to NTL next page, if agents go to different NTL screen, then this will need to get updated
  def nextPage(userContext: UserContext): play.api.mvc.Call =
    routes.NotificationTaskListController.onPageLoad()

  def guardPredicate(request: DataRequest[?]): Boolean = {
    val answers = request.userAnswers
    answers.get(PhoneNumberPage).isDefined &&
    answers.get(EmailAddressPage).isDefined &&
    (!nameRequired(request.userContext, answers) || answers.get(AddYourNamePage).isDefined)
  }

  private def nameRequired(userContext: UserContext, answers: UserAnswers): Boolean =
    userContext.userType match {
      case NovaUserType.PrivateIndividual | NovaUserType.NonVatOrganisation =>
        answers.get(BusinessPrivatePage).contains(BusinessOrPrivateIndividual.PrivateIndividual)
      case NovaUserType.Agent if userContext.isAgentWithoutClient =>
        answers.get(BusinessPrivatePage).contains(BusinessOrPrivateIndividual.PrivateIndividual)
      case NovaUserType.VatRegisteredOrganisation =>
        answers.get(VehicleBusinessUsePage).contains(false)
      case NovaUserType.Agent =>
        answers.get(AgentVehicleBusinessUsePage).contains(false)
    }

  def buildSectionData(userContext: UserContext, answers: UserAnswers): Option[YourDetails] =
    for {
      phoneNumber  <- answers.get(PhoneNumberPage)
      emailAddress <- answers.get(EmailAddressPage)
    } yield YourDetails(
      name = answers.get(AddYourNamePage),
      phoneNumber = phoneNumber,
      emailAddress = emailAddress
    )
}
