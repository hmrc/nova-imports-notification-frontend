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
import controllers.actions.*
import forms.IsYourAddressInTheUkFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.IsYourAddressInTheUkPage
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.IsYourAddressInTheUkView

import scala.concurrent.Future

class IsYourAddressInTheUkControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider = new IsYourAddressInTheUkFormProvider()
  val form         = formProvider()

  lazy val isYourAddressInTheUkRoute       = routes.IsYourAddressInTheUkController.onPageLoad(NormalMode).url
  lazy val isYourAddressInTheUkSubmitRoute = routes.IsYourAddressInTheUkController.onSubmit(NormalMode).url

  "IsYourAddressInTheUkController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, isYourAddressInTheUkRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[IsYourAddressInTheUkView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(IsYourAddressInTheUkPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, isYourAddressInTheUkRoute)

        val view = application.injector.instanceOf[IsYourAddressInTheUkView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, isYourAddressInTheUkSubmitRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, isYourAddressInTheUkSubmitRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[IsYourAddressInTheUkView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, isYourAddressInTheUkRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, isYourAddressInTheUkSubmitRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET when the user is an Agent (not permitted by data guard)" in {

      val application = new GuiceApplicationBuilder()
        .overrides(
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          bind[IdentifierAction].to[FakeAgentIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeAgentIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeAgentIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(emptyUserAnswers)))
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, isYourAddressInTheUkRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST when the user is an Agent (not permitted by data guard)" in {

      val application = new GuiceApplicationBuilder()
        .overrides(
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          bind[IdentifierAction].to[FakeAgentIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeAgentIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeAgentIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(emptyUserAnswers)))
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, isYourAddressInTheUkSubmitRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when the user is an OGD (Type 8) user" in {

      val application = new GuiceApplicationBuilder()
        .overrides(
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          bind[IdentifierAction].to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[UnauthorisedIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(emptyUserAnswers)))
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, isYourAddressInTheUkRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
