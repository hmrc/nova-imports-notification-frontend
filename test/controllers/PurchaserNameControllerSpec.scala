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
import forms.PurchaserNameFormProvider
import models.{CheckMode, DraftId, NameDetails, NormalMode, PurchaserBusinessOrIndividual, PurchaserOrOnBehalf, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.{PurchaserBusinessOrIndividualPage, PurchaserOrOnBehalfPage}
import pages.sections.purchaserDetails.PurchaserNamePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.PurchaserNameView

import scala.concurrent.Future

class PurchaserNameControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider = new PurchaserNameFormProvider()
  val form         = formProvider()

  lazy val purchaserNameRoute       = routes.PurchaserNameController.onPageLoad(NormalMode).url
  lazy val purchaserNameChangeRoute = routes.PurchaserNameController.onPageLoad(CheckMode).url

  val validTitle     = "Mr"
  val validFirstName = "John"
  val validLastName  = "Doe"

  // A user reaches /purchaser-name after answering IQ3.0 "As the purchaser",
  // or IQ3.0 "On behalf of a purchaser" and IQ3.1 "Private individual".
  private val requiredPreviousAnswers = emptyUserAnswers
    .set(pages.DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser)
    .success
    .value

  "PurchaserName Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(requiredPreviousAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, purchaserNameRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PurchaserNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must allow access when notifying on behalf of a private individual purchaser" in {

      val userAnswers = emptyUserAnswers
        .set(pages.DraftIdPage, DraftId("DRAFT-001"))
        .success
        .value
        .set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
        .success
        .value
        .set(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredPrivateIndividual)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, purchaserNameRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val answer      = NameDetails(validTitle, validFirstName, validLastName)
      val userAnswers = requiredPreviousAnswers.set(PurchaserNamePage, answer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, purchaserNameRoute)

        val view = application.injector.instanceOf[PurchaserNameView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(answer), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(requiredPreviousAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, purchaserNameRoute)
            .withFormUrlEncodedBody(
              ("title", validTitle),
              ("firstName", validFirstName),
              ("lastName", validLastName)
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must save the full name to UserAnswers on submission" in {

      val mockSessionRepository = mock[SessionRepository]
      val captor                = ArgumentCaptor.forClass(classOf[UserAnswers])

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(requiredPreviousAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, purchaserNameRoute)
            .withFormUrlEncodedBody(
              ("title", validTitle),
              ("firstName", validFirstName),
              ("lastName", validLastName)
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        verify(mockSessionRepository).set(captor.capture())
        captor.getValue.get(PurchaserNamePage) mustBe Some(NameDetails(validTitle, validFirstName, validLastName))
      }
    }

    "must return a Bad Request and errors when the first name is blank" in {

      val application = applicationBuilder(userAnswers = Some(requiredPreviousAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaserNameRoute)
            .withFormUrlEncodedBody(("title", validTitle), ("firstName", ""), ("lastName", validLastName))

        val boundForm = form.bind(Map("title" -> validTitle, "firstName" -> "", "lastName" -> validLastName))

        val view = application.injector.instanceOf[PurchaserNameView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised when upstream initial-question answers are missing" in {

      val answers = emptyUserAnswers.set(pages.DraftIdPage, DraftId("DRAFT-001")).success.value

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, purchaserNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when notifying on behalf of a business purchaser" in {

      val userAnswers = emptyUserAnswers
        .set(pages.DraftIdPage, DraftId("DRAFT-001"))
        .success
        .value
        .set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
        .success
        .value
        .set(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredBusiness)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, purchaserNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must allow CheckMode access when the purchaser name has already been answered" in {

      val userAnswers = requiredPreviousAnswers
        .set(PurchaserNamePage, NameDetails(validTitle, validFirstName, validLastName))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, purchaserNameChangeRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Unauthorised for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, purchaserNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when no draft is in progress" in {

      val answersWithoutDraft = emptyUserAnswers
        .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser)

      val application = applicationBuilder(userAnswers = Some(answersWithoutDraft)).build()

      running(application) {
        val request = FakeRequest(GET, purchaserNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaserNameRoute)
            .withFormUrlEncodedBody(("title", validTitle), ("firstName", validFirstName), ("lastName", validLastName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
