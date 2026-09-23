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
import controllers.actions.*
import controllers.{routes, vehicledetails}
import forms.TotalAmountPaidFormProvider
import models.{DraftId, ImportNumber, NormalMode, SupplierNumber, UserAnswers, VehicleNumber}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.TotalAmountPaidPage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import views.html.TotalAmountPaidView

import scala.concurrent.Future

class TotalAmountPaidControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new TotalAmountPaidFormProvider()
  val form         = formProvider()

  val supplierNumber = SupplierNumber(1)
  val importNumber   = ImportNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  val answer = "15000"

  lazy val supplierRoute = vehicledetails.routes.TotalAmountPaidController.onPageLoadSupplier(supplierNumber, vehicleNumber, NormalMode).url
  lazy val importRoute   = vehicledetails.routes.TotalAmountPaidController.onPageLoadImport(importNumber, vehicleNumber, NormalMode).url

  val supplierAnswers: UserAnswers = emptyUserAnswers
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(VehicleFromEuPage, true)
    .success
    .value
    .set(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .success
    .value
    .set(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))))
    .success
    .value

  val importAnswers: UserAnswers = emptyUserAnswers
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(VehicleFromEuPage, false)
    .success
    .value
    .set(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A")))
    .success
    .value
    .set(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))))
    .success
    .value

  private def applicationForUserType(identifierAction: Class[? <: IdentifierAction], userAnswers: UserAnswers): Application =
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

  private def applicationWithMockRepository(userAnswers: UserAnswers): (Application, SessionRepository) = {
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

  "TotalAmountPaidController" - {

    "supplier journey" - {

      "must return OK and the correct view for a GET" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[TotalAmountPaidView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            vehicledetails.routes.TotalAmountPaidController.onSubmitSupplier(supplierNumber, vehicleNumber, NormalMode)
          )(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = supplierAnswers.set(TotalAmountPaidPage(vehicleNumber), answer).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[TotalAmountPaidView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(answer),
            vehicledetails.routes.TotalAmountPaidController.onSubmitSupplier(supplierNumber, vehicleNumber, NormalMode)
          )(request, messages(application)).toString
        }
      }

      "must redirect to the next page and save the answer when valid data is submitted" in {
        val (application, mockSessionRepository) = applicationWithMockRepository(supplierAnswers)

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", answer))
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(TotalAmountPaidPage(vehicleNumber)) mustEqual Some(answer)
        }
      }

      "must return a Bad Request and the required error when nothing is entered" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", ""))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("totalAmountPaid.error.required"))
        }
      }

      "must return a Bad Request and the length error when more than 14 characters are entered" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", "1" * 15))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("totalAmountPaid.error.length"))
        }
      }

      "must return a Bad Request and the comma error when a comma is entered" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", "15,000"))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("totalAmountPaid.error.commaOrDecimalPoint"))
        }
      }

      "must return a Bad Request and the decimal point error when a decimal point is entered" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", "150.00"))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("totalAmountPaid.error.commaOrDecimalPoint"))
        }
      }

      "must return a Bad Request and the format error when other invalid characters are entered" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", "15000abc"))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("totalAmountPaid.error.invalid"))
        }
      }

      "must return OK for a GET for an agent with a client selected" in {
        val application = applicationForUserType(classOf[FakeAgentIdentifierAction], supplierAnswers)

        running(application) {
          status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must redirect to Unauthorised for a GET if no existing session data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val result = route(application, FakeRequest(GET, supplierRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a GET if draftId is missing" in {
        val answers     = supplierAnswers.remove(DraftIdPage).success.value
        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, supplierRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a GET if IQ1 was answered No" in {
        val answers     = supplierAnswers.set(VehicleFromEuPage, false).success.value
        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, supplierRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {
        val application = applicationBuilder(userAnswers = Some(supplierAnswers)).build()

        running(application) {
          val request =
            FakeRequest(GET, vehicledetails.routes.TotalAmountPaidController.onPageLoadSupplier(SupplierNumber(2), vehicleNumber, NormalMode).url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different supplier" in {
        val answers = supplierAnswers
          .set(AllSuppliersQuery, Map("1" -> Json.obj(), "2" -> Json.obj()))
          .success
          .value
          .set(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 2)))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, supplierRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "import journey" - {

      "must return OK and the correct view for a GET" in {
        val application = applicationForUserType(classOf[FakeVatTraderIdentifierAction], importAnswers)

        running(application) {
          val request = FakeRequest(GET, importRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[TotalAmountPaidView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            vehicledetails.routes.TotalAmountPaidController.onSubmitImport(importNumber, vehicleNumber, NormalMode)
          )(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = importAnswers.set(TotalAmountPaidPage(vehicleNumber), answer).success.value
        val application = applicationForUserType(classOf[FakeVatTraderIdentifierAction], userAnswers)

        running(application) {
          val request = FakeRequest(GET, importRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[TotalAmountPaidView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(answer),
            vehicledetails.routes.TotalAmountPaidController.onSubmitImport(importNumber, vehicleNumber, NormalMode)
          )(request, messages(application)).toString
        }
      }

      "must redirect to the next page and save the answer when valid data is submitted" in {
        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application = new GuiceApplicationBuilder()
          .overrides(
            bind[DataRequiredAction].to[DataRequiredActionImpl],
            bind[IdentifierAction].to[FakeVatTraderIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeVatTraderIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeVatTraderIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeVatTraderIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
            bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(importAnswers))),
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody(("value", answer))
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(TotalAmountPaidPage(vehicleNumber)) mustEqual Some(answer)
        }
      }

      "must return a Bad Request and the required error when nothing is entered" in {
        val application = applicationForUserType(classOf[FakeVatTraderIdentifierAction], importAnswers)

        running(application) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody(("value", ""))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include(messages(application)("totalAmountPaid.error.required"))
        }
      }

      "must allow an agent to access the page" in {
        val application = applicationForUserType(classOf[FakeAgentIdentifierAction], importAnswers)

        running(application) {
          status(route(application, FakeRequest(GET, importRoute)).value) mustEqual OK
        }
      }

      "must redirect to Unauthorised for a private individual (user type 1)" in {
        val application = applicationForUserType(classOf[FakeIdentifierAction], importAnswers)

        running(application) {
          val result = route(application, FakeRequest(GET, importRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a non-VAT organisation (user type 2)" in {
        val application = applicationForUserType(classOf[FakeOrganisationIdentifierAction], importAnswers)

        running(application) {
          val result = route(application, FakeRequest(GET, importRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a GET if no existing session data is found" in {
        val application = applicationForUserType(classOf[FakeVatTraderIdentifierAction], importAnswers)

        running(application) {
          val emptyApplication = new GuiceApplicationBuilder()
            .overrides(
              bind[DataRequiredAction].to[DataRequiredActionImpl],
              bind[IdentifierAction].to[FakeVatTraderIdentifierAction],
              bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeVatTraderIdentifierAction],
              bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeVatTraderIdentifierAction],
              bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeVatTraderIdentifierAction],
              bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
              bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(None))
            )
            .build()

          val result = route(emptyApplication, FakeRequest(GET, importRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a GET if draftId is missing" in {
        val answers     = importAnswers.remove(DraftIdPage).success.value
        val application = applicationForUserType(classOf[FakeVatTraderIdentifierAction], answers)

        running(application) {
          val result = route(application, FakeRequest(GET, importRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a GET if IQ1 was answered Yes" in {
        val answers     = importAnswers.set(VehicleFromEuPage, true).success.value
        val application = applicationForUserType(classOf[FakeVatTraderIdentifierAction], answers)

        running(application) {
          val result = route(application, FakeRequest(GET, importRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a GET if the import number in the URL is not one of the user's imports" in {
        val application = applicationForUserType(classOf[FakeVatTraderIdentifierAction], importAnswers)

        running(application) {
          val request =
            FakeRequest(GET, vehicledetails.routes.TotalAmountPaidController.onPageLoadImport(ImportNumber(2), vehicleNumber, NormalMode).url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different import" in {
        val answers = importAnswers
          .set(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A"), "2" -> Json.obj("importEntryNumber" -> "987654321B")))
          .success
          .value
          .set(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 2, "details" -> Json.obj("bringingVehicleNI" -> true))))
          .success
          .value

        val application = applicationForUserType(classOf[FakeVatTraderIdentifierAction], answers)

        running(application) {
          val result = route(application, FakeRequest(GET, importRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "must save the same session answer regardless of which journey route was used to reach the page" in {
      val (supplierApp, supplierRepo) = applicationWithMockRepository(supplierAnswers)

      running(supplierApp) {
        val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", answer))
        route(supplierApp, request).value.futureValue

        savedAnswers(supplierRepo).get(TotalAmountPaidPage(vehicleNumber)) mustEqual Some(answer)
      }

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val importApp = new GuiceApplicationBuilder()
        .overrides(
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          bind[IdentifierAction].to[FakeVatTraderIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeVatTraderIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeVatTraderIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeVatTraderIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(importAnswers))),
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(importApp) {
        val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody(("value", answer))
        route(importApp, request).value.futureValue

        savedAnswers(mockSessionRepository).get(TotalAmountPaidPage(vehicleNumber)) mustEqual Some(answer)
      }
    }
  }
}
