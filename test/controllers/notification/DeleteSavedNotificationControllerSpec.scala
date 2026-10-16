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
import connectors.{DeleteDraftNotificationError, GetDraftNotificationError, GetNotificationSummaryError, NovaImportsBackendConnector}
import controllers.routes
import models.{DraftId, DraftNotification, DraftNotificationSection, DraftNotifications, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class DeleteSavedNotificationControllerSpec extends SpecBase with MockitoSugar {

  private lazy val deleteSavedNotificationRoute =
    controllers.notification.routes.DeleteSavedNotificationController.onPageLoad("12345").url

  private val savedDraft = DraftNotification(
    "12345",
    "2026-03-12",
    None,
    Map("notifierDetails" -> DraftNotificationSection(Some(Json.obj("firstName" -> "FirstName", "lastName" -> "LastName"))))
  )

  private def connectorReturning(
    result: Either[GetDraftNotificationError, DraftNotification],
    deleted: Either[DeleteDraftNotificationError, Boolean] = Right(true),
    remainingDrafts: Int = 1
  ): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getDraftNotification(any[DraftId])(using any[HeaderCarrier])).thenReturn(Future.successful(result))
    when(connector.getNotificationSummary(any[Option[String]])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(Left(GetNotificationSummaryError.UpstreamError(502, "boom"))))
    when(connector.deleteDraftNotification(any[DraftId], any[Option[String]])(using any[HeaderCarrier])).thenReturn(Future.successful(deleted))
    when(connector.getDraftNotifications(any[Option[String]], any[Int], any[Int])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(DraftNotifications(Nil, remainingDrafts, 1, 1))))
    connector
  }

  private def sessionRepositorySaving(): SessionRepository = {
    val sessionRepository = mock[SessionRepository]
    when(sessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
    sessionRepository
  }

  private def application(connector: NovaImportsBackendConnector, sessionRepository: SessionRepository = mock[SessionRepository]): Application =
    applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .overrides(bind[NovaImportsBackendConnector].toInstance(connector), bind[SessionRepository].toInstance(sessionRepository))
      .build()

  "DeleteSavedNotificationController" - {

    "must return OK with the purchaser name and 12 March 2026 for a GET" in {
      given app: Application = application(connectorReturning(Right(savedDraft)))

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, deleteSavedNotificationRoute)

        val result = route(app, request).value
        val html   = contentAsString(result)

        status(result) mustEqual OK
        html must include("FirstName LastName")
        html must include("12 March 2026")
      }
    }

    "must redirect to JourneyRecovery for a GET when getDraftNotification fails" in {
      given app: Application = application(connectorReturning(Left(GetDraftNotificationError.UpstreamError(502, "boom"))))

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, deleteSavedNotificationRoute)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when the draft is Forbidden" in {
      given app: Application = application(connectorReturning(Left(GetDraftNotificationError.Forbidden)))

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, deleteSavedNotificationRoute)

        redirectLocation(route(app, request).value).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to VS1.0 without a delete when No is submitted" in {
      val connector          = connectorReturning(Right(savedDraft))
      val sessionRepository  = sessionRepositorySaving()
      given app: Application = application(connector, sessionRepository)

      running(app) {
        val request = FakeRequest(POST, deleteSavedNotificationRoute).withFormUrlEncodedBody("value" -> "false")

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.notification.routes.ViewSavedNotificationsController.onPageLoad().url
        verify(sessionRepository, never).set(any[UserAnswers])
        verify(connector, never).deleteDraftNotification(any[DraftId], any[Option[String]])(using any[HeaderCarrier])
      }
    }

    "must call deleteDraftNotification and redirect to VS1.0 when 1 draft remains" in {
      val connector          = connectorReturning(Right(savedDraft))
      val sessionRepository  = sessionRepositorySaving()
      given app: Application = application(connector, sessionRepository)

      running(app) {
        val request = FakeRequest(POST, deleteSavedNotificationRoute).withFormUrlEncodedBody("value" -> "true")

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.notification.routes.ViewSavedNotificationsController.onPageLoad().url
        verify(connector).deleteDraftNotification(eqTo(DraftId("12345")), eqTo(None))(using any[HeaderCarrier])
        verify(sessionRepository).set(any[UserAnswers])
      }
    }

    "must redirect to the landing page when 0 drafts remain" in {
      given app: Application = application(connectorReturning(Right(savedDraft), remainingDrafts = 0), sessionRepositorySaving())

      running(app) {
        val request = FakeRequest(POST, deleteSavedNotificationRoute).withFormUrlEncodedBody("value" -> "true")

        redirectLocation(route(app, request).value).value mustEqual routes.LandingPageController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecovery when deleteDraftNotification fails" in {
      val deleteFailed       = Left(DeleteDraftNotificationError.UpstreamError(500, "boom"))
      given app: Application = application(connectorReturning(Right(savedDraft), deleted = deleteFailed), sessionRepositorySaving())

      running(app) {
        val request = FakeRequest(POST, deleteSavedNotificationRoute).withFormUrlEncodedBody("value" -> "true")

        redirectLocation(route(app, request).value).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return 400 with the error message when no radio is selected" in {
      given app: Application = application(connectorReturning(Right(savedDraft)))

      running(app) {
        val request = FakeRequest(POST, deleteSavedNotificationRoute).withFormUrlEncodedBody("value" -> "")

        val result = route(app, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(app)("deleteSavedNotification.error.required"))
      }
    }
  }
}
