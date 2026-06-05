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
import connectors.{GetNotificationSummaryError, NovaImportsBackendConnector}
import controllers.actions.*
import models.{AgentSelectedClient, NotificationSummary, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.AgentSelectedClientPage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class LandingPageControllerSpec extends SpecBase with MockitoSugar {

  private lazy val landingPageRoute = routes.LandingPageController.onPageLoad().url
  private lazy val startUrl         = routes.StartController.start().url

  private val testTrader = "Tester Trader"

  private val emptySummary =
    NotificationSummary.IndividualOrOrganisation(
      traderName = Some(testTrader),
      vrn = Some("000000000"),
      hasDraftNotifications = false,
      isDeregistered = false
    )

  private val summaryWithDrafts =
    NotificationSummary.IndividualOrOrganisation(
      traderName = Some(testTrader),
      vrn = Some("000000000"),
      hasDraftNotifications = true,
      isDeregistered = false
    )

  private val summaryNoName =
    NotificationSummary.IndividualOrOrganisation(
      traderName = None,
      vrn = Some("000000000"),
      hasDraftNotifications = false,
      isDeregistered = false
    )

  private val agentSummaryWithoutDrafts =
    NotificationSummary.AgentWithoutClient(
      agentName = Some("ABC Consultancy"),
      hasDraftNotifications = false
    )

  private val agentSummaryNoName =
    NotificationSummary.AgentWithoutClient(
      agentName = None,
      hasDraftNotifications = false
    )

  private val agentSummaryWithDrafts =
    NotificationSummary.AgentWithoutClient(
      agentName = Some("ABC Consultancy"),
      hasDraftNotifications = true
    )

  private val agentSummaryWithClient =
    NotificationSummary.AgentWithClient(
      agentName = Some("ABC Consultancy"),
      clientTraderName = Some("Client Co"),
      clientVrn = "700011916",
      clientHasDraftNotifications = true,
      clientIsDeregistered = false
    )

  private val userAnswersWithClient =
    emptyUserAnswers.set(AgentSelectedClientPage, AgentSelectedClient(vrn = "700011916", name = Some("Client Co"))).success.value

  private def applicationWith(
    identifierAction: Class[? <: IdentifierAction],
    connector: NovaImportsBackendConnector = stubConnector(emptySummary),
    userAnswers: Option[UserAnswers] = None
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[IdentifierAction].to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[NovaImportsBackendConnector].toInstance(connector)
      )
      .build()

  private def stubConnector(summary: NotificationSummary): NovaImportsBackendConnector = {
    val m = mock[NovaImportsBackendConnector]
    when(m.getNotificationSummary(any[Option[String]])(using any[HeaderCarrier])) thenReturn Future.successful(Right(summary))
    m
  }

  private def failingSummaryConnector: NovaImportsBackendConnector = {
    val m = mock[NovaImportsBackendConnector]
    when(m.getNotificationSummary(any[Option[String]])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(Left(GetNotificationSummaryError.UpstreamError(500, "boom"))))
    m
  }

  "LandingPageController" - {

    "onPageLoad" - {

      "for a Private Individual with no drafts renders LP1.0 with the empty saved-notification message and trader name caption" in {
        given application: Application = applicationWith(classOf[FakeIdentifierAction])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Notification of Vehicle Arrivals (NOVA)")
          body must include("""<span class="govuk-caption-l">Tester Trader</span>""")
          body must include("Create a new notification")
          body must include("Update a submitted notification")
          body must include("Manage a saved notification")
          body must include("You do not have a saved notification")
          body must not include "View, continue or delete a notification"
          body must include(startUrl)
        }
      }

      "for a Private Individual with drafts renders the heading as an enabled link and the has-drafts body" in {
        given application: Application =
          applicationWith(classOf[FakeIdentifierAction], stubConnector(summaryWithDrafts))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("View, continue or delete a notification you’ve started but not yet submitted")
          body must not include "You do not have a saved notification"
          body must include("""<a class="govuk-link" href="/nova-imports/there-is-a-problem">Manage a saved notification</a>""")
        }
      }

      "for a Private Individual when the summary call fails defaults to no drafts" in {
        given application: Application =
          applicationWith(classOf[FakeIdentifierAction], failingSummaryConnector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("You do not have a saved notification")
        }
      }

      "for a Private Individual with no trader name renders LP1.0 without a name caption" in {
        given application: Application =
          applicationWith(classOf[FakeIdentifierAction], stubConnector(summaryNoName))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Notification of Vehicle Arrivals (NOVA)")
          body must not include "govuk-caption-l"
          body must not include testTrader
        }
      }

      "for a non-VAT Organisation renders LP1.0 the same as a Private Individual" in {
        given application: Application = applicationWith(classOf[FakeOrganisationIdentifierAction])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Notification of Vehicle Arrivals (NOVA)")
          body must include("""<span class="govuk-caption-l">Tester Trader</span>""")
          body must include("Create a new notification")
          body must include("Update a submitted notification")
          body must include("Manage a saved notification")
          body must include("You do not have a saved notification")
        }
      }

      "for a VAT-registered Organisation with no drafts renders LP2.0 with trader name, VRN and the empty saved-notification message" in {
        given application: Application = applicationWith(classOf[FakeVatTraderIdentifierAction])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Notification of Vehicle Arrivals (NOVA)")
          body must include("Tester")
          body must include("VAT registration number: GB000000000")
          body must include("Create a new notification")
          body must include("Update a submitted notification")
          body must include("Manage a saved notification")
          body must include("You do not have a saved notification")
          body must not include "View, continue or delete a notification"
          body must include(startUrl)
        }
      }

      "for a VAT-registered Organisation with drafts renders the has-drafts saved-notification message" in {
        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], stubConnector(summaryWithDrafts))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("View, continue or delete a notification you’ve started but not yet submitted")
          body must not include "You do not have a saved notification"
        }
      }

      "for a VAT-registered Organisation when the summary call fails redirects to JourneyRecovery" in {
        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], failingSummaryConnector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "for a VAT-registered Organisation when the summary returns an unexpected agent shape redirects to JourneyRecovery" in {
        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], stubConnector(agentSummaryWithoutDrafts))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "for a VAT-registered Organisation with no trader name renders LP2.0 with the VRN caption and no name, without redirecting to JourneyRecovery" in {
        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], stubConnector(summaryNoName))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("VAT registration number: GB000000000")
          body must not include testTrader
          redirectLocation(result) mustBe None
        }
      }

      "for an Agent with no client renders LP3.0 with the agent name caption and the empty saved-notification message" in {
        given application: Application =
          applicationWith(classOf[FakeAgentIdentifierAction], stubConnector(agentSummaryWithoutDrafts))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Notification of Vehicle Arrivals (NOVA)")
          body must include("ABC Consultancy")
          body must include("Create a new notification")
          body must include("Update a submitted notification")
          body must include("Manage a saved notification")
          body must include("You do not have a saved notification")
          body must include("Manage your clients")
          body must include(startUrl)
          body must include(routes.LoadingClientListController.onPageLoad().url)
          body must not include "View, continue or delete a notification"
        }
      }

      "for an Agent with no client and drafts renders the has-drafts saved-notification body" in {
        given application: Application =
          applicationWith(classOf[FakeAgentIdentifierAction], stubConnector(agentSummaryWithDrafts))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("View, continue or delete a notification you’ve started but not yet submitted")
          body must not include "You do not have a saved notification"
          body must include("""<a class="govuk-link" href="/nova-imports/there-is-a-problem">Manage a saved notification</a>""")
        }
      }

      "for an Agent with no client when the summary call fails defaults to no drafts and omits the agent caption" in {
        given application: Application =
          applicationWith(classOf[FakeAgentIdentifierAction], failingSummaryConnector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("You do not have a saved notification")
          body must include("Manage your clients")
          body must not include "ABC Consultancy"
        }
      }

      "for an Agent with no client and no agent name renders LP3.0 without a name caption" in {
        given application: Application =
          applicationWith(classOf[FakeAgentIdentifierAction], stubConnector(agentSummaryNoName))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must not include "ABC Consultancy"
          body must include("Manage your clients")
        }
      }

      "for an Agent with a selected client passes the client VRN to the backend and renders the agent landing page" in {
        val connector = stubConnector(agentSummaryWithClient)

        given application: Application =
          applicationWith(classOf[FakeAgentIdentifierAction], connector, userAnswers = Some(userAnswersWithClient))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("ABC Consultancy")
          verify(connector).getNotificationSummary(eqTo(Some("700011916")))(using any[HeaderCarrier])
        }
      }
    }
  }
}
