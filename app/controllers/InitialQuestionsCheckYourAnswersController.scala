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
import models.{NovaUserType, PurchaserOrOnBehalf, UserAnswers, UserContext}
import pages.*
import play.api.libs.json.{JsObject, Json}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.InitialQuestionsCheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class InitialQuestionsCheckYourAnswersController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  backendConnector: NovaImportsBackendConnector,
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

    val submissionData = for {
      draftId     <- request.userAnswers.get(DraftIdPage)
      sectionData <- buildSectionData(request.userContext, request.userAnswers)
    } yield (draftId, sectionData)

    submissionData match {
      case None =>
        logger.warn("Failed to submit 'initial-questions' — draftId or section data missing from UserAnswers")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

      case Some((draftId, sectionData)) =>
        val sectionJsonBody = Json.toJson(sectionData).as[JsObject]
        backendConnector.updateDraftSection(draftId, "initial-questions", sectionJsonBody).map {
          case Right(_)    => Redirect(routes.LandingPageController.onPageLoad()) // TODO: navigate to next screen - to be added later
          case Left(error) =>
            logger.warn(s"Failed to update 'initial-questions' section for draftId ${draftId.value}: $error")
            Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
    }
  }
}

object InitialQuestionsCheckYourAnswersController {

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
      answers.get(BusinessPrivatePage).isDefined &&
      answers.get(PurchaserOrOnBehalfPage).exists {
        case PurchaserOrOnBehalf.Purchaser           => true
        case PurchaserOrOnBehalf.OnBehalfOfPurchaser => answers.get(PurchaserBusinessOrIndividualPage).isDefined
      }

  private def vatRegisteredOrgAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(VehicleFromEuPage).isDefined &&
      answers.get(VehicleBusinessUsePage).isDefined

  private def agentWithoutClientAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(VehicleFromEuPage).isDefined &&
      answers.get(BusinessPrivatePage).isDefined &&
      answers.get(PurchaserOrOnBehalfPage).exists {
        case PurchaserOrOnBehalf.Purchaser           => true
        case PurchaserOrOnBehalf.OnBehalfOfPurchaser => answers.get(PurchaserBusinessOrIndividualPage).isDefined
      }

  private def agentWithClientAnswersComplete(answers: UserAnswers): Boolean =
    answers.get(VehicleFromEuPage).isDefined &&
      answers.get(AgentVehicleBusinessUsePage).isDefined

  def buildSectionData(userContext: UserContext, answers: UserAnswers): Option[InitialQuestions] =
    answers.get(VehicleFromEuPage).map { vehicleFromEuToNi =>
      userContext.userType match {
        case NovaUserType.VatRegisteredOrganisation =>
          InitialQuestions(
            vehicleFromEuToNi = vehicleFromEuToNi,
            vehicleIntoUkForBusinessUse = answers.get(VehicleBusinessUsePage),
            areYouBusinessOrPrivate = None,
            notifyingAsPurchaserOrOnBehalf = None,
            isPurchaserBusinessOrPrivateIndividual = None,
            agentClientVehicleBusinessUse = None
          )

        case NovaUserType.Agent if userContext.isAgentWithClient =>
          InitialQuestions(
            vehicleFromEuToNi = vehicleFromEuToNi,
            vehicleIntoUkForBusinessUse = None,
            areYouBusinessOrPrivate = None,
            notifyingAsPurchaserOrOnBehalf = None,
            isPurchaserBusinessOrPrivateIndividual = None,
            agentClientVehicleBusinessUse = answers.get(AgentVehicleBusinessUsePage)
          )

        case _ =>
          InitialQuestions(
            vehicleFromEuToNi = vehicleFromEuToNi,
            vehicleIntoUkForBusinessUse = None,
            areYouBusinessOrPrivate = answers.get(BusinessPrivatePage),
            notifyingAsPurchaserOrOnBehalf = answers.get(PurchaserOrOnBehalfPage),
            isPurchaserBusinessOrPrivateIndividual = answers.get(PurchaserBusinessOrIndividualPage),
            agentClientVehicleBusinessUse = None
          )
      }
    }
}
