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
import models.{Address, Country, DraftId, NormalMode, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.notifieraddress.{AddressJourneyIdPage, AddressPage}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.YourAddressCheckYourAnswersView

import scala.concurrent.Future

class YourAddressCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar {

  private lazy val onPageLoadRoute: String      = routes.YourAddressCheckYourAnswersController.onPageLoad().url
  private lazy val onSubmitRoute: String        = routes.YourAddressCheckYourAnswersController.onSubmit().url
  private lazy val onChangeAddressRoute: String = routes.YourAddressCheckYourAnswersController.onChangeAddress().url

  private val address = Address(
    lines = Seq("12 High Street", "Reading"),
    postcode = Some("RE12 9GC"),
    country = Country("GB", "United Kingdom")
  )

  private val answersWithAddress: UserAnswers =
    emptyUserAnswers
      .unsafeSet(AddressPage, address)
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))

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

  "YourAddressCheckYourAnswers Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationWith()

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[YourAddressCheckYourAnswersView]

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
      val application = applicationWith(userAnswers = Some(emptyUserAnswers.unsafeSet(AddressPage, address)))

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

    "must clear the stored address and journey id from the session and redirect to AYA1.0 on change address" in {
      val answersWithJourneyId = answersWithAddress.unsafeSet(AddressJourneyIdPage, "journey-123")

      val sessionRepository = stubSessionRepository()
      val application       = applicationWith(userAnswers = Some(answersWithJourneyId), sessionRepository = sessionRepository)

      running(application) {
        val request = FakeRequest(GET, onChangeAddressRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.IsYourAddressInTheUkController.onPageLoad(NormalMode).url

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
}
