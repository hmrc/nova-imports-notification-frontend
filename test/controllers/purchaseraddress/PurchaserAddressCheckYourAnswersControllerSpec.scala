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

package controllers.purchaseraddress

import base.SpecBase
import controllers.{purchaseraddress, routes}
import models.{Address, Country, DraftId, NormalMode, PurchaserOrOnBehalf, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.NotifyingAsPurchaserPage
import pages.sections.purchaseraddress.{PurchaserAddressJourneyIdPage, PurchaserAddressPage}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.PurchaserAddressCheckYourAnswersView

import scala.concurrent.Future

class PurchaserAddressCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar {

  private lazy val onPageLoadRoute: String      = purchaseraddress.routes.PurchaserAddressCheckYourAnswersController.onPageLoad().url
  private lazy val onSubmitRoute: String        = purchaseraddress.routes.PurchaserAddressCheckYourAnswersController.onSubmit().url
  private lazy val onChangeAddressRoute: String = purchaseraddress.routes.PurchaserAddressCheckYourAnswersController.onChangeAddress().url

  private val address = Address(
    lines = Seq("12 High Street", "Reading"),
    postcode = Some("RE12 9GC"),
    country = Country("GB", "United Kingdom")
  )

  private val answersWithAddress: UserAnswers =
    emptyUserAnswers
      .unsafeSet(PurchaserAddressPage, address)
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(NotifyingAsPurchaserPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)

  private def stubSessionRepository(): SessionRepository = {
    val m = mock[SessionRepository]
    when(m.set(any())).thenReturn(Future.successful(true))
    m
  }

  private def applicationWith(
    userAnswers: Option[UserAnswers] = Some(answersWithAddress),
    sessionRepository: SessionRepository = stubSessionRepository()
  ): Application =
    applicationBuilder(userAnswers)
      .overrides(bind[SessionRepository].toInstance(sessionRepository))
      .build()

  "PurchaserAddressCheckYourAnswers Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationWith()

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[PurchaserAddressCheckYourAnswersView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(answersWithAddress)(request, messages(application)).toString
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

    "must redirect to Unauthorised for a GET when the address has not been saved yet" in {
      val application = applicationWith(userAnswers = Some(emptyUserAnswers.unsafeSet(DraftIdPage, DraftId("DRAFT-001"))))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET when there is no draft id" in {
      val application = applicationWith(userAnswers = Some(emptyUserAnswers.unsafeSet(PurchaserAddressPage, address)))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to the notification task list on submit, leaving the saved address alone" in {
      val sessionRepository = stubSessionRepository()
      val application       = applicationWith(sessionRepository = sessionRepository)

      running(application) {
        val request = FakeRequest(POST, onSubmitRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url

        verify(sessionRepository, never).set(any())
      }
    }

    "must clear the stored address and journey id from the session and redirect to APA1.0 on change address" in {
      val answersWithJourneyId = answersWithAddress.unsafeSet(PurchaserAddressJourneyIdPage, "journey-123")

      val sessionRepository = stubSessionRepository()
      val application       = applicationWith(userAnswers = Some(answersWithJourneyId), sessionRepository = sessionRepository)

      running(application) {
        val request = FakeRequest(GET, onChangeAddressRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual purchaseraddress.routes.IsPurchaserAddressInTheUkController.onPageLoad(NormalMode).url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(sessionRepository).set(captor.capture())
        captor.getValue.get(PurchaserAddressPage) mustBe None
        captor.getValue.get(PurchaserAddressJourneyIdPage) mustBe None
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
}
