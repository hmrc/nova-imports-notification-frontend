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
import forms.PhoneNumberFormProvider
import models.requests.IdentifierRequest
import models.{AgentSelectedClient, NormalMode, PurchaserOrOnBehalf, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, BodyParser, BodyParsers, Call, PlayBodyParsers, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, EnrolmentIdentifier, Enrolments}
import views.html.PhoneNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PhoneNumberControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider = new PhoneNumberFormProvider()
  val form         = formProvider()

  val validPhoneNumber = "01632 960 001"

  val answersForIndividual: UserAnswers =
    emptyUserAnswers.set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser).success.value

  lazy val phoneNumberRoute       = routes.PhoneNumberController.onPageLoad(NormalMode).url
  lazy val phoneNumberSubmitRoute = routes.PhoneNumberController.onSubmit(NormalMode).url

  "PhoneNumberController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(answersForIndividual)).build()

      running(application) {
        val request = FakeRequest(GET, phoneNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PhoneNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = answersForIndividual.set(PhoneNumberPage, validPhoneNumber).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, phoneNumberRoute)

        val view = application.injector.instanceOf[PhoneNumberView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validPhoneNumber), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(answersForIndividual))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, phoneNumberSubmitRoute)
            .withFormUrlEncodedBody(("value", validPhoneNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(answersForIndividual)).build()

      running(application) {
        val request =
          FakeRequest(POST, phoneNumberSubmitRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[PhoneNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, phoneNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, phoneNumberSubmitRoute)
            .withFormUrlEncodedBody(("value", validPhoneNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "data guard" - {

      "for a VAT trader (user 4 or 5)" - {

        "must allow access when OQ1 has been answered" in {

          val answers = emptyUserAnswers.set(VehicleBusinessUsePage, true).success.value

          val application = vatTraderApplicationBuilder(Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, phoneNumberRoute)
            val result  = route(application, request).value

            status(result) mustEqual OK
          }
        }

        "must redirect to Unauthorised when OQ1 has not been answered" in {

          val application = vatTraderApplicationBuilder(Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, phoneNumberRoute)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
          }
        }
      }

      "for an Agent with a selected client (user 3 or 6 with client)" - {

        val client = AgentSelectedClient(vrn = "GB123456789", name = Some("Acme Ltd"))

        "must allow access when AQ1 has been answered" in {

          val answers = emptyUserAnswers
            .set(AgentSelectedClientPage, client)
            .success
            .value
            .set(AgentVehicleBusinessUsePage, true)
            .success
            .value

          val application = agentApplicationBuilder(Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, phoneNumberRoute)
            val result  = route(application, request).value

            status(result) mustEqual OK
          }
        }

        "must redirect to Unauthorised when AQ1 has not been answered" in {

          val answers = emptyUserAnswers.set(AgentSelectedClientPage, client).success.value

          val application = agentApplicationBuilder(Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, phoneNumberRoute)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
          }
        }
      }

      "for an Individual / NonVatOrg / Agent without client (users 1, 2, 3-no-client, 6-no-client)" - {

        "must allow access when IQ3 = Purchaser" in {

          val answers = emptyUserAnswers.set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, phoneNumberRoute)
            val result  = route(application, request).value

            status(result) mustEqual OK
          }
        }

        "must allow access when IQ3 = OnBehalfOfPurchaser and IQ3.1 is answered" in {

          val answers = emptyUserAnswers
            .set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
            .success
            .value
            .set(PurchaserBusinessOrIndividualPage, models.PurchaserBusinessOrIndividual.values.head)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, phoneNumberRoute)
            val result  = route(application, request).value

            status(result) mustEqual OK
          }
        }

        "must redirect to Unauthorised when IQ3 has not been answered" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, phoneNumberRoute)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
          }
        }

        "must redirect to Unauthorised when IQ3 = OnBehalfOfPurchaser but IQ3.1 has not been answered" in {

          val answers =
            emptyUserAnswers.set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, phoneNumberRoute)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
          }
        }
      }
    }

    "must redirect to Unauthorised when the user is OGD (user type 7+)" in {

      val application = new GuiceApplicationBuilder()
        .overrides(
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          bind[IdentifierAction].to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[UnauthorisedIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
          bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(answersForIndividual)))
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, phoneNumberRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }

  private def vatTraderApplicationBuilder(userAnswers: Option[UserAnswers]): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeVatTraderOrganisationIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeVatTraderOrganisationIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeVatTraderOrganisationIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  private def agentApplicationBuilder(userAnswers: Option[UserAnswers]): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeAgentIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )
}

class FakeVatTraderOrganisationIdentifierAction @Inject() (bodyParsers: PlayBodyParsers) extends IdentifierAction {

  private val vatEnrolments = Enrolments(
    Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated"))
  )

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] =
    block(IdentifierRequest(request, "id", AffinityGroup.Organisation, vatEnrolments))

  override def parser: BodyParser[AnyContent] = bodyParsers.default

  override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}
