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
import forms.SupplierBusinessNameFormProvider
import models.{BusinessOrPrivateIndividual, CheckMode, DraftId, NormalMode, SupplierNumber, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplierDetails.{SupplierBusinessNamePage, SupplierBusinessOrIndividualPage, SupplierNumberPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.SupplierBusinessNameView

import scala.concurrent.Future

class SupplierBusinessNameControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider = new SupplierBusinessNameFormProvider()
  val form         = formProvider()

  lazy val supplierBusinessNameRoute       = routes.SupplierBusinessNameController.onPageLoad(SupplierNumber(1), NormalMode).url
  lazy val supplierBusinessNameChangeRoute = routes.SupplierBusinessNameController.onPageLoad(SupplierNumber(1), CheckMode).url

  val validName = "Acme Trading Co Ltd"

  private val requiredAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(SupplierBusinessOrIndividualPage, BusinessOrPrivateIndividual.Business)
    .unsafeSet(SupplierNumberPage, 1)

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

  "SupplierBusinessNameController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessNameRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SupplierBusinessNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, SupplierNumber(1), NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers.unsafeSet(SupplierBusinessNamePage, validName))).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessNameRoute)

        val view = application.injector.instanceOf[SupplierBusinessNameView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validName), SupplierNumber(1), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must save the business name and redirect to the next page when valid data is submitted" in {

      val (application, mockSessionRepository) = applicationWithMockRepository(requiredAnswers)

      running(application) {
        val request = FakeRequest(POST, supplierBusinessNameRoute).withFormUrlEncodedBody(("value", validName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        savedAnswers(mockSessionRepository).get(SupplierBusinessNamePage) mustEqual Some(validName)
      }
    }

    "must keep the same supplier number when the user returns to change their answer" in {

      val (application, mockSessionRepository) = applicationWithMockRepository(requiredAnswers.unsafeSet(SupplierNumberPage, 3))

      running(application) {
        val request = FakeRequest(POST, routes.SupplierBusinessNameController.onSubmit(SupplierNumber(3), CheckMode).url)
          .withFormUrlEncodedBody(("value", validName))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER

        val answers = savedAnswers(mockSessionRepository)

        answers.get(SupplierNumberPage) mustEqual Some(3)
        answers.get(SupplierBusinessNamePage) mustEqual Some(validName)
      }
    }

    "must return a Bad Request and errors when the business name is blank" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, supplierBusinessNameRoute).withFormUrlEncodedBody(("value", ""))

        val view = application.injector.instanceOf[SupplierBusinessNameView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(form.bind(Map("value" -> "")), SupplierNumber(1), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request and errors when the business name exceeds 160 characters" in {

      val tooLong = "a" * 161

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, supplierBusinessNameRoute).withFormUrlEncodedBody(("value", tooLong))

        val view = application.injector.instanceOf[SupplierBusinessNameView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(form.bind(Map("value" -> tooLong)), SupplierNumber(1), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request and errors when the business name contains invalid characters" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, supplierBusinessNameRoute).withFormUrlEncodedBody(("value", "Acme#Ltd"))

        val view = application.injector.instanceOf[SupplierBusinessNameView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(form.bind(Map("value" -> "Acme#Ltd")), SupplierNumber(1), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Unauthorised for a GET if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, supplierBusinessNameRoute).withFormUrlEncodedBody(("value", validName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if draftId is missing" in {

      val answersWithoutDraftId = emptyUserAnswers
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(SupplierBusinessOrIndividualPage, BusinessOrPrivateIndividual.Business)
        .unsafeSet(SupplierNumberPage, 1)

      val application = applicationBuilder(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if draftId is missing" in {

      val answersWithoutDraftId = emptyUserAnswers
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(SupplierBusinessOrIndividualPage, BusinessOrPrivateIndividual.Business)
        .unsafeSet(SupplierNumberPage, 1)

      val application = applicationBuilder(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request = FakeRequest(POST, supplierBusinessNameRoute).withFormUrlEncodedBody(("value", validName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the vehicle was not brought from the EU" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers.unsafeSet(VehicleFromEuPage, false))).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the supplier is a private individual" in {

      val answersForPrivateIndividual =
        requiredAnswers.unsafeSet(SupplierBusinessOrIndividualPage, BusinessOrPrivateIndividual.PrivateIndividual)

      val application = applicationBuilder(userAnswers = Some(answersForPrivateIndividual)).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if the supplier is a private individual" in {

      val answersForPrivateIndividual =
        requiredAnswers.unsafeSet(SupplierBusinessOrIndividualPage, BusinessOrPrivateIndividual.PrivateIndividual)

      val application = applicationBuilder(userAnswers = Some(answersForPrivateIndividual)).build()

      running(application) {
        val request = FakeRequest(POST, supplierBusinessNameRoute).withFormUrlEncodedBody(("value", validName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the supplier type has not been answered" in {

      val answersWithoutSupplierType = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(SupplierNumberPage, 1)

      val application = applicationBuilder(userAnswers = Some(answersWithoutSupplierType)).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.SupplierBusinessNameController.onPageLoad(SupplierNumber(2), NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.SupplierBusinessNameController.onSubmit(SupplierNumber(2), NormalMode).url)
          .withFormUrlEncodedBody(("value", validName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if there is no supplier number in session" in {

      val answersWithoutSupplierNumber = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(SupplierBusinessOrIndividualPage, BusinessOrPrivateIndividual.Business)

      val application = applicationBuilder(userAnswers = Some(answersWithoutSupplierNumber)).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must allow access when the user returns to change their answer" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, supplierBusinessNameChangeRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }
  }
}
