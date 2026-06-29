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
import com.google.inject.name.Names
import controllers.actions.{DataRequiredAction, DataRequiredActionImpl, DataRetrievalAction, FakeAgentIdentifierAction, FakeDataRetrievalAction, FakeIdentifierAction, FakeVatTraderIdentifierAction, IdentifierAction}
import forms.PhoneNumberFormProvider
import models.{BusinessOrPrivateIndividual, CheckMode, DraftId, NameDetails, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import pages.sections.initialquestions.{BusinessOrPrivatePage, VehicleBusinessUsePage}
import pages.sections.notifierDetails.{EmailAddressPage, NameDetailsPage, PhoneNumberPage}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.PhoneNumberView

import scala.concurrent.Future

class PhoneNumberControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider = new PhoneNumberFormProvider()
  val form         = formProvider()

  val validPhoneNumber = "01632 960 001"

  val baseRequiredAnswers: UserAnswers = UserAnswers(userAnswersId).set(DraftIdPage, DraftId("DRAFT-001")).success.value

  val requiredAnswers: UserAnswers =
    baseRequiredAnswers.set(NameDetailsPage, NameDetails("Mr", "John", "Doe")).success.value

  val vatOrgRequiredAnswers: UserAnswers =
    baseRequiredAnswers
      .set(AboutYourDetailsPage, true)
      .success
      .value
      .set(VehicleBusinessUsePage, true)
      .success
      .value

  lazy val phoneNumberRoute       = routes.PhoneNumberController.onPageLoad(NormalMode).url
  lazy val phoneNumberSubmitRoute = routes.PhoneNumberController.onSubmit(NormalMode).url
  lazy val phoneNumberChangeRoute = routes.PhoneNumberController.onPageLoad(CheckMode).url

  val cyaCompleteAnswers: UserAnswers =
    baseRequiredAnswers
      .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
      .success
      .value
      .set(NameDetailsPage, NameDetails("Mr", "John", "Doe"))
      .success
      .value
      .set(PhoneNumberPage, validPhoneNumber)
      .success
      .value
      .set(EmailAddressPage, "name@example.com")
      .success
      .value

  // Built fresh (not via SpecBase.applicationBuilder) so the IdentifierAction bindings are declared in a single
  // .overrides(...) call — chaining a second .overrides on top of SpecBase's bindings trips Guice's
  // "BindingAlreadySet" guard in this Play version.
  private def vatTraderApplicationBuilder(userAnswers: Option[UserAnswers]): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeVatTraderIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeVatTraderIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeVatTraderIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  private def agentWithClientApplicationBuilder(userAnswers: Option[UserAnswers]): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  "PhoneNumberController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, phoneNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PhoneNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = requiredAnswers.set(PhoneNumberPage, validPhoneNumber).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, phoneNumberRoute)

        val view = application.injector.instanceOf[PhoneNumberView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validPhoneNumber), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, phoneNumberSubmitRoute)
            .withFormUrlEncodedBody(("value", validPhoneNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, phoneNumberSubmitRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[PhoneNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return OK for a VAT organisation that came directly from AYD1.0 (OQ1.0 = Yes, no NameDetailsPage)" in {

      val application = vatTraderApplicationBuilder(Some(vatOrgRequiredAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, phoneNumberRoute)
        val result  = route(application, request).value

        status(result) mustEqual OK
      }
    }

    // Regression: agents have no AYD1.0 step in their journey. An agent acting for a
    // client with AQ1.0 = Yes (business use) skips the name page and reaches /phone-number
    // directly — the guard must accept that without an AboutYourDetailsPage flag.
    "must return OK for an agent acting for a client with AQ1.0 = Yes (no AboutYourDetailsPage, no NameDetailsPage)" in {

      val agentBusinessUseAnswers = baseRequiredAnswers
        .set(AgentSelectedClientPage, models.AgentSelectedClient("123456789"))
        .success
        .value
        .set(AgentClientVehicleBusinessUsePage, true)
        .success
        .value

      val application = agentWithClientApplicationBuilder(Some(agentBusinessUseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, phoneNumberRoute)
        val result  = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Unauthorised when neither NameDetailsPage nor the VAT-org-direct path is satisfied" in {

      val application = applicationBuilder(userAnswers = Some(baseRequiredAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, phoneNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when AboutYourDetailsPage is set but VehicleBusinessUsePage is false (the /name path should have been taken)" in {

      val answersWithoutVatOrgPath = baseRequiredAnswers
        .set(AboutYourDetailsPage, true)
        .success
        .value
        .set(VehicleBusinessUsePage, false)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(answersWithoutVatOrgPath)).build()

      running(application) {
        val request = FakeRequest(GET, phoneNumberRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must allow CheckMode access from completed CYA2 data without AboutYourDetailsPage" in {

      val application = applicationBuilder(userAnswers = Some(cyaCompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, phoneNumberChangeRoute)
        val result  = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, phoneNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, phoneNumberSubmitRoute)
            .withFormUrlEncodedBody(("value", validPhoneNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
