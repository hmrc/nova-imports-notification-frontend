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
import models.{AgentSelectedClient, UserAnswers}
import pages.AgentSelectedClientPage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class BeforeYouContinueControllerSpec extends SpecBase {

  private lazy val beforeYouContinueRoute = routes.BeforeYouContinueController.onPageLoad().url
  private lazy val continueTarget         = routes.VehicleFromEuController.onPageLoad(models.NormalMode).url

  private val spreadsheetSection = "Notifying for multiple vehicles"

  private def applicationWith(
    identifierAction: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers] = Some(emptyUserAnswers)
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )
      .build()

  private val sampleClient = AgentSelectedClient(vrn = "GB123456789", name = Some("Acme Ltd"))

  private val userAnswersWithClient =
    emptyUserAnswers.set(AgentSelectedClientPage, sampleClient).success.value

  "BeforeYouContinueController" - {

    "onPageLoad" - {

      "for a Private Individual renders BY1.0 without the multiple-vehicles section" in {
        given application: Application = applicationWith(classOf[FakeIdentifierAction])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, beforeYouContinueRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Before you continue")
          body must include(continueTarget)
          body must not include spreadsheetSection
        }
      }

      "for an Organisation renders BY2.0 with the multiple-vehicles section" in {
        given application: Application = applicationWith(classOf[FakeOrganisationIdentifierAction])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, beforeYouContinueRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include(spreadsheetSection)
          body must include("Find the spreadsheet (opens in new tab)")
          body must include(continueTarget)
        }
      }

      "for an Agent with a selected client renders BY2.0" in {
        given application: Application =
          applicationWith(classOf[FakeAgentIdentifierAction], userAnswers = Some(userAnswersWithClient))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, beforeYouContinueRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include(spreadsheetSection)
        }
      }

      "for an Agent without a selected client renders BY1.0 (treated as individual)" in {
        given application: Application = applicationWith(classOf[FakeAgentIdentifierAction])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, beforeYouContinueRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Before you continue")
          body must not include spreadsheetSection
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {
        given application: Application =
          applicationWith(classOf[FakeIdentifierAction], userAnswers = None)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, beforeYouContinueRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
