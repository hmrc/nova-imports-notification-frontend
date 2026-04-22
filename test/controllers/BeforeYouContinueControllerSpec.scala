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

import base.SpecBase
import controllers.actions.*
import play.api.Application
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class BeforeYouContinueControllerSpec extends SpecBase {

  "BeforeYouContinueController" - {

    "onPageLoadIndividual" - {

      "must return OK and render the correct view" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.BeforeYouContinueController.onPageLoadIndividual().url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Before you continue")
        }
      }

      "must link the Continue button to the VehicleFromEu page" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.BeforeYouContinueController.onPageLoadIndividual().url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include(routes.VehicleFromEuController.onPageLoad(models.NormalMode).url)
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.BeforeYouContinueController.onPageLoadIndividual().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised when user is not a private individual" in {
        given application: Application = applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          privateIndividualBinding = classOf[UnauthorisedIdentifierAction]
        ).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.BeforeYouContinueController.onPageLoadIndividual().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }
  }
}
