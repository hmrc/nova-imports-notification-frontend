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
import forms.PaymentCurrencyFormProvider
import models.{CheckMode, Currency, DraftId, ImportNumber, Mode, NormalMode, SupplierNumber, UserAnswers, VehicleNumber}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.PaymentCurrencyPage
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
import views.html.PaymentCurrencyView

import scala.concurrent.Future

class PaymentCurrencyControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val supplierNumber = SupplierNumber(1)
  val importNumber   = ImportNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  val answer       = "EUR"
  val importAnswer = "JPY"

  lazy val supplierRoute =
    vehicledetails.routes.PaymentCurrencyController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode).url
  lazy val importRoute = vehicledetails.routes.PaymentCurrencyController.importOnPageLoad(importNumber, vehicleNumber, NormalMode).url

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

  private def supplierCurrenciesFor(application: Application): List[Currency] =
    application.injector.instanceOf[FrontendAppConfig].supplierCurrencies

  private def importCurrenciesFor(application: Application): List[Currency] =
    application.injector.instanceOf[FrontendAppConfig].currencies

  private def formFor(application: Application, currencies: List[Currency]): Form[String] =
    application.injector.instanceOf[PaymentCurrencyFormProvider].apply(currencies)

  private def supplierView(application: Application, form: Form[String], mode: Mode)(implicit request: FakeRequest[?]): String =
    application.injector
      .instanceOf[PaymentCurrencyView]
      .apply(supplierCurrenciesFor(application), form, PaymentCurrencyController.SupplierHeadingKey, supplierSubmitCall(mode))(
        request,
        messages(application)
      )
      .toString

  private def importView(application: Application, form: Form[String], mode: Mode)(implicit request: FakeRequest[?]): String =
    application.injector
      .instanceOf[PaymentCurrencyView]
      .apply(importCurrenciesFor(application), form, PaymentCurrencyController.ImportHeadingKey, importSubmitCall(mode))(
        request,
        messages(application)
      )
      .toString

  private def supplierSubmitCall(mode: Mode): Call =
    vehicledetails.routes.PaymentCurrencyController.supplierOnSubmit(supplierNumber, vehicleNumber, mode)

  private def importSubmitCall(mode: Mode): Call =
    vehicledetails.routes.PaymentCurrencyController.importOnSubmit(importNumber, vehicleNumber, mode)

  private def applicationWithMockRepository(
    userAnswers: UserAnswers,
    builder: Option[UserAnswers] => GuiceApplicationBuilder = applicationBuilder(_)
  ): (Application, SessionRepository) = {

    val mockSessionRepository = mock[SessionRepository]
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

    val application =
      builder(Some(userAnswers))
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

  private def assertUnauthorisedOnGet(
    userAnswers: Option[UserAnswers],
    url: String,
    builder: Option[UserAnswers] => GuiceApplicationBuilder = applicationBuilder(_)
  ) = {

    val application = builder(userAnswers).build()

    running(application) {
      val result = route(application, FakeRequest(GET, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }
  }

  private def assertUnauthorisedOnPost(
    userAnswers: Option[UserAnswers],
    url: String,
    builder: Option[UserAnswers] => GuiceApplicationBuilder = applicationBuilder(_)
  ) = {

    val application = builder(userAnswers).build()

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

  private def assertBadRequestWithError(
    userAnswers: UserAnswers,
    url: String,
    value: String,
    builder: Option[UserAnswers] => GuiceApplicationBuilder = applicationBuilder(_)
  )(expectedError: Messages => String) = {

    val application = builder(Some(userAnswers)).build()

    running(application) {
      val result = route(application, FakeRequest(POST, url).withFormUrlEncodedBody("value" -> value)).value

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) must include(expectedError(messages(application)))
    }
  }

  "PaymentCurrencyController" - {

    "on the supplier journey" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(supplierJourneyAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual supplierView(application, formFor(application, supplierCurrenciesFor(application)), NormalMode)(request)
        }
      }

      "must post back to the change URL for a GET in CheckMode" in {

        val application = applicationBuilder(userAnswers = Some(supplierJourneyAnswers)).build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              vehicledetails.routes.PaymentCurrencyController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode).url
            )

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual supplierView(application, formFor(application, supplierCurrenciesFor(application)), CheckMode)(request)
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

        val userAnswers = supplierJourneyAnswers.unsafeSet(PaymentCurrencyPage(vehicleNumber), answer)

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual supplierView(
            application,
            formFor(application, supplierCurrenciesFor(application)).fill(answer),
            NormalMode
          )(request)
        }
      }

      "must redirect to the next page and save the currency code when valid data is submitted" in {

        val (application, mockSessionRepository) = applicationWithMockRepository(supplierJourneyAnswers)

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody("value" -> answer)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(PaymentCurrencyPage(vehicleNumber)) mustEqual Some(answer)
        }
      }

      "must return a Bad Request and the required error when no currency is given" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, "")(msgs => msgs("paymentCurrency.error.required"))
      }

      "must return a Bad Request and the required error when the currency is not in the list" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, "ZZZ")(msgs => msgs("paymentCurrency.error.required"))
      }

      "must return a Bad Request and the required error when the currency is only offered on the import journey" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, importAnswer)(msgs => msgs("paymentCurrency.error.required"))
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
          vehicledetails.routes.PaymentCurrencyController.supplierOnPageLoad(SupplierNumber(2), vehicleNumber, NormalMode).url
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle number in the URL is not one of the user's vehicles" in {
        assertUnauthorisedOnGet(
          Some(supplierJourneyAnswers),
          vehicledetails.routes.PaymentCurrencyController.supplierOnPageLoad(supplierNumber, VehicleNumber(999), NormalMode).url
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

        val application = applicationBuilderWithVatTrader(userAnswers = Some(importJourneyAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, importRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual importView(application, formFor(application, importCurrenciesFor(application)), NormalMode)(request)
        }
      }

      "must post back to the change URL for a GET in CheckMode" in {

        val application = applicationBuilderWithVatTrader(userAnswers = Some(importJourneyAnswers)).build()

        running(application) {
          val request =
            FakeRequest(GET, vehicledetails.routes.PaymentCurrencyController.importOnPageLoad(importNumber, vehicleNumber, CheckMode).url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual importView(application, formFor(application, importCurrenciesFor(application)), CheckMode)(request)
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = importJourneyAnswers.unsafeSet(PaymentCurrencyPage(vehicleNumber), importAnswer)

        val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, importRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual importView(
            application,
            formFor(application, importCurrenciesFor(application)).fill(importAnswer),
            NormalMode
          )(request)
        }
      }

      "must redirect to the next page and save the currency code when valid data is submitted" in {

        val (application, mockSessionRepository) = applicationWithMockRepository(importJourneyAnswers, applicationBuilderWithVatTrader(_))

        running(application) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody("value" -> importAnswer)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(PaymentCurrencyPage(vehicleNumber)) mustEqual Some(importAnswer)
        }
      }

      "must return a Bad Request and the required error when no currency is given" in {
        assertBadRequestWithError(importJourneyAnswers, importRoute, "", applicationBuilderWithVatTrader(_))(msgs =>
          msgs("paymentCurrency.error.required")
        )
      }

      "must return OK for a GET for an agent" in {

        val application =
          new GuiceApplicationBuilder()
            .overrides(
              bind[DataRequiredAction].to[DataRequiredActionImpl],
              bind[IdentifierAction].to[FakeAgentIdentifierAction],
              bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeAgentIdentifierAction],
              bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
              bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeAgentIdentifierAction],
              bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
              bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(importJourneyAnswers)))
            )
            .build()

        running(application) {
          status(route(application, FakeRequest(GET, importRoute)).value) mustEqual OK
        }
      }

      "must redirect to Unauthorised for a GET for a private individual" in {
        assertUnauthorisedOnGet(Some(importJourneyAnswers), importRoute)
      }

      "must redirect to Unauthorised for a POST for a private individual" in {
        assertUnauthorisedOnPost(Some(importJourneyAnswers), importRoute)
      }

      "must redirect to Unauthorised for a GET if the vehicle was brought from the EU" in {
        assertUnauthorisedOnGet(Some(importJourneyAnswers.unsafeSet(VehicleFromEuPage, true)), importRoute, applicationBuilderWithVatTrader(_))
      }

      "must redirect to Unauthorised for a GET if the import number in the URL is not one of the user's imports" in {
        assertUnauthorisedOnGet(
          Some(importJourneyAnswers),
          vehicledetails.routes.PaymentCurrencyController.importOnPageLoad(ImportNumber(2), vehicleNumber, NormalMode).url,
          applicationBuilderWithVatTrader(_)
        )
      }

      "must redirect to Unauthorised for a GET if the import has no answers yet" in {
        assertUnauthorisedOnGet(
          Some(importJourneyAnswers.unsafeSet(AllImportsQuery, Map("1" -> Json.obj()))),
          importRoute,
          applicationBuilderWithVatTrader(_)
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different import" in {

        val answers = importJourneyAnswers
          .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A"), "2" -> Json.obj("importEntryNumber" -> "987654321B")))
          .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 2, "details" -> Json.obj("dateRoadUseKnown" -> true))))

        assertUnauthorisedOnGet(Some(answers), importRoute, applicationBuilderWithVatTrader(_))
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a supplier rather than an import" in {

        val answers =
          importJourneyAnswers.unsafeSet(
            AllVehiclesQuery,
            Map("1" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("dateRoadUseKnown" -> true)))
          )

        assertUnauthorisedOnGet(Some(answers), importRoute, applicationBuilderWithVatTrader(_))
      }

      "must redirect to Unauthorised for a GET when the vehicle has no answers yet" in {
        assertUnauthorisedOnGet(
          Some(importJourneyAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1)))),
          importRoute,
          applicationBuilderWithVatTrader(_)
        )
      }

      "must redirect to Unauthorised for a GET if draftId is missing" in {
        assertUnauthorisedOnGet(Some(importJourneyAnswers.remove(DraftIdPage).success.value), importRoute, applicationBuilderWithVatTrader(_))
      }

      "must redirect to Unauthorised for a POST if draftId is missing" in {
        assertUnauthorisedOnPost(Some(importJourneyAnswers.remove(DraftIdPage).success.value), importRoute, applicationBuilderWithVatTrader(_))
      }

      "must redirect to Unauthorised when the identifier rejects the user" in {
        assertUnauthorisedWhenIdentifierRejectsOnGet(importJourneyAnswers, importRoute)
      }
    }
  }
}
