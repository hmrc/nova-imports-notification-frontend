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
import connectors.{DeleteDraftNotificationError, NovaImportsBackendConnector}
import controllers.actions.*
import controllers.{notification, routes}
import forms.DeleteCurrentNotificationFormProvider
import models.PurchaserOrOnBehalf.Purchaser
import models.{BusinessOrPrivateIndividual, DraftId, PurchaserOrOnBehalf, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.{BusinessOrPrivatePage, NotifyingAsPurchaserPage, VehicleBusinessUsePage, VehicleFromEuPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import views.html.{DeleteCurrentNotificationView, PageNotFoundView}

import scala.concurrent.Future

class DeleteCurrentNotificationControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new DeleteCurrentNotificationFormProvider()
  private val form         = formProvider()

  private lazy val deleteCurrentNotificationPageLoadRoute =
    notification.routes.DeleteCurrentNotificationController.onPageLoad().url
  private def deleteCurrentNotificationSubmitRoute() =
    notification.routes.DeleteCurrentNotificationController.onPageLoad().url

  private val userAnswersWithGuardData: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
    .unsafeSet(NotifyingAsPurchaserPage, PurchaserOrOnBehalf.Purchaser)
    .unsafeSet(VehicleBusinessUsePage, true)

  private def stubSessionRepository(): SessionRepository = {
    val m = mock[SessionRepository]
    when(m.set(any())).thenReturn(Future.successful(true))
    m
  }
  private def backendConnector(): NovaImportsBackendConnector = {
    val backendConnector: NovaImportsBackendConnector = mock[NovaImportsBackendConnector]
    when(backendConnector.deleteDraftNotification(any())(any())).thenReturn(Future.successful(Right(true)))
    backendConnector
  }

  private def applicationWithMockRepository(
    userAnswers: UserAnswers,
    identifierAction: Class[? <: IdentifierAction] = classOf[FakeVatTraderIdentifierAction],
    sessionRepository: SessionRepository = stubSessionRepository(),
    backendConnector: NovaImportsBackendConnector = backendConnector()
  ): (play.api.Application, SessionRepository, NovaImportsBackendConnector) = {
    val application =
      new GuiceApplicationBuilder()
        .overrides(
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          bind[SessionRepository].toInstance(sessionRepository),
          bind[NovaImportsBackendConnector].toInstance(backendConnector),
          bind[IdentifierAction].to(identifierAction),
          bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
          bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswers)))
        )
        .build()

    (application, sessionRepository, backendConnector)
  }

  private def savedAnswers(mockSessionRepository: SessionRepository): UserAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
    verify(mockSessionRepository).set(captor.capture())
    captor.getValue
  }

  "DeleteCurrentNotificationController" - {

    "must return OK and the correct view for a GET when guard passes" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, deleteCurrentNotificationPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[DeleteCurrentNotificationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must redirect to the landing page when the delete is confirmed" in {

      val (application, _, connector) = applicationWithMockRepository(userAnswersWithGuardData)

      running(application) {
        val request =
          FakeRequest(POST, deleteCurrentNotificationSubmitRoute())
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url
        verify(connector).deleteDraftNotification(eqTo(DraftId("DRAFT-001")))(using any[HeaderCarrier])
      }
    }

    "must return to the NTL page when the delete is canceled" in {

      val (application, _, connector) = applicationWithMockRepository(userAnswersWithGuardData)

      running(application) {
        val request =
          FakeRequest(POST, deleteCurrentNotificationSubmitRoute())
            .withFormUrlEncodedBody("value" -> "false")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url
        verify(connector, never).deleteDraftNotification(eqTo(DraftId("DRAFT-001")))(using any[HeaderCarrier])
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteCurrentNotificationPageLoadRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[DeleteCurrentNotificationView]
        val result    = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised for a GET if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deleteCurrentNotificationPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, deleteCurrentNotificationPageLoadRoute)
          .withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to unauthorised page if the backend connector delete returns Forbidden failure response" in {

      val backendConnector: NovaImportsBackendConnector = mock[NovaImportsBackendConnector]
      when(backendConnector.deleteDraftNotification(any())(any())).thenReturn(Future.successful(Left(DeleteDraftNotificationError.Forbidden)))
      val (application, _, _) = applicationWithMockRepository(userAnswersWithGuardData, backendConnector = backendConnector)

      running(application) {
        val request = FakeRequest(POST, deleteCurrentNotificationSubmitRoute())
          .withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must show Page Not Found if the backend connector delete returns NotFound failure response" in {

      val backendConnector: NovaImportsBackendConnector = mock[NovaImportsBackendConnector]
      when(backendConnector.deleteDraftNotification(any())(any())).thenReturn(Future.successful(Left(DeleteDraftNotificationError.NotFound)))
      val (application, _, _) = applicationWithMockRepository(userAnswersWithGuardData, backendConnector = backendConnector)

      running(application) {
        val request = FakeRequest(POST, deleteCurrentNotificationSubmitRoute())
          .withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value
        val view   = application.injector.instanceOf[PageNotFoundView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view("https://www.gov.uk/find-hmrc-contacts/technical-support-with-hmrc-online-services")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to unauthorised page if the backend connector delete returns unexpected failure response" in {

      val backendConnector: NovaImportsBackendConnector = mock[NovaImportsBackendConnector]
      when(backendConnector.deleteDraftNotification(any())(any()))
        .thenReturn(Future.successful(Left(DeleteDraftNotificationError.UpstreamError(400, "failure"))))
      val (application, _, _) = applicationWithMockRepository(userAnswersWithGuardData, backendConnector = backendConnector)

      running(application) {
        val request = FakeRequest(POST, deleteCurrentNotificationSubmitRoute())
          .withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if IQ1 was answered No for data guard" in {

      val answersIq1No = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(VehicleFromEuPage, false)

      val application = applicationBuilder(userAnswers = Some(answersIq1No)).build()

      running(application) {
        val request = FakeRequest(GET, deleteCurrentNotificationPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if draftId is missing" in {

      val answersWithoutDraftId = emptyUserAnswers.set(VehicleFromEuPage, true).success.value
      val application           = applicationBuilder(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request = FakeRequest(GET, deleteCurrentNotificationPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if draftId is missing" in {

      val answersWithoutDraftId = emptyUserAnswers.set(VehicleFromEuPage, true).success.value
      val application           = applicationBuilder(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request = FakeRequest(POST, deleteCurrentNotificationPageLoadRoute)
          .withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must return OK for a GET when VAT-registered organisation has required answers" in {

      val answers: UserAnswers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(VehicleBusinessUsePage, true)
        .unsafeSet(VehicleFromEuPage, false)

      val (application, _, _) =
        applicationWithMockRepository(answers, identifierAction = classOf[FakeVatTraderIdentifierAction])

      running(application) {
        given request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, deleteCurrentNotificationPageLoadRoute)

        val result = route(application, request).value
        val view   = application.injector.instanceOf[DeleteCurrentNotificationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised for a GET when VAT-registered organisation has no answer for VehicleBusinessUsePage" in {

      val answers: UserAnswers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))

      val (application, _, _) =
        applicationWithMockRepository(answers, identifierAction = classOf[FakeVatTraderIdentifierAction])

      running(application) {
        given request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, deleteCurrentNotificationPageLoadRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must return OK for a GET when Non-VAT-registered organisation has required answers" in {

      val answers: UserAnswers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
        .unsafeSet(NotifyingAsPurchaserPage, Purchaser)

      val (application, _, _) =
        applicationWithMockRepository(answers, identifierAction = classOf[FakeOrganisationIdentifierAction])

      running(application) {
        given request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, deleteCurrentNotificationPageLoadRoute)

        val result = route(application, request).value
        val view   = application.injector.instanceOf[DeleteCurrentNotificationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised for a GET when Non-VAT-registered organisation has no answer for VehicleFromEuPage" in {

      val answers: UserAnswers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
        .unsafeSet(NotifyingAsPurchaserPage, Purchaser)

      val (application, _, _) =
        applicationWithMockRepository(answers, identifierAction = classOf[FakeOrganisationIdentifierAction])

      running(application) {
        given request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, deleteCurrentNotificationPageLoadRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when Non-VAT-registered organisation has no answer for BusinessOrPrivatePage" in {

      val answers: UserAnswers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(NotifyingAsPurchaserPage, Purchaser)

      val (application, _, _) =
        applicationWithMockRepository(answers, identifierAction = classOf[FakeOrganisationIdentifierAction])

      running(application) {
        given request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, deleteCurrentNotificationPageLoadRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when Non-VAT-registered organisation has no answer for NotifyingAsPurchaserPage" in {

      val answers: UserAnswers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)

      val (application, _, _) =
        applicationWithMockRepository(answers, identifierAction = classOf[FakeOrganisationIdentifierAction])

      running(application) {
        given request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, deleteCurrentNotificationPageLoadRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must return OK for a GET when an Agent without clients has required answers" in {

      val answers: UserAnswers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
        .unsafeSet(NotifyingAsPurchaserPage, Purchaser)

      val (application, _, _) =
        applicationWithMockRepository(answers, identifierAction = classOf[FakeAgentIdentifierAction])

      running(application) {
        given request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, deleteCurrentNotificationPageLoadRoute)

        val result = route(application, request).value
        val view   = application.injector.instanceOf[DeleteCurrentNotificationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised for a GET when an Agent without clients has no answer for VehicleFromEuPage" in {

      val answers: UserAnswers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
        .unsafeSet(NotifyingAsPurchaserPage, Purchaser)

      val (application, _, _) =
        applicationWithMockRepository(answers, identifierAction = classOf[FakeAgentIdentifierAction])

      running(application) {
        given request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, deleteCurrentNotificationPageLoadRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when an Agent without clients has no answer for BusinessOrPrivatePage" in {

      val answers: UserAnswers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(NotifyingAsPurchaserPage, Purchaser)

      val (application, _, _) =
        applicationWithMockRepository(answers, identifierAction = classOf[FakeAgentIdentifierAction])

      running(application) {
        given request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, deleteCurrentNotificationPageLoadRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when an Agent without clients has no answer for NotifyingAsPurchaserPage" in {

      val answers: UserAnswers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)

      val (application, _, _) =
        applicationWithMockRepository(answers, identifierAction = classOf[FakeAgentIdentifierAction])

      running(application) {
        given request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, deleteCurrentNotificationPageLoadRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

  }
}
