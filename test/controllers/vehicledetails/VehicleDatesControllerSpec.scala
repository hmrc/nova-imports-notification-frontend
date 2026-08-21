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

package controllers.vehicledetails

import base.SpecBase
import controllers.{routes, vehicledetails}
import forms.VehicleDatesFormProvider
import models.{DraftId, NormalMode, SupplierNumber, UserAnswers, VehicleDates, VehicleNumber}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.VehicleDatesPage
import play.api.inject.bind
import play.api.libs.json.Json
import queries.{AllSuppliersQuery, AllVehiclesQuery}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.VehicleDatesView

import scala.concurrent.Future

class VehicleDatesControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new VehicleDatesFormProvider()
  val form         = formProvider()

  val supplierNumber = SupplierNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  lazy val vehicleDatesRoute = vehicledetails.routes.VehicleDatesController.onPageLoad(supplierNumber, vehicleNumber, NormalMode).url

  val userAnswersWithGuardData: UserAnswers = emptyUserAnswers
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(VehicleFromEuPage, true)
    .success
    .value
    .set(AllSuppliersQuery, Map("1" -> Json.obj()))
    .success
    .value
    .set(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
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

  "VehicleDatesController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, vehicleDatesRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VehicleDatesView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, supplierNumber, vehicleNumber, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val answer = Set(VehicleDates.PurchaseInvoiceDate)

      val userAnswers = userAnswersWithGuardData
        .set(VehicleDatesPage(vehicleNumber), answer)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, vehicleDatesRoute)

        val view = application.injector.instanceOf[VehicleDatesView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(answer), supplierNumber, vehicleNumber, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val (application, _) = applicationWithMockRepository(userAnswersWithGuardData)

      running(application) {
        val request =
          FakeRequest(POST, vehicleDatesRoute)
            .withFormUrlEncodedBody(("value[]", VehicleDates.PurchaseInvoiceDate.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must save both date options when they are submitted together" in {

      val (application, mockSessionRepository) = applicationWithMockRepository(userAnswersWithGuardData)

      running(application) {
        val request =
          FakeRequest(POST, vehicleDatesRoute)
            .withFormUrlEncodedBody(
              ("value[]", VehicleDates.PurchaseInvoiceDate.toString),
              ("value[]", VehicleDates.AvailabilityAndFirstRegistration.toString)
            )

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER

        savedAnswers(mockSessionRepository).get(VehicleDatesPage(vehicleNumber)) mustEqual Some(
          Set(VehicleDates.PurchaseInvoiceDate, VehicleDates.AvailabilityAndFirstRegistration)
        )
      }
    }

    "must return a Bad Request and errors when nothing is selected" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(POST, vehicleDatesRoute).withFormUrlEncodedBody()

        val boundForm = form.bind(Map.empty[String, String])

        val view = application.injector.instanceOf[VehicleDatesView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, supplierNumber, vehicleNumber, NormalMode)(request, messages(application)).toString
      }
    }

    "must return a Bad Request when a date is selected alongside no dates" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, vehicleDatesRoute)
            .withFormUrlEncodedBody(
              ("value[]", VehicleDates.PurchaseInvoiceDate.toString),
              ("value[]", VehicleDates.NoDates.toString)
            )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("vehicleDates.error.required"))
      }
    }

    "must redirect to Unauthorised for a GET if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, vehicleDatesRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, vehicleDatesRoute)
            .withFormUrlEncodedBody(("value[]", VehicleDates.PurchaseInvoiceDate.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if IQ1 was answered No" in {

      val answersIq1No = emptyUserAnswers
        .set(DraftIdPage, DraftId("DRAFT-001"))
        .success
        .value
        .set(VehicleFromEuPage, false)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(answersIq1No)).build()

      running(application) {
        val request = FakeRequest(GET, vehicleDatesRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if draftId is missing" in {

      val answersWithoutDraftId = emptyUserAnswers.set(VehicleFromEuPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request = FakeRequest(GET, vehicleDatesRoute)

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
          FakeRequest(POST, vehicleDatesRoute)
            .withFormUrlEncodedBody(("value[]", VehicleDates.PurchaseInvoiceDate.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, vehicledetails.routes.VehicleDatesController.onPageLoad(SupplierNumber(2), vehicleNumber, NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, vehicledetails.routes.VehicleDatesController.onSubmit(SupplierNumber(2), vehicleNumber, NormalMode).url)
            .withFormUrlEncodedBody(("value[]", VehicleDates.PurchaseInvoiceDate.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the vehicle number in the URL is not one of the user's vehicles" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, vehicledetails.routes.VehicleDatesController.onPageLoad(supplierNumber, VehicleNumber(999), NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if the vehicle number in the URL is not one of the user's vehicles" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, vehicledetails.routes.VehicleDatesController.onSubmit(supplierNumber, VehicleNumber(999), NormalMode).url)
            .withFormUrlEncodedBody(("value[]", VehicleDates.PurchaseInvoiceDate.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different supplier" in {

      val answers = userAnswersWithGuardData
        .set(AllSuppliersQuery, Map("1" -> Json.obj(), "2" -> Json.obj()))
        .success
        .value
        .set(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 2)))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, vehicledetails.routes.VehicleDatesController.onPageLoad(supplierNumber, vehicleNumber, NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must return OK for a GET on vehicle 3 when the supplier has vehicles 1 and 3 in session" in {

      val answers = userAnswersWithGuardData
        .set(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1), "3" -> Json.obj("supplierNumber" -> 1)))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, vehicledetails.routes.VehicleDatesController.onPageLoad(supplierNumber, VehicleNumber(3), NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }
  }
}
