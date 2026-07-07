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
import controllers.routes
import models.UserAnswers
import models.requests.{DataRequest, OptionalDataRequest}
import org.scalatest.EitherValues
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRequiredActionSpec extends SpecBase with EitherValues {

  class Harness extends DataRequiredActionImpl {
    def callRefine[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]] = refine(request)
  }

  "Data Required Action" - {

    "when there is no session data" - {

      "must redirect to the unauthorised page so the user can go back to the landing page" in {

        val request = OptionalDataRequest(FakeRequest(), "id", AffinityGroup.Individual, Enrolments(Set.empty), None)
        val action  = new Harness()

        val result = action.callRefine(request).futureValue

        result.left.value mustBe Redirect(routes.UnauthorisedController.onPageLoad())
      }
    }

    "when there is session data" - {

      "must let the request through with the user answers" in {

        val answers = UserAnswers("id")
        val request = OptionalDataRequest(FakeRequest(), "id", AffinityGroup.Individual, Enrolments(Set.empty), Some(answers))
        val action  = new Harness()

        val result = action.callRefine(request).futureValue

        result.value.userAnswers mustBe answers
      }
    }
  }
}
