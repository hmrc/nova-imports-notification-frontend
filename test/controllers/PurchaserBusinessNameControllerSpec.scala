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
import forms.PurchaserBusinessNameFormProvider
import models.{CheckMode, DraftId, NormalMode, PurchaserBusinessOrIndividual, PurchaserOrOnBehalf, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.{PurchaserBusinessOrIndividualPage, PurchaserOrOnBehalfPage}
import pages.sections.purchaserDetails.PurchaserBusinessNamePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.PurchaserBusinessNameView

import scala.concurrent.Future

class PurchaserBusinessNameControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider                          = new PurchaserBusinessNameFormProvider()
  val form                                  = formProvider()
  lazy val purchaserBusinessNameRoute       = routes.PurchaserBusinessNameController.onPageLoad(NormalMode).url
  lazy val purchaserBusinessNameChangeRoute = routes.PurchaserBusinessNameController.onPageLoad(CheckMode).url
  val validName                             = "Acme Trading Co Ltd"

  val requiredAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
    .unsafeSet(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredBusiness)

  "PurchaserBusinessName Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, purchaserBusinessNameRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PurchaserBusinessNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = requiredAnswers.unsafeSet(PurchaserBusinessNamePage, validName)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, purchaserBusinessNameRoute)

        val view = application.injector.instanceOf[PurchaserBusinessNameView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validName), NormalMode)(request, messages(application)).toString
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
          FakeRequest(POST, purchaserBusinessNameRoute)
            .withFormUrlEncodedBody(("value", validName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must save the business name to UserAnswers on submission" in {

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
          FakeRequest(POST, purchaserBusinessNameRoute)
            .withFormUrlEncodedBody(("value", validName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        verify(mockSessionRepository).set(captor.capture())
        captor.getValue.get(PurchaserBusinessNamePage) mustBe Some(validName)
      }
    }

    "must return a Bad Request and errors when the business name is blank" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaserBusinessNameRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[PurchaserBusinessNameView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when the business name exceeds 160 characters" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()
      val tooLong     = "a" * 161

      running(application) {
        val request =
          FakeRequest(POST, purchaserBusinessNameRoute)
            .withFormUrlEncodedBody(("value", tooLong))

        val boundForm = form.bind(Map("value" -> tooLong))

        val view = application.injector.instanceOf[PurchaserBusinessNameView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when the business name contains invalid characters" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaserBusinessNameRoute)
            .withFormUrlEncodedBody(("value", "Acme#Ltd"))

        val boundForm = form.bind(Map("value" -> "Acme#Ltd"))

        val view = application.injector.instanceOf[PurchaserBusinessNameView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised for a GET when the purchaser is not on behalf of a purchaser" in {
      val answers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, purchaserBusinessNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when the purchaser is not a non-VAT registered business" in {
      val answers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, purchaserBusinessNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when no draft is in progress" in {
      val answers = emptyUserAnswers
        .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
        .unsafeSet(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredBusiness)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, purchaserBusinessNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST when the breadcrumb is missing" in {
      val answers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaserBusinessNameRoute)
            .withFormUrlEncodedBody(("value", validName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must allow CheckMode access with a valid breadcrumb" in {
      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, purchaserBusinessNameChangeRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Unauthorised for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, purchaserBusinessNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaserBusinessNameRoute)
            .withFormUrlEncodedBody(("value", validName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
