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
import forms.AgentVehicleBusinessUseFormProvider
import models.{AgentSelectedClient, NormalMode}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.{AgentClientVehicleBusinessUsePage, AgentSelectedClientPage}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.AgentVehicleBusinessUseView

import scala.concurrent.Future

class AgentVehicleBusinessUseControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new AgentVehicleBusinessUseFormProvider()
  val form         = formProvider()

  lazy val agentVehicleBusinessUseRoute = routes.AgentVehicleBusinessUseController.onPageLoad(NormalMode).url

  val sampleClient = AgentSelectedClient(vrn = "GB123456789", name = Some("Acme Ltd"))

  val userAnswersAgentWithClientAndIQ1 =
    emptyUserAnswers
      .set(AgentSelectedClientPage, sampleClient)
      .success
      .value
      .set(VehicleFromEuPage, true)
      .success
      .value

  val userAnswersAgentWithClient =
    emptyUserAnswers.set(AgentSelectedClientPage, sampleClient).success.value

  "AgentVehicleBusinessUse Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilderWithAgentAsNovaAgent(userAnswers = Some(userAnswersAgentWithClientAndIQ1)).build()

      running(application) {
        val request = FakeRequest(GET, agentVehicleBusinessUseRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AgentVehicleBusinessUseView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersAgentWithClientAndIQ1.set(AgentClientVehicleBusinessUsePage, true).success.value

      val application = applicationBuilderWithAgentAsNovaAgent(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, agentVehicleBusinessUseRoute)

        val view = application.injector.instanceOf[AgentVehicleBusinessUseView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilderWithAgentAsNovaAgent(userAnswers = Some(userAnswersAgentWithClientAndIQ1))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, agentVehicleBusinessUseRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilderWithAgentAsNovaAgent(userAnswers = Some(userAnswersAgentWithClientAndIQ1)).build()

      running(application) {
        val request =
          FakeRequest(POST, agentVehicleBusinessUseRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[AgentVehicleBusinessUseView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised for a GET if no existing data is found" in {

      val application = applicationBuilderWithAgentAsNovaAgent(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, agentVehicleBusinessUseRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing data is found" in {

      val application = applicationBuilderWithAgentAsNovaAgent(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, agentVehicleBusinessUseRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if the agent has no selected client" in {

      val application = applicationBuilderWithAgentAsNovaAgent(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, agentVehicleBusinessUseRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if the agent has no selected client" in {

      val application = applicationBuilderWithAgentAsNovaAgent(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, agentVehicleBusinessUseRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if IQ1 has not been answered" in {

      val application = applicationBuilderWithAgentAsNovaAgent(userAnswers = Some(userAnswersAgentWithClient)).build()

      running(application) {
        val request = FakeRequest(GET, agentVehicleBusinessUseRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if IQ1 has not been answered" in {

      val application = applicationBuilderWithAgentAsNovaAgent(userAnswers = Some(userAnswersAgentWithClient)).build()

      running(application) {
        val request =
          FakeRequest(POST, agentVehicleBusinessUseRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when user is not a Nova agent" in {

      val application = new GuiceApplicationBuilder()
        .overrides(
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          bind[IdentifierAction].to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[UnauthorisedIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswersAgentWithClientAndIQ1)))
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, agentVehicleBusinessUseRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST when user is not a Nova agent" in {

      val application = new GuiceApplicationBuilder()
        .overrides(
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          bind[IdentifierAction].to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[UnauthorisedIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswersAgentWithClientAndIQ1)))
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, agentVehicleBusinessUseRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
