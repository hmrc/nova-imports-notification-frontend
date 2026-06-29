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
import connectors.AddressLookupError
import controllers.actions.*
import forms.IsYourAddressInTheUkFormProvider
import models.NormalMode
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.NotificationTaskListPage
import pages.sections.initialquestions.VehicleBusinessUsePage
import pages.sections.notifieraddress.IsYourAddressInTheUkPage
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.AddressLookupService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.IsYourAddressInTheUkView

import scala.concurrent.Future

class IsYourAddressInTheUkControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new IsYourAddressInTheUkFormProvider()
  val form         = formProvider()

  lazy val isYourAddressInTheUkRoute       = routes.IsYourAddressInTheUkController.onPageLoad(NormalMode).url
  lazy val isYourAddressInTheUkSubmitRoute = routes.IsYourAddressInTheUkController.onSubmit(NormalMode).url

  private val answersSatisfyingGuard =
    emptyUserAnswers
      .set(NotificationTaskListPage, true)
      .success
      .value
      .set(VehicleBusinessUsePage, false)
      .success
      .value

  "IsYourAddressInTheUkController" - {

    "must return OK and the correct view for a GET when guard passes" in {

      val application = applicationBuilder(userAnswers = Some(answersSatisfyingGuard)).build()

      running(application) {
        val request = FakeRequest(GET, isYourAddressInTheUkRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[IsYourAddressInTheUkView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = answersSatisfyingGuard.set(IsYourAddressInTheUkPage, true).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, isYourAddressInTheUkRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[IsYourAddressInTheUkView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the ALF journey URL on successful submission (Yes -> UK journey)" in {

      val journeyUrl            = "http://localhost:9028/lookup-address/abc123/lookup"
      val mockSessionRepository = mock[SessionRepository]
      val mockAlfService        = mock[AddressLookupService]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockAlfService.initJourney(eqTo(true), any[String])(any[HeaderCarrier])).thenReturn(Future.successful(Right(journeyUrl)))

      val application = applicationBuilder(userAnswers = Some(answersSatisfyingGuard))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[AddressLookupService].toInstance(mockAlfService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, isYourAddressInTheUkSubmitRoute).withFormUrlEncodedBody("value" -> "true")
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyUrl
        verify(mockAlfService).initJourney(eqTo(true), any[String])(any[HeaderCarrier])
      }
    }

    "must redirect to the ALF journey URL on successful submission (No -> non-UK journey)" in {

      val journeyUrl            = "http://localhost:9028/lookup-address/xyz789/international/edit"
      val mockSessionRepository = mock[SessionRepository]
      val mockAlfService        = mock[AddressLookupService]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockAlfService.initJourney(eqTo(false), any[String])(any[HeaderCarrier])).thenReturn(Future.successful(Right(journeyUrl)))

      val application = applicationBuilder(userAnswers = Some(answersSatisfyingGuard))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[AddressLookupService].toInstance(mockAlfService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, isYourAddressInTheUkSubmitRoute).withFormUrlEncodedBody("value" -> "false")
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual journeyUrl
        verify(mockAlfService).initJourney(eqTo(false), any[String])(any[HeaderCarrier])
      }
    }

    "must redirect to Journey Recovery when ALF init fails" in {

      val mockSessionRepository = mock[SessionRepository]
      val mockAlfService        = mock[AddressLookupService]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockAlfService.initJourney(any[Boolean], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(AddressLookupError.UpstreamError(500, "boom"))))

      val application = applicationBuilder(userAnswers = Some(answersSatisfyingGuard))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[AddressLookupService].toInstance(mockAlfService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, isYourAddressInTheUkSubmitRoute).withFormUrlEncodedBody("value" -> "true")
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(answersSatisfyingGuard)).build()

      running(application) {
        val request   = FakeRequest(POST, isYourAddressInTheUkSubmitRoute).withFormUrlEncodedBody("value" -> "")
        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[IsYourAddressInTheUkView]
        val result    = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery when NotificationTaskListPage is not set (user bypassed NTL)" in {

      val answersWithoutNtl =
        emptyUserAnswers.set(VehicleBusinessUsePage, false).success.value
      val application = applicationBuilder(userAnswers = Some(answersWithoutNtl)).build()

      running(application) {
        val request = FakeRequest(GET, isYourAddressInTheUkRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when VehicleBusinessUsePage = true (address section hidden when OQ1.0 = Yes)" in {

      val answersBusinessUse =
        emptyUserAnswers
          .set(NotificationTaskListPage, true)
          .success
          .value
          .set(VehicleBusinessUsePage, true)
          .success
          .value
      val application = applicationBuilder(userAnswers = Some(answersBusinessUse)).build()

      running(application) {
        val request = FakeRequest(GET, isYourAddressInTheUkRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, isYourAddressInTheUkRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, isYourAddressInTheUkSubmitRoute).withFormUrlEncodedBody("value" -> "true")
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when the user is not a VAT-registered organisation" in {

      val application = new GuiceApplicationBuilder()
        .overrides(
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          bind[IdentifierAction].to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[UnauthorisedIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(answersSatisfyingGuard)))
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, isYourAddressInTheUkRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
