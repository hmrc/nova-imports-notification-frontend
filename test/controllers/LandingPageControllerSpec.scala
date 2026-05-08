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

  private val emptySummary =
    NotificationSummary.IndividualOrOrganisation(traderName = "Tester", vrn = "000000000", hasDraftNotifications = false)

  private val summaryWithDrafts =
    NotificationSummary.IndividualOrOrganisation(traderName = "Tester", vrn = "000000000", hasDraftNotifications = true)

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

      "for a Private Individual with no drafts renders LP1.0 with the empty saved-notifications message" in {
        given application: Application = applicationWith(classOf[FakeIdentifierAction])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Notification of Vehicle Arrivals (NOVA)")
          body must include("Create notifications")
          body must include("Update notifications")
          body must include("View saved notifications")
          body must include("You have no saved notifications.")
          body must not include "View, continue or delete saved notifications"
          body must include(startUrl)
        }
      }

      "for a Private Individual with drafts renders the has-drafts saved-notifications message" in {
        given application: Application =
          applicationWith(classOf[FakeIdentifierAction], stubConnector(summaryWithDrafts))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, landingPageRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("View, continue or delete saved notifications that have not yet been submitted.")
          body must not include "You have no saved notifications."
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
          body must include("You have no saved notifications.")
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

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }
  }
}
