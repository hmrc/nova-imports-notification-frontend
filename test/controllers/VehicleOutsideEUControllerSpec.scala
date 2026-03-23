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
import play.api.Application
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class VehicleOutsideEUControllerSpec extends SpecBase {

  "VehicleOutsideEUController" - {

    "onPageLoad" - {

      "must return OK and render the correct view" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.VehicleOutsideEUController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("If you’ve brought a vehicle into Northern Ireland from outside the EU")
        }
      }

      "must include the correct importing vehicles URL from configuration" in {
        val customConfig = Map(
          "urls.importingVehiclesIntoTheUKUrl" -> "https://test.example.com/importing-vehicles"
        )

        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure(customConfig)
          .build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.VehicleOutsideEUController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("https://test.example.com/importing-vehicles")
        }
      }

      "must include the correct EU countries URL from configuration" in {
        val customConfig = Map(
          "urls.countriesInTheEUUrl" -> "https://test.example.com/eu-countries"
        )

        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure(customConfig)
          .build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.VehicleOutsideEUController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("https://test.example.com/eu-countries")
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.VehicleOutsideEUController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
