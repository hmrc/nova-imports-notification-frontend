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

package controllers.notification

import base.SpecBase
import com.google.inject.name.Names
import connectors.{GetDraftNotificationError, GetDraftNotificationsError, GetNotificationSummaryError, NovaImportsBackendConnector}
import controllers.actions.*
import controllers.routes
import models.{AgentSelectedClient, DraftId, DraftNotificationSummary, DraftNotifications, NotificationSummary, UserAnswers, UserContext}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AgentSelectedClientPage, DraftIdPage}
import pages.sections.initialquestions.VehicleFromEuPage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.UserDataService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

class ViewSavedNotificationsControllerSpec extends SpecBase with MockitoSugar {

  private def savedNotificationsRoute(page: Int = 1): String =
    controllers.notification.routes.ViewSavedNotificationsController.onPageLoad(page).url

  private def draft(n: Int) = DraftNotificationSummary(s"$n", Some(s"Purchaser $n"), Some("withinEu"), Some(n), LocalDate.of(2026, 3, 1))

  private def notificationsOf(drafts: Seq[DraftNotificationSummary], total: Int, page: Int = 1) = DraftNotifications(drafts, total, page, 10)

  private def continueRoute(draftId: String): String =
    controllers.notification.routes.ViewSavedNotificationsController.onContinue(draftId).url

  private val clientAnswers =
    emptyUserAnswers.set(AgentSelectedClientPage, AgentSelectedClient(vrn = "700011916", name = Some("Client Co"))).success.value

  private def connectorReturning(
    result: Either[GetDraftNotificationsError, DraftNotifications],
    summary: Either[GetNotificationSummaryError, NotificationSummary] = Left(GetNotificationSummaryError.UpstreamError(502, "boom"))
  ): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getDraftNotifications(any[Option[String]], any[Int], any[Int])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(result))
    when(connector.getNotificationSummary(any[Option[String]])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(summary))
    connector
  }

  private def application(connector: NovaImportsBackendConnector): Application =
    applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .overrides(bind[NovaImportsBackendConnector].toInstance(connector))
      .configure("saved-notifications.page-size" -> 10)
      .build()

  private def agentWithClientApplication(
    connector: NovaImportsBackendConnector,
    overrides: GuiceApplicationBuilder => GuiceApplicationBuilder = identity,
    answers: UserAnswers = clientAnswers
  ): Application =
    overrides(
      new GuiceApplicationBuilder()
        .overrides(
          bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeAgentIdentifierAction],
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(answers))),
          bind[NovaImportsBackendConnector].toInstance(connector)
        )
    ).build()

  private def continueApplication(
    loaded: Either[GetDraftNotificationError, UserAnswers],
    sessionRepository: SessionRepository
  ): Application = {
    val userDataService = mock[UserDataService]
    when(userDataService.retrieveAndStoreDraftNotification(any[DraftId], any[UserAnswers], any[UserContext])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(loaded))
    when(sessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

    agentWithClientApplication(
      mock[NovaImportsBackendConnector],
      _.overrides(bind[SessionRepository].toInstance(sessionRepository), bind[UserDataService].toInstance(userDataService)),
      clientAnswers.set(DraftIdPage, DraftId("999")).flatMap(_.set(VehicleFromEuPage, true)).success.value
    )
  }

  "ViewSavedNotificationsController" - {

    "onPageLoad" - {

      "must be served from /saved-notifications" in {
        given app: Application = application(connectorReturning(Right(notificationsOf(Nil, 0))))

        running(app) {
          savedNotificationsRoute() mustEqual "/nova-imports/saved-notifications"
          savedNotificationsRoute(2) mustEqual "/nova-imports/saved-notifications?page=2"
        }
      }

      "must return OK with Showing 1 to 10 of 30 for page 1 of 30 drafts" in {
        val connector          = connectorReturning(Right(notificationsOf((1 to 10).map(draft), 30)))
        given app: Application = application(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, savedNotificationsRoute())

          val result = route(app, request).value
          val html   = contentAsString(result)

          status(result) mustEqual OK
          html must include("Showing <b>1</b> to <b>10</b> of <b>30</b> records")
          html must include("Purchaser 1")
          html must include(savedNotificationsRoute(2))
          verify(connector).getDraftNotifications(eqTo(None), eqTo(1), eqTo(10))(using any[HeaderCarrier])
        }
      }

      "must call getDraftNotifications with the client VRN for an agent with a client" in {
        val connector          = connectorReturning(Right(notificationsOf(Seq(draft(1)), 1)))
        given app: Application = agentWithClientApplication(connector)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, savedNotificationsRoute())

          status(route(app, request).value) mustEqual OK
          verify(connector).getDraftNotifications(eqTo(Some("700011916")), eqTo(1), eqTo(10))(using any[HeaderCarrier])
        }
      }

      "must render the client name instead of the purchaser name for an agent with a client" in {
        val summary            = NotificationSummary.AgentWithClient(None, Some("Client Trader Ltd"), "700011916", true, false)
        given app: Application = agentWithClientApplication(connectorReturning(Right(notificationsOf(Seq(draft(1)), 1)), Right(summary)))

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, savedNotificationsRoute())

          val html = contentAsString(route(app, request).value)

          html must include("Client Trader Ltd")
          html must not include "Purchaser 1"
        }
      }

      "must render the trader name for a VAT org draft with no purchaser name" in {
        val nameless           = draft(1).copy(purchaserName = None)
        val summary            = NotificationSummary.IndividualOrOrganisation(Some("Test Company 43 Ltd"), Some("740000003"), true, false)
        val connector          = connectorReturning(Right(notificationsOf(Seq(nameless), 1)), Right(summary))
        given app: Application = applicationBuilderWithVatTrader(Some(emptyUserAnswers))
          .overrides(bind[NovaImportsBackendConnector].toInstance(connector))
          .build()

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, savedNotificationsRoute())

          contentAsString(route(app, request).value) must include("Test Company 43 Ltd")
        }
      }

      "must redirect to Unauthorised when there are no drafts" in {
        given app: Application = application(connectorReturning(Right(notificationsOf(Nil, 0))))

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, savedNotificationsRoute())

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to the last page, page 3, when page 9 is requested and there are only 30 drafts" in {
        given app: Application = application(connectorReturning(Right(notificationsOf(Nil, 30, page = 9))))

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, savedNotificationsRoute(9))

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual savedNotificationsRoute(3)
        }
      }

      "must redirect to JourneyRecovery when getDraftNotifications fails" in {
        given app: Application = application(connectorReturning(Left(GetDraftNotificationsError.UpstreamError(502, "boom"))))

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, savedNotificationsRoute())

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onContinue" - {

      "must clear the session, keep the client, write draftId 12345 and redirect to the NTL" in {
        val sessionRepository  = mock[SessionRepository]
        given app: Application = continueApplication(Right(clientAnswers), sessionRepository)

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, continueRoute("12345"))

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(sessionRepository).set(captor.capture())
          captor.getValue.data.keys mustEqual Set(AgentSelectedClientPage.toString, DraftIdPage.toString)
          captor.getValue.get(DraftIdPage).value mustEqual DraftId("12345")
          captor.getValue.get(AgentSelectedClientPage).value.vrn mustEqual "700011916"
        }
      }

      "must redirect to JourneyRecovery when the draft fails to load" in {
        given app: Application = continueApplication(Left(GetDraftNotificationError.Forbidden), mock[SessionRepository])

        running(app) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, continueRoute("12345"))

          redirectLocation(route(app, request).value).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
