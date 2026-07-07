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
import connectors.{AddressLookupConnector, AddressLookupError, NovaImportsBackendConnector, UpdateSectionError}
import models.draftsections.NotifierAddress
import models.{Address, Country, DraftId, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.notifieraddress.{AddressJourneyIdPage, AddressPage}
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.AddressSanitiser
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.Future

class AddressLookupCallbackControllerSpec extends SpecBase with MockitoSugar {

  private val journeyId         = "abc-123"
  private val draftId           = DraftId("DRAFT-001")
  private lazy val callbackOk   = routes.AddressLookupCallbackController.callback(Some(journeyId)).url
  private lazy val callbackNoId = routes.AddressLookupCallbackController.callback(None).url

  private val cleanAddress = Address(
    lines = Seq("12 High Street", "Reading"),
    postcode = Some("RE12 9GC"),
    country = Country("GB", "United Kingdom")
  )

  private val dirtyAddress = cleanAddress.copy(lines = Seq("12 High Street £", "Reading"))

  private val answersWithDraftId: UserAnswers =
    emptyUserAnswers
      .set(DraftIdPage, draftId)
      .success
      .value
      .set(AddressJourneyIdPage, UUID.randomUUID().toString)
      .success
      .value

  private def stubAlfConnector(result: Either[AddressLookupError, Address]): AddressLookupConnector = {
    val m = mock[AddressLookupConnector]
    when(m.confirmedAddress(any[String])(any[HeaderCarrier])).thenReturn(Future.successful(result))
    m
  }

  private def stubBackendConnector(result: Either[UpdateSectionError, Long] = Right(0L)): NovaImportsBackendConnector = {
    val m = mock[NovaImportsBackendConnector]
    when(m.updateDraftSection(any, any, any)(any)).thenReturn(Future.successful(result))
    m
  }

  private def stubSessionRepository(userAnswers: UserAnswers = answersWithDraftId): SessionRepository = {
    val m = mock[SessionRepository]
    when(m.set(any())).thenReturn(Future.successful(true))
    when(m.setPage(any(), any(), any())(any())).thenReturn(Future.successful(userAnswers))
    m
  }

  private def applicationWith(
    userAnswers: Option[UserAnswers] = Some(answersWithDraftId),
    alfConnector: AddressLookupConnector = stubAlfConnector(Right(cleanAddress)),
    backendConnector: NovaImportsBackendConnector = stubBackendConnector(),
    sessionRepository: SessionRepository = stubSessionRepository()
  ): Application =
    applicationBuilder(userAnswers)
      .overrides(
        bind[AddressLookupConnector].toInstance(alfConnector),
        bind[NovaImportsBackendConnector].toInstance(backendConnector),
        bind[SessionRepository].toInstance(sessionRepository)
      )
      .build()

  "AddressLookupCallbackController.callback" - {

    "must redirect to JourneyRecovery when id query parameter is missing" in {
      val app = applicationWith()
      running(app) {
        val result = route(app, FakeRequest(GET, callbackNoId)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must save the address, call F4, and redirect to the next section when no sanitising is needed" in {
      val backendConnector  = stubBackendConnector()
      val sessionRepository = stubSessionRepository()
      val app               = applicationWith(backendConnector = backendConnector, sessionRepository = sessionRepository)

      running(app) {
        val result = route(app, FakeRequest(GET, callbackOk)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url

        val answers = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(sessionRepository).set(answers.capture())
        answers.getValue.get(AddressPage) mustBe Some(cleanAddress)

        val body = ArgumentCaptor.forClass(classOf[JsObject])
        verify(backendConnector).updateDraftSection(eqTo(draftId), eqTo("notifier-address"), body.capture())(any[HeaderCarrier])
        body.getValue mustBe Json.toJson(NotifierAddress.fromAddress(cleanAddress)).as[JsObject] + ("versionId", Json.toJson(0L))
      }
    }

    "must save the sanitised address and redirect to AddressChanged when sanitiser altered the address" in {
      val backendConnector  = stubBackendConnector()
      val sessionRepository = stubSessionRepository()
      val app               = applicationWith(
        alfConnector = stubAlfConnector(Right(dirtyAddress)),
        backendConnector = backendConnector,
        sessionRepository = sessionRepository
      )

      running(app) {
        val result = route(app, FakeRequest(GET, callbackOk)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AddressChangedController.onPageLoad().url

        val answers = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(sessionRepository).set(answers.capture())
        answers.getValue.get(AddressPage) mustBe Some(AddressSanitiser.sanitise(dirtyAddress))

        verify(backendConnector, org.mockito.Mockito.never).updateDraftSection(any, any, any)(any)
      }
    }

    "must redirect to JourneyRecovery (and NOT touch the session) when the sanitiser empties a mandatory line" in {
      val junkAddress       = cleanAddress.copy(lines = Seq("@", "###"))
      val backendConnector  = stubBackendConnector()
      val sessionRepository = stubSessionRepository()
      val app               = applicationWith(
        alfConnector = stubAlfConnector(Right(junkAddress)),
        backendConnector = backendConnector,
        sessionRepository = sessionRepository
      )

      running(app) {
        val result = route(app, FakeRequest(GET, callbackOk)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

        verify(sessionRepository, org.mockito.Mockito.never).set(any())
        verify(backendConnector, org.mockito.Mockito.never).updateDraftSection(any, any, any)(any)
      }
    }

    "must redirect to JourneyRecovery when the ALF connector returns an error" in {
      val app = applicationWith(alfConnector = stubAlfConnector(Left(AddressLookupError.UpstreamError(500, "boom"))))

      running(app) {
        val result = route(app, FakeRequest(GET, callbackOk)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecovery when F4 fails" in {
      val app = applicationWith(backendConnector = stubBackendConnector(Left(UpdateSectionError.UpstreamError(503, "downstream"))))

      running(app) {
        val result = route(app, FakeRequest(GET, callbackOk)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecovery when DraftId is missing" in {
      val app = applicationWith(
        userAnswers = Some(emptyUserAnswers),
        sessionRepository = stubSessionRepository(emptyUserAnswers)
      )

      running(app) {
        val result = route(app, FakeRequest(GET, callbackOk)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if no user answers are present" in {
      val app = applicationWith(userAnswers = None)

      running(app) {
        val result = route(app, FakeRequest(GET, callbackOk)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
