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
import models.{AgentSelectedClient, BusinessOrPrivateIndividual, ContactNumbers, DraftId, NameDetails, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import pages.sections.notifierDetails.{BusinessNamePage, EmailAddressPage, NameDetailsPage, PhoneNumberPage}
import pages.sections.initialquestions.{BusinessOrPrivatePage, VehicleBusinessUsePage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsObject
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

import YourDetailsCheckYourAnswersControllerSpec.*

class YourDetailsCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar {

  private lazy val yourDetailsCheckYourAnswersRoute = routes.YourDetailsCheckYourAnswersController.onPageLoad().url

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
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must include("Contact numbers")
          body must include("Email address")
          body must include(phone)
          body must include(email)
          body must not include "Smith"
        }
      }

      "for a VatRegisteredOrganisation whose vehicle is not for business use must return OK with the name row" in {
        given application: Application = applicationForPageLoad(classOf[FakeVatTraderIdentifierAction], Some(vatOrgWithNameAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

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
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must include("Smith")
          body must include(phone)
          body must include(email)
        }
      }

      "for a business user must return OK with the business name row included" in {
        given application: Application = applicationForPageLoad(classOf[FakeIdentifierAction], Some(businessNameAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must include(businessName)
          body must include(phone)
          body must include(email)
          body must not include "Smith"
        }
      }

      "for Agent with client vehicle that is not for business use must return OK with the name row" in {
        given application: Application = applicationForPageLoad(classOf[FakeAgentIdentifierAction], Some(agentWithClientAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must (include("Mr") and include("John") and include("Smith"))
          body must include(phone)
          body must include(email)
        }
      }

      "for an Agent with client vehicle that is for business use must return OK with phone and email but no name" in {
        given application: Application = applicationForPageLoad(classOf[FakeAgentIdentifierAction], Some(agentNoNameAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must include("Contact numbers")
          body must include("Email address")
          body must include(phone)
          body must include(email)
          body must not include "Smith"
        }
      }

      "for an Agent without a selected client who has provided phone and email but no name must return OK" in {
        given application: Application = applicationForPageLoad(classOf[FakeAgentIdentifierAction], Some(agentWithoutClientAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must include("Contact numbers")
          body must include("Email address")
          body must include(phone)
          body must include(email)
          body must not include "Smith"
        }
      }

      "for an Agent with no enrolments must return OK with phone and email entered but no name entered" in {
        given application: Application =
          applicationForPageLoad(classOf[FakeAgentNoEnrolmentsIdentifierAction], Some(agentNoEnrolmentsAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must include("Contact numbers")
          body must include("Email address")
          body must include(phone)
          body must include(email)
          body must not include "Smith"
        }
      }

      "for an Agent with no enrolments who has not provided an email must redirect to Unauthorised" in {
        given application: Application =
          applicationForPageLoad(classOf[FakeAgentNoEnrolmentsIdentifierAction], Some(agentNoEnrolmentsNoEmailAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised if no existing data is found" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a standard user who has not answered any questions" in {
        given application: Application = applicationForPageLoad(classOf[FakeIdentifierAction], Some(emptyUserAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a standard user who has not provided an email" in {
        val answers = emptyUserAnswers
          .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
          .success
          .value
          .set(PhoneNumberPage, contactNumbers)
          .success
          .value
          .set(DraftIdPage, DraftId("DRAFT-001"))
          .success
          .value

        given application: Application = applicationForPageLoad(classOf[FakeIdentifierAction], Some(answers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a business user who has not provided a business name" in {
        val answers = emptyUserAnswers
          .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
          .success
          .value
          .set(PhoneNumberPage, contactNumbers)
          .success
          .value
          .set(EmailAddressPage, email)
          .success
          .value
          .set(DraftIdPage, DraftId("DRAFT-001"))
          .success
          .value

        given application: Application = applicationForPageLoad(classOf[FakeIdentifierAction], Some(answers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a business user who has answered both personal name and business name entries" in {
        val answers = emptyUserAnswers
          .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
          .success
          .value
          .set(NameDetailsPage, name)
          .success
          .value
          .set(BusinessNamePage, businessName)
          .success
          .value
          .set(PhoneNumberPage, contactNumbers)
          .success
          .value
          .set(EmailAddressPage, email)
          .success
          .value
          .set(DraftIdPage, DraftId("DRAFT-001"))
          .success
          .value

        given application: Application = applicationForPageLoad(classOf[FakeIdentifierAction], Some(answers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a private individual who has not provided a name" in {
        val answers = emptyUserAnswers
          .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
          .success
          .value
          .set(PhoneNumberPage, contactNumbers)
          .success
          .value
          .set(EmailAddressPage, email)
          .success
          .value
          .set(DraftIdPage, DraftId("DRAFT-001"))
          .success
          .value

        given application: Application = applicationForPageLoad(classOf[FakeIdentifierAction], Some(answers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a private individual who has also provided a business name entry" in {
        val answers = emptyUserAnswers
          .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
          .success
          .value
          .set(NameDetailsPage, name)
          .success
          .value
          .set(BusinessNamePage, businessName)
          .success
          .value
          .set(PhoneNumberPage, contactNumbers)
          .success
          .value
          .set(EmailAddressPage, email)
          .success
          .value
          .set(DraftIdPage, DraftId("DRAFT-001"))
          .success
          .value

        given application: Application = applicationForPageLoad(classOf[FakeIdentifierAction], Some(answers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

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
          .set(PhoneNumberPage, contactNumbers)
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
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a VAT reg org that has set a name but should not (vehicle for business use = true)" in {
        val answers = emptyUserAnswers
          .set(VehicleBusinessUsePage, true)
          .success
          .value
          .set(NameDetailsPage, name)
          .success
          .value
          .set(PhoneNumberPage, contactNumbers)
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
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, yourDetailsCheckYourAnswersRoute)

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
          .thenReturn(Future.successful(Right(2L)))

        given application: Application = applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(vatOrgWithNameAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url
        }
      }

      // TODO: change correct downstream redirect once NTL set up for all user types
      "when succeeds must redirect to the notification task list for a PrivateIndividual" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(2L)))

        given application: Application = applicationForSubmit(classOf[FakeIdentifierAction], Some(individualWithNameAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url
        }
      }

      // TODO: change correct downstream redirect once NTL set up for all user types
      "when succeeds must redirect to the correct downstream Page for an Agent with a selected client" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(2L)))

        given application: Application = applicationForSubmit(classOf[FakeAgentIdentifierAction], Some(agentWithClientAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url
        }
      }

      // TODO: change correct downstream redirect once NTL set up for all user types
      "when succeeds must redirect to the correct downstream Page for an Agent with no enrolments" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(2L)))

        given application: Application =
          applicationForSubmit(classOf[FakeAgentNoEnrolmentsIdentifierAction], Some(agentNoEnrolmentsAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url
        }
      }

      "when the backend call returns an UpstreamError must redirect to Journey Recovery" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(500, "error"))))

        given application: Application = applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(vatOrgWithNameAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCheckYourAnswersRoute)

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
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCheckYourAnswersRoute)

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
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised if DraftId is missing" in {
        val answersWithoutDraftId = emptyUserAnswers
          .set(VehicleBusinessUsePage, true)
          .success
          .value
          .set(PhoneNumberPage, contactNumbers)
          .success
          .value
          .set(EmailAddressPage, email)
          .success
          .value

        given application: Application =
          applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(answersWithoutDraftId), mock[NovaImportsBackendConnector])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must send the correct individual notifier-details body with the current versionId" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(2L)))

        given application: Application = applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(vatOrgWithNameAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER

          val bodyCaptor = ArgumentCaptor.forClass(classOf[JsObject])
          verify(connector).updateDraftSection(any[DraftId], eqTo("notifier-details"), bodyCaptor.capture())(any[HeaderCarrier])
          val sentBody = bodyCaptor.getValue

          (sentBody \ "versionId").as[Long] mustEqual 1L
          (sentBody \ "title").as[String] mustEqual "Mr"
          (sentBody \ "firstName").as[String] mustEqual "John"
          (sentBody \ "lastName").as[String] mustEqual "Smith"
          (sentBody \ "phoneNumber").as[String] mustEqual phone
          (sentBody \ "emailAddress").as[String] mustEqual email
        }
      }

      "must send an organisation notifier-details body (no name fields) when no name was provided" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(2L)))

        given application: Application = applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(vatOrgNoNameAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER

          val bodyCaptor = ArgumentCaptor.forClass(classOf[JsObject])
          verify(connector).updateDraftSection(any[DraftId], eqTo("notifier-details"), bodyCaptor.capture())(any[HeaderCarrier])
          val sentBody = bodyCaptor.getValue

          (sentBody \ "phoneNumber").as[String] mustEqual phone
          (sentBody \ "emailAddress").as[String] mustEqual email
          (sentBody \ "title").toOption mustBe None
          (sentBody \ "firstName").toOption mustBe None
          (sentBody \ "lastName").toOption mustBe None
        }
      }

      "must send an organisation notifier-details body with the business name when a business name was provided" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(2L)))

        given application: Application = applicationForSubmit(classOf[FakeIdentifierAction], Some(businessNameAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER

          val bodyCaptor = ArgumentCaptor.forClass(classOf[JsObject])
          verify(connector).updateDraftSection(any[DraftId], eqTo("notifier-details"), bodyCaptor.capture())(any[HeaderCarrier])
          val sentBody = bodyCaptor.getValue

          (sentBody \ "businessName").as[String] mustEqual businessName
          (sentBody \ "phoneNumber").as[String] mustEqual phone
          (sentBody \ "emailAddress").as[String] mustEqual email
          (sentBody \ "title").toOption mustBe None
          (sentBody \ "firstName").toOption mustBe None
          (sentBody \ "lastName").toOption mustBe None
        }
      }

      "must redirect to Journey Recovery if the versionId is missing" in {
        val answersWithoutVersionId = emptyUserAnswers
          .set(VehicleBusinessUsePage, false)
          .success
          .value
          .set(NameDetailsPage, name)
          .success
          .value
          .set(PhoneNumberPage, contactNumbers)
          .success
          .value
          .set(EmailAddressPage, email)
          .success
          .value
          .set(DraftIdPage, DraftId("DRAFT-001"))
          .success
          .value

        given application: Application =
          applicationForSubmit(classOf[FakeVatTraderIdentifierAction], Some(answersWithoutVersionId), mock[NovaImportsBackendConnector])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, yourDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}

object YourDetailsCheckYourAnswersControllerSpec {

  import org.scalatest.TryValues.*

  private val name           = NameDetails("Mr", "John", "Smith")
  private val phone          = "01632 960 001"
  private val contactNumbers = ContactNumbers(Some(phone), None)
  private val email          = "name@example.com"
  private val businessName   = "ABC Ltd"
  private val sampleClient   = AgentSelectedClient(vrn = "123456789", name = Some("ABC Ltd"))

  private val emptyUserAnswers = UserAnswers("id")

  private val vatOrgNoNameAnswers = emptyUserAnswers
    .set(VehicleBusinessUsePage, true)
    .success
    .value
    .set(PhoneNumberPage, contactNumbers)
    .success
    .value
    .set(EmailAddressPage, email)
    .success
    .value
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(DraftVersionIdPage, 1L)
    .success
    .value

  private val vatOrgWithNameAnswers = emptyUserAnswers
    .set(VehicleBusinessUsePage, false)
    .success
    .value
    .set(NameDetailsPage, name)
    .success
    .value
    .set(PhoneNumberPage, contactNumbers)
    .success
    .value
    .set(EmailAddressPage, email)
    .success
    .value
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(DraftVersionIdPage, 1L)
    .success
    .value

  private val individualWithNameAnswers = emptyUserAnswers
    .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
    .success
    .value
    .set(NameDetailsPage, name)
    .success
    .value
    .set(PhoneNumberPage, contactNumbers)
    .success
    .value
    .set(EmailAddressPage, email)
    .success
    .value
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(DraftVersionIdPage, 1L)
    .success
    .value

  private val businessNameAnswers = emptyUserAnswers
    .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
    .success
    .value
    .set(BusinessNamePage, businessName)
    .success
    .value
    .set(PhoneNumberPage, contactNumbers)
    .success
    .value
    .set(EmailAddressPage, email)
    .success
    .value
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(DraftVersionIdPage, 1L)
    .success
    .value

  // agent with client, if client vehicle not for business use then name is required
  private val agentWithClientAnswers = emptyUserAnswers
    .set(AgentClientVehicleBusinessUsePage, false)
    .success
    .value
    .set(NameDetailsPage, name)
    .success
    .value
    .set(PhoneNumberPage, contactNumbers)
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
    .set(DraftVersionIdPage, 1L)
    .success
    .value

  // agent with client, if client vehicle is for business use then no name is required
  private val agentNoNameAnswers = emptyUserAnswers
    .set(AgentClientVehicleBusinessUsePage, true)
    .success
    .value
    .set(PhoneNumberPage, contactNumbers)
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
    .set(DraftVersionIdPage, 1L)
    .success
    .value

  private val agentNoEnrolmentsAnswers = emptyUserAnswers
    .set(AgentClientVehicleBusinessUsePage, true)
    .success
    .value
    .set(PhoneNumberPage, contactNumbers)
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
    .set(DraftVersionIdPage, 1L)
    .success
    .value

  private val agentWithoutClientAnswers = emptyUserAnswers
    .set(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
    .success
    .value
    .set(PhoneNumberPage, contactNumbers)
    .success
    .value
    .set(EmailAddressPage, email)
    .success
    .value
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(DraftVersionIdPage, 1L)
    .success
    .value

  private val agentNoEnrolmentsNoEmailAnswers = emptyUserAnswers
    .set(AgentClientVehicleBusinessUsePage, true)
    .success
    .value
    .set(PhoneNumberPage, contactNumbers)
    .success
    .value
    .set(AgentSelectedClientPage, sampleClient)
    .success
    .value
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(DraftVersionIdPage, 1L)
    .success
    .value
}
