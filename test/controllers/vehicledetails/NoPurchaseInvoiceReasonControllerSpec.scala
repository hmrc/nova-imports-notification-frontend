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
import com.google.inject.name.Names
import controllers.actions.{DataRequiredAction, DataRequiredActionImpl, DataRetrievalAction, FakeAgentNoEnrolmentsIdentifierAction, FakeDataRetrievalAction, FakeIdentifierAction, FakeOrganisationIdentifierAction, IdentifierAction}
import controllers.{routes, vehicledetails}
import forms.NoPurchaseInvoiceReasonFormProvider
import models.{DraftId, NormalMode, SupplierNumber, UserAnswers, VehicleDates, VehicleNumber}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{NoPurchaseInvoiceReasonPage, VehicleDatesPage}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import views.html.NoPurchaseInvoiceReasonView

import scala.concurrent.Future

class NoPurchaseInvoiceReasonControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new NoPurchaseInvoiceReasonFormProvider()
  val form         = formProvider()

  val supplierNumber = SupplierNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  val answer = "The supplier did not issue an invoice"

  lazy val noPurchaseInvoiceReasonRoute =
    vehicledetails.routes.NoPurchaseInvoiceReasonController.onPageLoad(supplierNumber, vehicleNumber, NormalMode).url

  val userAnswersWithGuardData: UserAnswers = emptyUserAnswers
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(VehicleFromEuPage, true)
    .success
    .value
    .set(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .success
    .value
    .set(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
    .success
    .value
    .set(VehicleDatesPage(SupplierNumber(1), VehicleNumber(1)), Set[VehicleDates](VehicleDates.AvailabilityAndFirstRegistration))
    .success
    .value

  private def applicationForUserType(identifierAction: Class[? <: IdentifierAction], userAnswers: UserAnswers): play.api.Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswers)))
      )
      .build()

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

  "NoPurchaseInvoiceReasonController" - {

    "must be served from /supplier/1/vehicle/1/no-purchase-invoice-reason" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        noPurchaseInvoiceReasonRoute mustEqual "/nova-imports/supplier/1/vehicle/1/no-purchase-invoice-reason"
      }
    }

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, noPurchaseInvoiceReasonRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NoPurchaseInvoiceReasonView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, supplierNumber, vehicleNumber, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithGuardData
        .set(NoPurchaseInvoiceReasonPage(supplierNumber, vehicleNumber), answer)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, noPurchaseInvoiceReasonRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NoPurchaseInvoiceReasonView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(answer), supplierNumber, vehicleNumber, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page and save the reason when valid data is submitted" in {

      val (application, mockSessionRepository) = applicationWithMockRepository(userAnswersWithGuardData)

      running(application) {
        val request =
          FakeRequest(POST, noPurchaseInvoiceReasonRoute)
            .withFormUrlEncodedBody(("value", answer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        savedAnswers(mockSessionRepository).get(NoPurchaseInvoiceReasonPage(supplierNumber, vehicleNumber)) mustEqual Some(answer)
      }
    }

    "must return a Bad Request and the required error when nothing is entered" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, noPurchaseInvoiceReasonRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("noPurchaseInvoiceReason.error.required"))
      }
    }

    "must return a Bad Request and the length error when more than 160 characters are entered" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, noPurchaseInvoiceReasonRoute)
            .withFormUrlEncodedBody(("value", "A" * 161))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("noPurchaseInvoiceReason.error.length"))
      }
    }

    "must return a Bad Request and the format error when invalid characters are entered" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, noPurchaseInvoiceReasonRoute)
            .withFormUrlEncodedBody(("value", "The invoice cost #100"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("noPurchaseInvoiceReason.error.invalid"))
      }
    }

    "must return OK for a GET for an agent with no client selected" in {

      val application = applicationForUserType(classOf[FakeAgentNoEnrolmentsIdentifierAction], userAnswersWithGuardData)

      running(application) {
        val request = FakeRequest(GET, noPurchaseInvoiceReasonRoute)

        status(route(application, request).value) mustEqual OK
      }
    }

    "must return OK for a GET for an organisation" in {

      val application = applicationForUserType(classOf[FakeOrganisationIdentifierAction], userAnswersWithGuardData)

      running(application) {
        val request = FakeRequest(GET, noPurchaseInvoiceReasonRoute)

        status(route(application, request).value) mustEqual OK
      }
    }

    "must redirect to Unauthorised for a GET if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, noPurchaseInvoiceReasonRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, noPurchaseInvoiceReasonRoute)
            .withFormUrlEncodedBody(("value", answer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if draftId is missing" in {

      val answersWithoutDraftId = userAnswersWithGuardData.remove(DraftIdPage).success.value

      val application = applicationBuilder(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request = FakeRequest(GET, noPurchaseInvoiceReasonRoute)

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
          FakeRequest(POST, noPurchaseInvoiceReasonRoute)
            .withFormUrlEncodedBody(("value", answer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if IQ1 was answered No" in {

      val answersIq1No = userAnswersWithGuardData.set(VehicleFromEuPage, false).success.value

      val application = applicationBuilder(userAnswers = Some(answersIq1No)).build()

      running(application) {
        val request = FakeRequest(GET, noPurchaseInvoiceReasonRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(GET, vehicledetails.routes.NoPurchaseInvoiceReasonController.onPageLoad(SupplierNumber(2), vehicleNumber, NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the vehicle number in the URL is not one of the user's vehicles" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(GET, vehicledetails.routes.NoPurchaseInvoiceReasonController.onPageLoad(supplierNumber, VehicleNumber(999), NormalMode).url)

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
        val request = FakeRequest(GET, noPurchaseInvoiceReasonRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must save the reason against the vehicle in the URL" in {

      val answers = userAnswersWithGuardData
        .set(
          AllSuppliersQuery,
          Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false), "2" -> Json.obj("usePersonalDetailsAsSupplier" -> false))
        )
        .success
        .value
        .set(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1), "3" -> Json.obj("supplierNumber" -> 2)))
        .success
        .value
        .set(VehicleDatesPage(SupplierNumber(2), VehicleNumber(3)), Set[VehicleDates](VehicleDates.AvailabilityAndFirstRegistration))
        .success
        .value

      val (application, mockSessionRepository) = applicationWithMockRepository(answers)

      running(application) {
        val request =
          FakeRequest(POST, vehicledetails.routes.NoPurchaseInvoiceReasonController.onSubmit(SupplierNumber(2), VehicleNumber(3), NormalMode).url)
            .withFormUrlEncodedBody(("value", answer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val saved = savedAnswers(mockSessionRepository)
        saved.get(NoPurchaseInvoiceReasonPage(SupplierNumber(2), VehicleNumber(3))) mustEqual Some(answer)
        saved.get(NoPurchaseInvoiceReasonPage(supplierNumber, vehicleNumber)) mustEqual None
      }
    }
  }
}
