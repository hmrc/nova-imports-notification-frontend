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
import forms.EmailAddressFormProvider
import models.{BusinessOrPrivateIndividual, CheckMode, DraftId, NameDetails, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.BusinessOrPrivatePage
import pages.sections.notifierDetails.{EmailAddressPage, NameDetailsPage, PhoneNumberPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.EmailAddressView

import scala.concurrent.Future

class EmailAddressControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider                 = new EmailAddressFormProvider()
  val form                         = formProvider()
  lazy val emailAddressRoute       = routes.EmailAddressController.onPageLoad(NormalMode).url
  lazy val emailAddressChangeRoute = routes.EmailAddressController.onPageLoad(CheckMode).url
  val validEmail                   = "name@example.com"

  val requiredAnswers: UserAnswers = emptyUserAnswers
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(PhoneNumberPage, "07000000000")
    .success
    .value

  val cyaCompleteAnswers: UserAnswers = emptyUserAnswers
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
    .success
    .value
    .set(NameDetailsPage, NameDetails("Mr", "John", "Doe"))
    .success
    .value
    .set(PhoneNumberPage, "07000000000")
    .success
    .value
    .set(EmailAddressPage, validEmail)
    .success
    .value

  "EmailAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, emailAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EmailAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = requiredAnswers.set(EmailAddressPage, validEmail).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, emailAddressRoute)

        val view = application.injector.instanceOf[EmailAddressView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validEmail), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, emailAddressRoute)
            .withFormUrlEncodedBody(("value", validEmail))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must save the email to UserAnswers on submission" in {

      val mockSessionRepository = mock[SessionRepository]
      val captor                = ArgumentCaptor.forClass(classOf[UserAnswers])

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, emailAddressRoute)
            .withFormUrlEncodedBody(("value", validEmail))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        verify(mockSessionRepository).set(captor.capture())
        captor.getValue.get(EmailAddressPage) mustBe Some(validEmail)
      }
    }

    "must return a Bad Request and errors when the email is blank" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, emailAddressRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[EmailAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when the email exceeds 70 characters" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()
      val tooLong     = "a" * 71 + "@b.com"

      running(application) {
        val request =
          FakeRequest(POST, emailAddressRoute)
            .withFormUrlEncodedBody(("value", tooLong))

        val boundForm = form.bind(Map("value" -> tooLong))

        val view = application.injector.instanceOf[EmailAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when the email is malformed" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, emailAddressRoute)
            .withFormUrlEncodedBody(("value", "name+plus@example.com"))

        val boundForm = form.bind(Map("value" -> "name+plus@example.com"))

        val view = application.injector.instanceOf[EmailAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised when PhoneNumberPage is not set" in {
      val answers     = emptyUserAnswers.set(DraftIdPage, DraftId("DRAFT-001")).success.value
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, emailAddressRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when no draft is in progress" in {
      val answers     = emptyUserAnswers.set(PhoneNumberPage, "07000000000").success.value
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, emailAddressRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must allow CheckMode access from completed CYA2 data" in {
      val application = applicationBuilder(userAnswers = Some(cyaCompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, emailAddressChangeRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, emailAddressRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, emailAddressRoute)
            .withFormUrlEncodedBody(("value", validEmail))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
