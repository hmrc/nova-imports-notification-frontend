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
import config.FrontendAppConfig
import controllers.actions.Actions
import controllers.utils.IsDraftIdDefined
import models.DraftNotification.SectionId
import models.requests.DataRequest
import models.{BusinessOrPrivateIndividual, NormalMode, NotificationSummary, NovaUserType, PurchaserBusinessOrIndividual, PurchaserOrOnBehalf, SectionStatus, UserAnswers, UserContext}
import pages.{DraftIdPage, NotificationTaskListPage}
import pages.sections.initialquestions.{BusinessOrPrivatePage, PurchaserBusinessOrIndividualPage, PurchaserOrOnBehalfPage, VehicleBusinessUsePage, VehicleFromEuPage}
import pages.sections.notifieraddress.AddressJourneyIdPage
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{NotificationSummaryService, UserDataService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.NotificationTaskListView

import scala.concurrent.{ExecutionContext, Future}

class NotificationTaskListController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  notificationSummaryService: NotificationSummaryService,
  userDataService: UserDataService,
  sessionRepository: SessionRepository,
  view: NotificationTaskListView
)(implicit ec: ExecutionContext, appConfig: FrontendAppConfig)
    extends BaseController
    with Logging {

  import NotificationTaskListController.*

  def onPageLoad(): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val draftId = request.userAnswers.get(DraftIdPage).get

    def render(userName: Option[String], vrn: Option[String], answers: UserAnswers): Future[Result] = {
      val sections    = userDataService.determineAndUpdateStatus(answers, request.userContext)
      val sectionLink = determineSectionLink(sections, answers, request.userContext)
      for {
        flagged <- Future.fromTry(answers.set(NotificationTaskListPage, true))
        _       <- sessionRepository.set(flagged)
      } yield Ok(
        view(
          userName,
          vrn,
          sections,
          showAddYourAddress(request.userContext, flagged),
          showAboutThePurchaser(request.userContext, flagged),
          sectionLink
        )
      )
    }

    userDataService.retrieveAndStoreDraftNotification(draftId, request.userAnswers).flatMap {
      case Left(error) =>
        logger.warn(s"Failed to retrieve draft notification for draftId ${draftId.value}: $error")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

      case Right(updatedAnswers) =>
        notificationSummaryService.getSummaryAndStoreDeregisteredStatus(updatedAnswers, None).flatMap {
          case Right((org: NotificationSummary.IndividualOrOrganisation, savedAnswers)) =>
            render(org.traderName, org.vrn, savedAnswers)

          case Right((agent: NotificationSummary.AgentWithoutClient, savedAnswers)) =>
            render(agent.agentName, None, savedAnswers)

          case Right((other, _)) =>
            logger.warn(s"Unexpected notification summary shape for the notification task list: $other")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

          case Left(error) =>
            logger.warn(s"Failed to fetch notification summary for the notification task list: $error")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
    }
  }
}

object NotificationTaskListController {

  def guardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) && (request.userContext.userType match {
      case NovaUserType.VatRegisteredOrganisation =>
        request.userAnswers.get(VehicleBusinessUsePage).isDefined
      case NovaUserType.PrivateIndividual | NovaUserType.NonVatOrganisation =>
        standardInitialQuestionsComplete(request.userAnswers)
      case NovaUserType.Agent if request.userContext.isAgentWithoutClient =>
        standardInitialQuestionsComplete(request.userAnswers)
      case NovaUserType.Agent =>
        false
    })

  private def standardInitialQuestionsComplete(answers: UserAnswers): Boolean =
    answers.get(VehicleFromEuPage).contains(true) &&
      answers.get(BusinessOrPrivatePage).isDefined &&
      answers.get(PurchaserOrOnBehalfPage).exists {
        case PurchaserOrOnBehalf.Purchaser           => true
        case PurchaserOrOnBehalf.OnBehalfOfPurchaser => answers.get(PurchaserBusinessOrIndividualPage).isDefined
      }

  def showAddYourAddress(userContext: UserContext, answers: UserAnswers): Boolean =
    userContext.userType match {
      case NovaUserType.VatRegisteredOrganisation                           => answers.get(VehicleBusinessUsePage).contains(false)
      case NovaUserType.PrivateIndividual | NovaUserType.NonVatOrganisation => true
      case NovaUserType.Agent                                               => false
    }

  def showAboutThePurchaser(userContext: UserContext, answers: UserAnswers): Boolean =
    userContext.userType match {
      case NovaUserType.Agent                                               => true
      case NovaUserType.PrivateIndividual | NovaUserType.NonVatOrganisation =>
        answers.get(PurchaserOrOnBehalfPage).contains(PurchaserOrOnBehalf.OnBehalfOfPurchaser)
      case NovaUserType.VatRegisteredOrganisation => false
    }

  def determineSectionLink(sections: Map[String, SectionStatus], userAnswers: UserAnswers, userContext: UserContext)(implicit
    appConfig: FrontendAppConfig
  ): Map[String, String] =
    sections.flatMap {
      case (section @ SectionId.NotifierDetails, status) =>
        if (status == SectionStatus.Completed) Map(section -> routes.YourDetailsCheckYourAnswersController.onPageLoad().url)
        else Map(section                                   -> notifierDetailsStartLink(userContext, userAnswers))

      case (section @ SectionId.NotifierAddress, status) =>
        userAnswers.get(AddressJourneyIdPage) match
          case Some(journeyId) if status == SectionStatus.Completed => Map(section -> appConfig.addressLookupFrontendConfirmPath(journeyId))
          case _ => Map(section -> routes.IsYourAddressInTheUkController.onPageLoad(NormalMode).url)

      case (section @ SectionId.PurchaserDetails, status) =>
        if (status == SectionStatus.Completed) Map(section -> routes.PurchaserDetailsCheckYourAnswersController.onPageLoad().url)
        else if (userAnswers.get(PurchaserBusinessOrIndividualPage).contains(PurchaserBusinessOrIndividual.NonVatRegisteredBusiness))
          Map(section    -> routes.PurchaserBusinessNameController.onPageLoad(NormalMode).url)
        else Map(section -> routes.PurchaserNameController.onPageLoad(NormalMode).url)

      case (section @ SectionId.PurchaserAddress, _) =>
        Map(section -> routes.IsPurchaserAddressInTheUkController.onPageLoad(NormalMode).url)

      case (section @ SectionId.Vehicles, _) => Map(section -> routes.AddVehicleDetailsController.onPageLoad(NormalMode).url)

      case _ => Map.empty[String, String]
    }

  private def notifierDetailsStartLink(userContext: UserContext, answers: UserAnswers): String =
    userContext.userType match {
      case NovaUserType.VatRegisteredOrganisation => routes.AboutYourDetailsController.onPageLoad().url
      case NovaUserType.Agent                     => routes.PhoneNumberController.onPageLoad(NormalMode).url
      case _                                      =>
        if (answers.get(BusinessOrPrivatePage).contains(BusinessOrPrivateIndividual.PrivateIndividual))
          routes.AddYourNameController.onPageLoad(NormalMode).url
        else
          routes.BusinessNameController.onPageLoad(NormalMode).url
    }
}
