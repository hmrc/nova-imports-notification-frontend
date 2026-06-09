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
import connectors.NovaImportsBackendConnector
import controllers.actions.*
import models.{AgentSelectedClient, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.AgentSelectedClientPage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository

import scala.concurrent.Future

class BeforeYouContinueControllerSpec extends SpecBase with MockitoSugar {

  private lazy val beforeYouContinueRoute = routes.BeforeYouContinueController.onPageLoad().url
  private lazy val onSubmitRoute          = routes.BeforeYouContinueController.onSubmit().url
  private lazy val vehicleFromEuTarget    = routes.VehicleFromEuController.onPageLoad(models.NormalMode).url

  private val spreadsheetSection = "Notifying for multiple vehicles"

  private val sampleClient = AgentSelectedClient(vrn = "GB123456789", name = Some("Acme Ltd"))

  private val userAnswersWithClient =
    emptyUserAnswers.set(AgentSelectedClientPage, sampleClient).success.value

  private def mockSessionRepository: SessionRepository = {
    val sessionRepo = mock[SessionRepository]

    when(sessionRepo.setPage(any(), any(), any())(any()))
      .thenReturn(Future.successful(emptyUserAnswers))

    sessionRepo
  }

  private def applicationWith(
    identifierAction: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers] = Some(emptyUserAnswers)
  ): Application =
    applicationWith(identifierAction, userAnswers, mock[NovaImportsBackendConnector], mockSessionRepository)

  private def applicationWith(
    identifierAction: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers],
    connector: NovaImportsBackendConnector,
    sessionRepo: SessionRepository
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[NovaImportsBackendConnector].toInstance(connector),
        bind[SessionRepository].toInstance(sessionRepo)
      )
      .build()

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
          body must include(onSubmitRoute)
          body must not include spreadsheetSection
        }
      }

      "for an Organisation with a VAT enrolment renders BY2.0 with the multiple-vehicles section" in {
        given application: Application = applicationWith(classOf[FakeVatTraderIdentifierAction])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, beforeYouContinueRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include(spreadsheetSection)
          body must include("Find the spreadsheet (opens in new tab)")
          body must include(onSubmitRoute)
        }
      }

      "for an Organisation without a VAT enrolment renders BY1.0" in {
        given application: Application = applicationWith(classOf[FakeOrganisationIdentifierAction])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, beforeYouContinueRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Before you continue")
          body must not include spreadsheetSection
        }
      }

      "for an Agent with a selected client renders BY2.0" in {
        given application: Application =
          applicationWith(classOf[FakeAgentIdentifierAction], Some(userAnswersWithClient))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, beforeYouContinueRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include(spreadsheetSection)
        }
      }

      "for an Agent without a selected client renders BY1.0" in {
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

      "renders the individual view when there is no existing session" in {
        given application: Application =
          applicationWith(classOf[FakeIdentifierAction], None)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, beforeYouContinueRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Before you continue")
          body must not include spreadsheetSection
        }
      }
    }

    "onSubmit" - {

      "must save acknowledgement and redirect to VehicleFromEuController for a non-agent user" in {
        val sessionRepo = mockSessionRepository

        given application: Application =
          applicationWith(classOf[FakeIdentifierAction], Some(emptyUserAnswers), mock[NovaImportsBackendConnector], sessionRepo)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, onSubmitRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual vehicleFromEuTarget
          verify(sessionRepo).setPage(any(), any(), any())(any())
        }
      }

      "must save acknowledgement and redirect to VehicleFromEuController for an agent with a selected client" in {
        val sessionRepo = mockSessionRepository

        given application: Application =
          applicationWith(classOf[FakeAgentIdentifierAction], Some(userAnswersWithClient), mock[NovaImportsBackendConnector], sessionRepo)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, onSubmitRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual vehicleFromEuTarget
          verify(sessionRepo).setPage(any(), any(), any())(any())
        }
      }

      "must save acknowledgement and redirect to VehicleFromEuController for an agent without a selected client" in {
        val sessionRepo = mockSessionRepository

        given application: Application =
          applicationWith(classOf[FakeAgentIdentifierAction], Some(emptyUserAnswers), mock[NovaImportsBackendConnector], sessionRepo)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, onSubmitRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual vehicleFromEuTarget
          verify(sessionRepo).setPage(any(), any(), any())(any())
        }
      }

      "creates a new session and redirects to VehicleFromEuController when there is no existing session" in {
        val sessionRepo = mockSessionRepository

        given application: Application =
          applicationWith(classOf[FakeIdentifierAction], None, mock[NovaImportsBackendConnector], sessionRepo)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, onSubmitRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual vehicleFromEuTarget
          verify(sessionRepo).setPage(any(), any(), any())(any())
        }
      }
    }
  }
}
