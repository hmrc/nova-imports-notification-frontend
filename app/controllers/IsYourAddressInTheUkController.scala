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
import forms.IsYourAddressInTheUkFormProvider
import models.Mode
import models.requests.DataRequest
import pages.IsYourAddressInTheUkPage
import play.api.Logging
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AddressLookupService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.IsYourAddressInTheUkView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IsYourAddressInTheUkController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  actions: Actions,
  formProvider: IsYourAddressInTheUkFormProvider,
  view: IsYourAddressInTheUkView,
  addressLookupService: AddressLookupService
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  val form: Form[Boolean] = formProvider()

  private val dataGuard: DataRequest[?] => Boolean = !_.userContext.isAgent

  def onPageLoad(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(dataGuard) { implicit request =>
    Ok(view(form.withDefault(request.userAnswers.get(IsYourAddressInTheUkPage)), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(dataGuard).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        ukMode =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(IsYourAddressInTheUkPage, ukMode))
            _              <- sessionRepository.set(updatedAnswers)
            callbackUrl = routes.AddressLookupCallbackController.callback(None).absoluteURL()
            initResult <- addressLookupService.initJourney(ukMode, callbackUrl)
          } yield initResult match {
            case Right(journeyUrl) =>
              Redirect(journeyUrl)
            case Left(error) =>
              logger.warn(s"Failed to init ALF journey (ukMode=$ukMode): $error")
              Redirect(routes.JourneyRecoveryController.onPageLoad())
          }
      )
  }
}
