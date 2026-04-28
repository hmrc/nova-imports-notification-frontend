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

package controllers.actions

import com.google.inject.{Inject, Singleton}
import controllers.routes
import models.{UserAnswers, UserContext}
import models.requests.DataRequest
import play.api.mvc.{ActionFilter, Call, Result, Results}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GuardAction @Inject() ()(using ec: ExecutionContext) {

  def apply(predicate: UserAnswers => Boolean): ActionFilter[DataRequest] =
    filterWith(req => predicate(req.userAnswers), Results.Redirect(routes.JourneyRecoveryController.onPageLoad()))

  def forUserContext(predicate: UserContext => Boolean): ActionFilter[DataRequest] =
    filterWith(req => predicate(req.userContext), Results.Redirect(routes.JourneyRecoveryController.onPageLoad()))

  def forUserContext(predicate: UserContext => Boolean, onFailure: Call): ActionFilter[DataRequest] =
    filterWith(req => predicate(req.userContext), Results.Redirect(onFailure))

  private def filterWith(test: DataRequest[?] => Boolean, failureResult: Result): ActionFilter[DataRequest] =
    new ActionFilter[DataRequest] {
      override def executionContext: ExecutionContext = ec

      override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] =
        Future.successful {
          if test(request) then None
          else Some(failureResult)
        }
    }
}
