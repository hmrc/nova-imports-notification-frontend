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
import controllers.vehicledetails.VehicleSpreadsheetUploadController.guardPredicate
import models.UserContext
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import views.html.UploadSuccessfulView

import javax.inject.Inject

class UploadSuccessfulController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  view: UploadSuccessfulView
) extends BaseController {

  import UploadSuccessfulController.*

  def onPageLoad(): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
    Ok(view(continueRoute(request.userContext)))
  }
}

object UploadSuccessfulController {

  def continueRoute(userContext: UserContext): Call =
    if (userContext.isVatRegisteredOrganisation) controllers.routes.NotificationTaskListController.onPageLoad()
    else controllers.routes.LandingPageController.onPageLoad()
}
