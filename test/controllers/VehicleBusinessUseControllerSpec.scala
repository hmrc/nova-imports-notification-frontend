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
import controllers.actions.*
import com.google.inject.name.Names
import forms.VehicleBusinessUseFormProvider
import models.NormalMode
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{VehicleBusinessUsePage, VehicleFromEuPage}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.VehicleBusinessUseView

import scala.concurrent.Future

class VehicleBusinessUseControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new VehicleBusinessUseFormProvider()
  val form         = formProvider()

  lazy val vehicleBusinessUseRoute = routes.VehicleBusinessUseController.onPageLoad(NormalMode).url

  val userAnswersWithVehicleFromEu = emptyUserAnswers.set(VehicleFromEuPage, true).success.value

  "VehicleBusinessUse Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithVehicleFromEu)).build()

      running(application) {
        val request = FakeRequest(GET, vehicleBusinessUseRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VehicleBusinessUseView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithVehicleFromEu.set(VehicleBusinessUsePage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, vehicleBusinessUseRoute)

        val view = application.injector.instanceOf[VehicleBusinessUseView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithVehicleFromEu))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, vehicleBusinessUseRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithVehicleFromEu)).build()

      running(application) {
        val request =
          FakeRequest(POST, vehicleBusinessUseRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[VehicleBusinessUseView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, vehicleBusinessUseRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, vehicleBusinessUseRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if IQ1 has not been answered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, vehicleBusinessUseRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if IQ1 has not been answered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, vehicleBusinessUseRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when user is not a VAT-registered organisation" in {

      val application = new GuiceApplicationBuilder()
        .overrides(
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          bind[IdentifierAction].to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[UnauthorisedIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswersWithVehicleFromEu)))
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, vehicleBusinessUseRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST when user is not a VAT-registered organisation" in {

      val application = new GuiceApplicationBuilder()
        .overrides(
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          bind[IdentifierAction].to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[UnauthorisedIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswersWithVehicleFromEu)))
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, vehicleBusinessUseRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
