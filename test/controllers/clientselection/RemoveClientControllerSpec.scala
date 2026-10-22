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
import connectors.{GetClientListError, NovaImportsBackendConnector, RemoveClientError}
import controllers.actions.*
import controllers.{clientselection, routes}
import forms.RemoveClientFormProvider
import models.{ClientList, ClientListQuery, ClientSummary}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import views.html.RemoveClientView

import scala.concurrent.Future

class RemoveClientControllerSpec extends SpecBase with MockitoSugar {

  private val vrn    = "700000001"
  private val client = ClientSummary("Client Co", vrn)
  private val other  = ClientSummary("Other Co", "700000002")

  private val form = new RemoveClientFormProvider()()

  private def removeRoute = clientselection.routes.RemoveClientController.onPageLoad(vrn).url
  private def clientsUrl  = clientselection.routes.ViewClientsController.onPageLoad(None, None, 1).url

  private val clientLookup = ClientListQuery(vrn = Some(vrn))
  private val otherClients = ClientListQuery(start = 0, count = 2)

  private def listOf(clients: ClientSummary*) = ClientList(clients, clients.size, Seq("C"))

  private def connector(
    lookup: Either[GetClientListError, ClientList] = Right(listOf(client)),
    remaining: Either[GetClientListError, ClientList] = Right(listOf(client, other)),
    removal: Either[RemoveClientError, Unit] = Right(())
  ): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getClientList(eqTo(clientLookup))(using any[HeaderCarrier])).thenReturn(Future.successful(lookup))
    when(connector.getClientList(eqTo(otherClients))(using any[HeaderCarrier])).thenReturn(Future.successful(remaining))
    when(connector.removeClient(any[String])(using any[HeaderCarrier])).thenReturn(Future.successful(removal))
    connector
  }

  private def application(connector: NovaImportsBackendConnector): Application =
    applicationBuilderWithAgentAsNovaAgent(userAnswers = Some(emptyUserAnswers))
      .overrides(bind[NovaImportsBackendConnector].toInstance(connector))
      .build()

  private def nonAgentApplication(connector: NovaImportsBackendConnector): Application =
    new GuiceApplicationBuilder()
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

  private def submit(app: Application, answer: String) =
    route(app, FakeRequest(POST, removeRoute).withFormUrlEncodedBody("value" -> answer)).value

  "RemoveClientController" - {

    "onPageLoad" - {

      "must be served from /remove-client/:vrn" in {
        running(application(connector())) {
          removeRoute mustEqual s"/nova-imports/remove-client/$vrn"
        }
      }

      "must show the client's name and VRN" in {
        val app = application(connector())

        running(app) {
          val request = FakeRequest(GET, removeRoute)
          val result  = route(app, request).value
          val view    = app.injector.instanceOf[RemoveClientView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, client)(request, messages(app)).toString
        }
      }

      "must redirect to Unauthorised when the client is not on the agent's list" in {
        val app = application(connector(lookup = Right(listOf())))

        running(app) {
          val result = route(app, FakeRequest(GET, removeRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised when the search only returns a client with a similar VRN" in {
        val app = application(connector(lookup = Right(listOf(ClientSummary("Client Co", s"${vrn}9")))))

        running(app) {
          val result = route(app, FakeRequest(GET, removeRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to journey recovery when the client list cannot be fetched" in {
        val app = application(connector(lookup = Left(GetClientListError.UpstreamError(500, "boom"))))

        running(app) {
          val result = route(app, FakeRequest(GET, removeRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to journey recovery when the client list call fails to connect" in {
        val backend = connector()
        when(backend.getClientList(eqTo(clientLookup))(using any[HeaderCarrier])).thenReturn(Future.failed(new RuntimeException("boom")))
        val app = application(backend)

        running(app) {
          val result = route(app, FakeRequest(GET, removeRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised when the user is not an agent" in {
        val backend = connector()
        val app     = nonAgentApplication(backend)

        running(app) {
          val result = route(app, FakeRequest(GET, removeRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
          verify(backend, never).getClientList(any[ClientListQuery])(using any[HeaderCarrier])
        }
      }
    }

    "onSubmit" - {

      "must remove the client and redirect to the client list when the agent has other clients" in {
        val backend = connector()
        val app     = application(backend)

        running(app) {
          val result = submit(app, "true")

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientsUrl
          verify(backend).removeClient(eqTo(vrn))(using any[HeaderCarrier])
          verify(backend).getClientList(eqTo(otherClients))(using any[HeaderCarrier])
        }
      }

      "must deselect the client and go to the landing page when the agent has no other clients" in {
        val app = application(connector(remaining = Right(listOf(client))))

        running(app) {
          val result = submit(app, "true")

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.LandingPageController.deselectClient().url
        }
      }

      "must deselect the client and go to the landing page when the list is already empty" in {
        val app = application(connector(remaining = Right(listOf())))

        running(app) {
          val result = submit(app, "true")

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.LandingPageController.deselectClient().url
        }
      }

      "must redirect to the client list when the remaining clients cannot be fetched" in {
        val app = application(connector(remaining = Left(GetClientListError.UpstreamError(500, "boom"))))

        running(app) {
          val result = submit(app, "true")

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientsUrl
        }
      }

      "must redirect to journey recovery when the client cannot be removed" in {
        val app = application(connector(removal = Left(RemoveClientError.UpstreamError(502, "boom"))))

        running(app) {
          val result = submit(app, "true")

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to journey recovery when the remove call fails to connect" in {
        val backend = connector()
        when(backend.removeClient(any[String])(using any[HeaderCarrier])).thenReturn(Future.failed(new RuntimeException("boom")))
        val app = application(backend)

        running(app) {
          val result = submit(app, "true")

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised without removing the client when the user is not an agent" in {
        val backend = connector()
        val app     = nonAgentApplication(backend)

        running(app) {
          val result = submit(app, "true")

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
          verify(backend, never).removeClient(any[String])(using any[HeaderCarrier])
        }
      }

      "must redirect to the client list without removing the client when No is selected" in {
        val backend = connector()
        val app     = application(backend)

        running(app) {
          val result = submit(app, "false")

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientsUrl
          verify(backend, never).removeClient(any[String])(using any[HeaderCarrier])
        }
      }

      "must return a Bad Request with the client details and the required error when nothing is selected" in {
        val backend = connector()
        val app     = application(backend)

        running(app) {
          val request = FakeRequest(POST, removeRoute).withFormUrlEncodedBody("value" -> "")
          val result  = route(app, request).value
          val view    = app.injector.instanceOf[RemoveClientView]

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(form.bind(Map("value" -> "")), client)(request, messages(app)).toString
          verify(backend, never).removeClient(any[String])(using any[HeaderCarrier])
        }
      }
    }
  }
}
