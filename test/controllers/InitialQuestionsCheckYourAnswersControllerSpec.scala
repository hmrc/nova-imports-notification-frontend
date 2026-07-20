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
import connectors.{NovaImportsBackendConnector, UpdateSectionError}
import controllers.actions.*
import models.responses.CreateDraftResponse
import models.{AgentSelectedClient, BusinessOrPrivateIndividual, DraftId, PurchaserBusinessOrIndividual, PurchaserOrOnBehalf, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import pages.sections.initialquestions.{BusinessOrPrivatePage, PurchaserBusinessOrIndividualPage, PurchaserOrOnBehalfPage, VehicleBusinessUsePage, VehicleFromEuPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class InitialQuestionsCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar {

  private lazy val initialQuestionsCyaRoute =
    routes.InitialQuestionsCheckYourAnswersController.onPageLoad().url

  private val draftId = "DRAFT-001"

  private val orgUserAnswers = emptyUserAnswers
    .set(VehicleFromEuPage, true)
    .success
    .value
    .set(VehicleBusinessUsePage, true)
    .success
    .value
    .set(DraftIdPage, DraftId(draftId))
    .success
    .value

  private val individualUserAnswers = emptyUserAnswers
    .set(VehicleFromEuPage, true)
    .success
    .value
    .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
    .success
    .value
    .set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser)
    .success
    .value
    .set(DraftIdPage, DraftId(draftId))
    .success
    .value

  private val individualUserOnBehalfAnswers = emptyUserAnswers
    .set(VehicleFromEuPage, true)
    .success
    .value
    .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
    .success
    .value
    .set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
    .success
    .value
    .set(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredBusiness)
    .success
    .value
    .set(DraftIdPage, DraftId(draftId))
    .success
    .value

  private val sampleClient = AgentSelectedClient(vrn = "123456789", name = Some("ABC Ltd"))

  private val agentWithClientAnswers = emptyUserAnswers
    .set(VehicleFromEuPage, true)
    .success
    .value
    .set(AgentClientVehicleBusinessUsePage, true)
    .success
    .value
    .set(AgentSelectedClientPage, sampleClient)
    .success
    .value
    .set(DraftIdPage, DraftId(draftId))
    .success
    .value

  private val individualUserAnswersEuPageFalse = emptyUserAnswers
    .set(VehicleFromEuPage, false)
    .success
    .value
    .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
    .success
    .value
    .set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser)
    .success
    .value
    .set(DraftIdPage, DraftId(draftId))
    .success
    .value

  private def connectorWithSuccessfulSubmit(): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]

    when(connector.createDraft(any[Option[String]])(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(CreateDraftResponse(draftId, 1L))))

    when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
      .thenReturn(
        Future.successful(Right(2L)),
        Future.successful(Right(3L))
      )

    connector
  }

  private def connectorWithUpdateFailure(error: UpdateSectionError): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]

    when(connector.createDraft(any[Option[String]])(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(CreateDraftResponse(draftId, 1L))))

    when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Left(error)))

    connector
  }

  private def applicationForPageLoad(
    identifierAction: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers]
  ): Application =
    applicationForSubmit(identifierAction, userAnswers, mock[NovaImportsBackendConnector])

  private def applicationForSubmit(
    identifierAction: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers],
    connector: NovaImportsBackendConnector
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[NovaImportsBackendConnector].toInstance(connector)
      )
      .build()

  "InitialQuestionsCheckYourAnswersController" - {

    "onPageLoad" - {

      "for a VatRegisteredOrganisation must return OK and render the correct rows" in {
        given application: Application =
          applicationForPageLoad(classOf[FakeVatTraderIdentifierAction], Some(orgUserAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, initialQuestionsCyaRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must include("whether this notification is for a vehicle brought into Northern Ireland from an EU country")
          body must include("Have you brought a vehicle into the UK for business use?")
        }
      }

      "for a PrivateIndividual notifying as the purchaser must return OK and render three rows" in {
        given application: Application =
          applicationForPageLoad(classOf[FakeIdentifierAction], Some(individualUserAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, initialQuestionsCyaRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must include("whether this notification is for a vehicle brought into Northern Ireland from an EU country")
          body must include("Are you a business or private individual?")
          body must include("Are you notifying as the purchaser, or on behalf of a purchaser?")
          body must not include "Type of purchaser"
        }
      }

      "for a PrivateIndividual notifying on behalf of a purchaser must return OK and render all four rows" in {
        given application: Application =
          applicationForPageLoad(classOf[FakeIdentifierAction], Some(individualUserOnBehalfAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, initialQuestionsCyaRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must include("whether this notification is for a vehicle brought into Northern Ireland from an EU country")
          body must include("Are you a business or private individual?")
          body must include("Are you notifying as the purchaser, or on behalf of a purchaser?")
          body must include("Is the purchaser you’re notifying on behalf of a business or private individual?")
        }
      }

      "for an Agent with a selected client must return OK and render the correct rows" in {
        given application: Application =
          applicationForPageLoad(classOf[FakeAgentIdentifierAction], Some(agentWithClientAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, initialQuestionsCyaRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must include("whether this notification is for a vehicle brought into Northern Ireland from an EU country")
          body must include("whether your client has brought a vehicle into the UK for business use")
        }
      }

      "must redirect to Unauthorised if no existing data is found" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a standard user who has not answered any questions" in {
        given application: Application =
          applicationForPageLoad(classOf[FakeIdentifierAction], Some(emptyUserAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a standard user who has only answered IQ1.0" in {
        val answersWithIq1Only = emptyUserAnswers
          .set(VehicleFromEuPage, true)
          .success
          .value

        given application: Application =
          applicationForPageLoad(classOf[FakeIdentifierAction], Some(answersWithIq1Only))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a standard user who answered OnBehalfOfPurchaser for IQ3.0 without answering IQ3.1" in {
        val answersWithMissingIq3_1 = emptyUserAnswers
          .set(VehicleFromEuPage, true)
          .success
          .value
          .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
          .success
          .value
          .set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser)
          .success
          .value

        given application: Application =
          applicationForPageLoad(classOf[FakeIdentifierAction], Some(answersWithMissingIq3_1))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a VatRegisteredOrganisation who has not answered OQ1.0" in {
        val answersWithMissingOq1 = emptyUserAnswers
          .set(VehicleFromEuPage, true)
          .success
          .value

        given application: Application =
          applicationForPageLoad(classOf[FakeVatTraderIdentifierAction], Some(answersWithMissingOq1))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for an Agent with a selected client who has not answered AQ1.0" in {
        val answersWithMissingAq1 = emptyUserAnswers
          .set(VehicleFromEuPage, true)
          .success
          .value
          .set(AgentSelectedClientPage, sampleClient)
          .success
          .value

        given application: Application =
          applicationForPageLoad(classOf[FakeAgentIdentifierAction], Some(answersWithMissingAq1))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a standard user when IQ1.0 was answered No" in {
        given application: Application =
          applicationForPageLoad(classOf[FakeIdentifierAction], Some(individualUserAnswersEuPageFalse))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "when succeeds must redirect to NTL3.0 for a VatRegisteredOrganisation" in {
        val connector = connectorWithSuccessfulSubmit()

        given application: Application =
          applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(orgUserAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(POST, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url
        }
      }

      "when succeeds must redirect to the next page for an Agent with a selected client" in {
        val connector = connectorWithSuccessfulSubmit()

        given application: Application =
          applicationForSubmit(classOf[FakeAgentIdentifierAction], Some(agentWithClientAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(POST, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url
        }
      }

      "when succeeds must redirect to the notification task list for a PrivateIndividual" in {
        val connector = connectorWithSuccessfulSubmit()

        given application: Application =
          applicationForSubmit(classOf[FakeIdentifierAction], Some(individualUserAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(POST, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url
        }
      }

      "when createDraft returns an error must redirect to Journey Recovery" in {
        val connector = mock[NovaImportsBackendConnector]

        when(connector.createDraft(any[Option[String]])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(connectors.CreateDraftError.UpstreamError(500, "error"))))

        given application: Application =
          applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(orgUserAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(POST, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when the backend call returns an UpstreamError must redirect to Journey Recovery" in {
        val connector =
          connectorWithUpdateFailure(UpdateSectionError.UpstreamError(500, "error"))

        given application: Application =
          applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(orgUserAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(POST, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when the backend call returns Forbidden must redirect to Journey Recovery" in {
        val connector =
          connectorWithUpdateFailure(UpdateSectionError.Forbidden)

        given application: Application =
          applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(orgUserAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(POST, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when the backend call returns NotFound must redirect to Journey Recovery" in {
        val connector =
          connectorWithUpdateFailure(UpdateSectionError.NotFound)

        given application: Application =
          applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(orgUserAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(POST, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery if section data cannot be built" in {
        val connector = connectorWithSuccessfulSubmit()

        val answersWithoutVehicleFromEu = emptyUserAnswers
          .set(VehicleBusinessUsePage, true)
          .success
          .value

        given application: Application =
          applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(answersWithoutVehicleFromEu), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(POST, initialQuestionsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }
  }
}
