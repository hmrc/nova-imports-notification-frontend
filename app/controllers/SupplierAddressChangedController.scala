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

import controllers.actions.*
import controllers.utils.{IsDraftIdDefined, IsSupplierNumberInSession}
import models.SupplierNumber
import models.requests.DataRequest
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplierDetails.SupplierAddressPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.SupplierAddressChangedView

import javax.inject.Inject

class SupplierAddressChangedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  view: SupplierAddressChangedView
) extends BaseController {

  import SupplierAddressChangedController.*

  def onPageLoad(supplierNumber: SupplierNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierNumber)) { implicit request =>
      request.userAnswers.get(SupplierAddressPage) match {
        case Some(address) => Ok(view(address, supplierNumber))
        case None          => Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }
}

object SupplierAddressChangedController {

  // The supplier number in the URL must be one of the suppliers the user has in session,
  // and a supplier address must have been captured to display on this screen.
  def guardPredicate(supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      IsSupplierNumberInSession(request.userAnswers, supplierNumber) &&
      request.userAnswers.get(SupplierAddressPage).isDefined
}
