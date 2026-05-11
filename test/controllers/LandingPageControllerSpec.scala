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
import models.NotificationSummary
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
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
    NotificationSummary.IndividualOrOrganisation(traderName = testTrader, vrn = "000000000", hasDraftNotifications = false)

  private val summaryWithDrafts =
    NotificationSummary.IndividualOrOrganisation(traderName = testTrader, vrn = "000000000", hasDraftNotifications = true)

  private val agentSummaryWithoutDrafts =
    NotificationSummary.AgentWithoutClient(
      traderName = "ABC Consultancy",
      vrn = "000000000",
      hasDraftNotifications = false,
      hasClients = true
    )

  private val agentSummaryWithDrafts =
    NotificationSummary.AgentWithoutClient(
      traderName = "ABC Consultancy",
      vrn = "000000000",
      hasDraftNotifications = true,
      hasClients = true
    )

  private def applicationWith(
    identifierAction: Class[? <: IdentifierAction],
    connector: NovaImportsBackendConnector = stubConnector(emptySummary)
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[IdentifierAction].to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(None)),
        bind[NovaImportsBackendConnector].toInstance(connector)
      )
      .build()

  private def stubConnector(summary: NotificationSummary): NovaImportsBackendConnector = {
    val m = mock[NovaImportsBackendConnector]
    when(m.getNotificationSummary()(any[HeaderCarrier])) thenReturn Future.successful(Right(summary))
    m
  }

  private def failingSummaryConnector: NovaImportsBackendConnector = {
    val m = mock[NovaImportsBackendConnector]
    when(m.getNotificationSummary()(any[HeaderCarrier]))
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
          body must include("""<span class="govuk-caption-xl">Tester Trader</span>""")
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

      "for an Organisation redirects to Unauthorised (LP2.0 not yet built)" in {
        given application: Application = applicationWith(classOf[FakeOrganisationIdentifierAction])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "for an Agent redirects to Unauthorised (LP3.0 / LP3.1 not yet built)" in {
        given application: Application = applicationWith(classOf[FakeAgentIdentifierAction])

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

      "for an Agent with drafts renders the has-drafts saved-notification message" in {
        given application: Application =
          applicationWith(classOf[FakeAgentIdentifierAction], stubConnector(agentSummaryWithDrafts))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("View, continue or delete a notification you’ve started but not yet submitted")
          body must not include "You do not have a saved notification"
        }
      }

      "for an Agent when the summary call fails defaults to no drafts and omits the trader caption" in {
        given application: Application =
          applicationWith(classOf[FakeAgentIdentifierAction], failingSummaryConnector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("You do not have a saved notification")
          body must include("Manage your clients")
          body must not include "ABC Consultancy"
        }
      }
    }
  }
}
