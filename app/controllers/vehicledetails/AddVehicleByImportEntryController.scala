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

import controllers.{BaseController, routes, vehicledetails}
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import controllers.vehicledetails.AddVehicleByImportEntryController.*
import models.{ImportNumber, NormalMode}
import models.requests.DataRequest
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{ImportEntryDatePage, ImportEntryNumberPage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ImportService, VehicleService}
import views.html.AddVehicleByImportEntryView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AddVehicleByImportEntryController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  view: AddVehicleByImportEntryView,
  importService: ImportService,
  vehicleService: VehicleService
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad(importNumber: ImportNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(importService, importNumber)) { implicit request =>
      request.userAnswers.get(ImportEntryNumberPage(importNumber)) match {
        case Some(importEntryNumber) => Ok(view(importEntryNumber, importNumber))
        case None                    => Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }

  // adds a numbered vehicle collection for the import in the URL
  def onSubmit(importNumber: ImportNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(importService, importNumber)).async { implicit request =>
      vehicleService.addForImport(request.userAnswers, importNumber).map { vehicleNumber =>
        Redirect(vehicledetails.routes.DateOfFirstRegistrationKnownController.onPageLoad(importNumber, vehicleNumber, NormalMode))
      }
    }
}

object AddVehicleByImportEntryController {

  def guardPredicate(importService: ImportService, importNumber: ImportNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(false) &&
      request.userAnswers.get(ImportEntryDatePage(importNumber)).isDefined &&
      request.userAnswers.get(ImportEntryNumberPage(importNumber)).isDefined &&
      (request.userContext.isVatRegisteredOrganisation || request.userContext.isAgent) &&
      importService.numberHasValues(request.userAnswers, importNumber)
}
