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
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class NoAuthorisedClientsControllerSpec extends SpecBase {

  private lazy val noAuthorisedClientsRoute =
    routes.NoAuthorisedClientsController.onPageLoad().url

  "NoAuthorisedClientsController" - {

    "onPageLoad" - {

      "must return OK and render the correct view for an authorised agent" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, noAuthorisedClientsRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("You have no authorised clients")
          contentAsString(result) must include("You do not have any clients authorised to use the NOVA service.")
          contentAsString(result) must include("What you need to do")
          contentAsString(result) must include("Return to home")
        }
      }

      "must include the HMRC online account URL from configuration" in {
        val customConfig = Map(
          "urls.hmrcOnlineAccountAuthorisationUrl" -> "https://test.example.com/hmrc-online"
        )

        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure(customConfig)
          .build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, noAuthorisedClientsRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("https://test.example.com/hmrc-online")
        }
      }

      "must include the Online Agent Authorisation URL from configuration" in {
        val customConfig = Map(
          "urls.onlineAgentAuthorisationUrl" -> "https://test.example.com/oaa"
        )

        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure(customConfig)
          .build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, noAuthorisedClientsRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("https://test.example.com/oaa")
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, noAuthorisedClientsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised when the user is not an agent" in {
        given application: Application = new GuiceApplicationBuilder()
          .overrides(
            bind[DataRequiredAction].to[DataRequiredActionImpl],
            bind[IdentifierAction].to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[UnauthorisedIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
            bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(emptyUserAnswers)))
          )
          .build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, noAuthorisedClientsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }
  }
}
