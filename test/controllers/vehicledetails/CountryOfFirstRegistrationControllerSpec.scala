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
import controllers.actions.*
import controllers.{routes, vehicledetails}
import forms.CountryOfFirstRegistrationFormProvider
import models.{CheckMode, Country, DraftId, ImportNumber, Mode, NormalMode, SupplierNumber, UserAnswers, VehicleNumber}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.CountryOfFirstRegistrationPage
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
import views.html.CountryOfFirstRegistrationView

import scala.concurrent.Future

class CountryOfFirstRegistrationControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val supplierNumber = SupplierNumber(1)
  val importNumber   = ImportNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  val answer = "FR"

  lazy val supplierRoute =
    vehicledetails.routes.CountryOfFirstRegistrationController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode).url
  lazy val importRoute = vehicledetails.routes.CountryOfFirstRegistrationController.importOnPageLoad(importNumber, vehicleNumber, NormalMode).url

  val supplierJourneyAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("dateRoadUseKnown" -> true))))

  val importJourneyAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, false)
    .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A")))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1, "details" -> Json.obj("dateRoadUseKnown" -> true))))

  private def countriesFor(application: Application): List[Country] =
    application.injector.instanceOf[FrontendAppConfig].countries

  private def formFor(application: Application): Form[String] =
    application.injector.instanceOf[CountryOfFirstRegistrationFormProvider].apply(countriesFor(application))

  private def supplierSubmitCall(mode: Mode): Call =
    vehicledetails.routes.CountryOfFirstRegistrationController.supplierOnSubmit(supplierNumber, vehicleNumber, mode)

  private def importSubmitCall(mode: Mode): Call =
    vehicledetails.routes.CountryOfFirstRegistrationController.importOnSubmit(importNumber, vehicleNumber, mode)

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
      val result = route(application, FakeRequest(POST, url).withFormUrlEncodedBody("value" -> answer)).value

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

  private def assertBadRequestWithError(userAnswers: UserAnswers, url: String, value: String)(expectedError: Messages => String) = {

    val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

    running(application) {
      val result = route(application, FakeRequest(POST, url).withFormUrlEncodedBody("value" -> value)).value

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) must include(expectedError(messages(application)))
    }
  }

  "CountryOfFirstRegistrationController" - {

    "on the supplier journey" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(supplierJourneyAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryOfFirstRegistrationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(countriesFor(application), formFor(application), supplierSubmitCall(NormalMode))(
            request,
            messages(application)
          ).toString
        }
      }

      "must post back to the change URL for a GET in CheckMode" in {

        val application = applicationBuilder(userAnswers = Some(supplierJourneyAnswers)).build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              vehicledetails.routes.CountryOfFirstRegistrationController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode).url
            )

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryOfFirstRegistrationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(countriesFor(application), formFor(application), supplierSubmitCall(CheckMode))(
            request,
            messages(application)
          ).toString
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

        val userAnswers = supplierJourneyAnswers.unsafeSet(CountryOfFirstRegistrationPage(vehicleNumber), answer)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryOfFirstRegistrationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(countriesFor(application), formFor(application).fill(answer), supplierSubmitCall(NormalMode))(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page and save the country code when valid data is submitted" in {

        val (application, mockSessionRepository) = applicationWithMockRepository(supplierJourneyAnswers)

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody("value" -> answer)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(CountryOfFirstRegistrationPage(vehicleNumber)) mustEqual Some(answer)
        }
      }

      "must return a Bad Request and the required error when no country is given" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, "")(msgs => msgs("countryOfFirstRegistration.error.required"))
      }

      "must return a Bad Request and the required error when the country is not in the list" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, "ZZ")(msgs => msgs("countryOfFirstRegistration.error.required"))
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
          vehicledetails.routes.CountryOfFirstRegistrationController.supplierOnPageLoad(SupplierNumber(2), vehicleNumber, NormalMode).url
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle number in the URL is not one of the user's vehicles" in {
        assertUnauthorisedOnGet(
          Some(supplierJourneyAnswers),
          vehicledetails.routes.CountryOfFirstRegistrationController.supplierOnPageLoad(supplierNumber, VehicleNumber(999), NormalMode).url
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
          val request = FakeRequest(GET, importRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryOfFirstRegistrationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(countriesFor(application), formFor(application), importSubmitCall(NormalMode))(
            request,
            messages(application)
          ).toString
        }
      }

      "must post back to the change URL for a GET in CheckMode" in {

        val application = applicationBuilder(userAnswers = Some(importJourneyAnswers)).build()

        running(application) {
          val request =
            FakeRequest(GET, vehicledetails.routes.CountryOfFirstRegistrationController.importOnPageLoad(importNumber, vehicleNumber, CheckMode).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryOfFirstRegistrationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(countriesFor(application), formFor(application), importSubmitCall(CheckMode))(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = importJourneyAnswers.unsafeSet(CountryOfFirstRegistrationPage(vehicleNumber), answer)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, importRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryOfFirstRegistrationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(countriesFor(application), formFor(application).fill(answer), importSubmitCall(NormalMode))(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page and save the country code when valid data is submitted" in {

        val (application, mockSessionRepository) = applicationWithMockRepository(importJourneyAnswers)

        running(application) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody("value" -> answer)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(CountryOfFirstRegistrationPage(vehicleNumber)) mustEqual Some(answer)
        }
      }

      "must return a Bad Request and the required error when no country is given" in {
        assertBadRequestWithError(importJourneyAnswers, importRoute, "")(msgs => msgs("countryOfFirstRegistration.error.required"))
      }

      "must redirect to Unauthorised for a GET if the vehicle was brought from the EU" in {
        assertUnauthorisedOnGet(Some(importJourneyAnswers.unsafeSet(VehicleFromEuPage, true)), importRoute)
      }

      "must redirect to Unauthorised for a GET if the import number in the URL is not one of the user's imports" in {
        assertUnauthorisedOnGet(
          Some(importJourneyAnswers),
          vehicledetails.routes.CountryOfFirstRegistrationController.importOnPageLoad(ImportNumber(2), vehicleNumber, NormalMode).url
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
