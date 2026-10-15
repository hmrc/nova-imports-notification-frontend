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
import controllers.actions.{DataRequiredAction, DataRequiredActionImpl, DataRetrievalAction, FakeDataRetrievalAction, FakeIdentifierAction, FakeOrganisationIdentifierAction, IdentifierAction}
import controllers.{routes, vehicledetails}
import forms.ImportEntryNumberFormProvider
import models.{DraftId, ImportNumber, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.ImportEntryNumberPage
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.AllImportsQuery
import repositories.SessionRepository
import views.html.ImportEntryNumberView

import scala.concurrent.Future

class ImportEntryNumberControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val form = new ImportEntryNumberFormProvider()()

  val importNumber = ImportNumber(1)

  val answer = "12GB3456ACD789EF21"

  lazy val importEntryNumberRoute =
    vehicledetails.routes.ImportEntryNumberController.onPageLoad(importNumber, NormalMode).url

  lazy val importTwoRoute =
    vehicledetails.routes.ImportEntryNumberController.onPageLoad(ImportNumber(2), NormalMode).url

  val userAnswersWithGuardData: UserAnswers = emptyUserAnswers
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(VehicleFromEuPage, false)
    .success
    .value
    .set(AllImportsQuery, Map("1" -> Json.obj()))
    .success
    .value

  private def applicationFor(identifierAction: Class[? <: IdentifierAction]): play.api.Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswersWithGuardData)))
      )
      .build()

  "ImportEntryNumberController" - {

    "must return OK and the empty AVD-IE1.0 view for a GET" in {

      val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, importEntryNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ImportEntryNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, importNumber, NormalMode)(request, messages(application)).toString
      }
    }

    "must fill the AVD-IE1.0 form with the saved import entry number for a GET" in {

      val userAnswers = userAnswersWithGuardData.set(ImportEntryNumberPage(importNumber), answer).success.value

      val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, importEntryNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ImportEntryNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(answer), importNumber, NormalMode)(request, messages(application)).toString
      }
    }

    "must save the import entry number for import 1 and redirect for a valid POST" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilderWithVatTrader(userAnswers = Some(userAnswersWithGuardData))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, importEntryNumberRoute)
            .withFormUrlEncodedBody(("value", answer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(captor.capture())
        captor.getValue.get(ImportEntryNumberPage(importNumber)) mustEqual Some(answer)
      }
    }

    "must return a Bad Request and the error message for an empty POST" in {

      val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, importEntryNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("importEntryNumber.error.required"))
      }
    }

    "must redirect a GET to Unauthorised when there is no session data" in {

      val application = applicationBuilderWithVatTrader(userAnswers = None).build()

      running(application) {
        val result = route(application, FakeRequest(GET, importEntryNumberRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect a POST to Unauthorised when there is no draftId in session" in {

      val answersWithoutDraftId = userAnswersWithGuardData.remove(DraftIdPage).success.value

      val application = applicationBuilderWithVatTrader(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request =
          FakeRequest(POST, importEntryNumberRoute)
            .withFormUrlEncodedBody(("value", answer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect a GET to Unauthorised when IQ1.0 was answered Yes" in {

      val answersIq1Yes = userAnswersWithGuardData.set(VehicleFromEuPage, true).success.value

      val application = applicationBuilderWithVatTrader(userAnswers = Some(answersIq1Yes)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, importEntryNumberRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect a GET to Unauthorised for a private individual" in {

      val application = applicationFor(classOf[FakeIdentifierAction])

      running(application) {
        val result = route(application, FakeRequest(GET, importEntryNumberRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect a GET to Unauthorised for a non-VAT organisation" in {

      val application = applicationFor(classOf[FakeOrganisationIdentifierAction])

      running(application) {
        val result = route(application, FakeRequest(GET, importEntryNumberRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect a GET to Unauthorised when import 2 is not in the current session" in {

      val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, importTwoRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect a POST to Unauthorised when import 2 is not in the current session" in {

      val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request =
          FakeRequest(POST, importTwoRoute)
            .withFormUrlEncodedBody(("value", answer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
