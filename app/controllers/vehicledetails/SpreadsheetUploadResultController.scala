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

package controllers.vehicledetails

import connectors.NovaImportsBackendConnector
import controllers.BaseController
import controllers.actions.Actions
import controllers.utils.IsDraftIdDefined
import controllers.vehicledetails.SpreadsheetUploadResultController.guardPredicate
import models.requests.DataRequest
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.ExecutionContext

// TODO - Delete when UVS-2.0 is built
class SpreadsheetUploadResultController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  connector: NovaImportsBackendConnector
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      connector.getUploadResult(request.userAnswers.get(DraftIdPage).get).map {
        case Right(result) => redirectFor(result.fileStatus)
        case Left(error)   =>
          logger.warn(s"Could not retrieve the vehicle spreadsheet upload result: $error")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

  private def redirectFor(fileStatus: String): Result = fileStatus match {
    case "VALIDATED"          => Redirect(routes.CheckVehicleSpreadsheetDetailsController.onPageLoad())
    case "VALIDATION_FAILED"  => Redirect(routes.CheckVehicleSpreadsheetErrorsController.onPageLoad())
    case _                    => Redirect(controllers.routes.LandingPageController.onPageLoad()) // TODO: navigate to UVS-2.0 when built
  }
}

object SpreadsheetUploadResultController {

  def guardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).isDefined &&
      (request.userContext.isVatRegisteredOrganisation || request.userContext.isAgentWithClient)
}
