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
import forms.VehicleRegistrationNumberFormProvider
import models.*
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AgentSelectedClientPage, DraftIdPage}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{AddVehicleDetailsHeavyCommercialPage, AddVehicleTypePage, VehicleRegistrationNumberKnownPage, VehicleRegistrationNumberPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import viewmodels.VehicleDetailsInset
import views.html.VehicleRegistrationNumberView

import scala.concurrent.Future

class VehicleRegistrationNumberControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val form = new VehicleRegistrationNumberFormProvider()()

  val supplierNumber = SupplierNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  val answer = "ABC12DE"

  lazy val pageRoute =
    vehicledetails.routes.VehicleRegistrationNumberController.onPageLoad(supplierNumber, vehicleNumber, NormalMode).url

  lazy val submitCall =
    vehicledetails.routes.VehicleRegistrationNumberController.onSubmit(supplierNumber, vehicleNumber, NormalMode)

  val userAnswersWithGuardData: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
    .unsafeSet(AddVehicleTypePage(vehicleNumber), AddVehicleType.Hcv)
    .unsafeSet(
      AddVehicleDetailsHeavyCommercialPage(vehicleNumber),
      AddVehicleDetailsHeavyCommercial("Volvo", "FH16", "Tractor unit", "Sleeper cab")
    )
    .unsafeSet(VehicleRegistrationNumberKnownPage(vehicleNumber), true)

  private def applicationForUserType(identifierAction: Class[? <: IdentifierAction], userAnswers: UserAnswers = userAnswersWithGuardData) =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswers)))
      )
      .build()

  private def applicationWithMockRepository(userAnswers: UserAnswers): (Application, SessionRepository) = {
    val mockSessionRepository = mock[SessionRepository]
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

    val application = applicationBuilder(userAnswers = Some(userAnswers))
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

  private def mustBeUnauthorised(application: Application, url: String = pageRoute) =
    running(application) {
      val result = route(application, FakeRequest(GET, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }

  "VehicleRegistrationNumberController" - {

    "must return OK and the correct view with the vehicle details for a GET" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, pageRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[VehicleRegistrationNumberView]
        val msgs    = messages(application)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          VehicleDetailsInset(userAnswersWithGuardData, vehicleNumber)(msgs),
          submitCall
        )(request, msgs).toString
        contentAsString(result) must include("<b>Volvo FH16</b>")
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = userAnswersWithGuardData.unsafeSet(VehicleRegistrationNumberPage(vehicleNumber), answer)
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, pageRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[VehicleRegistrationNumberView]
        val msgs    = messages(application)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(answer),
          VehicleDetailsInset(userAnswers, vehicleNumber)(msgs),
          submitCall
        )(request, msgs).toString
      }
    }

    "must redirect to the next page and save the answer when valid data is submitted" in {
      val (application, mockSessionRepository) = applicationWithMockRepository(userAnswersWithGuardData)

      running(application) {
        val request = FakeRequest(POST, pageRoute).withFormUrlEncodedBody(("value", answer))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        savedAnswers(mockSessionRepository).get(VehicleRegistrationNumberPage(vehicleNumber)) mustEqual Some(answer)
      }
    }

    "must return a Bad Request with the vehicle details and the required error when nothing is entered" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(POST, pageRoute).withFormUrlEncodedBody(("value", ""))
        val result  = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("vehicleRegistrationNumber.error.required"))
        contentAsString(result) must include("<b>Volvo FH16</b>")
      }
    }

    "must return a Bad Request and the length error when more than 12 characters are entered" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(POST, pageRoute).withFormUrlEncodedBody(("value", "ABCDEFGHIJKL1"))
        val result  = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("vehicleRegistrationNumber.error.length"))
      }
    }

    "must return a Bad Request and the format error when disallowed characters are entered" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val result = route(application, FakeRequest(POST, pageRoute).withFormUrlEncodedBody(("value", "ABC12£"))).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("vehicleRegistrationNumber.error.invalid"))
      }
    }

    "must return OK for a GET for a private individual (user type 1)" in {
      val application = applicationForUserType(classOf[FakeIdentifierAction])
      running(application)(status(route(application, FakeRequest(GET, pageRoute)).value) mustEqual OK)
    }

    "must return OK for a GET for a VAT-registered organisation (user types 4 and 5)" in {
      val application = applicationForUserType(classOf[FakeVatTraderIdentifierAction])
      running(application)(status(route(application, FakeRequest(GET, pageRoute)).value) mustEqual OK)
    }

    "must return OK for a GET for a VAT agent with a client selected (user type 6)" in {
      val answers     = userAnswersWithGuardData.unsafeSet(AgentSelectedClientPage, AgentSelectedClient(vrn = "123456789", name = Some("Client Ltd")))
      val application = applicationForUserType(classOf[FakeAgentIdentifierAction], answers)
      running(application)(status(route(application, FakeRequest(GET, pageRoute)).value) mustEqual OK)
    }

    "must redirect to Unauthorised for a GET if no existing session data is found" in {
      mustBeUnauthorised(applicationBuilder(userAnswers = None).build())
    }

    "must redirect to Unauthorised for a GET if draftId is missing" in {
      val answers = userAnswersWithGuardData.remove(DraftIdPage).success.value
      mustBeUnauthorised(applicationBuilder(userAnswers = Some(answers)).build())
    }

    "must redirect to Unauthorised for a GET if IQ1 was answered No" in {
      val answers = userAnswersWithGuardData.unsafeSet(VehicleFromEuPage, false)
      mustBeUnauthorised(applicationBuilder(userAnswers = Some(answers)).build())
    }

    "must redirect to Unauthorised for a GET if IQ1 was not answered" in {
      val answers = userAnswersWithGuardData.remove(VehicleFromEuPage).success.value
      mustBeUnauthorised(applicationBuilder(userAnswers = Some(answers)).build())
    }

    "must redirect to Unauthorised for a GET if the vehicle registration number known page was answered No" in {
      val answers = userAnswersWithGuardData.unsafeSet(VehicleRegistrationNumberKnownPage(vehicleNumber), false)
      mustBeUnauthorised(applicationBuilder(userAnswers = Some(answers)).build())
    }

    "must redirect to Unauthorised for a GET if the vehicle registration number known page was not answered" in {
      val answers = userAnswersWithGuardData.remove(VehicleRegistrationNumberKnownPage(vehicleNumber)).success.value
      mustBeUnauthorised(applicationBuilder(userAnswers = Some(answers)).build())
    }

    "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {
      mustBeUnauthorised(
        applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build(),
        vehicledetails.routes.VehicleRegistrationNumberController.onPageLoad(SupplierNumber(2), vehicleNumber, NormalMode).url
      )
    }

    "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different supplier" in {
      val answers = userAnswersWithGuardData
        .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj(), "2" -> Json.obj()))
        .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 2)))

      mustBeUnauthorised(applicationBuilder(userAnswers = Some(answers)).build())
    }

    "must redirect to Unauthorised for a GET if the vehicle in the URL is not in the session" in {
      mustBeUnauthorised(
        applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build(),
        vehicledetails.routes.VehicleRegistrationNumberController.onPageLoad(supplierNumber, VehicleNumber(2), NormalMode).url
      )
    }

    "must redirect to Unauthorised for a POST if IQ1 was answered No" in {
      val answers     = userAnswersWithGuardData.unsafeSet(VehicleFromEuPage, false)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(POST, pageRoute).withFormUrlEncodedBody(("value", answer))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
