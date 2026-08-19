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
import connectors.{NovaImportsBackendConnector, UpdateSectionError}
import models.draftsections.{NotifierAddress, PurchaserAddress, SupplierAddress}
import models.{Address, Country, DraftId, NormalMode, PurchaserOrOnBehalf, SupplierNumber, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{DraftIdPage, DraftVersionIdPage}
import pages.sections.initialquestions.PurchaserOrOnBehalfPage
import pages.sections.notifieraddress.{AddressJourneyIdPage, AddressPage}
import pages.sections.purchaseraddress.{PurchaserAddressJourneyIdPage, PurchaserAddressPage}
import pages.sections.supplierdetails.SupplierNumberPage
import pages.sections.supplieraddress.{IsSupplierAddressInTheUkPage, SupplierAddressJourneyIdPage, SupplierAddressPage}
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import views.html.AddressChangedView

import scala.concurrent.Future

class AddressChangedControllerSpec extends SpecBase with MockitoSugar {

  private lazy val onPageLoadRoute: String      = routes.AddressChangedController.onPageLoad().url
  private lazy val onSubmitRoute: String        = routes.AddressChangedController.onSubmit().url
  private lazy val onChangeAddressRoute: String = routes.AddressChangedController.onChangeAddress().url

  private val draftId = DraftId("DRAFT-001")
  private val address = Address(
    lines = Seq("12 High Street", "Reading"),
    postcode = Some("RE12 9GC"),
    country = Country("GB", "United Kingdom")
  )

  private val answersWithAddressAndDraft: UserAnswers =
    emptyUserAnswers
      .set(AddressPage, address)
      .success
      .value
      .set(DraftIdPage, draftId)
      .success
      .value
      .set(DraftVersionIdPage, 0L)
      .success
      .value

  private def stubBackendConnector(result: Either[UpdateSectionError, Long] = Right(0L)): NovaImportsBackendConnector = {
    val m = mock[NovaImportsBackendConnector]
    when(m.updateDraftSection(any, any, any)(any)).thenReturn(Future.successful(result))
    m
  }

  private def stubSessionRepository(): SessionRepository = {
    val m = mock[SessionRepository]
    when(m.setPage(any(), any(), any())(any())).thenReturn(Future.successful(answersWithAddressAndDraft))
    when(m.set(any())).thenReturn(Future.successful(true))
    m
  }

  private def applicationWith(
    userAnswers: Option[UserAnswers] = Some(answersWithAddressAndDraft),
    backendConnector: NovaImportsBackendConnector = stubBackendConnector(),
    sessionRepository: SessionRepository = stubSessionRepository()
  ): Application =
    applicationBuilder(userAnswers)
      .overrides(
        bind[NovaImportsBackendConnector].toInstance(backendConnector),
        bind[SessionRepository].toInstance(sessionRepository)
      )
      .build()

  "AddressChanged Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationWith()

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[AddressChangedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          address,
          "addressChanged",
          routes.AddressChangedController.onChangeAddress(),
          routes.AddressChangedController.onSubmit()
        )(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised for a GET if no session data is found" in {
      val application = applicationWith(userAnswers = None)

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must save the stored address and redirect to the next section on submit" in {
      val backendConnector = stubBackendConnector()
      val application      = applicationWith(backendConnector = backendConnector)

      running(application) {
        val request = FakeRequest(POST, onSubmitRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url

        val body = ArgumentCaptor.forClass(classOf[JsObject])
        verify(backendConnector).updateDraftSection(eqTo(draftId), eqTo("notifier-address"), body.capture())(any[HeaderCarrier])
        body.getValue mustBe Json.toJson(NotifierAddress.fromAddress(address)).as[JsObject] + ("versionId", Json.toJson(0L))
      }
    }

    "must redirect to Journey Recovery on submit when saving fails" in {
      val backendConnector = stubBackendConnector(Left(UpdateSectionError.UpstreamError(503, "downstream")))
      val application      = applicationWith(backendConnector = backendConnector)

      running(application) {
        val request = FakeRequest(POST, onSubmitRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery on submit when DraftId is missing" in {
      val answersWithoutDraft = emptyUserAnswers.set(AddressPage, address).success.value
      val backendConnector    = stubBackendConnector()
      val application         = applicationWith(userAnswers = Some(answersWithoutDraft), backendConnector = backendConnector)

      running(application) {
        val request = FakeRequest(POST, onSubmitRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

        verify(backendConnector, org.mockito.Mockito.never).updateDraftSection(any, any, any)(any)
      }
    }

    "must clear the stored address and journey id from the session on change address" in {
      val answersWithJourneyId = answersWithAddressAndDraft
        .set(AddressJourneyIdPage, "journey-123")
        .success
        .value

      val sessionRepository = stubSessionRepository()
      val application       = applicationWith(userAnswers = Some(answersWithJourneyId), sessionRepository = sessionRepository)

      running(application) {
        val request = FakeRequest(GET, onChangeAddressRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual notifieraddress.routes.IsYourAddressInTheUkController.onPageLoad(NormalMode).url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(sessionRepository).set(captor.capture())
        captor.getValue.get(AddressPage) mustBe None
        captor.getValue.get(AddressJourneyIdPage) mustBe None
      }
    }

    "must redirect to Unauthorised on change address if no session data is found" in {
      val application = applicationWith(userAnswers = None)

      running(application) {
        val request = FakeRequest(GET, onChangeAddressRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }

  "AddressChanged Controller with Supplier" - {

    val supplierNumber = SupplierNumber(1)

    lazy val supplierPageLoadRoute      = routes.AddressChangedController.supplierOnPageLoad(supplierNumber).url
    lazy val supplierSubmitRoute        = routes.AddressChangedController.supplierOnSubmit(supplierNumber).url
    lazy val supplierChangeAddressRoute = routes.AddressChangedController.supplierOnChangeAddress(supplierNumber).url

    val supplierAnswers: UserAnswers =
      emptyUserAnswers
        .set(SupplierAddressPage, address)
        .success
        .value
        .set(IsSupplierAddressInTheUkPage, true)
        .success
        .value
        .set(SupplierNumberPage, 1)
        .success
        .value
        .set(DraftIdPage, draftId)
        .success
        .value
        .set(DraftVersionIdPage, 0L)
        .success
        .value

    "must render the supplier copy and post to the supplier routes" in {
      val application = applicationWith(userAnswers = Some(supplierAnswers))

      running(application) {
        val request = FakeRequest(GET, supplierPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[AddressChangedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          address,
          "supplierAddressChanged",
          routes.AddressChangedController.supplierOnChangeAddress(supplierNumber),
          routes.AddressChangedController.supplierOnSubmit(supplierNumber)
        )(request, messages(application)).toString
      }
    }

    "must save to the supplier-address draft section on submit" in {
      val backendConnector = stubBackendConnector()
      val application      = applicationWith(userAnswers = Some(supplierAnswers), backendConnector = backendConnector)

      running(application) {
        val result = route(application, FakeRequest(POST, supplierSubmitRoute)).value

        status(result) mustEqual SEE_OTHER

        val body = ArgumentCaptor.forClass(classOf[JsObject])
        verify(backendConnector).updateDraftSection(eqTo(draftId), eqTo("supplier-address"), body.capture())(any[HeaderCarrier])
        body.getValue mustBe Json.toJson(SupplierAddress.fromAddress(address)).as[JsObject] + ("versionId", Json.toJson(0L))
      }
    }

    "must clear only the supplier address on change address" in {
      val sessionRepository = stubSessionRepository()
      val application       = applicationWith(userAnswers = Some(supplierAnswers), sessionRepository = sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(GET, supplierChangeAddressRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual supplieraddress.routes.IsSupplierAddressInTheUKController.onPageLoad(supplierNumber, NormalMode).url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(sessionRepository).set(captor.capture())
        captor.getValue.get(SupplierAddressPage) mustBe None
        captor.getValue.get(SupplierAddressJourneyIdPage) mustBe None
      }
    }

    "must redirect to Unauthorised when the supplier number is not one held in session" in {
      val application = applicationWith(userAnswers = Some(supplierAnswers))

      running(application) {
        val route9 = routes.AddressChangedController.supplierOnPageLoad(SupplierNumber(9)).url
        val result = route(application, FakeRequest(GET, route9)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when no supplier address has been stored" in {
      val answers     = supplierAnswers.remove(SupplierAddressPage).success.value
      val application = applicationWith(userAnswers = Some(answers))

      running(application) {
        val result = route(application, FakeRequest(GET, supplierPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }

  "AddressChanged Controller with Purchaser" - {

    lazy val purchaserPageLoadRoute      = routes.AddressChangedController.purchaserOnPageLoad().url
    lazy val purchaserSubmitRoute        = routes.AddressChangedController.purchaserOnSubmit().url
    lazy val purchaserChangeAddressRoute = routes.AddressChangedController.purchaserOnChangeAddress().url

    val purchaserAnswers: UserAnswers =
      emptyUserAnswers
        .set(PurchaserAddressPage, address)
        .success
        .value
        .set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
        .success
        .value
        .set(DraftIdPage, draftId)
        .success
        .value
        .set(DraftVersionIdPage, 0L)
        .success
        .value

    "must render the purchaser copy and post to the purchaser routes" in {
      val application = applicationWith(userAnswers = Some(purchaserAnswers))

      running(application) {
        val request = FakeRequest(GET, purchaserPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[AddressChangedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          address,
          "purchaserAddressChanged",
          routes.AddressChangedController.purchaserOnChangeAddress(),
          routes.AddressChangedController.purchaserOnSubmit()
        )(request, messages(application)).toString
      }
    }

    "must save to the purchaser-address draft section on submit" in {
      val backendConnector = stubBackendConnector()
      val application      = applicationWith(userAnswers = Some(purchaserAnswers), backendConnector = backendConnector)

      running(application) {
        val result = route(application, FakeRequest(POST, purchaserSubmitRoute)).value

        status(result) mustEqual SEE_OTHER

        val body = ArgumentCaptor.forClass(classOf[JsObject])
        verify(backendConnector).updateDraftSection(eqTo(draftId), eqTo("purchaser-address"), body.capture())(any[HeaderCarrier])
        body.getValue mustBe Json.toJson(PurchaserAddress.fromAddress(address)).as[JsObject] + ("versionId", Json.toJson(0L))
      }
    }

    "must clear only the purchaser address on change address" in {
      val sessionRepository = stubSessionRepository()
      val application       = applicationWith(userAnswers = Some(purchaserAnswers), sessionRepository = sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(GET, purchaserChangeAddressRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual purchaseraddress.routes.IsPurchaserAddressInTheUkController.onPageLoad(NormalMode).url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(sessionRepository).set(captor.capture())
        captor.getValue.get(PurchaserAddressPage) mustBe None
        captor.getValue.get(PurchaserAddressJourneyIdPage) mustBe None
      }
    }

    "must redirect to Unauthorised when a non-agent has not answered on behalf of the purchaser" in {
      val answers     = purchaserAnswers.remove(PurchaserOrOnBehalfPage).success.value
      val application = applicationWith(userAnswers = Some(answers))

      running(application) {
        val result = route(application, FakeRequest(GET, purchaserPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when no purchaser address has been stored" in {
      val answers     = purchaserAnswers.remove(PurchaserAddressPage).success.value
      val application = applicationWith(userAnswers = Some(answers))

      running(application) {
        val result = route(application, FakeRequest(GET, purchaserPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
