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

package controllers.vehicledetails

import base.SpecBase
import com.google.inject.name.Names
import config.FrontendAppConfig
import controllers.actions.*
import controllers.{routes, supplierdetails, vehicledetails}
import forms.AddVehicleDetailsFormProvider
import models.{AddVehicleDetails, AgentSelectedClient, DraftId, NormalMode, PurchaserOrOnBehalf, SupplierNumber, UserAnswers}
import play.api.libs.json.Json
import queries.AllSuppliersQuery
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.AgentSelectedClientPage
import pages.DraftIdPage
import pages.sections.initialquestions.{NotifyingAsPurchaserPage, VehicleFromEuPage}
import pages.sections.vehicledetails.AddVehicleDetailsPage
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.{AddVehicleDetailsBySupplierOnlyView, AddVehicleDetailsView}

import scala.concurrent.Future

class AddVehicleDetailsControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new AddVehicleDetailsFormProvider()
  val form         = formProvider()

  lazy val addVehicleDetailsRoute = vehicledetails.routes.AddVehicleDetailsController.onPageLoad(NormalMode).url

  val userAnswersWithIQ1Yes: UserAnswers = emptyUserAnswers
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(VehicleFromEuPage, true)
    .success
    .value

  private def spreadsheetUrl(app: play.api.Application): String =
    app.injector.instanceOf[FrontendAppConfig].multipleVehiclesSpreadsheetsUrl

  private def applicationFor(
    standardIdentifier: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers]
  ): play.api.Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(standardIdentifier),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )
      .build()

  private def agentApplicationBuilder(userAnswers: UserAnswers): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeAgentIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswers)))
      )

  private def organisationApplicationBuilder(userAnswers: UserAnswers): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeOrganisationIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeOrganisationIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswers)))
      )

  private def applicationWithMockRepository(
    userAnswers: UserAnswers,
    builder: Option[GuiceApplicationBuilder] = None
  ): (play.api.Application, SessionRepository) = {

    val mockSessionRepository = mock[SessionRepository]
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
    when(mockSessionRepository.setPage(any(), any(), any())(any())) thenReturn Future.successful(userAnswers)

    val application =
      builder
        .getOrElse(applicationBuilder(userAnswers = Some(userAnswers)))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

    (application, mockSessionRepository)
  }

  private def savedAnswers(mockSessionRepository: SessionRepository): UserAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
    verify(mockSessionRepository).set(captor.capture())
    captor.getValue
  }

  "AddVehicleDetailsController" - {

    "must return OK and the simplified by-supplier-only view for a GET for a private individual" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithIQ1Yes)).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddVehicleDetailsBySupplierOnlyView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(NormalMode)(request, messages(application)).toString
      }
    }

    "must return OK and the simplified by-supplier-only view for a GET for a non-VAT organisation" in {

      val application = organisationApplicationBuilder(userAnswersWithIQ1Yes).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddVehicleDetailsBySupplierOnlyView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(NormalMode)(request, messages(application)).toString
      }
    }

    "must return OK and the simplified by-supplier-only view for a GET for an agent without a selected client" in {

      val application = agentApplicationBuilder(userAnswersWithIQ1Yes).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddVehicleDetailsBySupplierOnlyView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(NormalMode)(request, messages(application)).toString
      }
    }

    "must return OK and the radio-choice view for a GET for a VAT-registered organisation" in {

      val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswersWithIQ1Yes)).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddVehicleDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, spreadsheetUrl(application))(request, messages(application)).toString
      }
    }

    "must return OK and the radio-choice view for a GET for an agent with a selected client" in {

      val sampleClient = AgentSelectedClient(vrn = "GB123456789", name = Some("Acme Ltd"))
      val answers      = userAnswersWithIQ1Yes.set(AgentSelectedClientPage, sampleClient).success.value
      val application  = agentApplicationBuilder(answers).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddVehicleDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, spreadsheetUrl(application))(request, messages(application)).toString
      }
    }

    "must populate the radio-choice view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithIQ1Yes
        .set(AddVehicleDetailsPage, AddVehicleDetails.BySupplier)
        .success
        .value

      val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val view = application.injector.instanceOf[AddVehicleDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(AddVehicleDetails.BySupplier), NormalMode, spreadsheetUrl(application))(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilderWithVatTrader(userAnswers = Some(userAnswersWithIQ1Yes))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, addVehicleDetailsRoute)
            .withFormUrlEncodedBody(("value", AddVehicleDetails.BySpreadsheet.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must add supplier 1 and send the user to that supplier's first screen when a private individual submits the by-supplier-only page" in {

      val (application, _) = applicationWithMockRepository(userAnswersWithIQ1Yes)

      running(application) {
        val request = FakeRequest(POST, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          supplierdetails.routes.UsePersonalDetailsAsSupplierController.onPageLoad(SupplierNumber(1), NormalMode).url
      }
    }

    "must add supplier 1 and send a non-VAT organisation to UsePersonalDetailsAsSupplierController" in {

      val (application, _) = applicationWithMockRepository(userAnswersWithIQ1Yes, Some(organisationApplicationBuilder(userAnswersWithIQ1Yes)))

      running(application) {
        val request = FakeRequest(POST, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          supplierdetails.routes.UsePersonalDetailsAsSupplierController.onPageLoad(SupplierNumber(1), NormalMode).url
      }
    }

    "must send a private individual who answered 'As the purchaser' to IQ3 to UsePersonalDetailsAsSupplierController" in {

      val answers          = userAnswersWithIQ1Yes.set(NotifyingAsPurchaserPage, PurchaserOrOnBehalf.Purchaser).success.value
      val (application, _) = applicationWithMockRepository(answers)

      running(application) {
        val request = FakeRequest(POST, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          supplierdetails.routes.UsePersonalDetailsAsSupplierController.onPageLoad(SupplierNumber(1), NormalMode).url
      }
    }

    "must send a non-VAT organisation who answered 'As the purchaser' to IQ3 to UsePersonalDetailsAsSupplierController" in {

      val answers          = userAnswersWithIQ1Yes.set(NotifyingAsPurchaserPage, PurchaserOrOnBehalf.Purchaser).success.value
      val (application, _) = applicationWithMockRepository(answers, Some(organisationApplicationBuilder(answers)))

      running(application) {
        val request = FakeRequest(POST, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          supplierdetails.routes.UsePersonalDetailsAsSupplierController.onPageLoad(SupplierNumber(1), NormalMode).url
      }
    }

    "must add the next supplier when the user already has one" in {

      val answersWithSupplier = userAnswersWithIQ1Yes.set(AllSuppliersQuery, Map("1" -> Json.obj())).success.value
      val (application, _)    = applicationWithMockRepository(answersWithSupplier)

      running(application) {
        val request = FakeRequest(POST, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          supplierdetails.routes.UsePersonalDetailsAsSupplierController.onPageLoad(SupplierNumber(2), NormalMode).url
      }
    }

    "must send a private individual who answered 'On behalf of purchaser' to IQ3 to UsePurchaserDetailsAsSupplierController" in {

      val answers          = userAnswersWithIQ1Yes.set(NotifyingAsPurchaserPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser).success.value
      val (application, _) = applicationWithMockRepository(answers)

      running(application) {
        val request = FakeRequest(POST, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          supplierdetails.routes.UsePurchaserDetailsAsSupplierController.onPageLoad(SupplierNumber(1), NormalMode).url
      }
    }

    "must send a non-VAT organisation who answered 'On behalf of purchaser' to IQ3 to UsePurchaserDetailsAsSupplierController" in {

      val answers          = userAnswersWithIQ1Yes.set(NotifyingAsPurchaserPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser).success.value
      val (application, _) = applicationWithMockRepository(answers, Some(organisationApplicationBuilder(answers)))

      running(application) {
        val request = FakeRequest(POST, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          supplierdetails.routes.UsePurchaserDetailsAsSupplierController.onPageLoad(SupplierNumber(1), NormalMode).url
      }
    }

    "must add supplier 1 and send an agent without a selected client to UsePurchaserDetailsAsSupplierController" in {

      val (application, _) = applicationWithMockRepository(userAnswersWithIQ1Yes, Some(agentApplicationBuilder(userAnswersWithIQ1Yes)))

      running(application) {
        val request = FakeRequest(POST, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          supplierdetails.routes.UsePurchaserDetailsAsSupplierController.onPageLoad(SupplierNumber(1), NormalMode).url
      }
    }

    "must send an agent without a selected client to UsePurchaserDetailsAsSupplierController even if they answered 'As the purchaser' to IQ3" in {

      val answers          = userAnswersWithIQ1Yes.set(NotifyingAsPurchaserPage, PurchaserOrOnBehalf.Purchaser).success.value
      val (application, _) = applicationWithMockRepository(answers, Some(agentApplicationBuilder(answers)))

      running(application) {
        val request = FakeRequest(POST, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          supplierdetails.routes.UsePurchaserDetailsAsSupplierController.onPageLoad(SupplierNumber(1), NormalMode).url
      }
    }

    "must add supplier 1 and send an agent with a selected client to UsePersonalDetailsAsSupplierController" in {

      val sampleClient     = AgentSelectedClient(vrn = "GB123456789", name = Some("Acme Ltd"))
      val answers          = userAnswersWithIQ1Yes.set(AgentSelectedClientPage, sampleClient).success.value
      val (application, _) = applicationWithMockRepository(answers, Some(agentApplicationBuilder(answers)))

      running(application) {
        val request =
          FakeRequest(POST, addVehicleDetailsRoute)
            .withFormUrlEncodedBody(("value", AddVehicleDetails.BySupplier.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          supplierdetails.routes.UsePersonalDetailsAsSupplierController.onPageLoad(SupplierNumber(1), NormalMode).url
      }
    }

    "must add supplier 1 and send a VAT-registered organisation who bought on behalf of the purchaser to UsePersonalDetailsAsSupplierController" in {

      val answers          = userAnswersWithIQ1Yes.set(NotifyingAsPurchaserPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser).success.value
      val (application, _) = applicationWithMockRepository(answers, Some(applicationBuilderWithVatTrader(Some(answers))))

      running(application) {
        val request =
          FakeRequest(POST, addVehicleDetailsRoute)
            .withFormUrlEncodedBody(("value", AddVehicleDetails.BySupplier.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          supplierdetails.routes.UsePersonalDetailsAsSupplierController.onPageLoad(SupplierNumber(1), NormalMode).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswersWithIQ1Yes)).build()

      running(application) {
        val request =
          FakeRequest(POST, addVehicleDetailsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[AddVehicleDetailsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, spreadsheetUrl(application))(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, addVehicleDetailsRoute)
            .withFormUrlEncodedBody(("value", AddVehicleDetails.BySupplier.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if IQ1 has not been answered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if IQ1 was answered No" in {

      val answersIq1No = emptyUserAnswers.set(VehicleFromEuPage, false).success.value
      val application  = applicationBuilder(userAnswers = Some(answersIq1No)).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must show the Unauthorised (ERR1) screen for an Agent who has a draft but answered No to IQ1" in {

      // QA scenario: an agent completes CYA1.0 answering No to IQ1.0, then tries to access AVD1.0.
      val answersIq1No = emptyUserAnswers
        .set(DraftIdPage, DraftId("DRAFT-001"))
        .success
        .value
        .set(VehicleFromEuPage, false)
        .success
        .value
      val application = agentApplicationBuilder(answersIq1No).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if draftId is missing" in {

      val answersWithoutDraftId = emptyUserAnswers.set(VehicleFromEuPage, true).success.value
      val application           = applicationBuilder(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a POST if draftId is missing" in {

      val answersWithoutDraftId = emptyUserAnswers.set(VehicleFromEuPage, true).success.value
      val application           = applicationBuilder(userAnswers = Some(answersWithoutDraftId)).build()

      running(application) {
        val request =
          FakeRequest(POST, addVehicleDetailsRoute)
            .withFormUrlEncodedBody(("value", AddVehicleDetails.BySupplier.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to the unauthorised page for a GET when the user is not allowed to access the service" in {

      val application = applicationFor(classOf[UnauthorisedIdentifierAction], Some(userAnswersWithIQ1Yes))

      running(application) {
        val request = FakeRequest(GET, addVehicleDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to the unauthorised page for a POST when the user is not allowed to access the service" in {

      val application = applicationFor(classOf[UnauthorisedIdentifierAction], Some(userAnswersWithIQ1Yes))

      running(application) {
        val request =
          FakeRequest(POST, addVehicleDetailsRoute)
            .withFormUrlEncodedBody(("value", AddVehicleDetails.BySupplier.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
