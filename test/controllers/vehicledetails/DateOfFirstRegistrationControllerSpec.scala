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
import forms.DateOfFirstRegistrationFormProvider
import models.{CheckMode, DraftId, ImportNumber, Mode, NormalMode, SupplierNumber, UserAnswers, VehicleDates, VehicleNumber}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{DateOfFirstRegistrationPage, VehicleDatesPage}
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import views.html.DateOfFirstRegistrationView

import java.time.LocalDate
import scala.concurrent.Future

class DateOfFirstRegistrationControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val supplierNumber = SupplierNumber(1)
  val importNumber   = ImportNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  val answer: LocalDate = LocalDate.of(2026, 3, 27)

  lazy val supplierRoute = vehicledetails.routes.DateOfFirstRegistrationController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode).url
  lazy val importRoute   = vehicledetails.routes.DateOfFirstRegistrationController.importOnPageLoad(importNumber, vehicleNumber, NormalMode).url

  val supplierJourneyAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
    .unsafeSet(VehicleDatesPage(supplierNumber, vehicleNumber), Set[VehicleDates](VehicleDates.AvailabilityAndFirstRegistration))

  val importJourneyAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, false)
    .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A")))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1, "details" -> Json.obj("dateRoadUseKnown" -> true))))

  private def dateFields(day: String, month: String, year: String) =
    Seq("value.day" -> day, "value.month" -> month, "value.year" -> year)

  private def formFor(application: Application): Form[LocalDate] =
    application.injector.instanceOf[DateOfFirstRegistrationFormProvider].apply()(messages(application))

  private def supplierSubmitCall(mode: Mode): Call =
    vehicledetails.routes.DateOfFirstRegistrationController.supplierOnSubmit(supplierNumber, vehicleNumber, mode)

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

  private def assertUnauthorisedOnGet(userAnswers: Option[UserAnswers], url: String) = {

    val application = applicationBuilder(userAnswers = userAnswers).build()

    running(application) {
      val result = route(application, FakeRequest(GET, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }
  }

  private def assertUnauthorisedOnPost(userAnswers: Option[UserAnswers], url: String) = {

    val application = applicationBuilder(userAnswers = userAnswers).build()

    running(application) {
      val result = route(application, FakeRequest(POST, url).withFormUrlEncodedBody(dateFields("27", "03", "2026")*)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }
  }

  private def unauthorisedIdentifierApplication(userAnswers: UserAnswers): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[UnauthorisedIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswers)))
      )
      .build()

  private def assertUnauthorisedWhenIdentifierRejectsOnGet(userAnswers: UserAnswers, url: String) = {

    val application = unauthorisedIdentifierApplication(userAnswers)

    running(application) {
      val result = route(application, FakeRequest(GET, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }
  }

  private def assertBadRequestWithError(userAnswers: UserAnswers, url: String, fields: Seq[(String, String)])(expectedError: Messages => String) = {

    val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

    running(application) {
      val result = route(application, FakeRequest(POST, url).withFormUrlEncodedBody(fields*)).value

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) must include(expectedError(messages(application)))
    }
  }

  "DateOfFirstRegistrationController" - {

    "on the supplier journey" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(supplierJourneyAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[DateOfFirstRegistrationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(formFor(application), supplierSubmitCall(NormalMode))(request, messages(application)).toString
        }
      }

      "must post back to the change URL for a GET in CheckMode" in {

        val application = applicationBuilder(userAnswers = Some(supplierJourneyAnswers)).build()

        running(application) {
          val request =
            FakeRequest(GET, vehicledetails.routes.DateOfFirstRegistrationController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[DateOfFirstRegistrationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(formFor(application), supplierSubmitCall(CheckMode))(request, messages(application)).toString
        }
      }

      "must show a back link on the page" in {

        val application = applicationBuilder(userAnswers = Some(supplierJourneyAnswers)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, supplierRoute)).value

          contentAsString(result) must include("govuk-back-link")
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers =
          supplierJourneyAnswers.unsafeSet(DateOfFirstRegistrationPage(vehicleNumber), answer)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[DateOfFirstRegistrationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(formFor(application).fill(answer), supplierSubmitCall(NormalMode))(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page and save the date when valid data is submitted" in {

        val (application, mockSessionRepository) = applicationWithMockRepository(supplierJourneyAnswers)

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(dateFields("27", "03", "2026")*)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(DateOfFirstRegistrationPage(vehicleNumber)) mustEqual Some(answer)
        }
      }

      "must return a Bad Request and the empty date error when no date is entered" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("", "", ""))(msgs =>
          msgs("dateOfFirstRegistration.error.required.all")
        )
      }

      "must return a Bad Request and the missing day error when only the day is missing" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("", "03", "2026"))(msgs =>
          msgs("dateOfFirstRegistration.error.required", msgs("date.error.day"))
        )
      }

      "must return a Bad Request and the missing month error when only the month is missing" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("27", "", "2026"))(msgs =>
          msgs("dateOfFirstRegistration.error.required", msgs("date.error.month"))
        )
      }

      "must return a Bad Request and the missing year error when only the year is missing" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("27", "03", ""))(msgs =>
          msgs("dateOfFirstRegistration.error.required", msgs("date.error.year"))
        )
      }

      "must return a Bad Request and the two-part error when the day and month are missing" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("", "", "2026"))(msgs =>
          msgs("dateOfFirstRegistration.error.required.two", msgs("date.error.day"), msgs("date.error.month"))
        )
      }

      "must return a Bad Request and the two-part error when the day and year are missing" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("", "03", ""))(msgs =>
          msgs("dateOfFirstRegistration.error.required.two", msgs("date.error.day"), msgs("date.error.year"))
        )
      }

      "must return a Bad Request and the two-part error when the month and year are missing" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("27", "", ""))(msgs =>
          msgs("dateOfFirstRegistration.error.required.two", msgs("date.error.month"), msgs("date.error.year"))
        )
      }

      "must return a Bad Request and the format error when the date is not made up of numbers" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("aa", "03", "2026"))(msgs =>
          msgs("dateOfFirstRegistration.error.invalid")
        )
      }

      "must return a Bad Request and the real date error when the date does not exist" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("31", "02", "2026"))(msgs =>
          msgs("dateOfFirstRegistration.error.notARealDate")
        )
      }

      "must return a Bad Request and the future date error when the date is in the future" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("01", "01", "2999"))(msgs =>
          msgs("dateOfFirstRegistration.error.future")
        )
      }

      "must redirect to Unauthorised for a GET if no existing session data is found" in {
        assertUnauthorisedOnGet(None, supplierRoute)
      }

      "must redirect to Unauthorised when the identifier rejects the user" in {
        assertUnauthorisedWhenIdentifierRejectsOnGet(supplierJourneyAnswers, supplierRoute)
      }

      "must redirect to Unauthorised for a POST if no existing session data is found" in {
        assertUnauthorisedOnPost(None, supplierRoute)
      }

      "must redirect to Unauthorised for a GET if draftId is missing" in {
        assertUnauthorisedOnGet(Some(supplierJourneyAnswers.remove(DraftIdPage).success.value), supplierRoute)
      }

      "must redirect to Unauthorised for a POST if draftId is missing" in {
        assertUnauthorisedOnPost(Some(supplierJourneyAnswers.remove(DraftIdPage).success.value), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if the vehicle was not brought from the EU" in {
        assertUnauthorisedOnGet(Some(supplierJourneyAnswers.unsafeSet(VehicleFromEuPage, false)), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {
        assertUnauthorisedOnGet(
          Some(supplierJourneyAnswers),
          vehicledetails.routes.DateOfFirstRegistrationController.supplierOnPageLoad(SupplierNumber(2), vehicleNumber, NormalMode).url
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle number in the URL is not one of the user's vehicles" in {
        assertUnauthorisedOnGet(
          Some(supplierJourneyAnswers),
          vehicledetails.routes.DateOfFirstRegistrationController.supplierOnPageLoad(supplierNumber, VehicleNumber(999), NormalMode).url
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different supplier" in {

        val answers = supplierJourneyAnswers
          .unsafeSet(
            AllSuppliersQuery,
            Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false), "2" -> Json.obj("usePersonalDetailsAsSupplier" -> false))
          )
          .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 2, "details" -> Json.obj("dateRoadUseKnown" -> true))))

        assertUnauthorisedOnGet(Some(answers), supplierRoute)
      }

      "must redirect to Unauthorised for a GET when the vehicle has no answers yet" in {

        val answers = supplierJourneyAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))

        assertUnauthorisedOnGet(Some(answers), supplierRoute)
      }

      "must return OK for a GET when the vehicle dates answer did not include the date of first registration" in {

        val answers =
          supplierJourneyAnswers.unsafeSet(VehicleDatesPage(supplierNumber, vehicleNumber), Set[VehicleDates](VehicleDates.PurchaseInvoiceDate))

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, supplierRoute)).value

          status(result) mustEqual OK
        }
      }
    }

    "on the import journey" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(importJourneyAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, importRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[DateOfFirstRegistrationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            formFor(application),
            vehicledetails.routes.DateOfFirstRegistrationController.importOnSubmit(importNumber, vehicleNumber, NormalMode)
          )(request, messages(application)).toString
        }
      }

      "must post back to the change URL for a GET in CheckMode" in {

        val application = applicationBuilder(userAnswers = Some(importJourneyAnswers)).build()

        running(application) {
          val request =
            FakeRequest(GET, vehicledetails.routes.DateOfFirstRegistrationController.importOnPageLoad(importNumber, vehicleNumber, CheckMode).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[DateOfFirstRegistrationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            formFor(application),
            vehicledetails.routes.DateOfFirstRegistrationController.importOnSubmit(importNumber, vehicleNumber, CheckMode)
          )(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers =
          importJourneyAnswers.unsafeSet(DateOfFirstRegistrationPage(vehicleNumber), answer)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, importRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[DateOfFirstRegistrationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            formFor(application).fill(answer),
            vehicledetails.routes.DateOfFirstRegistrationController.importOnSubmit(importNumber, vehicleNumber, NormalMode)
          )(request, messages(application)).toString
        }
      }

      "must redirect to the next page and save the date when valid data is submitted" in {

        val (application, mockSessionRepository) = applicationWithMockRepository(importJourneyAnswers)

        running(application) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody(dateFields("27", "03", "2026")*)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(DateOfFirstRegistrationPage(vehicleNumber)) mustEqual Some(answer)
        }
      }

      "must return a Bad Request and the empty date error when no date is entered" in {
        assertBadRequestWithError(importJourneyAnswers, importRoute, dateFields("", "", ""))(msgs =>
          msgs("dateOfFirstRegistration.error.required.all")
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle was brought from the EU" in {
        assertUnauthorisedOnGet(Some(importJourneyAnswers.unsafeSet(VehicleFromEuPage, true)), importRoute)
      }

      "must redirect to Unauthorised for a GET if the import number in the URL is not one of the user's imports" in {
        assertUnauthorisedOnGet(
          Some(importJourneyAnswers),
          vehicledetails.routes.DateOfFirstRegistrationController.importOnPageLoad(ImportNumber(2), vehicleNumber, NormalMode).url
        )
      }

      "must redirect to Unauthorised for a GET if the import has no answers yet" in {
        assertUnauthorisedOnGet(Some(importJourneyAnswers.unsafeSet(AllImportsQuery, Map("1" -> Json.obj()))), importRoute)
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different import" in {

        val answers = importJourneyAnswers
          .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A"), "2" -> Json.obj("importEntryNumber" -> "987654321B")))
          .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 2, "details" -> Json.obj("dateRoadUseKnown" -> true))))

        assertUnauthorisedOnGet(Some(answers), importRoute)
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a supplier rather than an import" in {

        val answers =
          importJourneyAnswers.unsafeSet(
            AllVehiclesQuery,
            Map("1" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("dateRoadUseKnown" -> true)))
          )

        assertUnauthorisedOnGet(Some(answers), importRoute)
      }

      "must redirect to Unauthorised for a GET when the vehicle has no answers yet" in {
        assertUnauthorisedOnGet(Some(importJourneyAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1)))), importRoute)
      }

      "must redirect to Unauthorised for a GET if draftId is missing" in {
        assertUnauthorisedOnGet(Some(importJourneyAnswers.remove(DraftIdPage).success.value), importRoute)
      }

      "must redirect to Unauthorised for a POST if draftId is missing" in {
        assertUnauthorisedOnPost(Some(importJourneyAnswers.remove(DraftIdPage).success.value), importRoute)
      }

      "must redirect to Unauthorised when the identifier rejects the user" in {
        assertUnauthorisedWhenIdentifierRejectsOnGet(importJourneyAnswers, importRoute)
      }
    }
  }
}
