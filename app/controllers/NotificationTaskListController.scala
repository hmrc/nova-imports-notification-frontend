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
import models.DraftNotification.SectionId
import models.{NormalMode, NotificationSummary, SectionStatus, UserAnswers}
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleBusinessUsePage
import pages.sections.notifieraddress.AddressJourneyIdPage
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
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
  view: NotificationTaskListView
)(implicit ec: ExecutionContext, appConfig: FrontendAppConfig)
    extends BaseController
    with Logging {

  import NotificationTaskListController.*

  def onPageLoad(): Action[AnyContent] = actions.vatTraderAuthAndGetDataWithGuard(guardPredicate).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val draftId = request.userAnswers.get(DraftIdPage).get // guard guarantees presence

    userDataService.retrieveAndStoreDraftNotification(draftId, request.userAnswers).flatMap {
      case Left(error) =>
        logger.warn(s"Failed to retrieve draft notification for draftId ${draftId.value}: $error")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

      case Right(updatedAnswers) =>
        notificationSummaryService.getSummaryAndStoreDeregisteredStatus(updatedAnswers, None).map {
          case Right((org: NotificationSummary.IndividualOrOrganisation, savedAnswers)) =>
            val sections    = userDataService.determineAndUpdateStatus(savedAnswers, request.userContext)
            val sectionLink = determineSectionLink(sections, savedAnswers)
            Ok(view(org.traderName, org.vrn, sections, showAddYourAddress(savedAnswers), sectionLink))

          case Right((other, _)) =>
            logger.warn(s"Unexpected notification summary shape for VAT-registered Organisation: $other")
            Redirect(routes.JourneyRecoveryController.onPageLoad())

          case Left(error) =>
            logger.warn(s"Failed to fetch notification summary for VAT-registered Organisation: $error")
            Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
    }
  }
}

object NotificationTaskListController {

  // Reached after OQ1.0 has been answered and a draft has been created (via CYA1.0 → Save and continue).
  val guardPredicate: UserAnswers => Boolean =
    answers => answers.get(VehicleBusinessUsePage).isDefined && answers.get(DraftIdPage).isDefined

  // AC2: 'Add your address' is shown only when the user answered 'No' to OQ1.0.
  def showAddYourAddress(answers: UserAnswers): Boolean =
    answers.get(VehicleBusinessUsePage).contains(false)

  def determineSectionLink(sections: Map[String, SectionStatus], userAnswers: UserAnswers)(implicit
    appConfig: FrontendAppConfig
  ): Map[String, String] =
    sections.flatMap {
      case (section @ SectionId.NotifierDetails, status) =>
        if (status == SectionStatus.Completed) Map(section -> routes.LandingPageController.onPageLoad().url) // TODO - Update to CYA
        else Map(section                                   -> routes.AboutYourDetailsController.onPageLoad().url)

      case (section @ SectionId.NotifierAddress, status) =>
        userAnswers.get(AddressJourneyIdPage) match
          case Some(journeyId) if status == SectionStatus.Completed => Map(section -> appConfig.addressLookupFrontendConfirmPath(journeyId))
          case _ => Map(section -> routes.IsYourAddressInTheUkController.onPageLoad(NormalMode).url)

      case (section @ SectionId.Vehicles, status) => Map(section -> routes.AddVehicleDetailsController.onPageLoad(NormalMode).url)

      case _ => Map.empty[String, String]
    }
}
