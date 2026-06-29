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
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.NotificationTaskListPage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository

import scala.concurrent.Future

class AboutYourDetailsControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  private lazy val aboutYourDetailsRoute = routes.AboutYourDetailsController.onPageLoad().url

  private val answersWithNtlVisited = emptyUserAnswers.set(NotificationTaskListPage, true).success.value

  "AboutYourDetailsController" - {

    "onPageLoad" - {

      "must return OK and render the correct view when the guard passes" in {
        given application: Application = applicationBuilder(userAnswers = Some(answersWithNtlVisited)).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, aboutYourDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("About your details")
          contentAsString(result) must include("You need to provide your contact details.")
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, aboutYourDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery if NotificationTaskListPage is not set" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, aboutYourDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to unauthorised page when the user is not a VAT-registered organisation" in {
        given application: Application = new GuiceApplicationBuilder()
          .overrides(
            bind[DataRequiredAction].to[DataRequiredActionImpl],
            bind[IdentifierAction].to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[UnauthorisedIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
            bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
            bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(answersWithNtlVisited)))
          )
          .build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, aboutYourDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must redirect to the next page when the guard passes" in {
        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        given application: Application = applicationBuilder(userAnswers = Some(answersWithNtlVisited))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, aboutYourDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must redirect to Journey Recovery if no existing userAnswers are found" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, aboutYourDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery if NotificationTaskListPage is not set" in {
        given application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, aboutYourDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
