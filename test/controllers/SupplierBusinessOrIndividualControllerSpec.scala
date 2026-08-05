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
import forms.SupplierBusinessOrIndividualFormProvider
import models.{BusinessOrPrivateIndividual, CheckMode, DraftId, NormalMode, SupplierNumber, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplierDetails.{SupplierBusinessOrIndividualPage, SupplierNumberPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.SupplierBusinessOrIndividualView

import scala.concurrent.Future

class SupplierBusinessOrIndividualControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new SupplierBusinessOrIndividualFormProvider()
  val form         = formProvider()

  lazy val supplierBusinessOrIndividualRoute = routes.SupplierBusinessOrIndividualController.onPageLoad(SupplierNumber(1), NormalMode).url

  val userAnswersWithGuardData: UserAnswers = emptyUserAnswers
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(VehicleFromEuPage, true)
    .success
    .value
    .set(SupplierNumberPage, 1)
    .success
    .value

  private def applicationWithMockRepository(userAnswers: UserAnswers): (play.api.Application, SessionRepository) = {

    val mockSessionRepository = mock[SessionRepository]
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

    val application =
      applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

    (application, mockSessionRepository)
  }

  private def savedAnswers(mockSessionRepository: SessionRepository): UserAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
    verify(mockSessionRepository).set(captor.capture())
    captor.getValue
  }

  "SupplierBusinessOrIndividualController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessOrIndividualRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SupplierBusinessOrIndividualView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, SupplierNumber(1), NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithGuardData
        .set(SupplierBusinessOrIndividualPage, BusinessOrPrivateIndividual.Business)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessOrIndividualRoute)

        val view = application.injector.instanceOf[SupplierBusinessOrIndividualView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(BusinessOrPrivateIndividual.Business), SupplierNumber(1), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val (application, _) = applicationWithMockRepository(userAnswersWithGuardData)

      running(application) {
        val request =
          FakeRequest(POST, supplierBusinessOrIndividualRoute)
            .withFormUrlEncodedBody(("value", BusinessOrPrivateIndividual.Business.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must keep the same supplier number when the user returns to change their answer" in {

      val answersForSupplierThree              = userAnswersWithGuardData.set(SupplierNumberPage, 3).success.value
      val (application, mockSessionRepository) = applicationWithMockRepository(answersForSupplierThree)

      running(application) {
        val request =
          FakeRequest(POST, routes.SupplierBusinessOrIndividualController.onSubmit(SupplierNumber(3), CheckMode).url)
            .withFormUrlEncodedBody(("value", BusinessOrPrivateIndividual.Business.toString))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER

        val answers = savedAnswers(mockSessionRepository)

        answers.get(SupplierNumberPage) mustEqual Some(3)
        answers.get(SupplierBusinessOrIndividualPage) mustEqual Some(BusinessOrPrivateIndividual.Business)
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, supplierBusinessOrIndividualRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[SupplierBusinessOrIndividualView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, SupplierNumber(1), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised for a GET if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessOrIndividualRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, supplierBusinessOrIndividualRoute)
            .withFormUrlEncodedBody(("value", BusinessOrPrivateIndividual.Business.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if IQ1 was answered No for data guard" in {

      val answersIq1No = emptyUserAnswers
        .set(DraftIdPage, DraftId("DRAFT-001"))
        .success
        .value
        .set(VehicleFromEuPage, false)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(answersIq1No)).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessOrIndividualRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if draftId is missing" in {

      val answersWithoutDraftId = emptyUserAnswers.set(VehicleFromEuPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessOrIndividualRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if draftId is missing" in {

      val answersWithoutDraftId = emptyUserAnswers.set(VehicleFromEuPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request =
          FakeRequest(POST, supplierBusinessOrIndividualRoute)
            .withFormUrlEncodedBody(("value", BusinessOrPrivateIndividual.Business.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, routes.SupplierBusinessOrIndividualController.onPageLoad(SupplierNumber(2), NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, routes.SupplierBusinessOrIndividualController.onSubmit(SupplierNumber(2), NormalMode).url)
            .withFormUrlEncodedBody(("value", BusinessOrPrivateIndividual.Business.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if there is no supplier number in session" in {

      val answersWithoutSupplierNumber = emptyUserAnswers
        .set(DraftIdPage, DraftId("DRAFT-001"))
        .success
        .value
        .set(VehicleFromEuPage, true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(answersWithoutSupplierNumber)).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessOrIndividualRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
