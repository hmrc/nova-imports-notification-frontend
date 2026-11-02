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
import forms.DateOfFirstRegistrationKnownFormProvider
import models.{CheckMode, DraftId, ImportNumber, Mode, NormalMode, UserAnswers, VehicleNumber}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.DateOfFirstRegistrationKnownPage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllVehiclesQuery}
import repositories.SessionRepository
import views.html.DateOfFirstRegistrationKnownView

import scala.concurrent.Future

class DateOfFirstRegistrationKnownControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val form = new DateOfFirstRegistrationKnownFormProvider()()

  val importNumber  = ImportNumber(1)
  val vehicleNumber = VehicleNumber(1)

  lazy val pageRoute = vehicledetails.routes.DateOfFirstRegistrationKnownController.onPageLoad(importNumber, vehicleNumber, NormalMode).url

  val importJourneyAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, false)
    .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A")))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1)))

  private def submitCall(mode: Mode): Call =
    vehicledetails.routes.DateOfFirstRegistrationKnownController.onSubmit(importNumber, vehicleNumber, mode)

  private def applicationFor(identifierAction: Class[? <: IdentifierAction], userAnswers: Option[UserAnswers]): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )
      .build()

  private def applicationWithMockRepository(userAnswers: UserAnswers): (Application, SessionRepository) = {

    val mockSessionRepository = mock[SessionRepository]
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

    val application =
      applicationBuilderWithVatTrader(userAnswers = Some(userAnswers))
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

  private def assertUnauthorisedOnGet(application: Application, url: String = pageRoute) =
    running(application) {
      val result = route(application, FakeRequest(GET, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }

  private def assertUnauthorisedOnPost(application: Application, url: String = pageRoute) =
    running(application) {
      val result = route(application, FakeRequest(POST, url).withFormUrlEncodedBody("value" -> "true")).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }

  private def vatTrader(userAnswers: Option[UserAnswers]): Application =
    applicationBuilderWithVatTrader(userAnswers = userAnswers).build()

  "DateOfFirstRegistrationKnownController" - {

    "must return OK and the correct view for a GET" in {

      val application = vatTrader(Some(importJourneyAnswers))

      running(application) {
        val request = FakeRequest(GET, pageRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DateOfFirstRegistrationKnownView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, submitCall(NormalMode))(request, messages(application)).toString
      }
    }

    "must post back to the change URL for a GET in CheckMode" in {

      val application = vatTrader(Some(importJourneyAnswers))

      running(application) {
        val request =
          FakeRequest(GET, vehicledetails.routes.DateOfFirstRegistrationKnownController.onPageLoad(importNumber, vehicleNumber, CheckMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DateOfFirstRegistrationKnownView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, submitCall(CheckMode))(request, messages(application)).toString
      }
    }

    "must show a back link on the page" in {

      val application = vatTrader(Some(importJourneyAnswers))

      running(application) {
        val result = route(application, FakeRequest(GET, pageRoute)).value

        contentAsString(result) must include("govuk-back-link")
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = importJourneyAnswers.unsafeSet(DateOfFirstRegistrationKnownPage(vehicleNumber), false)

      val application = vatTrader(Some(userAnswers))

      running(application) {
        val request = FakeRequest(GET, pageRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DateOfFirstRegistrationKnownView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(false), submitCall(NormalMode))(request, messages(application)).toString
      }
    }

    "must save Yes and redirect to the next page" in {

      val (application, mockSessionRepository) = applicationWithMockRepository(importJourneyAnswers)

      running(application) {
        val result = route(application, FakeRequest(POST, pageRoute).withFormUrlEncodedBody("value" -> "true")).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        savedAnswers(mockSessionRepository).get(DateOfFirstRegistrationKnownPage(vehicleNumber)) mustEqual Some(true)
      }
    }

    "must save No and redirect to the next page" in {

      val (application, mockSessionRepository) = applicationWithMockRepository(importJourneyAnswers)

      running(application) {
        val result = route(application, FakeRequest(POST, pageRoute).withFormUrlEncodedBody("value" -> "false")).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        savedAnswers(mockSessionRepository).get(DateOfFirstRegistrationKnownPage(vehicleNumber)) mustEqual Some(false)
      }
    }

    "must return a Bad Request and the required error when nothing is selected" in {

      val application = vatTrader(Some(importJourneyAnswers))

      running(application) {
        val request = FakeRequest(POST, pageRoute).withFormUrlEncodedBody("value" -> "")

        val result = route(application, request).value

        val view = application.injector.instanceOf[DateOfFirstRegistrationKnownView]

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(form.bind(Map("value" -> "")), submitCall(NormalMode))(request, messages(application)).toString
        contentAsString(result) must include(messages(application)("dateOfFirstRegistrationKnown.error.required"))
      }
    }

    "must return OK for a GET for an agent with a VAT agent enrolment" in {

      val application = applicationFor(classOf[FakeAgentIdentifierAction], Some(importJourneyAnswers))

      running(application) {
        status(route(application, FakeRequest(GET, pageRoute)).value) mustEqual OK
      }
    }

    "must return OK for a GET for an agent without enrolments" in {

      val application = applicationFor(classOf[FakeAgentNoEnrolmentsIdentifierAction], Some(importJourneyAnswers))

      running(application) {
        status(route(application, FakeRequest(GET, pageRoute)).value) mustEqual OK
      }
    }

    "must redirect a GET to Unauthorised for a private individual" in {
      assertUnauthorisedOnGet(applicationFor(classOf[FakeIdentifierAction], Some(importJourneyAnswers)))
    }

    "must redirect a POST to Unauthorised for a private individual" in {
      assertUnauthorisedOnPost(applicationFor(classOf[FakeIdentifierAction], Some(importJourneyAnswers)))
    }

    "must redirect a GET to Unauthorised for a non-VAT organisation" in {
      assertUnauthorisedOnGet(applicationFor(classOf[FakeOrganisationIdentifierAction], Some(importJourneyAnswers)))
    }

    "must redirect a GET to Unauthorised when the identifier rejects the user" in {
      assertUnauthorisedOnGet(applicationFor(classOf[UnauthorisedIdentifierAction], Some(importJourneyAnswers)))
    }

    "must redirect a GET to Unauthorised if no existing session data is found" in {
      assertUnauthorisedOnGet(vatTrader(None))
    }

    "must redirect a POST to Unauthorised if no existing session data is found" in {
      assertUnauthorisedOnPost(vatTrader(None))
    }

    "must redirect a GET to Unauthorised if draftId is missing" in {
      assertUnauthorisedOnGet(vatTrader(Some(importJourneyAnswers.remove(DraftIdPage).success.value)))
    }

    "must redirect a POST to Unauthorised if draftId is missing" in {
      assertUnauthorisedOnPost(vatTrader(Some(importJourneyAnswers.remove(DraftIdPage).success.value)))
    }

    "must redirect a GET to Unauthorised if IQ1.0 was answered Yes" in {
      assertUnauthorisedOnGet(vatTrader(Some(importJourneyAnswers.unsafeSet(VehicleFromEuPage, true))))
    }

    "must redirect a GET to Unauthorised if IQ1.0 has not been answered" in {
      assertUnauthorisedOnGet(vatTrader(Some(importJourneyAnswers.remove(VehicleFromEuPage).success.value)))
    }

    "must redirect a GET to Unauthorised if the import number in the URL is not one of the user's imports" in {
      assertUnauthorisedOnGet(
        vatTrader(Some(importJourneyAnswers)),
        vehicledetails.routes.DateOfFirstRegistrationKnownController.onPageLoad(ImportNumber(2), vehicleNumber, NormalMode).url
      )
    }

    "must redirect a GET to Unauthorised if the vehicle number in the URL is not one of the user's vehicles" in {
      assertUnauthorisedOnGet(
        vatTrader(Some(importJourneyAnswers)),
        vehicledetails.routes.DateOfFirstRegistrationKnownController.onPageLoad(importNumber, VehicleNumber(999), NormalMode).url
      )
    }

    "must redirect a GET to Unauthorised if the vehicle in the URL belongs to a different import" in {
      val answers = importJourneyAnswers
        .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A"), "2" -> Json.obj("importEntryNumber" -> "123456789B")))
        .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 2)))

      assertUnauthorisedOnGet(vatTrader(Some(answers)))
    }

    "must redirect a GET to Unauthorised if the vehicle has been deleted" in {
      val answers = importJourneyAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1, "deleted" -> true)))

      assertUnauthorisedOnGet(vatTrader(Some(answers)))
    }
  }
}
