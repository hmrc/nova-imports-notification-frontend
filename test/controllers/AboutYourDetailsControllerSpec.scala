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
import com.google.inject.name.Names
import controllers.actions.*
import navigation.{FakeNavigator, Navigator}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleBusinessUsePage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class AboutYourDetailsControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  private lazy val aboutYourDetailsRoute = routes.AboutYourDetailsController.onPageLoad().url

  // guard requires OQ1.0 to have been answered
  private val answersWithVehicleBusinessUseAnswered = emptyUserAnswers.set(VehicleBusinessUsePage, true).success.value

  "AboutYourDetailsController" - {

    "onPageLoad" - {

      "must return OK and render the correct view when the guard passes" in {
        given application: Application = applicationBuilder(userAnswers = Some(answersWithVehicleBusinessUseAnswered)).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, aboutYourDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("About your details")
          contentAsString(result) must include("You need to provide your contact details.")
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, aboutYourDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery if OQ1.0 has not been answered" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, aboutYourDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to unauthorised page when the user is not a VAT-registered organisation" in {
        given application: Application = new GuiceApplicationBuilder()
          .overrides(
            bind[DataRequiredAction].to[DataRequiredActionImpl],
            bind[IdentifierAction].to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[UnauthorisedIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
            bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(answersWithVehicleBusinessUseAnswered)))
          )
          .build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, aboutYourDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must redirect to the next page when the guard passes" in {
        given application: Application = applicationBuilder(userAnswers = Some(answersWithVehicleBusinessUseAnswered))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, aboutYourDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must redirect to Journey Recovery if no existing userAnswers are found" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, aboutYourDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery if OQ1.0 has not been answered" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, aboutYourDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
