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

import base.SpecBase
import models.UserAnswers
import models.requests.DataRequest
import play.api.libs.json.Json
import play.api.mvc.{ActionBuilder, AnyContent, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}

import scala.concurrent.{ExecutionContext, Future}

class GuardActionSpec extends SpecBase {

  private given ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  private val guardAction = new GuardAction()

  private def fakeActionBuilder(userAnswers: UserAnswers): ActionBuilder[DataRequest, AnyContent] =
    new ActionBuilder[DataRequest, AnyContent] {
      override def parser: play.api.mvc.BodyParser[AnyContent]  = play.api.mvc.BodyParsers.utils.ignore(play.api.mvc.AnyContentAsEmpty)
      override protected def executionContext: ExecutionContext = ec
      override def invokeBlock[A](
        request: play.api.mvc.Request[A],
        block: DataRequest[A] => Future[play.api.mvc.Result]
      ): Future[play.api.mvc.Result] =
        block(DataRequest(request, userAnswersId, AffinityGroup.Individual, Enrolments(Set.empty), userAnswers))
    }

  "GuardAction" - {

    "must allow request through when predicate returns true" in {
      val action = fakeActionBuilder(emptyUserAnswers)
        .andThen(guardAction(_ => true))
        .apply((_: DataRequest[AnyContent]) => Results.Ok)
      val result = action(FakeRequest())

      status(result) mustEqual OK
    }

    "must redirect to JourneyRecoveryController when predicate returns false" in {
      val action = fakeActionBuilder(emptyUserAnswers)
        .andThen(guardAction(_ => false))
        .apply((_: DataRequest[AnyContent]) => Results.Ok)
      val result = action(FakeRequest())

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
    }

    "must allow request through when user answers contain the required data" in {
      val answersWithData = emptyUserAnswers.copy(data = Json.obj("someKey" -> "someValue"))

      val action = fakeActionBuilder(answersWithData)
        .andThen(guardAction(_.data.keys.contains("someKey")))
        .apply((_: DataRequest[AnyContent]) => Results.Ok)
      val result = action(FakeRequest())

      status(result) mustEqual OK
    }

    "must redirect to JourneyRecoveryController when user answers do not contain the required data" in {
      val action = fakeActionBuilder(emptyUserAnswers)
        .andThen(guardAction(_.data.keys.contains("someKey")))
        .apply((_: DataRequest[AnyContent]) => Results.Ok)
      val result = action(FakeRequest())

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
    }
  }
}
