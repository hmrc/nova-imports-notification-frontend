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
import models.{AddYourName, AgentSelectedClient, BusinessOrPrivateIndividual, DraftId, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class YourDetailsCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar {

  private lazy val yourDetailsCyaRoute = routes.YourDetailsCheckYourAnswersController.onPageLoad().url

  private val name         = AddYourName("Mr", "John", "Smith")
  private val phone        = "01632 960 001"
  private val email        = "name@example.com"
  private val sampleClient = AgentSelectedClient(vrn = "123456789", name = Some("ABC Ltd"))

  private val vatOrgNoNameAnswers = emptyUserAnswers
    .set(VehicleBusinessUsePage, true)
    .success
    .value
    .set(PhoneNumberPage, phone)
    .success
    .value
    .set(EmailAddressPage, email)
    .success
    .value
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value

  private val vatOrgWithNameAnswers = emptyUserAnswers
    .set(VehicleBusinessUsePage, false)
    .success
    .value
    .set(AddYourNamePage, name)
    .success
    .value
    .set(PhoneNumberPage, phone)
    .success
    .value
    .set(EmailAddressPage, email)
    .success
    .value
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value

  private val individualWithNameAnswers = emptyUserAnswers
    .set(BusinessPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
    .success
    .value
    .set(AddYourNamePage, name)
    .success
    .value
    .set(PhoneNumberPage, phone)
    .success
    .value
    .set(EmailAddressPage, email)
    .success
    .value
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value

  // agent with client, if client vehicle not for business use then name is required
  private val agentWithClientAnswers = emptyUserAnswers
    .set(AgentVehicleBusinessUsePage, false)
    .success
    .value
    .set(AddYourNamePage, name)
    .success
    .value
    .set(PhoneNumberPage, phone)
    .success
    .value
    .set(EmailAddressPage, email)
    .success
    .value
    .set(AgentSelectedClientPage, sampleClient)
    .success
    .value
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value

  // agent with client, if client vehicle is for business use then no name is required
  private val agentNoNameAnswers = emptyUserAnswers
    .set(AgentVehicleBusinessUsePage, true)
    .success
    .value
    .set(PhoneNumberPage, phone)
    .success
    .value
    .set(EmailAddressPage, email)
    .success
    .value
    .set(AgentSelectedClientPage, sampleClient)
    .success
    .value
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value

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

  "YourDetailsCheckYourAnswersController" - {

    "onPageLoad" - {

      "for a VatRegisteredOrganisation with vehicle for business use must return OK with phone and email but no name" in {
        given application: Application = applicationForPageLoad(classOf[FakeVatTraderIdentifierAction], Some(vatOrgNoNameAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCyaRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must include("Phone number")
          body must include("Email address")
          body must include(phone)
          body must include(email)
          body must not include "Smith"
        }
      }

      "for a VatRegisteredOrganisation whose vehicle is not for business use must return OK with the name row" in {
        given application: Application = applicationForPageLoad(classOf[FakeVatTraderIdentifierAction], Some(vatOrgWithNameAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCyaRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must (include("Mr") and include("John") and include("Smith"))
          body must include(phone)
          body must include(email)
        }
      }

      "for a PrivateIndividual must return OK with the name row" in {
        given application: Application = applicationForPageLoad(classOf[FakeIdentifierAction], Some(individualWithNameAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCyaRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must include("Smith")
          body must include(phone)
          body must include(email)
        }
      }

      "for Agent with client vehicle that is not for business use must return OK with the name row" in {
        given application: Application = applicationForPageLoad(classOf[FakeAgentIdentifierAction], Some(agentWithClientAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCyaRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must (include("Mr") and include("John") and include("Smith"))
          body must include(phone)
          body must include(email)
        }
      }

      "for an Agent wit client vehicle that is for business use must return OK with phone and email but no name" in {
        given application: Application = applicationForPageLoad(classOf[FakeAgentIdentifierAction], Some(agentNoNameAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCyaRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must include("Phone number")
          body must include("Email address")
          body must include(phone)
          body must include(email)
          body must not include "Smith"
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a standard user who has not answered any questions" in {
        given application: Application = applicationForPageLoad(classOf[FakeIdentifierAction], Some(emptyUserAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a standard user who has not provided an email" in {
        val answers = emptyUserAnswers
          .set(BusinessPrivatePage, BusinessOrPrivateIndividual.Business)
          .success
          .value
          .set(PhoneNumberPage, phone)
          .success
          .value
          .set(DraftIdPage, DraftId("DRAFT-001"))
          .success
          .value

        given application: Application = applicationForPageLoad(classOf[FakeIdentifierAction], Some(answers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a VatRegisteredOrganisation that should have a name but does not have one" in {
        val answers = emptyUserAnswers
          .set(VehicleBusinessUsePage, false)
          .success
          .value
          .set(PhoneNumberPage, phone)
          .success
          .value
          .set(EmailAddressPage, email)
          .success
          .value
          .set(DraftIdPage, DraftId("DRAFT-001"))
          .success
          .value

        given application: Application = applicationForPageLoad(classOf[FakeVatTraderIdentifierAction], Some(answers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "when succeeds must redirect to NTL for a VatRegisteredOrganisation" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(())))

        given application: Application = applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(vatOrgWithNameAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url
        }
      }

      "when succeeds must redirect to NTL for a PrivateIndividual" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(())))

        given application: Application = applicationForSubmit(classOf[FakeIdentifierAction], Some(individualWithNameAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url
        }
      }

      "when succeeds must redirect to NTL for an Agent with a selected client" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(())))

        given application: Application = applicationForSubmit(classOf[FakeAgentIdentifierAction], Some(agentWithClientAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url
        }
      }

      "when the backend call returns an UpstreamError must redirect to Journey Recovery" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(500, "error"))))

        given application: Application = applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(vatOrgWithNameAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when the backend call returns Forbidden must redirect to Journey Recovery" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.Forbidden)))

        given application: Application = applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(vatOrgWithNameAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when the backend call returns NotFound must redirect to Journey Recovery" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.NotFound)))

        given application: Application = applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(vatOrgWithNameAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery if DraftId is missing" in {
        val answersWithoutDraftId = emptyUserAnswers
          .set(VehicleBusinessUsePage, true)
          .success
          .value
          .set(PhoneNumberPage, phone)
          .success
          .value
          .set(EmailAddressPage, email)
          .success
          .value

        given application: Application =
          applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(answersWithoutDraftId), mock[NovaImportsBackendConnector])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCyaRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
