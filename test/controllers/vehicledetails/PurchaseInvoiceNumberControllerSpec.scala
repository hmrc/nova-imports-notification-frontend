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
import forms.PurchaseInvoiceNumberFormProvider
import models.{DraftId, NormalMode, SupplierNumber, UserAnswers, VehicleNumber}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.PurchaseInvoiceNumberPage
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import views.html.PurchaseInvoiceNumberView

import scala.concurrent.Future

class PurchaseInvoiceNumberControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new PurchaseInvoiceNumberFormProvider()
  val form         = formProvider()

  val supplierNumber = SupplierNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  val answer = "INV-2026-001"

  lazy val purchaseInvoiceNumberRoute =
    vehicledetails.routes.PurchaseInvoiceNumberController.onPageLoad(supplierNumber, vehicleNumber, NormalMode).url

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

  "PurchaseInvoiceNumberController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, purchaseInvoiceNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PurchaseInvoiceNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, supplierNumber, vehicleNumber, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithGuardData
        .set(PurchaseInvoiceNumberPage(supplierNumber, vehicleNumber), answer)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, purchaseInvoiceNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PurchaseInvoiceNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(answer), supplierNumber, vehicleNumber, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page and save the invoice number when valid data is submitted" in {

      val (application, mockSessionRepository) = applicationWithMockRepository(userAnswersWithGuardData)

      running(application) {
        val request =
          FakeRequest(POST, purchaseInvoiceNumberRoute)
            .withFormUrlEncodedBody(("value", answer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        savedAnswers(mockSessionRepository).get(PurchaseInvoiceNumberPage(supplierNumber, vehicleNumber)) mustEqual Some(answer)
      }
    }

    "must return a Bad Request and the required error when nothing is entered" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaseInvoiceNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("purchaseInvoiceNumber.error.required"))
      }
    }

    "must return a Bad Request and the format error when invalid characters are entered" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaseInvoiceNumberRoute)
            .withFormUrlEncodedBody(("value", "INV 123#"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("purchaseInvoiceNumber.error.invalid"))
      }
    }

    "must return a Bad Request and the length error when more than 20 characters are entered" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaseInvoiceNumberRoute)
            .withFormUrlEncodedBody(("value", "A" * 21))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("purchaseInvoiceNumber.error.length"))
      }
    }

    "must return OK for a GET for an agent with no client selected" in {

      val application = applicationForUserType(classOf[FakeAgentNoEnrolmentsIdentifierAction], userAnswersWithGuardData)

      running(application) {
        val request = FakeRequest(GET, purchaseInvoiceNumberRoute)

        status(route(application, request).value) mustEqual OK
      }
    }

    "must return OK for a GET for an organisation" in {

      val application = applicationForUserType(classOf[FakeOrganisationIdentifierAction], userAnswersWithGuardData)

      running(application) {
        val request = FakeRequest(GET, purchaseInvoiceNumberRoute)

        status(route(application, request).value) mustEqual OK
      }
    }

    "must redirect to Unauthorised for a GET if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, purchaseInvoiceNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, purchaseInvoiceNumberRoute)
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
        val request = FakeRequest(GET, purchaseInvoiceNumberRoute)

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
          FakeRequest(POST, purchaseInvoiceNumberRoute)
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
        val request = FakeRequest(GET, purchaseInvoiceNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(GET, vehicledetails.routes.PurchaseInvoiceNumberController.onPageLoad(SupplierNumber(2), vehicleNumber, NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, vehicledetails.routes.PurchaseInvoiceNumberController.onSubmit(SupplierNumber(2), vehicleNumber, NormalMode).url)
            .withFormUrlEncodedBody(("value", answer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the vehicle number in the URL is not one of the user's vehicles" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(GET, vehicledetails.routes.PurchaseInvoiceNumberController.onPageLoad(supplierNumber, VehicleNumber(999), NormalMode).url)

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
        val request = FakeRequest(GET, purchaseInvoiceNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must save the invoice number against the vehicle in the URL" in {

      val answers = userAnswersWithGuardData
        .set(AllSuppliersQuery, Map("1" -> Json.obj(), "2" -> Json.obj()))
        .success
        .value
        .set(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1), "3" -> Json.obj("supplierNumber" -> 2)))
        .success
        .value

      val (application, mockSessionRepository) = applicationWithMockRepository(answers)

      running(application) {
        val request =
          FakeRequest(POST, vehicledetails.routes.PurchaseInvoiceNumberController.onSubmit(SupplierNumber(2), VehicleNumber(3), NormalMode).url)
            .withFormUrlEncodedBody(("value", answer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val saved = savedAnswers(mockSessionRepository)
        saved.get(PurchaseInvoiceNumberPage(SupplierNumber(2), VehicleNumber(3))) mustEqual Some(answer)
        saved.get(PurchaseInvoiceNumberPage(supplierNumber, vehicleNumber)) mustEqual None
      }
    }
  }
}
