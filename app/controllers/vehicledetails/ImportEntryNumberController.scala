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
import controllers.vehicledetails.ImportEntryNumberController.*
import forms.ImportEntryNumberFormProvider
import models.requests.DataRequest
import models.{ImportNumber, Mode, NovaUserType}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.ImportEntryNumberPage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.ImportService
import views.html.ImportEntryNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ImportEntryNumberController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: ImportEntryNumberFormProvider,
  view: ImportEntryNumberView,
  importService: ImportService
)(implicit ec: ExecutionContext)
    extends BaseController {

  val form: Form[String] = formProvider()

  def onPageLoad(importNumber: ImportNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(importService, importNumber)) { implicit request =>
      Ok(view(form.withDefault(request.userAnswers.get(ImportEntryNumberPage(importNumber))), importNumber, mode))
    }

  def onSubmit(importNumber: ImportNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(importService, importNumber)).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, importNumber, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(ImportEntryNumberPage(importNumber), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                ImportEntryNumberPage(importNumber),
                mode,
                updatedAnswers,
                NovaUserType.from(request.affinityGroup, request.enrolments)
              )
            )
        )
    }
}

object ImportEntryNumberController {

  // the first import question, so the collection is still empty at this point
  def guardPredicate(importService: ImportService, importNumber: ImportNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(false) &&
      (request.userContext.isVatRegisteredOrganisation || request.userContext.isAgent) &&
      importService.numberExists(request.userAnswers, importNumber)
}
