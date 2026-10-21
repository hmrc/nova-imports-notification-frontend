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
import controllers.actions.{DataRequiredAction, DataRequiredActionImpl, DataRetrievalAction, FakeDataRetrievalAction, FakeOrganisationIdentifierAction, IdentifierAction}
import controllers.{routes, vehicledetails}
import forms.ImportEntryDateFormProvider
import models.{DraftId, ImportNumber, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{ImportEntryDatePage, ImportEntryNumberPage}
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.AllImportsQuery
import repositories.SessionRepository
import views.html.ImportEntryDateView

import java.time.LocalDate
import scala.concurrent.Future

class ImportEntryDateControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val importNumber = ImportNumber(1)

  val answer: LocalDate = LocalDate.of(2026, 3, 27)

  lazy val importEntryDateRoute =
    vehicledetails.routes.ImportEntryDateController.onPageLoad(importNumber, NormalMode).url

  lazy val importTwoRoute =
    vehicledetails.routes.ImportEntryDateController.onPageLoad(ImportNumber(2), NormalMode).url

  val userAnswersWithGuardData: UserAnswers = emptyUserAnswers
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(VehicleFromEuPage, false)
    .success
    .value
    .set(ImportEntryNumberPage(importNumber), "12GB3456ACD789EF21")
    .success
    .value

  private def dateFields(day: String, month: String, year: String) =
    Seq("value.day" -> day, "value.month" -> month, "value.year" -> year)

  private def formFor(application: Application): Form[LocalDate] =
    application.injector.instanceOf[ImportEntryDateFormProvider].apply()(messages(application))

  private def applicationFor(identifierAction: Class[? <: IdentifierAction]): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswersWithGuardData)))
      )
      .build()

  private def assertBadRequestWithError(userAnswers: UserAnswers, url: String, fields: Seq[(String, String)])(expectedError: Messages => String) = {

    val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswers)).build()

    running(application) {
      val result = route(application, FakeRequest(POST, url).withFormUrlEncodedBody(fields*)).value

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) must include(expectedError(messages(application)))
    }
  }

  "ImportEntryDateController" - {

    "must return OK and the empty AVD-IE2.0 view for a GET" in {

      val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, importEntryDateRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ImportEntryDateView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(formFor(application), importNumber, NormalMode)(request, messages(application)).toString
      }
    }

    "must fill the AVD-IE2.0 form with the saved import entry date for a GET" in {

      val userAnswers = userAnswersWithGuardData.set(ImportEntryDatePage(importNumber), answer).success.value

      val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, importEntryDateRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ImportEntryDateView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(formFor(application).fill(answer), importNumber, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must save the import entry date for import 1 and redirect for a valid POST" in {

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
          FakeRequest(POST, importEntryDateRoute)
            .withFormUrlEncodedBody(dateFields("27", "03", "2026")*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(captor.capture())
        captor.getValue.get(ImportEntryDatePage(importNumber)) mustEqual Some(answer)
      }
    }

    "must return a Bad Request and the empty date error when no date is entered" in {
      assertBadRequestWithError(userAnswersWithGuardData, importEntryDateRoute, dateFields("", "", ""))(msgs =>
        msgs("importEntryDate.error.required.all")
      )
    }

    "must return a Bad Request and the missing day error when only the day is missing" in {
      assertBadRequestWithError(userAnswersWithGuardData, importEntryDateRoute, dateFields("", "03", "2026"))(msgs =>
        msgs("importEntryDate.error.required", msgs("date.error.day"))
      )
    }

    "must return a Bad Request and the missing month error when only the month is missing" in {
      assertBadRequestWithError(userAnswersWithGuardData, importEntryDateRoute, dateFields("27", "", "2026"))(msgs =>
        msgs("importEntryDate.error.required", msgs("date.error.month"))
      )
    }

    "must return a Bad Request and the missing year error when only the year is missing" in {
      assertBadRequestWithError(userAnswersWithGuardData, importEntryDateRoute, dateFields("27", "03", ""))(msgs =>
        msgs("importEntryDate.error.required", msgs("date.error.year"))
      )
    }

    "must return a Bad Request and the two-part error when the day and month are missing" in {
      assertBadRequestWithError(userAnswersWithGuardData, importEntryDateRoute, dateFields("", "", "2026"))(msgs =>
        msgs("importEntryDate.error.required.two", msgs("date.error.day"), msgs("date.error.month"))
      )
    }

    "must return a Bad Request and the two-part error when the day and year are missing" in {
      assertBadRequestWithError(userAnswersWithGuardData, importEntryDateRoute, dateFields("", "03", ""))(msgs =>
        msgs("importEntryDate.error.required.two", msgs("date.error.day"), msgs("date.error.year"))
      )
    }

    "must return a Bad Request and the two-part error when the month and year are missing" in {
      assertBadRequestWithError(userAnswersWithGuardData, importEntryDateRoute, dateFields("27", "", ""))(msgs =>
        msgs("importEntryDate.error.required.two", msgs("date.error.month"), msgs("date.error.year"))
      )
    }

    "must return a Bad Request and the format error when the date is not made up of numbers" in {
      assertBadRequestWithError(userAnswersWithGuardData, importEntryDateRoute, dateFields("aa", "03", "2026"))(msgs =>
        msgs("importEntryDate.error.invalid")
      )
    }

    "must return a Bad Request and the real date error when the date does not exist" in {
      assertBadRequestWithError(userAnswersWithGuardData, importEntryDateRoute, dateFields("31", "02", "2026"))(msgs =>
        msgs("importEntryDate.error.notARealDate")
      )
    }

    "must return a Bad Request and the future date error when the date is in the future" in {
      assertBadRequestWithError(userAnswersWithGuardData, importEntryDateRoute, dateFields("01", "01", "2999"))(msgs =>
        msgs("importEntryDate.error.future")
      )
    }

    "must redirect a GET to Unauthorised when there is no session data" in {

      val application = applicationBuilderWithVatTrader(userAnswers = None).build()

      running(application) {
        val result = route(application, FakeRequest(GET, importEntryDateRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect a POST to Unauthorised when there is no draftId in session" in {

      val answersWithoutDraftId = userAnswersWithGuardData.remove(DraftIdPage).success.value

      val application = applicationBuilderWithVatTrader(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request =
          FakeRequest(POST, importEntryDateRoute)
            .withFormUrlEncodedBody(dateFields("27", "03", "2026")*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect a GET to Unauthorised when IQ1.0 was answered Yes" in {

      val answersIq1Yes = userAnswersWithGuardData.set(VehicleFromEuPage, true).success.value

      val application = applicationBuilderWithVatTrader(userAnswers = Some(answersIq1Yes)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, importEntryDateRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect a GET to Unauthorised for a non-VAT organisation" in {

      val application = applicationFor(classOf[FakeOrganisationIdentifierAction])

      running(application) {
        val result = route(application, FakeRequest(GET, importEntryDateRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect a GET to Unauthorised when import 1 has no answers yet" in {

      val answersWithEmptyImport = userAnswersWithGuardData.set(AllImportsQuery, Map("1" -> Json.obj())).success.value

      val application = applicationBuilderWithVatTrader(userAnswers = Some(answersWithEmptyImport)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, importEntryDateRoute)).value

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
  }
}
