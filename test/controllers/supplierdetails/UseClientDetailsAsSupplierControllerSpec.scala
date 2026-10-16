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
import forms.UseClientDetailsAsSupplierFormProvider
import models.{AddVehicleDetails, AgentSelectedClient, CheckMode, DraftId, NormalMode, SupplierNumber, TraderInformation, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{AgentSelectedClientPage, DraftIdPage}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplierdetails.UseClientDetailsAsSupplierPage
import pages.sections.vehicledetails.AddVehicleDetailsPage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.AllSuppliersQuery
import repositories.SessionRepository
import viewmodels.checkAnswers.SupplierClientDetailsSummary
import views.html.UseClientDetailsAsSupplierView

import scala.concurrent.Future

class UseClientDetailsAsSupplierControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new UseClientDetailsAsSupplierFormProvider()
  val form         = formProvider()

  val supplierNumber = SupplierNumber(1)

  lazy val pageLoadRoute = supplierdetails.routes.UseClientDetailsAsSupplierController.onPageLoad(supplierNumber, NormalMode).url
  lazy val submitRoute   = supplierdetails.routes.UseClientDetailsAsSupplierController.onSubmit(supplierNumber, NormalMode).url

  private val selectedClient = AgentSelectedClient("100000001", Some("ABC Ltd"))

  private val clientTraderInformation = TraderInformation(
    traderName = Some("ABC Ltd"),
    tradingName = None,
    addressLine1 = Some("1 Arundel Mews"),
    addressLine2 = Some("Sunnymede"),
    addressLine3 = None,
    addressLine4 = None,
    postcode = Some("BN11 5RG")
  )

  private def stubConnector(
    result: Either[GetTraderInformationError, TraderInformation] = Right(clientTraderInformation)
  ): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getClientTraderInformation(any())(any())) thenReturn Future.successful(result)
    connector
  }

  private val answersSatisfyingGuard: UserAnswers =
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(AddVehicleDetailsPage, AddVehicleDetails.BySupplier)
      .unsafeSet(VehicleFromEuPage, true)
      .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj()))
      .unsafeSet(AgentSelectedClientPage, selectedClient)

  private def agentApplicationBuilder(
    userAnswers: Option[UserAnswers],
    connector: NovaImportsBackendConnector = stubConnector()
  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[NovaImportsBackendConnector].toInstance(connector)
      )

  private def expectedView(application: Application, boundForm: play.api.data.Form[Boolean], mode: models.Mode = NormalMode)(implicit
    request: play.api.mvc.Request[?]
  ): String = {
    implicit val msgs: play.api.i18n.Messages = messages(application)

    val view      = application.injector.instanceOf[UseClientDetailsAsSupplierView]
    val appConfig = application.injector.instanceOf[FrontendAppConfig]

    view(
      boundForm,
      supplierNumber,
      mode,
      SupplierClientDetailsSummary(Some("ABC Ltd"), Some(clientTraderInformation)),
      appConfig.vatNotice728Url
    ).toString
  }

  private def assertUnauthorised(userAnswers: Option[UserAnswers], url: String = pageLoadRoute): Unit = {
    val application = agentApplicationBuilder(userAnswers).build()

    running(application) {
      val result = route(application, FakeRequest(GET, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }
  }

  "UseClientDetailsAsSupplierController" - {

    "must return OK and the correct view for a GET" in {

      val application = agentApplicationBuilder(Some(answersSatisfyingGuard)).build()

      running(application) {
        implicit val request: play.api.test.FakeRequest[play.api.mvc.AnyContentAsEmpty.type] = FakeRequest(GET, pageLoadRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView(application, form)
      }
    }

    "must return OK and the correct view for a GET in CheckMode" in {

      val application = agentApplicationBuilder(Some(answersSatisfyingGuard)).build()

      running(application) {
        implicit val request: play.api.test.FakeRequest[play.api.mvc.AnyContentAsEmpty.type] =
          FakeRequest(GET, supplierdetails.routes.UseClientDetailsAsSupplierController.onPageLoad(supplierNumber, CheckMode).url)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView(application, form, CheckMode)
      }
    }

    "must populate the view when the question has previously been answered" in {

      val application = agentApplicationBuilder(Some(answersSatisfyingGuard.unsafeSet(UseClientDetailsAsSupplierPage(supplierNumber), true))).build()

      running(application) {
        implicit val request: play.api.test.FakeRequest[play.api.mvc.AnyContentAsEmpty.type] = FakeRequest(GET, pageLoadRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView(application, form.fill(true))
      }
    }

    "must save the answer and redirect to the next page when Yes is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = agentApplicationBuilder(Some(answersSatisfyingGuard))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must save the answer and redirect to the next page when No is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = agentApplicationBuilder(Some(answersSatisfyingGuard))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody("value" -> "false")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and the required error when nothing is selected" in {

      val application = agentApplicationBuilder(Some(answersSatisfyingGuard)).build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody("value" -> "")

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(application)("useClientDetailsAsSupplier.error.required"))
      }
    }

    "must redirect to Unauthorised for a GET if no existing session data is found" in {
      assertUnauthorised(None)
    }

    "must redirect to Unauthorised for a GET if the draftId is missing" in {
      assertUnauthorised(Some(answersSatisfyingGuard.remove(DraftIdPage).success.value))
    }

    "must redirect to Unauthorised for a GET if the vehicle was not brought from the EU" in {
      assertUnauthorised(Some(answersSatisfyingGuard.unsafeSet(VehicleFromEuPage, false)))
    }

    "must redirect to Unauthorised for a GET if the supplier number is not one of the user's suppliers" in {
      assertUnauthorised(
        Some(answersSatisfyingGuard),
        supplierdetails.routes.UseClientDetailsAsSupplierController.onPageLoad(SupplierNumber(2), NormalMode).url
      )
    }

    "must redirect to Unauthorised for a GET if the agent has not selected a client" in {
      assertUnauthorised(Some(answersSatisfyingGuard.remove(AgentSelectedClientPage).success.value))
    }

    "must redirect to Unauthorised for a GET if the user is not an agent" in {

      val application = applicationBuilder(userAnswers = Some(answersSatisfyingGuard))
        .overrides(bind[NovaImportsBackendConnector].toInstance(stubConnector()))
        .build()

      running(application) {
        val result = route(application, FakeRequest(GET, pageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must still render the page with the address not provided when the client lookup fails" in {

      val application =
        agentApplicationBuilder(Some(answersSatisfyingGuard), stubConnector(Left(GetTraderInformationError.NotFound))).build()

      running(application) {
        val result = route(application, FakeRequest(GET, pageLoadRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include("ABC Ltd")
        contentAsString(result) must include(messages(application)("useClientDetailsAsSupplier.notProvided"))
      }
    }

    "must redirect to Unauthorised for a POST if the agent has not selected a client" in {

      val application = agentApplicationBuilder(Some(answersSatisfyingGuard.remove(AgentSelectedClientPage).success.value)).build()

      running(application) {
        val request = FakeRequest(POST, submitRoute).withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
