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

import controllers.BaseController
import controllers.actions.Actions
import controllers.utils.IsDraftIdDefined
import models.requests.DataRequest
import pages.sections.initialquestions.VehicleFromEuPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.UploadSpreadsheetErrorUnknownView

import javax.inject.Inject

class UploadSpreadsheetErrorUnknownController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  view: UploadSpreadsheetErrorUnknownView
) extends BaseController {

  import UploadSpreadsheetErrorUnknownController.*

  def onPageLoad(): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
    Ok(view())
  }
}

object UploadSpreadsheetErrorUnknownController {

  def guardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).isDefined &&
      (request.userContext.isVatRegisteredOrganisation || request.userContext.isAgentWithClient)
}
