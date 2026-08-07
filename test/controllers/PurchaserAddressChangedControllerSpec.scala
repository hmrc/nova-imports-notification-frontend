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
import connectors.{NovaImportsBackendConnector, UpdateSectionError}
import controllers.actions.*
import models.draftsections.PurchaserAddress
import models.{Address, Country, DraftId, NormalMode, PurchaserOrOnBehalf, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.PurchaserOrOnBehalfPage
import pages.sections.purchaseraddress.{PurchaserAddressJourneyIdPage, PurchaserAddressPage}
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import views.html.PurchaserAddressChangedView

import scala.concurrent.Future

class PurchaserAddressChangedControllerSpec extends SpecBase with MockitoSugar {

  private lazy val onPageLoadRoute: String      = routes.PurchaserAddressChangedController.onPageLoad().url
  private lazy val onSubmitRoute: String        = routes.PurchaserAddressChangedController.onSubmit().url
  private lazy val onChangeAddressRoute: String = routes.PurchaserAddressChangedController.onChangeAddress().url

  private val draftId = DraftId("DRAFT-001")
  private val address = Address(
    lines = Seq("12 High Street", "Reading"),
    postcode = Some("RE12 9GC"),
    country = Country("GB", "United Kingdom")
  )

  private val answersWithAddressAndDraft: UserAnswers =
    emptyUserAnswers
      .set(PurchaserAddressPage, address)
      .success
      .value
      .set(DraftIdPage, draftId)
      .success
      .value
      .set(DraftVersionIdPage, 0L)
      .success
      .value
      .set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
      .success
      .value

  private def stubBackendConnector(result: Either[UpdateSectionError, Long] = Right(0L)): NovaImportsBackendConnector = {
    val m = mock[NovaImportsBackendConnector]
    when(m.updateDraftSection(any, any, any)(any)).thenReturn(Future.successful(result))
    m
  }

  private def stubSessionRepository(): SessionRepository = {
    val m = mock[SessionRepository]
    when(m.setPage(any(), any(), any())(any())).thenReturn(Future.successful(answersWithAddressAndDraft))
    when(m.set(any())).thenReturn(Future.successful(true))
    m
  }

  private def applicationWith(
    userAnswers: Option[UserAnswers] = Some(answersWithAddressAndDraft),
    backendConnector: NovaImportsBackendConnector = stubBackendConnector(),
    sessionRepository: SessionRepository = stubSessionRepository()
  ): Application =
    applicationBuilder(userAnswers)
      .overrides(
        bind[NovaImportsBackendConnector].toInstance(backendConnector),
        bind[SessionRepository].toInstance(sessionRepository)
      )
      .build()

  private def agentApplicationWith(
    userAnswers: Option[UserAnswers],
    backendConnector: NovaImportsBackendConnector = stubBackendConnector(),
    sessionRepository: SessionRepository = stubSessionRepository()
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[NovaImportsBackendConnector].toInstance(backendConnector),
        bind[SessionRepository].toInstance(sessionRepository)
      )
      .build()

  "PurchaserAddressChanged Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationWith()

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[PurchaserAddressChangedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(address)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when an agent has a stored purchaser address" in {
      val agentAnswers = emptyUserAnswers
        .set(PurchaserAddressPage, address)
        .success
        .value
        .set(DraftIdPage, draftId)
        .success
        .value
      val application = agentApplicationWith(Some(agentAnswers))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Unauthorised for a GET if no session data is found" in {
      val application = applicationWith(userAnswers = None)

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when a non-agent answered as the purchaser (not on behalf)" in {
      val answers = answersWithAddressAndDraft
        .set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser)
        .success
        .value
      val application = applicationWith(userAnswers = Some(answers))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must call F4 with the stored purchaser address and redirect to the task list on submit" in {
      val backendConnector = stubBackendConnector()
      val application      = applicationWith(backendConnector = backendConnector)

      running(application) {
        val request = FakeRequest(POST, onSubmitRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url

        val body = ArgumentCaptor.forClass(classOf[JsObject])
        verify(backendConnector).updateDraftSection(eqTo(draftId), eqTo("purchaser-address"), body.capture())(any[HeaderCarrier])
        body.getValue mustBe Json.toJson(PurchaserAddress.fromAddress(address)).as[JsObject] + ("versionId", Json.toJson(0L))
      }
    }

    "must redirect to Journey Recovery on submit when F4 fails" in {
      val backendConnector = stubBackendConnector(Left(UpdateSectionError.UpstreamError(503, "downstream")))
      val application      = applicationWith(backendConnector = backendConnector)

      running(application) {
        val request = FakeRequest(POST, onSubmitRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery on submit when DraftId is missing" in {
      val answersWithoutDraft = emptyUserAnswers
        .set(PurchaserAddressPage, address)
        .success
        .value
        .set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
        .success
        .value
      val backendConnector = stubBackendConnector()
      val application      = applicationWith(userAnswers = Some(answersWithoutDraft), backendConnector = backendConnector)

      running(application) {
        val request = FakeRequest(POST, onSubmitRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

        verify(backendConnector, org.mockito.Mockito.never).updateDraftSection(any, any, any)(any)
      }
    }

    "must clear the stored purchaser address and journey id from the session and redirect to APA1.0 on change address" in {
      val answersWithJourneyId = answersWithAddressAndDraft
        .set(PurchaserAddressJourneyIdPage, "journey-123")
        .success
        .value

      val sessionRepository = stubSessionRepository()
      val application       = applicationWith(userAnswers = Some(answersWithJourneyId), sessionRepository = sessionRepository)

      running(application) {
        val request = FakeRequest(GET, onChangeAddressRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.IsPurchaserAddressInTheUkController.onPageLoad(NormalMode).url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(sessionRepository).set(captor.capture())
        captor.getValue.get(PurchaserAddressPage) mustBe None
        captor.getValue.get(PurchaserAddressJourneyIdPage) mustBe None
      }
    }

    "must redirect to Unauthorised on change address if no session data is found" in {
      val application = applicationWith(userAnswers = None)

      running(application) {
        val request = FakeRequest(GET, onChangeAddressRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
