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
import models.draftsections.NotifierAddress
import models.{Address, Country, DraftId, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AddressPage, DraftIdPage}
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import views.html.AddressChangedView

import scala.concurrent.Future

class AddressChangedControllerSpec extends SpecBase with MockitoSugar {

  private lazy val onPageLoadRoute: String = routes.AddressChangedController.onPageLoad().url
  private lazy val onSubmitRoute: String   = routes.AddressChangedController.onSubmit().url

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

  private def stubBackendConnector(result: Either[UpdateSectionError, Unit] = Right(())): NovaImportsBackendConnector = {
    val m = mock[NovaImportsBackendConnector]
    when(m.updateDraftSection(any, any, any)(any)).thenReturn(Future.successful(result))
    m
  }

  private def applicationWith(
    userAnswers: Option[UserAnswers] = Some(answersWithAddressAndDraft),
    backendConnector: NovaImportsBackendConnector = stubBackendConnector()
  ): Application =
    applicationBuilder(userAnswers)
      .overrides(bind[NovaImportsBackendConnector].toInstance(backendConnector))
      .build()

  "AddressChanged Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationWith()

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[AddressChangedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(address)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no address is found" in {
      val application = applicationWith(userAnswers = None)

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must call F4 with the stored address and redirect to the next section on submit" in {
      val backendConnector = stubBackendConnector()
      val application      = applicationWith(backendConnector = backendConnector)

      running(application) {
        val request = FakeRequest(POST, onSubmitRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url

        val body = ArgumentCaptor.forClass(classOf[JsObject])
        verify(backendConnector).updateDraftSection(eqTo(draftId), eqTo("notifier-address"), body.capture())(any[HeaderCarrier])
        body.getValue mustBe Json.toJson(NotifierAddress.fromAddress(address)).as[JsObject]
      }
    }

    "must redirect to Journey Recovery on submit when F4 fails" in {
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
  }
}
