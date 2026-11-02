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
import com.google.inject.name.Names
import controllers.actions.*
import controllers.{purchaseraddress, routes}
import models.{Address, Country, DraftId, NormalMode, PurchaserOrOnBehalf, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.NotifyingAsPurchaserPage
import pages.sections.purchaseraddress.{PurchaserAddressJourneyIdPage, PurchaserAddressPage}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
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
    lines = Seq("Address Line 1", "Address Line 2"),
    postcode = Some("AA1 1AA"),
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

  private val answersWithoutAddress: UserAnswers =
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(NotifyingAsPurchaserPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)

  private def applicationFor(
    identifierAction: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers] = Some(answersWithAddress)
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )
      .build()

  "PurchaserAddressCheckYourAnswers Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationFor(classOf[FakeIdentifierAction])

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[PurchaserAddressCheckYourAnswersView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(answersWithAddress)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised for a GET if no session data is found" in {
      val application = applicationFor(classOf[FakeIdentifierAction], userAnswers = None)

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "with a purchaser address in the session" - {

      "must return OK for a GET from a for a private individual" in {
        val application = applicationFor(classOf[FakeIdentifierAction])

        running(application) {
          status(route(application, FakeRequest(GET, onPageLoadRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from a non-VAT organisation" in {
        val application = applicationFor(classOf[FakeOrganisationIdentifierAction])

        running(application) {
          status(route(application, FakeRequest(GET, onPageLoadRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent with the agent enrolment" in {
        val application = applicationFor(classOf[FakeAgentIdentifierAction])

        running(application) {
          status(route(application, FakeRequest(GET, onPageLoadRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent without agent enrolment" in {
        val application = applicationFor(classOf[FakeAgentNoEnrolmentsIdentifierAction])

        running(application) {
          status(route(application, FakeRequest(GET, onPageLoadRoute)).value) mustEqual OK
        }
      }
    }

    "with no purchaser address in the session" - {

      "must redirect a GET to Unauthorised for a private individual" in {
        val application = applicationFor(classOf[FakeIdentifierAction], Some(answersWithoutAddress))

        running(application) {
          val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect a GET to Unauthorised for a non-VAT organisation" in {
        val application = applicationFor(classOf[FakeOrganisationIdentifierAction], Some(answersWithoutAddress))

        running(application) {
          val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect a GET to Unauthorised for an agent with the agent enrolment" in {
        val application = applicationFor(classOf[FakeAgentIdentifierAction], Some(answersWithoutAddress))

        running(application) {
          val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect a GET to Unauthorised for an agent without agent enrolment" in {
        val application = applicationFor(classOf[FakeAgentNoEnrolmentsIdentifierAction], Some(answersWithoutAddress))

        running(application) {
          val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "must redirect to Unauthorised for a GET when there is no draft id" in {
      val application = applicationFor(classOf[FakeIdentifierAction], userAnswers = Some(emptyUserAnswers.unsafeSet(PurchaserAddressPage, address)))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to the notification task list on submit, leaving the saved address alone" in {
      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(classOf[FakeIdentifierAction])

      running(application) {
        val request = FakeRequest(POST, onSubmitRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url

        verify(sessionRepository, never).set(any())
      }
    }

    "must retain the stored address and journey id from the session and redirect to APA1.0 on change address" in {
      val answersWithJourneyId = answersWithAddress.unsafeSet(PurchaserAddressJourneyIdPage, "journey-123")

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(classOf[FakeIdentifierAction], userAnswers = Some(answersWithJourneyId))

      running(application) {
        val request = FakeRequest(GET, onChangeAddressRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual purchaseraddress.routes.IsPurchaserAddressInTheUkController.onPageLoad(NormalMode).url

        verifyNoInteractions(sessionRepository)
      }
    }

    "must redirect to Unauthorised on change address if no session data is found" in {
      val application = applicationFor(classOf[FakeIdentifierAction], userAnswers = None)

      running(application) {
        val request = FakeRequest(GET, onChangeAddressRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
