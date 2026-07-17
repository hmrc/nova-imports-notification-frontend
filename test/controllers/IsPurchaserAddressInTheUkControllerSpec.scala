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
import forms.IsPurchaserAddressInTheUkFormProvider
import models.{DraftId, NormalMode, PurchaserOrOnBehalf, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.PurchaserOrOnBehalfPage
import pages.sections.purchaseraddress.IsPurchaserAddressInTheUkPage
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.IsPurchaserAddressInTheUkView

import scala.concurrent.Future

class IsPurchaserAddressInTheUkControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new IsPurchaserAddressInTheUkFormProvider()
  val form         = formProvider()

  lazy val isPurchaserAddressInTheUkRoute       = routes.IsPurchaserAddressInTheUkController.onPageLoad(NormalMode).url
  lazy val isPurchaserAddressInTheUkSubmitRoute = routes.IsPurchaserAddressInTheUkController.onSubmit(NormalMode).url

  private val nonAgentAnswersSatisfyingGuard: UserAnswers =
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)

  private val agentAnswersSatisfyingGuard: UserAnswers =
    emptyUserAnswers.unsafeSet(DraftIdPage, DraftId("DRAFT-001"))

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

  "IsPurchaserAddressInTheUkController" - {

    "must return OK and the correct view for a GET when guard passes (non-agent on behalf of the purchaser)" in {

      val application = applicationBuilder(userAnswers = Some(nonAgentAnswersSatisfyingGuard)).build()

      running(application) {
        val request = FakeRequest(GET, isPurchaserAddressInTheUkRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[IsPurchaserAddressInTheUkView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = nonAgentAnswersSatisfyingGuard.unsafeSet(IsPurchaserAddressInTheUkPage, true)
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, isPurchaserAddressInTheUkRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[IsPurchaserAddressInTheUkView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode)(request, messages(application)).toString
      }
    }

    "must return OK for an agent whose draft has been created (agents need only a draft id)" in {

      val application = agentApplicationBuilder(Some(agentAnswersSatisfyingGuard)).build()

      running(application) {
        val request = FakeRequest(GET, isPurchaserAddressInTheUkRoute)
        val result  = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(nonAgentAnswersSatisfyingGuard))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, isPurchaserAddressInTheUkSubmitRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(nonAgentAnswersSatisfyingGuard)).build()

      running(application) {
        val request   = FakeRequest(POST, isPurchaserAddressInTheUkSubmitRoute).withFormUrlEncodedBody(("value", ""))
        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[IsPurchaserAddressInTheUkView]
        val result    = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised for a non-agent who answered as the purchaser (not on behalf)" in {

      val answers = emptyUserAnswers
        .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
        .unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, isPurchaserAddressInTheUkRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a non-agent when no draft has been created" in {

      val answers     = emptyUserAnswers.unsafeSet(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, isPurchaserAddressInTheUkRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, isPurchaserAddressInTheUkRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, isPurchaserAddressInTheUkSubmitRoute).withFormUrlEncodedBody(("value", "true"))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
