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
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import controllers.vehicledetails.ImportEntryDateController.*
import forms.ImportEntryDateFormProvider
import models.requests.DataRequest
import models.{ImportNumber, Mode, NovaUserType}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.ImportEntryDatePage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.ImportService
import views.html.ImportEntryDateView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ImportEntryDateController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: ImportEntryDateFormProvider,
  view: ImportEntryDateView,
  importService: ImportService
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad(importNumber: ImportNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(importService, importNumber)) { implicit request =>
      Ok(view(formProvider().withDefault(request.userAnswers.get(ImportEntryDatePage(importNumber))), importNumber, mode))
    }

  def onSubmit(importNumber: ImportNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(importService, importNumber)).async { implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, importNumber, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(ImportEntryDatePage(importNumber), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                ImportEntryDatePage(importNumber),
                mode,
                updatedAnswers,
                NovaUserType.from(request.affinityGroup, request.enrolments)
              )
            )
        )
    }
}

object ImportEntryDateController {

  def guardPredicate(importService: ImportService, importNumber: ImportNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(false) &&
      (request.userContext.isVatRegisteredOrganisation || request.userContext.isAgent) &&
      importService.numberHasValues(request.userAnswers, importNumber)
}
