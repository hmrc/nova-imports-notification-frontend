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
import forms.PurchaseInvoiceDateFormProvider
import models.{DraftId, NormalMode, SupplierNumber, UserAnswers, VehicleDates, VehicleNumber}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{PurchaseInvoiceDatePage, VehicleDatesPage}
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import views.html.PurchaseInvoiceDateView

import java.time.LocalDate
import scala.concurrent.Future

class PurchaseInvoiceDateControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val supplierNumber = SupplierNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  val answer: LocalDate = LocalDate.of(2026, 3, 27)

  lazy val purchaseInvoiceDateRoute =
    vehicledetails.routes.PurchaseInvoiceDateController.onPageLoad(supplierNumber, vehicleNumber, NormalMode).url

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

  private def dateFields(day: String, month: String, year: String) =
    Seq("value.day" -> day, "value.month" -> month, "value.year" -> year)

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

  "PurchaseInvoiceDateController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, purchaseInvoiceDateRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PurchaseInvoiceDateView]
        val form = new PurchaseInvoiceDateFormProvider()()(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, supplierNumber, vehicleNumber, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithGuardData
        .set(PurchaseInvoiceDatePage(vehicleNumber), answer)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, purchaseInvoiceDateRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PurchaseInvoiceDateView]
        val form = new PurchaseInvoiceDateFormProvider()()(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(answer), supplierNumber, vehicleNumber, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page and save the date when valid data is submitted" in {

      val (application, mockSessionRepository) = applicationWithMockRepository(userAnswersWithGuardData)

      running(application) {
        val request =
          FakeRequest(POST, purchaseInvoiceDateRoute)
            .withFormUrlEncodedBody(dateFields("27", "03", "2026")*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        savedAnswers(mockSessionRepository).get(PurchaseInvoiceDatePage(vehicleNumber)) mustEqual Some(answer)
      }
    }

    "must return a Bad Request and the empty date error when no date is entered" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaseInvoiceDateRoute)
            .withFormUrlEncodedBody(dateFields("", "", "")*)

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("purchaseInvoiceDate.error.required.all"))
      }
    }

    "must return a Bad Request and the incomplete date error when a part of the date is missing" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaseInvoiceDateRoute)
            .withFormUrlEncodedBody(dateFields("27", "03", "")*)

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("purchaseInvoiceDate.error.required", messages(application)("date.error.year")))
      }
    }

    "must return a Bad Request and the format error when the date is not made up of numbers" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaseInvoiceDateRoute)
            .withFormUrlEncodedBody(dateFields("aa", "03", "2026")*)

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("purchaseInvoiceDate.error.invalid"))
      }
    }

    "must return a Bad Request and the real date error when the date does not exist" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaseInvoiceDateRoute)
            .withFormUrlEncodedBody(dateFields("31", "02", "2026")*)

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("purchaseInvoiceDate.error.notARealDate"))
      }
    }

    "must redirect to Unauthorised for a GET if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, purchaseInvoiceDateRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaseInvoiceDateRoute)
            .withFormUrlEncodedBody(dateFields("27", "03", "2026")*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if draftId is missing" in {

      val answersWithoutDraftId = userAnswersWithGuardData.remove(DraftIdPage).success.value

      val application = applicationBuilder(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request = FakeRequest(GET, purchaseInvoiceDateRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if draftId is missing" in {

      val answersWithoutDraftId = userAnswersWithGuardData.remove(DraftIdPage).success.value

      val application = applicationBuilder(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaseInvoiceDateRoute)
            .withFormUrlEncodedBody(dateFields("27", "03", "2026")*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if IQ1 was answered No" in {

      val answersIq1No = userAnswersWithGuardData.set(VehicleFromEuPage, false).success.value

      val application = applicationBuilder(userAnswers = Some(answersIq1No)).build()

      running(application) {
        val request = FakeRequest(GET, purchaseInvoiceDateRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must return OK for a GET when the purchase invoice date was not one of the dates selected on AVD3.0" in {

      val answersWithoutInvoiceDate = userAnswersWithGuardData
        .set(VehicleDatesPage(supplierNumber, vehicleNumber), Set(VehicleDates.AvailabilityAndFirstRegistration))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(answersWithoutInvoiceDate)).build()

      running(application) {
        val request = FakeRequest(GET, purchaseInvoiceDateRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must return OK for a GET when AVD3.0 has not been answered at all" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, purchaseInvoiceDateRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(GET, vehicledetails.routes.PurchaseInvoiceDateController.onPageLoad(SupplierNumber(2), vehicleNumber, NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the vehicle number in the URL is not one of the user's vehicles" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(GET, vehicledetails.routes.PurchaseInvoiceDateController.onPageLoad(supplierNumber, VehicleNumber(999), NormalMode).url)

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
        val request = FakeRequest(GET, purchaseInvoiceDateRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
