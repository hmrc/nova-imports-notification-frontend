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
import connectors.NovaImportsBackendConnector
import controllers.actions.Actions
import models.{NotificationSummary, UserAnswers}
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleBusinessUsePage
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.UserDataService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.NotificationTaskListView

import scala.concurrent.ExecutionContext

class NotificationTaskListController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  backendConnector: NovaImportsBackendConnector,
  userDataService: UserDataService,
  view: NotificationTaskListView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import NotificationTaskListController.*

  def onPageLoad(): Action[AnyContent] = actions.vatTraderAuthAndGetDataWithGuard(guardPredicate).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val draftId = request.userAnswers.get(DraftIdPage).get // guard guarantees presence

    for {
      summaryResult        <- backendConnector.getNotificationSummary()
      updatedAnswersResult <- userDataService.retrieveAndStoreDraftNotification(draftId, request.userAnswers)
    } yield (summaryResult, updatedAnswersResult) match {
      case (Left(error), _) =>
        logger.warn(s"Failed to fetch notification summary for VAT-registered Organisation: $error")
        Redirect(routes.JourneyRecoveryController.onPageLoad())

      case (_, Left(error)) =>
        logger.warn(s"Failed to retrieve draft notification for draftId ${draftId.value}: $error")
        Redirect(routes.JourneyRecoveryController.onPageLoad())

      case (Right(summary), Right(updatedAnswers)) =>
        val sections = userDataService.determineAndUpdateStatus(updatedAnswers, request.userContext)

        summary match {
          case org: NotificationSummary.IndividualOrOrganisation =>
            Ok(view(org.traderName, org.vrn, sections, showAddYourAddress(updatedAnswers)))

          case other =>
            logger.warn(s"Unexpected notification summary shape for VAT-registered Organisation: $other")
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
}
