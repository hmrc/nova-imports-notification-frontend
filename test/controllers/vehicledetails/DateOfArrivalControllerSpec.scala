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
import config.FrontendAppConfig
import connectors.{GetEuMemberStatesError, NovaImportsBackendConnector}
import controllers.actions.*
import controllers.{routes, vehicledetails}
import forms.DateOfArrivalFormProvider
import models.*
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.*
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.VehicleDetailsInset
import views.html.DateOfArrivalView

import java.time.LocalDate
import scala.concurrent.Future

class DateOfArrivalControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val supplierNumber = SupplierNumber(1)
  val importNumber   = ImportNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  val answer: LocalDate                = LocalDate.of(2026, 3, 27)
  val firstRegistrationDate: LocalDate = LocalDate.of(2026, 3, 10)

  val euMemberStates: EuMemberStates = EuMemberStates(Set(Country("DE", Some("Germany")), Country("FR", Some("France"))))

  lazy val supplierRoute = vehicledetails.routes.DateOfArrivalController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode).url
  lazy val importRoute   = vehicledetails.routes.DateOfArrivalController.importOnPageLoad(importNumber, vehicleNumber, NormalMode).url

  val supplierJourneyAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
    .unsafeSet(VehicleDatesPage(supplierNumber, vehicleNumber), Set[VehicleDates](VehicleDates.FirstRegistration, VehicleDates.MadeAvailable))

  val importJourneyAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, false)
    .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A")))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1, "details" -> Json.obj("dateRoadUseKnown" -> true))))

  private def supplierWithFirstRegistration(countryCode: String): UserAnswers =
    supplierJourneyAnswers
      .unsafeSet(DateOfFirstRegistrationPage(vehicleNumber), firstRegistrationDate)
      .unsafeSet(CountryOfFirstRegistrationPage(vehicleNumber), countryCode)

  private def withCar(answers: UserAnswers): UserAnswers =
    answers
      .unsafeSet(AddVehicleTypePage(vehicleNumber), AddVehicleType.Car)
      .unsafeSet(AddVehicleDetailsCarPage(vehicleNumber), AddVehicleDetailsCar("Ford", "Focus", "1.0 EcoBoost", "Titanium", "Hatchback"))

  private def dateFields(day: String, month: String, year: String) =
    List("value.day" -> day, "value.month" -> month, "value.year" -> year)

  private def formFor(application: Application, isRequired: Boolean): Form[Option[LocalDate]] =
    application.injector.instanceOf[DateOfArrivalFormProvider].apply(isRequired)(messages(application))

  private def supplierSubmitCall(mode: Mode): Call =
    vehicledetails.routes.DateOfArrivalController.supplierOnSubmit(supplierNumber, vehicleNumber, mode)

  private def importSubmitCall(mode: Mode): Call =
    vehicledetails.routes.DateOfArrivalController.importOnSubmit(importNumber, vehicleNumber, mode)

  private def expectedView(application: Application, form: Form[Option[LocalDate]], answers: UserAnswers, submitCall: Call)(implicit
    request: Request[?]
  ): String = {
    val msgs      = messages(application)
    val appConfig = application.injector.instanceOf[FrontendAppConfig]
    application.injector
      .instanceOf[DateOfArrivalView]
      .apply(
        form,
        VehicleDetailsInset(answers, vehicleNumber)(msgs),
        submitCall,
        appConfig.secureRegistrationSchemeUrl,
        appConfig.registerAVehicleUrl
      )(request, msgs)
      .toString
  }

  final case class TestApplication(application: Application, sessionRepository: SessionRepository, backendConnector: NovaImportsBackendConnector)

  private def testApplication(
    userAnswers: UserAnswers,
    euMemberStatesResponse: Future[Either[GetEuMemberStatesError, EuMemberStates]] = Future.successful(Right(euMemberStates))
  ): TestApplication = {

    val mockSessionRepository = mock[SessionRepository]
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

    val mockBackendConnector = mock[NovaImportsBackendConnector]
    when(mockBackendConnector.getEuMemberStates()(using any[HeaderCarrier])) thenReturn euMemberStatesResponse

    val application =
      applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[NovaImportsBackendConnector].toInstance(mockBackendConnector)
        )
        .build()

    TestApplication(application, mockSessionRepository, mockBackendConnector)
  }

  private def savedAnswers(mockSessionRepository: SessionRepository): UserAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
    verify(mockSessionRepository).set(captor.capture())
    captor.getValue
  }

  private def assertRedirectsAndSaves(userAnswers: UserAnswers, url: String, fields: List[(String, String)])(
    assertSaved: UserAnswers => Unit
  ): NovaImportsBackendConnector = {

    val testApp = testApplication(userAnswers)

    running(testApp.application) {
      val result = route(testApp.application, FakeRequest(POST, url).withFormUrlEncodedBody(fields*)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual onwardRoute.url
      assertSaved(savedAnswers(testApp.sessionRepository))
    }

    testApp.backendConnector
  }

  private def assertBadRequestWithError(userAnswers: UserAnswers, url: String, fields: List[(String, String)])(
    expectedError: String
  ): TestApplication = {

    val testApp = testApplication(userAnswers)

    running(testApp.application) {
      val result = route(testApp.application, FakeRequest(POST, url).withFormUrlEncodedBody(fields*)).value

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) must include(expectedError)
      verify(testApp.sessionRepository, never()).set(any())
    }

    testApp
  }

  private def assertJourneyRecoveryWithoutSaving(euMemberStatesResponse: Future[Either[GetEuMemberStatesError, EuMemberStates]]) = {

    val testApp = testApplication(supplierWithFirstRegistration("DE"), euMemberStatesResponse)

    running(testApp.application) {
      val result = route(testApp.application, FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(dateFields("27", "03", "2026")*)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      verify(testApp.sessionRepository, never()).set(any())
    }
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

  "DateOfArrivalController" - {

    "on the supplier journey" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(supplierJourneyAnswers)).build()

        running(application) {
          implicit val request = FakeRequest(GET, supplierRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual expectedView(
            application,
            formFor(application, isRequired = true),
            supplierJourneyAnswers,
            supplierSubmitCall(NormalMode)
          )
        }
      }

      "must link to the secure registration scheme and the Register a Vehicle service" in {

        val application = applicationBuilder(userAnswers = Some(supplierJourneyAnswers)).build()

        running(application) {
          val content = contentAsString(route(application, FakeRequest(GET, supplierRoute)).value)

          content must include("""href="https://www.gov.uk/vehicle-registration-schemes-for-the-motor-trade/secure-registration-scheme"""")
          content must include("""href="https://register-vehicle.service.gov.uk/"""")
        }
      }

      "must post back to the change URL for a GET in CheckMode" in {

        val application = applicationBuilder(userAnswers = Some(supplierJourneyAnswers)).build()

        running(application) {
          implicit val request =
            FakeRequest(GET, vehicledetails.routes.DateOfArrivalController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode).url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual expectedView(
            application,
            formFor(application, isRequired = true),
            supplierJourneyAnswers,
            supplierSubmitCall(CheckMode)
          )
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = supplierJourneyAnswers.unsafeSet(DateOfArrivalPage(vehicleNumber), answer)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          implicit val request = FakeRequest(GET, supplierRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual expectedView(
            application,
            formFor(application, isRequired = true).fill(Some(answer)),
            userAnswers,
            supplierSubmitCall(NormalMode)
          )
        }
      }

      "must show the vehicle details inset on a GET" in {

        val application = applicationBuilder(userAnswers = Some(withCar(supplierJourneyAnswers))).build()

        running(application) {
          val result = route(application, FakeRequest(GET, supplierRoute)).value

          status(result) mustEqual OK
          contentAsString(result) must include("<b>Ford Focus 1.0 EcoBoost Titanium Hatchback</b>")
        }
      }

      "must save the date in the vehicle details and redirect to the next page without calling the backend when no first registration is answered" in {

        val backendConnector = assertRedirectsAndSaves(supplierJourneyAnswers, supplierRoute, dateFields("27", "03", "2026")) { saved =>
          saved.get(DateOfArrivalPage(vehicleNumber)) mustEqual Some(answer)
          (saved.data \ "vehicles" \ "1" \ "details" \ "dateArrivedInUk").as[LocalDate] mustEqual answer
        }

        verify(backendConnector, never()).getEuMemberStates()(using any[HeaderCarrier])
      }

      "must return a Bad Request and the empty date error when no date is entered" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("", "", ""))("Enter the date the vehicle arrived in the UK")
      }

      "must return a Bad Request and the missing day error when only the day is missing" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("", "03", "2026"))(
          "The date the vehicle arrived in the UK must include a day"
        )
      }

      "must return a Bad Request and the missing month error when only the month is missing" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("27", "", "2026"))(
          "The date the vehicle arrived in the UK must include a month"
        )
      }

      "must return a Bad Request and the missing year error when only the year is missing" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("27", "03", ""))(
          "The date the vehicle arrive in the UK must include a year"
        )
      }

      "must return a Bad Request and the two-part error when the day and month are missing" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("", "", "2026"))(
          "The date the vehicle arrived in the UK must include a day and month"
        )
      }

      "must return a Bad Request and the two-part error when the day and year are missing" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("", "03", ""))(
          "The date the vehicle arrived in the UK must include a day and year"
        )
      }

      "must return a Bad Request and the two-part error when the month and year are missing" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("27", "", ""))(
          "The date the vehicle arrived in the UK must include a month and year"
        )
      }

      "must return a Bad Request and the format error when the day is a single digit" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("7", "03", "2026"))(
          "Enter the date the vehicle arrived in the UK in the correct format"
        )
      }

      "must return a Bad Request and the real date error when the date does not exist" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("31", "02", "2026"))(
          "The date the vehicle arrived in the UK must be a real date"
        )
      }

      "must return a Bad Request and the future date error when the date is in the future" in {
        val nextYear = LocalDate.now.plusYears(1).getYear.toString
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, dateFields("01", "01", nextYear))(
          "The date the vehicle arrived in the UK must be today or in the past"
        )
      }

      "must show the vehicle details inset again on a Bad Request" in {
        assertBadRequestWithError(withCar(supplierJourneyAnswers), supplierRoute, dateFields("", "", ""))(
          "<b>Ford Focus 1.0 EcoBoost Titanium Hatchback</b>"
        )
      }

      "must return a Bad Request when the vehicle was first registered in an EU country and arrived on the same date" in {

        val testApp = assertBadRequestWithError(supplierWithFirstRegistration("DE"), supplierRoute, dateFields("10", "03", "2026"))(
          "The date the vehicle arrived in the UK must be after the date the vehicle was first registered"
        )

        verify(testApp.backendConnector, times(1)).getEuMemberStates()(using any[HeaderCarrier])
      }

      "must save the date when the vehicle was first registered in an EU country and arrived the day after" in {
        assertRedirectsAndSaves(supplierWithFirstRegistration("DE"), supplierRoute, dateFields("11", "03", "2026")) { saved =>
          saved.get(DateOfArrivalPage(vehicleNumber)) mustEqual Some(LocalDate.of(2026, 3, 11))
        }
      }

      "must return a Bad Request when the vehicle was first registered in the UK and arrived on the same date" in {
        assertBadRequestWithError(supplierWithFirstRegistration("GB"), supplierRoute, dateFields("10", "03", "2026"))(
          "The date the vehicle arrived in the UK must be after the date the vehicle was first registered"
        )
      }

      "must save the date when the vehicle was first registered outside the EU and arrived on the same date" in {
        assertRedirectsAndSaves(supplierWithFirstRegistration("US"), supplierRoute, dateFields("10", "03", "2026")) { saved =>
          saved.get(DateOfArrivalPage(vehicleNumber)) mustEqual Some(firstRegistrationDate)
        }
      }

      "must return a Bad Request when the vehicle was first registered outside the EU and arrived the day before" in {
        assertBadRequestWithError(supplierWithFirstRegistration("US"), supplierRoute, dateFields("09", "03", "2026"))(
          "The date the vehicle arrived in the UK must be the same as or after the date the vehicle was first registered"
        )
      }

      "must ignore first registration answers when the vehicle dates no longer include the date of first registration" in {

        val answers = supplierJourneyAnswers
          .unsafeSet(VehicleDatesPage(supplierNumber, vehicleNumber), Set[VehicleDates](VehicleDates.MadeAvailable))
          .unsafeSet(DateOfFirstRegistrationPage(vehicleNumber), firstRegistrationDate)
          .unsafeSet(CountryOfFirstRegistrationPage(vehicleNumber), "DE")

        val backendConnector = assertRedirectsAndSaves(answers, supplierRoute, dateFields("01", "03", "2026")) { saved =>
          saved.get(DateOfArrivalPage(vehicleNumber)) mustEqual Some(LocalDate.of(2026, 3, 1))
        }

        verify(backendConnector, never()).getEuMemberStates()(using any[HeaderCarrier])
      }

      "must not compare with the first registration date when the country of first registration is not answered" in {

        val answers = supplierJourneyAnswers.unsafeSet(DateOfFirstRegistrationPage(vehicleNumber), firstRegistrationDate)

        val backendConnector = assertRedirectsAndSaves(answers, supplierRoute, dateFields("01", "03", "2026")) { saved =>
          saved.get(DateOfArrivalPage(vehicleNumber)) mustEqual Some(LocalDate.of(2026, 3, 1))
        }

        verify(backendConnector, never()).getEuMemberStates()(using any[HeaderCarrier])
      }

      "must redirect to JourneyRecovery without saving when the backend returns an error for the EU member states" in {
        assertJourneyRecoveryWithoutSaving(Future.successful(Left(GetEuMemberStatesError.UpstreamError(500, "boom"))))
      }

      "must redirect to JourneyRecovery without saving when the EU member states call fails" in {
        assertJourneyRecoveryWithoutSaving(Future.failed(new RuntimeException("connection refused")))
      }

      "must redirect to Unauthorised for a GET if no existing session data is found" in {
        assertUnauthorisedOnGet(None, supplierRoute)
      }

      "must redirect to Unauthorised for a POST if no existing session data is found" in {
        assertUnauthorisedOnPost(None, supplierRoute)
      }

      "must redirect to Unauthorised when the identifier rejects the user" in {
        assertUnauthorisedWhenIdentifierRejectsOnGet(supplierJourneyAnswers, supplierRoute)
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
          vehicledetails.routes.DateOfArrivalController.supplierOnPageLoad(SupplierNumber(2), vehicleNumber, NormalMode).url
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle number in the URL is not one of the user's vehicles" in {
        assertUnauthorisedOnGet(
          Some(supplierJourneyAnswers),
          vehicledetails.routes.DateOfArrivalController.supplierOnPageLoad(supplierNumber, VehicleNumber(999), NormalMode).url
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
    }

    "on the import journey" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(importJourneyAnswers)).build()

        running(application) {
          implicit val request = FakeRequest(GET, importRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual expectedView(
            application,
            formFor(application, isRequired = false),
            importJourneyAnswers,
            importSubmitCall(NormalMode)
          )
        }
      }

      "must post back to the change URL for a GET in CheckMode" in {

        val application = applicationBuilder(userAnswers = Some(importJourneyAnswers)).build()

        running(application) {
          implicit val request =
            FakeRequest(GET, vehicledetails.routes.DateOfArrivalController.importOnPageLoad(importNumber, vehicleNumber, CheckMode).url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual expectedView(
            application,
            formFor(application, isRequired = false),
            importJourneyAnswers,
            importSubmitCall(CheckMode)
          )
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = importJourneyAnswers.unsafeSet(DateOfArrivalPage(vehicleNumber), answer)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          implicit val request = FakeRequest(GET, importRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual expectedView(
            application,
            formFor(application, isRequired = false).fill(Some(answer)),
            userAnswers,
            importSubmitCall(NormalMode)
          )
        }
      }

      "must save the date and redirect to the next page when valid data is submitted" in {
        assertRedirectsAndSaves(importJourneyAnswers, importRoute, dateFields("27", "03", "2026")) { saved =>
          saved.get(DateOfArrivalPage(vehicleNumber)) mustEqual Some(answer)
        }
      }

      "must redirect to the next page without saving a date when no date is entered" in {
        assertRedirectsAndSaves(importJourneyAnswers, importRoute, dateFields("", "", "")) { saved =>
          saved.get(DateOfArrivalPage(vehicleNumber)) mustEqual None
        }
      }

      "must remove a previously saved date when no date is entered" in {

        val userAnswers = importJourneyAnswers.unsafeSet(DateOfArrivalPage(vehicleNumber), answer)

        assertRedirectsAndSaves(userAnswers, importRoute, dateFields("", "", "")) { saved =>
          saved.get(DateOfArrivalPage(vehicleNumber)) mustEqual None
        }
      }

      "must return a Bad Request and the missing year error when only the year is missing" in {
        assertBadRequestWithError(importJourneyAnswers, importRoute, dateFields("27", "03", ""))(
          "The date the vehicle arrive in the UK must include a year"
        )
      }

      "must return a Bad Request and the future date error when the date is in the future" in {
        val nextYear = LocalDate.now.plusYears(1).getYear.toString
        assertBadRequestWithError(importJourneyAnswers, importRoute, dateFields("01", "01", nextYear))(
          "The date the vehicle arrived in the UK must be today or in the past"
        )
      }

      "must not compare with the first registration date" in {

        val answers = importJourneyAnswers
          .unsafeSet(DateOfFirstRegistrationPage(vehicleNumber), firstRegistrationDate)
          .unsafeSet(CountryOfFirstRegistrationPage(vehicleNumber), "DE")

        val backendConnector = assertRedirectsAndSaves(answers, importRoute, dateFields("01", "03", "2026")) { saved =>
          saved.get(DateOfArrivalPage(vehicleNumber)) mustEqual Some(LocalDate.of(2026, 3, 1))
        }

        verify(backendConnector, never()).getEuMemberStates()(using any[HeaderCarrier])
      }

      "must redirect to Unauthorised for a GET if the vehicle was brought from the EU" in {
        assertUnauthorisedOnGet(Some(importJourneyAnswers.unsafeSet(VehicleFromEuPage, true)), importRoute)
      }

      "must redirect to Unauthorised for a GET if the import number in the URL is not one of the user's imports" in {
        assertUnauthorisedOnGet(
          Some(importJourneyAnswers),
          vehicledetails.routes.DateOfArrivalController.importOnPageLoad(ImportNumber(2), vehicleNumber, NormalMode).url
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
