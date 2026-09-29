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
import connectors.{GetClientListError, NovaImportsBackendConnector}
import controllers.actions.*
import controllers.{clientselection, routes}
import models.{ClientList, ClientListQuery, ClientSummary}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ViewClientsControllerSpec extends SpecBase with MockitoSugar {

  private def clientsRoute(searchBy: Option[String] = None, search: Option[String] = None, page: Int = 1): String =
    clientselection.routes.ViewClientsController.onPageLoad(searchBy, search, page).url

  private lazy val submitRoute = clientselection.routes.ViewClientsController.onSubmit().url

  private def client(n: Int) = ClientSummary(s"Client $n", f"7$n%08d")

  private def listOf(clients: Seq[ClientSummary], total: Int) = ClientList(clients, total, Seq("C"))

  private def connectorReturning(result: Either[GetClientListError, ClientList]): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getClientList(any[ClientListQuery])(using any[HeaderCarrier])).thenReturn(Future.successful(result))
    connector
  }

  private def application(connector: NovaImportsBackendConnector): Application =
    applicationBuilderWithAgentAsNovaAgent(userAnswers = Some(emptyUserAnswers))
      .overrides(bind[NovaImportsBackendConnector].toInstance(connector))
      .configure("client-list.page-size" -> 10, "urls.technicalSupportUrl" -> "http://support")
      .build()

  "ViewClientsController" - {

    "onPageLoad" - {

      "must be served from /clients" in {
        given app: Application = application(connectorReturning(Right(listOf(Nil, 0))))

        running(app) {
          clientsRoute() mustEqual "/nova-imports/clients"
          clientsRoute(Some("name"), Some("Client"), 2) mustEqual "/nova-imports/clients?searchBy=name&search=Client&page=2"
        }
      }

      "must fetch the first page of all clients and render them" in {
        val clients            = (1 to 10).map(client)
        val connector          = connectorReturning(Right(listOf(clients, 30)))
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, clientsRoute())

          val result = route(app, request).value
          val html   = contentAsString(result)

          status(result) mustEqual OK
          html must include("Client list")
          html must include("Showing <strong>1</strong> to <strong>10</strong> of <strong>30</strong> records")
          html must include("Client 1")
          html must include("700000001")
          html must include(clientselection.routes.SelectClientController.select("700000001").url)
          html must include(clientsRoute(page = 2))
          html must include("http://support")
          html must include(routes.LandingPageController.onPageLoad().url)
          verify(connector).getClientList(eqTo(ClientListQuery(start = 0, count = 10)))(using any[HeaderCarrier])
        }
      }

      "must fetch the requested page" in {
        val connector          = connectorReturning(Right(listOf((11 to 20).map(client), 30)))
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, clientsRoute(page = 2))

          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Showing <strong>11</strong> to <strong>20</strong> of <strong>30</strong> records")
          verify(connector).getClientList(eqTo(ClientListQuery(start = 10, count = 10)))(using any[HeaderCarrier])
        }
      }

      "must search by name and keep the search in the form" in {
        val connector          = connectorReturning(Right(listOf(Seq(client(1)), 1)))
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, clientsRoute(Some("name"), Some("Client 1")))

          val result = route(app, request).value
          val html   = contentAsString(result)

          status(result) mustEqual OK
          html must include("""value="Client 1"""")
          html must include regex """<option value="name"\s+selected"""
          verify(connector).getClientList(eqTo(ClientListQuery(name = Some("Client 1"), start = 0, count = 10)))(using any[HeaderCarrier])
        }
      }

      "must search by VRN" in {
        val connector          = connectorReturning(Right(listOf(Seq(client(1)), 1)))
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, clientsRoute(Some("vrn"), Some("700000001")))

          val result = route(app, request).value

          status(result) mustEqual OK
          verify(connector).getClientList(eqTo(ClientListQuery(vrn = Some("700000001"), start = 0, count = 10)))(using any[HeaderCarrier])
        }
      }

      "must ignore an unknown search type and show all clients" in {
        val connector          = connectorReturning(Right(listOf(Seq(client(1)), 1)))
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, clientsRoute(Some("email"), Some("x")))

          val result = route(app, request).value

          status(result) mustEqual OK
          verify(connector).getClientList(eqTo(ClientListQuery(start = 0, count = 10)))(using any[HeaderCarrier])
        }
      }

      "must show the no clients found message when a search returns nothing" in {
        given app: Application = application(connectorReturning(Right(listOf(Nil, 0))))

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, clientsRoute(Some("name"), Some("Nobody")))

          val result = route(app, request).value
          val html   = contentAsString(result)

          status(result) mustEqual OK
          html must include("No clients found")
          html must include("Check the client name or VAT registration number and try again.")
          html must not include "govuk-table"
        }
      }

      "must redirect to the no authorised clients page when the agent has no clients" in {
        given app: Application = application(connectorReturning(Right(listOf(Nil, 0))))

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, clientsRoute())

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientselection.routes.NoAuthorisedClientsController.onPageLoad().url
        }
      }

      "must redirect to the last page when the requested page is past the end" in {
        given app: Application = application(connectorReturning(Right(listOf(Nil, 30))))

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, clientsRoute(Some("name"), Some("Client"), 9))

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientsRoute(Some("name"), Some("Client"), 3)
        }
      }

      "must treat a page below one as the first page" in {
        val connector          = connectorReturning(Right(listOf(Seq(client(1)), 1)))
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, clientsRoute(page = 0))

          val result = route(app, request).value

          status(result) mustEqual OK
          verify(connector).getClientList(eqTo(ClientListQuery(start = 0, count = 10)))(using any[HeaderCarrier])
        }
      }

      "must redirect to the could not retrieve page when the list cannot be fetched" in {
        given app: Application = application(connectorReturning(Left(GetClientListError.UpstreamError(502, "boom"))))

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, clientsRoute())

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientselection.routes.CouldNotRetrieveClientListController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised when the user is not an agent" in {
        val connector = connectorReturning(Right(listOf(Seq(client(1)), 1)))

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
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, clientsRoute())

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
          verify(connector, never).getClientList(any[ClientListQuery])(using any[HeaderCarrier])
        }
      }
    }

    "onSubmit" - {

      "must redirect to the search results for a valid search" in {
        val connector          = connectorReturning(Right(listOf(Nil, 0)))
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, submitRoute).withFormUrlEncodedBody("searchBy" -> "vrn", "search" -> " 700000001 ")

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientsRoute(Some("vrn"), Some("700000001"))
          verify(connector, never).getClientList(any[ClientListQuery])(using any[HeaderCarrier])
        }
      }

      "must return a Bad Request with the combined error when nothing is entered" in {
        given app: Application = application(connectorReturning(Right(listOf(Nil, 0))))

        running(app) {
          given request: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, submitRoute).withFormUrlEncodedBody("searchBy" -> "", "search" -> "")

          val result = route(app, request).value
          val html   = contentAsString(result)

          status(result) mustEqual BAD_REQUEST
          html must include("Select what you want to search by, and enter a client name or VAT registration number")
          html must include("Select what you want to search by")
          html must include("Enter a client name or VAT registration number")
          html must not include "govuk-table"
          html must not include "No clients found"
        }
      }

      "must return a Bad Request asking for the client name when searching by name with no term" in {
        given app: Application = application(connectorReturning(Right(listOf(Nil, 0))))

        running(app) {
          given request: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, submitRoute).withFormUrlEncodedBody("searchBy" -> "name", "search" -> "")

          val result = route(app, request).value
          val html   = contentAsString(result)

          status(result) mustEqual BAD_REQUEST
          html must include("Enter the client’s name")
          html must not include "Select what you want to search by, and enter"
        }
      }

      "must return a Bad Request asking for the VRN when searching by VRN with no term" in {
        given app: Application = application(connectorReturning(Right(listOf(Nil, 0))))

        running(app) {
          given request: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, submitRoute).withFormUrlEncodedBody("searchBy" -> "vrn", "search" -> "")

          val result = route(app, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include("Enter the client’s VAT registration number")
        }
      }

      "must return a Bad Request when a term is entered without a search type" in {
        given app: Application = application(connectorReturning(Right(listOf(Nil, 0))))

        running(app) {
          given request: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, submitRoute).withFormUrlEncodedBody("searchBy" -> "", "search" -> "Client")

          val result = route(app, request).value
          val html   = contentAsString(result)

          status(result) mustEqual BAD_REQUEST
          html must include("Select what you want to search by")
          html must not include "Select what you want to search by, and enter"
        }
      }
    }
  }
}
