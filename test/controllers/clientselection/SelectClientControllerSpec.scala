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

package controllers.clientselection

import base.SpecBase
import com.google.inject.name.Names
import connectors.{GetNotificationSummaryError, NovaImportsBackendConnector}
import controllers.actions.*
import controllers.{clientselection, routes}
import models.{AgentSelectedClient, NotificationSummary}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
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

class SelectClientControllerSpec extends SpecBase with MockitoSugar {

  private val vrn = "700000001"

  private lazy val selectRoute = clientselection.routes.SelectClientController.select(vrn).url

  private val clientSummary = NotificationSummary.AgentWithClient(
    agentName = Some("Agent Co"),
    clientTraderName = Some("Client Co"),
    clientVrn = vrn,
    clientHasDraftNotifications = false,
    clientIsDeregistered = false
  )

  private def connectorReturning(result: Either[GetNotificationSummaryError, NotificationSummary]): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getNotificationSummary(any[Option[String]])(using any[HeaderCarrier])).thenReturn(Future.successful(result))
    connector
  }

  private def sessionRepository: SessionRepository = {
    val repo = mock[SessionRepository]
    when(repo.setPage(any(), any(), any())(any())).thenReturn(Future.successful(emptyUserAnswers))
    repo
  }

  private def application(connector: NovaImportsBackendConnector, repo: SessionRepository): Application =
    applicationBuilderWithAgentAsNovaAgent(userAnswers = Some(emptyUserAnswers))
      .overrides(
        bind[NovaImportsBackendConnector].toInstance(connector),
        bind[SessionRepository].toInstance(repo)
      )
      .build()

  "SelectClientController" - {

    "select" - {

      "must be served from /clients/select/:vrn" in {
        given app: Application = application(connectorReturning(Right(clientSummary)), sessionRepository)

        running(app) {
          selectRoute mustEqual s"/nova-imports/clients/select/$vrn"
        }
      }

      "must store the client and redirect to the landing page when the agent has the client" in {
        val connector          = connectorReturning(Right(clientSummary))
        val repo               = sessionRepository
        given app: Application = application(connector, repo)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, selectRoute)

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url
          verify(connector).getNotificationSummary(eqTo(Some(vrn)))(using any[HeaderCarrier])
          verify(repo).setPage(any(), eqTo(AgentSelectedClientPage), eqTo(AgentSelectedClient(vrn, Some("Client Co"))))(any())
        }
      }

      "must redirect to Unauthorised without storing anything when the agent does not have the client" in {
        val repo               = sessionRepository
        given app: Application = application(connectorReturning(Left(GetNotificationSummaryError.ClientNotFound)), repo)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, selectRoute)

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
          verify(repo, never).setPage(any(), any(), any())(any())
        }
      }

      "must redirect to journey recovery when the client check fails" in {
        val repo               = sessionRepository
        given app: Application = application(connectorReturning(Left(GetNotificationSummaryError.UpstreamError(500, "boom"))), repo)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, selectRoute)

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(repo, never).setPage(any(), any(), any())(any())
        }
      }

      "must redirect to journey recovery when the summary is not for an agent with a client" in {
        val repo    = sessionRepository
        val summary = NotificationSummary.AgentWithoutClient(agentName = Some("Agent Co"), hasDraftNotifications = false)

        given app: Application = application(connectorReturning(Right(summary)), repo)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, selectRoute)

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(repo, never).setPage(any(), any(), any())(any())
        }
      }

      "must redirect to Unauthorised when the user is not an agent" in {
        val connector = connectorReturning(Right(clientSummary))

        given app: Application = new GuiceApplicationBuilder()
          .overrides(
            bind[DataRequiredAction].to[DataRequiredActionImpl],
            bind[IdentifierAction].to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[UnauthorisedIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
            bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(emptyUserAnswers))),
            bind[NovaImportsBackendConnector].toInstance(connector)
          )
          .build()

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, selectRoute)

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
          verify(connector, never).getNotificationSummary(any[Option[String]])(using any[HeaderCarrier])
        }
      }
    }
  }
}
