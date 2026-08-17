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
import controllers.utils.IsDraftIdDefined
import models.NormalMode
import models.requests.DataRequest
import pages.sections.notifieraddress.{AddressJourneyIdPage, AddressPage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.YourAddressCheckYourAnswersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourAddressCheckYourAnswersController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  actions: Actions,
  view: YourAddressCheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import YourAddressCheckYourAnswersController.*

  def onPageLoad: Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
    Ok(view(request.userAnswers))
  }

  def onChangeAddress: Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
    for {
      cleared <- Future.fromTry(request.userAnswers.remove(AddressPage).flatMap(_.remove(AddressJourneyIdPage)))
      _       <- sessionRepository.set(cleared)
    } yield Redirect(routes.IsYourAddressInTheUkController.onPageLoad(NormalMode))
  }

  def onSubmit: Action[AnyContent] = actions.authAndGetDataWithUserTypeGuard(guardPredicate) { _ =>
    Redirect(routes.NotificationTaskListController.onPageLoad())
  }
}

object YourAddressCheckYourAnswersController {

  def guardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) && !request.userContext.isAgent && request.userAnswers.get(AddressPage).isDefined
}
