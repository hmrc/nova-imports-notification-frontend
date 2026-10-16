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
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class DownloadClientListControllerSpec extends SpecBase with MockitoSugar {

  private lazy val downloadRoute = clientselection.routes.DownloadClientListController.download().url

  private def connectorReturning(result: Either[GetClientListError, ClientList]): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getClientList(any[ClientListQuery])(using any[HeaderCarrier])).thenReturn(Future.successful(result))
    connector
  }

  private def application(connector: NovaImportsBackendConnector): Application =
    applicationBuilderWithAgentAsNovaAgent(userAnswers = Some(emptyUserAnswers))
      .overrides(bind[NovaImportsBackendConnector].toInstance(connector))
      .build()

  "DownloadClientListController" - {

    "must be served from /clients/download" in {
      given app: Application = application(connectorReturning(Right(ClientList(Nil, 0, Nil))))

      running(app) {
        downloadRoute mustEqual "/nova-imports/clients/download"
      }
    }

    "must fetch every client and return them as a CSV attachment named clients.csv" in {
      val clients            = List(ClientSummary("Client 1", "700000001"), ClientSummary("Client 2", "700000002"))
      val connector          = connectorReturning(Right(ClientList(clients, 2, List("C"))))
      given app: Application = application(connector)

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual OK
        contentType(result).value mustEqual "text/csv"
        charset(result).value mustEqual "utf-8"
        header(CONTENT_DISPOSITION, result).value mustEqual "attachment; filename=\"clients.csv\""
        contentAsString(result) mustEqual "Client name,Client reference\r\nClient 1,700000001\r\nClient 2,700000002\r\n"
        verify(connector).getClientList(eqTo(ClientListQuery()))(using any[HeaderCarrier])
      }
    }

    "must redirect to the journey recovery page when the list cannot be fetched" in {
      given app: Application = application(connectorReturning(Left(GetClientListError.UpstreamError(502, "boom"))))

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the journey recovery page when the backend cannot be reached" in {
      val connector = mock[NovaImportsBackendConnector]
      when(connector.getClientList(any[ClientListQuery])(using any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("connection refused")))
      given app: Application = application(connector)

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when the user is not an agent" in {
      val connector = connectorReturning(Right(ClientList(Nil, 0, Nil)))

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
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        verify(connector, never).getClientList(any[ClientListQuery])(using any[HeaderCarrier])
      }
    }
  }
}
