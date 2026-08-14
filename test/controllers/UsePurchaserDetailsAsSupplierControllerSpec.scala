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
import controllers.actions.*
import forms.UsePurchaserDetailsAsSupplierFormProvider
import models.{AddVehicleDetails, Address, Country, DraftId, NormalMode, PurchaserOrOnBehalf, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{AddVehicleDetailsPage, DraftIdPage}
import pages.sections.initialquestions.{PurchaserOrOnBehalfPage, VehicleFromEuPage}
import pages.sections.purchaserDetails.PurchaserBusinessNamePage
import pages.sections.purchaseraddress.PurchaserAddressPage
import pages.sections.supplierDetails.UsePurchaserDetailsAsSupplierPage
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import viewmodels.checkAnswers.SupplierPurchaserDetailsSummary
import views.html.UsePurchaserDetailsAsSupplierView

import scala.concurrent.Future

class UsePurchaserDetailsAsSupplierControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new UsePurchaserDetailsAsSupplierFormProvider()
  val form         = formProvider()

  lazy val usePurchaserDetailsAsSupplierRoute       = routes.UsePurchaserDetailsAsSupplierController.onPageLoad(NormalMode).url
  lazy val usePurchaserDetailsAsSupplierSubmitRoute = routes.UsePurchaserDetailsAsSupplierController.onSubmit(NormalMode).url

  // A non-agent individual who bought on behalf of the purchaser (IQ3 = OnBehalfOfPurchaser).
  private val answersSatisfyingGuard: UserAnswers =
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(AddVehicleDetailsPage, AddVehicleDetails.BySupplier)
      .unsafeSet(VehicleFromEuPage, true)
      .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
      .unsafeSet(PurchaserBusinessNamePage, "ABC Ltd")
      .unsafeSet(PurchaserAddressPage, Address(Seq("1 Arundel Mews"), Some("BN11 5RG"), Country("GB", "United Kingdom")))

  // An agent without a selected client needs no IQ3 answer to pass the guard.
  private val agentWithoutClientAnswersSatisfyingGuard: UserAnswers =
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

  private def vatTraderApplicationBuilder(userAnswers: Option[UserAnswers]): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeVatTraderIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeVatTraderIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeVatTraderIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  "UsePurchaserDetailsAsSupplierController" - {

    "must return OK and the correct view for a GET when the guard passes (non-agent who bought on behalf of the purchaser)" in {

      val application = applicationBuilder(userAnswers = Some(answersSatisfyingGuard)).build()

      running(application) {
        val request          = FakeRequest(GET, usePurchaserDetailsAsSupplierRoute)
        val result           = route(application, request).value
        val view             = application.injector.instanceOf[UsePurchaserDetailsAsSupplierView]
        val msgs             = messages(application)
        val purchaserDetails = SupplierPurchaserDetailsSummary.fromSession(answersSatisfyingGuard)(msgs)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, purchaserDetails)(request, msgs).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = answersSatisfyingGuard.unsafeSet(UsePurchaserDetailsAsSupplierPage, true)
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request          = FakeRequest(GET, usePurchaserDetailsAsSupplierRoute)
        val result           = route(application, request).value
        val view             = application.injector.instanceOf[UsePurchaserDetailsAsSupplierView]
        val msgs             = messages(application)
        val purchaserDetails = SupplierPurchaserDetailsSummary.fromSession(userAnswers)(msgs)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, purchaserDetails)(request, msgs).toString
      }
    }

    "must return OK for an agent without a selected client, even with no IQ3 answer in the session" in {

      val application = agentApplicationBuilder(Some(agentWithoutClientAnswersSatisfyingGuard)).build()

      running(application) {
        val request = FakeRequest(GET, usePurchaserDetailsAsSupplierRoute)
        val result  = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Unauthorised for a VAT-registered organisation (they belong on the personal-details page)" in {

      val answers     = answersSatisfyingGuard.unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser)
      val application = vatTraderApplicationBuilder(Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, usePurchaserDetailsAsSupplierRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a non-agent who answered IQ3 as the purchaser (they belong on the personal-details page)" in {

      val answers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(AddVehicleDetailsPage, AddVehicleDetails.BySupplier)
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, usePurchaserDetailsAsSupplierRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a non-agent who did not choose to add vehicle details by supplier" in {

      val answers     = emptyUserAnswers.unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, usePurchaserDetailsAsSupplierRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when IQ1 (vehicle from EU) was not answered Yes" in {

      val answers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(AddVehicleDetailsPage, AddVehicleDetails.BySupplier)
        .unsafeSet(VehicleFromEuPage, false)
        .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, usePurchaserDetailsAsSupplierRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a non-agent when no draft has been created" in {

      val answers     = emptyUserAnswers.unsafeSet(AddVehicleDetailsPage, AddVehicleDetails.BySupplier)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, usePurchaserDetailsAsSupplierRoute)
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
          FakeRequest(POST, usePurchaserDetailsAsSupplierSubmitRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(answersSatisfyingGuard)).build()

      running(application) {
        val request          = FakeRequest(POST, usePurchaserDetailsAsSupplierSubmitRoute).withFormUrlEncodedBody(("value", ""))
        val boundForm        = form.bind(Map("value" -> ""))
        val view             = application.injector.instanceOf[UsePurchaserDetailsAsSupplierView]
        val msgs             = messages(application)
        val purchaserDetails = SupplierPurchaserDetailsSummary.fromSession(answersSatisfyingGuard)(msgs)
        val result           = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, purchaserDetails)(request, msgs).toString
      }
    }

    "must redirect to Unauthorised for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, usePurchaserDetailsAsSupplierRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, usePurchaserDetailsAsSupplierSubmitRoute).withFormUrlEncodedBody(("value", "true"))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
