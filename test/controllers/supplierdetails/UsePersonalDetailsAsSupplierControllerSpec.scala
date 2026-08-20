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

package controllers.supplierdetails

import base.SpecBase
import com.google.inject.name.Names
import config.FrontendAppConfig
import connectors.{GetTraderInformationError, NovaImportsBackendConnector}
import controllers.actions.*
import controllers.{routes, supplierdetails}
import forms.UsePersonalDetailsAsSupplierFormProvider
import models.{AddVehicleDetails, Address, Country, DraftId, NameDetails, NormalMode, PurchaserOrOnBehalf, TraderInformation, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.{PurchaserOrOnBehalfPage, VehicleBusinessUsePage, VehicleFromEuPage}
import pages.sections.vehicledetails.AddVehicleDetailsPage
import pages.sections.notifieraddress.AddressPage
import pages.sections.notifierdetails.NameDetailsPage
import pages.sections.supplierdetails.UsePersonalDetailsAsSupplierPage
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import viewmodels.checkAnswers.SupplierPersonalDetailsSummary
import views.html.UsePersonalDetailsAsSupplierView

import scala.concurrent.Future

class UsePersonalDetailsAsSupplierControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new UsePersonalDetailsAsSupplierFormProvider()
  val form         = formProvider()

  lazy val usePersonalDetailsAsSupplierRoute       = supplierdetails.routes.UsePersonalDetailsAsSupplierController.onPageLoad(NormalMode).url
  lazy val usePersonalDetailsAsSupplierSubmitRoute = supplierdetails.routes.UsePersonalDetailsAsSupplierController.onSubmit(NormalMode).url

  // Everything the guard requires for the default Individual (type 1) identity.
  private val answersSatisfyingGuard: UserAnswers =
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(AddVehicleDetailsPage, AddVehicleDetails.BySupplier)
      .unsafeSet(VehicleFromEuPage, true)
      .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser)
      .unsafeSet(NameDetailsPage, NameDetails("Mr", "John", "Smith"))
      .unsafeSet(AddressPage, Address(Seq("1 High Street"), Some("AB1 2CD"), Country("GB", "United Kingdom")))

  // A VAT-registered organisation (types 4/5) needs no IQ3 answer and no session details to pass.
  private val vatTraderAnswersSatisfyingGuard: UserAnswers =
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(AddVehicleDetailsPage, AddVehicleDetails.BySupplier)
      .unsafeSet(VehicleFromEuPage, true)

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

  private val traderInformation: TraderInformation = TraderInformation(
    traderName = Some("ABC LTD"),
    tradingName = Some("ABC Trading"),
    addressLine1 = Some("1 High Street"),
    addressLine2 = Some("Testtown"),
    addressLine3 = None,
    addressLine4 = None,
    postcode = Some("TF3 4ER")
  )

  private def connectorReturning(result: Either[GetTraderInformationError, TraderInformation]): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getTraderInformation()(any())) thenReturn Future.successful(result)
    connector
  }

  "UsePersonalDetailsAsSupplierController" - {

    "must return OK and the correct view for a GET when the guard passes (non-agent who chose add by supplier)" in {

      val application = applicationBuilder(userAnswers = Some(answersSatisfyingGuard)).build()

      running(application) {
        val request         = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
        val result          = route(application, request).value
        val view            = application.injector.instanceOf[UsePersonalDetailsAsSupplierView]
        val appConfig       = application.injector.instanceOf[FrontendAppConfig]
        val msgs            = messages(application)
        val personalDetails = SupplierPersonalDetailsSummary.fromSession(answersSatisfyingGuard)(msgs)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, personalDetails, appConfig.vatNotice728Url)(request, msgs).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = answersSatisfyingGuard.unsafeSet(UsePersonalDetailsAsSupplierPage, true)
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request         = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
        val result          = route(application, request).value
        val view            = application.injector.instanceOf[UsePersonalDetailsAsSupplierView]
        val appConfig       = application.injector.instanceOf[FrontendAppConfig]
        val msgs            = messages(application)
        val personalDetails = SupplierPersonalDetailsSummary.fromSession(userAnswers)(msgs)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, personalDetails, appConfig.vatNotice728Url)(request, msgs).toString
      }
    }

    "must redirect to Unauthorised for an agent (agents cannot use their personal details as the supplier details)" in {

      val application = agentApplicationBuilder(Some(answersSatisfyingGuard)).build()

      running(application) {
        val request = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(answersSatisfyingGuard))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, usePersonalDetailsAsSupplierSubmitRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(answersSatisfyingGuard)).build()

      running(application) {
        val request         = FakeRequest(POST, usePersonalDetailsAsSupplierSubmitRoute).withFormUrlEncodedBody(("value", ""))
        val boundForm       = form.bind(Map("value" -> ""))
        val view            = application.injector.instanceOf[UsePersonalDetailsAsSupplierView]
        val appConfig       = application.injector.instanceOf[FrontendAppConfig]
        val msgs            = messages(application)
        val personalDetails = SupplierPersonalDetailsSummary.fromSession(answersSatisfyingGuard)(msgs)
        val result          = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, personalDetails, appConfig.vatNotice728Url)(request, msgs).toString
      }
    }

    "must redirect to Unauthorised for a non-agent who did not choose to add vehicle details by supplier" in {

      val answers     = emptyUserAnswers.unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must return OK for a VAT-registered organisation (types 4/5) that has no IQ3 answer and no personal details in the session" in {

      val application = applicationBuilderWithVatTrader(Some(vatTraderAnswersSatisfyingGuard)).build()

      running(application) {
        val request = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
        val result  = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "for a VAT-registered organisation who answered OQ1.0 Yes" - {

      val answers = vatTraderAnswersSatisfyingGuard.unsafeSet(VehicleBusinessUsePage, true)

      "must render the name and address from the RDS trader record" in {

        val application = applicationBuilderWithVatTrader(Some(answers))
          .overrides(bind[NovaImportsBackendConnector].toInstance(connectorReturning(Right(traderInformation))))
          .build()

        running(application) {
          val request = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
          val result  = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("ABC LTD")
          contentAsString(result) must include("1 High Street")
          contentAsString(result) must include("TF3 4ER")
          contentAsString(result) must not include "Not provided"
        }
      }

      "must render 'Not provided' when the vrn has no trader record" in {

        val application = applicationBuilderWithVatTrader(Some(answers))
          .overrides(bind[NovaImportsBackendConnector].toInstance(connectorReturning(Left(GetTraderInformationError.NotFound))))
          .build()

        running(application) {
          val request = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
          val result  = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Not provided")
        }
      }

      "must render 'Not provided' when the trader lookup throws" in {

        val connector = mock[NovaImportsBackendConnector]
        when(connector.getTraderInformation()(any())) thenReturn Future.failed(new RuntimeException("connection reset"))

        val application = applicationBuilderWithVatTrader(Some(answers))
          .overrides(bind[NovaImportsBackendConnector].toInstance(connector))
          .build()

        running(application) {
          val request = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
          val result  = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Not provided")
        }
      }

      "must ignore any stale personal details left in the session" in {

        val staleAnswers = answers
          .unsafeSet(NameDetailsPage, NameDetails("Mr", "John", "Smith"))
          .unsafeSet(AddressPage, Address(Seq("1 Session Street"), Some("AB1 2CD"), Country("GB", "United Kingdom")))

        val application = applicationBuilderWithVatTrader(Some(staleAnswers))
          .overrides(bind[NovaImportsBackendConnector].toInstance(connectorReturning(Right(traderInformation))))
          .build()

        running(application) {
          val request = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
          val result  = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("ABC LTD")
          contentAsString(result) must not include "Mr John Smith"
          contentAsString(result) must not include "1 Session Street"
        }
      }

      "must render the trader record on a Bad Request" in {

        val application = applicationBuilderWithVatTrader(Some(answers))
          .overrides(bind[NovaImportsBackendConnector].toInstance(connectorReturning(Right(traderInformation))))
          .build()

        running(application) {
          val request = FakeRequest(POST, usePersonalDetailsAsSupplierSubmitRoute).withFormUrlEncodedBody(("value", ""))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include("ABC LTD")
        }
      }
    }

    "for a VAT-registered organisation who answered OQ1.0 No" - {

      val answers = vatTraderAnswersSatisfyingGuard
        .unsafeSet(VehicleBusinessUsePage, false)
        .unsafeSet(NameDetailsPage, NameDetails("Mr", "John", "Smith"))
        .unsafeSet(AddressPage, Address(Seq("1 Session Street"), Some("AB1 2CD"), Country("GB", "United Kingdom")))

      "must render the name and address from the session, without looking up the trader record" in {

        val connector = mock[NovaImportsBackendConnector]

        val application = applicationBuilderWithVatTrader(Some(answers))
          .overrides(bind[NovaImportsBackendConnector].toInstance(connector))
          .build()

        running(application) {
          val request = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
          val result  = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Mr John Smith")
          contentAsString(result) must include("1 Session Street")
          verify(connector, never).getTraderInformation()(any())
        }
      }

      "must render 'Not provided' when the session details have not been captured yet" in {

        val connector = mock[NovaImportsBackendConnector]

        val application = applicationBuilderWithVatTrader(Some(vatTraderAnswersSatisfyingGuard.unsafeSet(VehicleBusinessUsePage, false)))
          .overrides(bind[NovaImportsBackendConnector].toInstance(connector))
          .build()

        running(application) {
          val request = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
          val result  = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Not provided")
          verify(connector, never).getTraderInformation()(any())
        }
      }
    }

    "must return OK and render the details as 'Not provided' for a non-VAT-registered user whose personal details are not in the session" in {

      val answers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(AddVehicleDetailsPage, AddVehicleDetails.BySupplier)
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
        val result  = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Not provided")
      }
    }

    "must proceed to the next page for a POST from a non-VAT-registered user whose personal details are not in the session" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val answers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(AddVehicleDetailsPage, AddVehicleDetails.BySupplier)
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser)
      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, usePersonalDetailsAsSupplierSubmitRoute).withFormUrlEncodedBody(("value", "true"))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to Unauthorised when IQ1 (vehicle from EU) was not answered Yes" in {

      val answers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(AddVehicleDetailsPage, AddVehicleDetails.BySupplier)
        .unsafeSet(VehicleFromEuPage, false)
        .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a non-VAT-registered user who answered IQ3 as on behalf of the purchaser" in {

      val answers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(AddVehicleDetailsPage, AddVehicleDetails.BySupplier)
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a non-agent when no draft has been created" in {

      val answers     = emptyUserAnswers.unsafeSet(AddVehicleDetailsPage, AddVehicleDetails.BySupplier)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, usePersonalDetailsAsSupplierRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, usePersonalDetailsAsSupplierSubmitRoute).withFormUrlEncodedBody(("value", "true"))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
