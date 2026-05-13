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
import connectors.{CreateDraftError, NovaImportsBackendConnector}
import controllers.actions.*
import models.{AgentSelectedClient, DraftId, UserAnswers}
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
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class BeforeYouContinueControllerSpec extends SpecBase with MockitoSugar {

  private lazy val beforeYouContinueRoute = routes.BeforeYouContinueController.onPageLoad().url
  private lazy val onSubmitRoute          = routes.BeforeYouContinueController.onSubmit().url
  private lazy val vehicleFromEuTarget    = routes.VehicleFromEuController.onPageLoad(models.NormalMode).url

  private val spreadsheetSection = "Notifying for multiple vehicles"

  private def applicationWith(
    identifierAction: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers] = Some(emptyUserAnswers)
  ): Application =
    applicationWith(identifierAction, userAnswers, mock[NovaImportsBackendConnector], mock[SessionRepository])

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

      "renders the individual view when there is no existing session" in {
        given application: Application =
          applicationWith(classOf[FakeIdentifierAction], userAnswers = None)

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

      "when the backend call succeeds" - {

        "must save the draft id and redirect to VehicleFromEuController for a non-agent user" in {
          val draftId     = DraftId("DRAFT-001")
          val connector   = mock[NovaImportsBackendConnector]
          val sessionRepo = mock[SessionRepository]

          when(connector.createDraft(any())(any[HeaderCarrier])) thenReturn Future.successful(Right(draftId))
          when(sessionRepo.setPage(any(), any(), any())(any())) thenReturn Future.successful(true)

          given application: Application =
            applicationWith(classOf[FakeIdentifierAction], Some(emptyUserAnswers), connector, sessionRepo)

          running(application) {
            given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, onSubmitRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual vehicleFromEuTarget
            verify(connector).createDraft(org.mockito.ArgumentMatchers.eq(None))(any[HeaderCarrier])
            verify(sessionRepo).setPage(any(), any(), any())(any())
          }
        }

        "must pass the client VRN and redirect to VehicleFromEuController for an agent with a selected client" in {
          val draftId     = DraftId("DRAFT-002")
          val connector   = mock[NovaImportsBackendConnector]
          val sessionRepo = mock[SessionRepository]

          when(connector.createDraft(any())(any[HeaderCarrier])) thenReturn Future.successful(Right(draftId))
          when(sessionRepo.setPage(any(), any(), any())(any())) thenReturn Future.successful(true)

          given application: Application =
            applicationWith(classOf[FakeAgentIdentifierAction], Some(userAnswersWithClient), connector, sessionRepo)

          running(application) {
            given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, onSubmitRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual vehicleFromEuTarget
            verify(connector).createDraft(org.mockito.ArgumentMatchers.eq(Some(sampleClient.vrn)))(any[HeaderCarrier])
          }
        }

        "must pass None and redirect to VehicleFromEuController for an agent without a selected client" in {
          val draftId     = DraftId("DRAFT-003")
          val connector   = mock[NovaImportsBackendConnector]
          val sessionRepo = mock[SessionRepository]

          when(connector.createDraft(any())(any[HeaderCarrier])) thenReturn Future.successful(Right(draftId))
          when(sessionRepo.setPage(any(), any(), any())(any())) thenReturn Future.successful(true)

          given application: Application =
            applicationWith(classOf[FakeAgentIdentifierAction], Some(emptyUserAnswers), connector, sessionRepo)

          running(application) {
            given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, onSubmitRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual vehicleFromEuTarget
            verify(connector).createDraft(org.mockito.ArgumentMatchers.eq(None))(any[HeaderCarrier])
          }
        }
      }

      "when the backend call fails" - {

        "must redirect to JourneyRecovery and not write to the session" in {
          val connector   = mock[NovaImportsBackendConnector]
          val sessionRepo = mock[SessionRepository]

          when(connector.createDraft(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Left(CreateDraftError.UpstreamError(500, "boom"))))

          given application: Application =
            applicationWith(classOf[FakeIdentifierAction], Some(emptyUserAnswers), connector, sessionRepo)

          running(application) {
            given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, onSubmitRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            verify(sessionRepo, org.mockito.Mockito.never()).setPage(any(), any(), any())(any())
          }
        }
      }

      "creates a new session and redirects to VehicleFromEuController when there is no existing session" in {
        val draftId     = DraftId("DRAFT-NEW")
        val connector   = mock[NovaImportsBackendConnector]
        val sessionRepo = mock[SessionRepository]

        when(connector.createDraft(any())(any[HeaderCarrier])) thenReturn Future.successful(Right(draftId))
        when(sessionRepo.setPage(any(), any(), any())(any())) thenReturn Future.successful(true)

        given application: Application =
          applicationWith(classOf[FakeIdentifierAction], userAnswers = None, connector, sessionRepo)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, onSubmitRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual vehicleFromEuTarget
          verify(connector).createDraft(org.mockito.ArgumentMatchers.eq(None))(any[HeaderCarrier])
          verify(sessionRepo).setPage(any(), any(), any())(any())
        }
      }
    }
  }
}
