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
import config.FrontendAppConfig
import controllers.actions.*
import forms.AddImportVehicleDetailsFormProvider
import models.{AddImportVehicleDetails, DraftId, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{AddImportVehicleDetailsPage, DraftIdPage}
import pages.sections.initialquestions.VehicleFromEuPage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.AddImportVehicleDetailsView

import scala.concurrent.Future

class AddImportVehicleDetailsControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new AddImportVehicleDetailsFormProvider()
  private val form         = formProvider()

  private lazy val onPageLoadRoute = routes.AddImportVehicleDetailsController.onPageLoad(NormalMode).url
  private lazy val onSubmitRoute   = routes.AddImportVehicleDetailsController.onSubmit(NormalMode).url

  // Guard: draft created + IQ1.0 answered No. Access user type is set via the "standard" identifier binding.
  private val answersSatisfyingGuard: UserAnswers = emptyUserAnswers
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(VehicleFromEuPage, false)
    .success
    .value

  private def spreadsheetUrl(app: Application): String =
    app.injector.instanceOf[FrontendAppConfig].multipleVehiclesSpreadsheetsUrl

  private def builderFor(
    standardIdentifier: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers]
  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(standardIdentifier),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  private def applicationFor(
    standardIdentifier: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers]
  ): Application =
    builderFor(standardIdentifier, userAnswers).build()

  "AddImportVehicleDetailsController" - {

    "must return OK and the correct view for a GET (VAT-registered organisation)" in {
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answersSatisfyingGuard))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[AddImportVehicleDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, spreadsheetUrl(application))(request, messages(application)).toString
      }
    }

    "must allow an agent to access the page" in {
      val application = applicationFor(classOf[FakeAgentIdentifierAction], Some(answersSatisfyingGuard))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = answersSatisfyingGuard.set(AddImportVehicleDetailsPage, AddImportVehicleDetails.ByImportEntryNumber).success.value
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(userAnswers))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[AddImportVehicleDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(AddImportVehicleDetails.ByImportEntryNumber), NormalMode, spreadsheetUrl(application))(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = builderFor(classOf[FakeVatTraderIdentifierAction], Some(answersSatisfyingGuard))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", AddImportVehicleDetails.ByImportEntryNumber.toString))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answersSatisfyingGuard))

      running(application) {
        val request   = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", ""))
        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[AddImportVehicleDetailsView]
        val result    = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, spreadsheetUrl(application))(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised for a private individual (user type 1)" in {
      val application = applicationFor(classOf[FakeIdentifierAction], Some(answersSatisfyingGuard))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a non-VAT organisation (user type 2)" in {
      val application = applicationFor(classOf[FakeOrganisationIdentifierAction], Some(answersSatisfyingGuard))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when IQ1 was answered Yes" in {
      val answersIq1Yes = answersSatisfyingGuard.set(VehicleFromEuPage, true).success.value
      val application   = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answersIq1Yes))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when IQ1 has not been answered" in {
      val answersNoIq1 = emptyUserAnswers.set(DraftIdPage, DraftId("DRAFT-001")).success.value
      val application  = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answersNoIq1))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when the draft id is missing" in {
      val answersNoDraft = emptyUserAnswers.set(VehicleFromEuPage, false).success.value
      val application    = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answersNoDraft))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if no session data is found" in {
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], None)

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when the identifier rejects the user (user types 7/8 - OGD agent)" in {
      val application = applicationFor(classOf[UnauthorisedIdentifierAction], Some(answersSatisfyingGuard))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
