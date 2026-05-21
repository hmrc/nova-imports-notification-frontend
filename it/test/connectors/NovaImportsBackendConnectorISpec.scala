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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import models.{DraftId, NotificationSummary}
import play.api.libs.json.Json
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

import scala.concurrent.ExecutionContext.Implicits.global

class NovaImportsBackendConnectorISpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with WireMockSupport {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.nova-imports-backend.protocol" -> "http",
        "microservice.services.nova-imports-backend.host"     -> wireMockHost,
        "microservice.services.nova-imports-backend.port"     -> wireMockPort
      )
      .build()

  private given HeaderCarrier = HeaderCarrier()

  private val connector = app.injector.instanceOf[NovaImportsBackendConnector]

  "createDraft" - {

    "returns a DraftId on 201 with no body when no client is selected" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/nova-imports/draft-notifications"))
          .willReturn(aResponse().withStatus(201).withBody("""{"draftId":"12345"}"""))
      )

      connector.createDraft(None).futureValue mustEqual Right(DraftId("12345"))
    }

    "sends the {clientVrn} JSON body and returns a DraftId when an agent has a selected client" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/nova-imports/draft-notifications"))
          .withRequestBody(equalToJson("""{"clientVrn":"700011916"}"""))
          .willReturn(aResponse().withStatus(201).withBody("""{"draftId":"67890"}"""))
      )

      connector.createDraft(Some("700011916")).futureValue mustEqual Right(DraftId("67890"))
    }

    "returns ClientNotFound on 403" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/nova-imports/draft-notifications"))
          .willReturn(aResponse().withStatus(403))
      )

      connector.createDraft(Some("000000000")).futureValue mustEqual Left(CreateDraftError.ClientNotFound)
    }

    "returns UpstreamError on a 5xx response" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/nova-imports/draft-notifications"))
          .willReturn(aResponse().withStatus(503).withBody("upstream down"))
      )

      connector.createDraft(None).futureValue mustEqual Left(CreateDraftError.UpstreamError(503, "upstream down"))
    }

    "returns UpstreamError on 201 when the response body is missing draftId" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/nova-imports/draft-notifications"))
          .willReturn(aResponse().withStatus(201).withBody("""{"unexpected":"shape"}"""))
      )

      connector.createDraft(None).futureValue mustEqual Left(
        CreateDraftError.UpstreamError(201, "Missing draftId in response body")
      )
    }
  }

  "getNotificationSummary" - {

    "returns IndividualOrOrganisation when the backend returns the IndividualOrOrganisation shape" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/nova-imports/notification-summary"))
          .willReturn(
            okJson(
              """{"traderName":"ABC LTD","vrn":"123456789","hasDraftNotifications":true,"hasClients":null}"""
            )
          )
      )

      connector.getNotificationSummary().futureValue mustEqual Right(
        NotificationSummary.IndividualOrOrganisation(
          traderName = "ABC LTD",
          vrn = "123456789",
          hasDraftNotifications = true
        )
      )
    }

    "returns AgentWithoutClient when the backend returns the AgentWithoutClient shape" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/nova-imports/notification-summary"))
          .willReturn(
            okJson(
              """{"traderName":"ABC LTD","vrn":"123456789","hasDraftNotifications":false,"hasClients":true}"""
            )
          )
      )

      connector.getNotificationSummary().futureValue mustEqual Right(
        NotificationSummary.AgentWithoutClient(
          traderName = "ABC LTD",
          vrn = "123456789",
          hasDraftNotifications = false,
          hasClients = true
        )
      )
    }

    "returns AgentWithClient when the backend returns the AgentWithClient shape" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/nova-imports/notification-summary"))
          .willReturn(
            okJson(
              """{"traderName":"ABC LTD","vrn":"0","clientTraderName":"CLIENT LTD","clientVrn":"700011916","clientHasDraftNotifications":true,"hasClients":true}"""
            )
          )
      )

      connector.getNotificationSummary().futureValue mustEqual Right(
        NotificationSummary.AgentWithClient(
          traderName = "ABC LTD",
          vrn = "0",
          clientTraderName = "CLIENT LTD",
          clientVrn = "700011916",
          clientHasDraftNotifications = true,
          hasClients = true
        )
      )
    }

    "returns UpstreamError on a 5xx response" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/nova-imports/notification-summary"))
          .willReturn(aResponse().withStatus(500).withBody("boom"))
      )

      connector.getNotificationSummary().futureValue mustEqual Left(
        GetNotificationSummaryError.UpstreamError(500, "boom")
      )
    }

    "returns UpstreamError when the 200 body cannot be parsed as a NotificationSummary" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/nova-imports/notification-summary"))
          .willReturn(okJson("""{"unexpected":"shape"}"""))
      )

      connector.getNotificationSummary().futureValue match {
        case Left(GetNotificationSummaryError.UpstreamError(200, message)) =>
          message must startWith("Malformed notification summary")
        case other                                                         =>
          fail(s"expected UpstreamError(200, ...) but got $other")
      }
    }
  }

  "updateDraftSection" - {

    val draftId   = DraftId("abc-123")
    val sectionId = "initial-questions"
    val url       = s"/nova-imports/draft-notifications/${draftId.value}/sections/$sectionId"
    val body      = Json.obj("vehicleFromEuToNi" -> true)

    "returns Right(()) on 200" in {
      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .withRequestBody(equalToJson(body.toString))
          .willReturn(aResponse().withStatus(200))
      )

      connector.updateDraftSection(draftId, sectionId, body).futureValue mustEqual Right(())
    }

    "returns Forbidden on 403" in {
      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(aResponse().withStatus(403))
      )

      connector.updateDraftSection(draftId, sectionId, body).futureValue mustEqual Left(UpdateSectionError.Forbidden)
    }

    "returns NotFound on 404" in {
      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(aResponse().withStatus(404))
      )

      connector.updateDraftSection(draftId, sectionId, body).futureValue mustEqual Left(UpdateSectionError.NotFound)
    }

    "returns UpstreamError on a 5xx response" in {
      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(aResponse().withStatus(500).withBody("upstream error"))
      )

      connector.updateDraftSection(draftId, sectionId, body).futureValue mustEqual Left(
        UpdateSectionError.UpstreamError(500, "upstream error")
      )
    }
  }
}
