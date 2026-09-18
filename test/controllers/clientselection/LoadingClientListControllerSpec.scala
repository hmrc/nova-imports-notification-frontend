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
import connectors.{GetClientListStatusError, NovaImportsBackendConnector, RefreshClientListError}
import controllers.actions.*
import controllers.{clientselection, routes}
import models.ClientListStatus
import models.responses.ClientListRefresh
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import org.mockito.ArgumentMatchers.any

import scala.concurrent.Future

class LoadingClientListControllerSpec extends SpecBase with MockitoSugar {

  private def loadingRoute(interval: Option[Int] = None, attempt: Int = 0): String =
    clientselection.routes.LoadingClientListController.onPageLoad(interval, attempt).url

  private def metaRefresh(seconds: Int, interval: Int, attempt: Int): String =
    s"""<meta http-equiv="refresh" content="$seconds;url=${loadingRoute(Some(interval), attempt).replace("&", "&amp;")}">"""

  private def connectorReturning(
    status: Either[GetClientListStatusError, ClientListStatus],
    refresh: Either[RefreshClientListError, ClientListRefresh] = Right(ClientListRefresh(success = true, browserInterval = Some(8000)))
  ): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getClientListStatus()(using any[HeaderCarrier])).thenReturn(Future.successful(status))
    when(connector.refreshClientList()(using any[HeaderCarrier])).thenReturn(Future.successful(refresh))
    connector
  }

  private def application(connector: NovaImportsBackendConnector, maxRetries: Int = 3): Application =
    applicationBuilderWithAgentAsNovaAgent(userAnswers = Some(emptyUserAnswers))
      .overrides(bind[NovaImportsBackendConnector].toInstance(connector))
      .configure("client-list.max-retries" -> maxRetries, "client-list.fallback-interval-ms" -> 5000)
      .build()

  "LoadingClientListController" - {

    "onPageLoad" - {

      "must be served from /load-client-list" in {
        given app: Application = application(connectorReturning(Right(ClientListStatus.Succeeded)))

        running(app) {
          loadingRoute() mustEqual "/nova-imports/load-client-list"
          loadingRoute(Some(8000), 2) mustEqual "/nova-imports/load-client-list?interval=8000&attempt=2"
        }
      }

      "must redirect to the client list when the status is Succeeded" in {
        val connector          = connectorReturning(Right(ClientListStatus.Succeeded))
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, loadingRoute())

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientselection.routes.ViewClientsController.onPageLoad(None, None, 1).url
          verify(connector, never).refreshClientList()(using any[HeaderCarrier])
        }
      }

      "must start a refresh and poll at the returned interval when the status is InitiateDownload" in {
        val connector          = connectorReturning(Right(ClientListStatus.InitiateDownload))
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, loadingRoute())

          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include(metaRefresh(8, 8000, 1))
          contentAsString(result) must include("Retrieving your client list")
          verify(connector).refreshClientList()(using any[HeaderCarrier])
        }
      }

      "must round a sub-second interval up to one second" in {
        val connector = connectorReturning(
          Right(ClientListStatus.InitiateDownload),
          Right(ClientListRefresh(success = true, browserInterval = Some(500)))
        )
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, loadingRoute())

          val result = route(app, request).value

          contentAsString(result) must include(metaRefresh(1, 500, 1))
        }
      }

      "must redirect to the could not retrieve page when the refresh does not start" in {
        val connector = connectorReturning(
          Right(ClientListStatus.InitiateDownload),
          Right(ClientListRefresh(success = false, browserInterval = None))
        )
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, loadingRoute())

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientselection.routes.CouldNotRetrieveClientListController.onPageLoad().url
        }
      }

      "must redirect to the could not retrieve page when the refresh call fails" in {
        val connector = connectorReturning(
          Right(ClientListStatus.InitiateDownload),
          Left(RefreshClientListError.UpstreamError(500, "boom"))
        )
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, loadingRoute())

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientselection.routes.CouldNotRetrieveClientListController.onPageLoad().url
        }
      }

      "must keep polling at the same interval when the status is InProgress" in {
        val connector          = connectorReturning(Right(ClientListStatus.InProgress))
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, loadingRoute(Some(8000), 1))

          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include(metaRefresh(8, 8000, 2))
          verify(connector, never).refreshClientList()(using any[HeaderCarrier])
        }
      }

      "must poll at the fallback interval when the status is InProgress and no interval is known" in {
        val connector          = connectorReturning(Right(ClientListStatus.InProgress))
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, loadingRoute())

          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include(metaRefresh(5, 5000, 1))
        }
      }

      "must redirect to the could not retrieve page once the retry limit is reached while InProgress" in {
        val connector          = connectorReturning(Right(ClientListStatus.InProgress))
        given app: Application = application(connector, maxRetries = 3)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, loadingRoute(Some(8000), 3))

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientselection.routes.CouldNotRetrieveClientListController.onPageLoad().url
        }
      }

      "must keep polling without starting another refresh when InitiateDownload comes back mid-poll" in {
        val connector          = connectorReturning(Right(ClientListStatus.InitiateDownload))
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, loadingRoute(Some(8000), 1))

          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include(metaRefresh(8, 8000, 2))
          verify(connector, never).refreshClientList()(using any[HeaderCarrier])
        }
      }

      "must redirect to the could not retrieve page once the retry limit is reached while InitiateDownload" in {
        val connector          = connectorReturning(Right(ClientListStatus.InitiateDownload))
        given app: Application = application(connector, maxRetries = 3)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, loadingRoute(Some(8000), 3))

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientselection.routes.CouldNotRetrieveClientListController.onPageLoad().url
          verify(connector, never).refreshClientList()(using any[HeaderCarrier])
        }
      }

      "must treat a negative attempt as the first" in {
        val connector          = connectorReturning(Right(ClientListStatus.InProgress))
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, loadingRoute(Some(8000), -5))

          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include(metaRefresh(8, 8000, 1))
        }
      }

      "must redirect to the could not retrieve page when the status is Failed" in {
        given app: Application = application(connectorReturning(Right(ClientListStatus.Failed)))

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, loadingRoute())

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientselection.routes.CouldNotRetrieveClientListController.onPageLoad().url
        }
      }

      "must redirect to the could not retrieve page when the status call fails" in {
        given app: Application = application(connectorReturning(Left(GetClientListStatusError.UpstreamError(500, "boom"))))

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, loadingRoute())

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientselection.routes.CouldNotRetrieveClientListController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised when the user is not an agent" in {
        val connector = connectorReturning(Right(ClientListStatus.Succeeded))

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
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, loadingRoute())

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
          verify(connector, never).getClientListStatus()(using any[HeaderCarrier])
        }
      }
    }
  }
}
