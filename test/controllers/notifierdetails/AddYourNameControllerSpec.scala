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

package controllers.notifierdetails

import base.SpecBase
import com.google.inject.name.Names
import controllers.actions.{DataRequiredAction, DataRequiredActionImpl, DataRetrievalAction, FakeAgentIdentifierAction, FakeAgentNoEnrolmentsIdentifierAction, FakeDataRetrievalAction, FakeIdentifierAction, IdentifierAction}
import controllers.{notifierdetails, routes}
import forms.AddYourNameFormProvider
import models.{BusinessOrPrivateIndividual, CheckMode, ContactNumbers, DraftId, NameDetails, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.{BusinessOrPrivatePage, VehicleFromEuPage}
import pages.sections.notifierdetails.{EmailAddressPage, PhoneNumberPage}
import pages.sections.notifierdetails.NameDetailsPage
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.AddYourNameView

import scala.concurrent.Future

class AddYourNameControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider = new AddYourNameFormProvider()
  val form         = formProvider()

  lazy val addYourNameRoute       = notifierdetails.routes.AddYourNameController.onPageLoad(NormalMode).url
  lazy val addYourNameChangeRoute = notifierdetails.routes.AddYourNameController.onPageLoad(CheckMode).url

  val validTitle     = "Mr"
  val validFirstName = "John"
  val validLastName  = "Doe"

  // A private-individual user reaches /name directly from the initial questions
  // (no NotificationTaskList / AYD1.0 stop) — AYD1.0 is VAT-org only.
  private val requiredPreviousAnswers = emptyUserAnswers
    .set(pages.DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(VehicleFromEuPage, true)
    .success
    .value
    .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
    .success
    .value

  private val cyaCompleteAnswers = emptyUserAnswers
    .set(pages.DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
    .success
    .value
    .set(NameDetailsPage, NameDetails(validTitle, validFirstName, validLastName))
    .success
    .value
    .set(PhoneNumberPage, ContactNumbers(Some("01632 960 001"), None))
    .success
    .value
    .set(EmailAddressPage, "name@example.com")
    .success
    .value

  private def agentApplicationBuilder(userAnswers: Option[UserAnswers]): GuiceApplicationBuilder =
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

  private def nonVatAgentApplicationBuilder(userAnswers: Option[UserAnswers]): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeAgentNoEnrolmentsIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeAgentNoEnrolmentsIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeAgentNoEnrolmentsIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  "AddYourName Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(requiredPreviousAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addYourNameRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddYourNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val answer      = NameDetails(validTitle, validFirstName, validLastName)
      val userAnswers = requiredPreviousAnswers.set(NameDetailsPage, answer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addYourNameRoute)

        val view = application.injector.instanceOf[AddYourNameView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(answer), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(requiredPreviousAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, addYourNameRoute)
            .withFormUrlEncodedBody(
              ("title", validTitle),
              ("firstName", validFirstName),
              ("lastName", validLastName)
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must save the full name to UserAnswers on submission" in {

      val mockSessionRepository = mock[SessionRepository]
      val captor                = ArgumentCaptor.forClass(classOf[UserAnswers])

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(requiredPreviousAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, addYourNameRoute)
            .withFormUrlEncodedBody(
              ("title", validTitle),
              ("firstName", validFirstName),
              ("lastName", validLastName)
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        verify(mockSessionRepository).set(captor.capture())
        captor.getValue.get(NameDetailsPage) mustBe Some(NameDetails(validTitle, validFirstName, validLastName))
      }
    }

    "must return a Bad Request and errors when the first name is blank" in {

      val application = applicationBuilder(userAnswers = Some(requiredPreviousAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, addYourNameRoute)
            .withFormUrlEncodedBody(("title", validTitle), ("firstName", ""), ("lastName", validLastName))

        val boundForm = form.bind(Map("title" -> validTitle, "firstName" -> "", "lastName" -> validLastName))

        val view = application.injector.instanceOf[AddYourNameView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised when upstream initial-question answers are missing" in {

      val answers = emptyUserAnswers.set(pages.DraftIdPage, DraftId("DRAFT-001")).success.value

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, addYourNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a VAT agent (HMCE-VAT-AGNT) without a client who answered private individual" in {

      val application = agentApplicationBuilder(Some(requiredPreviousAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addYourNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must return OK for a non-VAT agent without a client who answered private individual" in {

      val application = nonVatAgentApplicationBuilder(Some(requiredPreviousAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addYourNameRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Unauthorised for a non-VAT agent without a client who answered business" in {

      val businessAnswers = emptyUserAnswers
        .unsafeSet(pages.DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)

      val application = nonVatAgentApplicationBuilder(Some(businessAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addYourNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must allow CheckMode access from completed CYA2 data without AboutYourDetailsPage" in {

      val application = applicationBuilder(userAnswers = Some(cyaCompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addYourNameChangeRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Unauthorised for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, addYourNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when no draft is in progress" in {

      val answersWithoutDraft = emptyUserAnswers
        .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
        .unsafeSet(VehicleFromEuPage, true)

      val application = applicationBuilder(userAnswers = Some(answersWithoutDraft)).build()

      running(application) {
        val request = FakeRequest(GET, addYourNameRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, addYourNameRoute)
            .withFormUrlEncodedBody(("title", validTitle), ("firstName", validFirstName), ("lastName", validLastName))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
