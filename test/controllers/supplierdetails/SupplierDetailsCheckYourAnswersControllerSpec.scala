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
import connectors.{NovaImportsBackendConnector, UpdateSectionError}
import controllers.supplierdetails.SupplierDetailsCheckYourAnswersControllerSpec.*
import controllers.{routes, supplierdetails}
import models.{Address, BusinessOrPrivateIndividual, Country, DraftId, NameDetails, NormalMode, SupplierNumber, UserAnswers, VatNumberDetails}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.notifieraddress.AddressPage
import pages.sections.notifierdetails.NameDetailsPage
import pages.sections.purchaseraddress.PurchaserAddressPage
import pages.sections.purchaserdetails.PurchaserNamePage
import pages.sections.supplieraddress.{IsSupplierAddressInTheUkPage, SupplierAddressJourneyIdPage, SupplierAddressPage}
import pages.sections.supplierdetails.*
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json, Writes}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.AllSuppliersQuery
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class SupplierDetailsCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar {

  private def supplierDetailsCheckYourAnswersRoutePageLoad(supplierNumber: SupplierNumber = SupplierNumber(1)) =
    supplierdetails.routes.SupplierDetailsCheckYourAnswersController.onPageLoad(supplierNumber).url

  private def supplierDetailsCheckYourAnswersRouteSubmit(supplierNumber: SupplierNumber = SupplierNumber(1)) =
    supplierdetails.routes.SupplierDetailsCheckYourAnswersController.onSubmit(supplierNumber).url

  private def supplierDetailsCheckYourAnswersRouteOnChangeAddress(supplierNumber: SupplierNumber = SupplierNumber(1)) =
    supplierdetails.routes.SupplierDetailsCheckYourAnswersController.onChangeAddress(supplierNumber).url

  private def applicationForPageLoad(
    userAnswers: Option[UserAnswers]
  ): Application =
    applicationForSubmit(userAnswers, mock[NovaImportsBackendConnector])

  private def stubSessionRepository(userAnswers: UserAnswers = individualVatRegisteredSupplierDetailsAnswers): SessionRepository = {
    val m = mock[SessionRepository]
    when(m.set(any())).thenReturn(Future.successful(true))
    when(m.setPage(any(), any(), any())(any())).thenReturn(Future.successful(userAnswers))
    m
  }

  private def applicationForSubmit(
    userAnswers: Option[UserAnswers],
    connector: NovaImportsBackendConnector,
    sessionRepository: SessionRepository = stubSessionRepository()
  ): Application =
    applicationBuilder(userAnswers)
      .overrides(
        bind[SessionRepository].toInstance(sessionRepository),
        bind[NovaImportsBackendConnector].toInstance(connector)
      )
      .build()

  "SupplierDetailsCheckYourAnswersController" - {

    "onPageLoad" - {

      "for a Self Supplying user using Personal details must return OK with name and address" in {
        given application: Application = applicationForPageLoad(Some(selfSupplyPersonalDetailsAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, supplierDetailsCheckYourAnswersRoutePageLoad())

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check the supplier details before adding vehicles")
          body must include("Supplier’s name")
          body must include("Supplier’s address")

          body must not include "Not provided"
          body must not include "Supplier’s business name"
          body must not include "Is the supplier a business or private individual?"
          body must not include "Is the supplier VAT registered?"
          body must not include "Supplier’s VAT registration details"
        }
      }

      "for a Self Supplying user using Personal details that have not been provided must return OK Not Provided" in {
        given application: Application = applicationForPageLoad(Some(selfSupplyPersonalDetailsEmptyAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, supplierDetailsCheckYourAnswersRoutePageLoad())

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check the supplier details before adding vehicles")
          body must include("Supplier’s name")
          body must include("Supplier’s address")
          body must include("Not provided")

          body must not include "Supplier’s business name"
          body must not include "Is the supplier a business or private individual?"
          body must not include "Is the supplier VAT registered?"
          body must not include "Supplier’s VAT registration details"
        }
      }

      "for a Self Supplying user using Purchaser details must return OK with name and address" in {
        given application: Application = applicationForPageLoad(Some(selfSupplyPurchaserDetailsAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, supplierDetailsCheckYourAnswersRoutePageLoad())

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check the supplier details before adding vehicles")
          body must include("Supplier’s name")
          body must include("Supplier’s address")

          body must not include "Not provided"
          body must not include "Supplier’s business name"
          body must not include "Is the supplier a business or private individual?"
          body must not include "Is the supplier VAT registered?"
          body must not include "Supplier’s VAT registration details"
        }
      }

      "for a Self Supplying user using Purchaser details that have not been provided must return OK Not Provided" in {
        given application: Application = applicationForPageLoad(Some(selfSupplyPurchaserDetailsEmptyAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, supplierDetailsCheckYourAnswersRoutePageLoad())

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check the supplier details before adding vehicles")
          body must include("Supplier’s name")
          body must include("Supplier’s address")
          body must include("Not provided")

          body must not include "Supplier’s business name"
          body must not include "Is the supplier a business or private individual?"
          body must not include "Is the supplier VAT registered?"
          body must not include "Supplier’s VAT registration details"
        }
      }

      "for a vat registered individual must return OK with correct rows" in {
        given application: Application = applicationForPageLoad(Some(individualVatRegisteredSupplierDetailsAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, supplierDetailsCheckYourAnswersRoutePageLoad())

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check the supplier details before adding vehicles")
          body must include("Is the supplier a business or private individual?")
          body must include("Supplier’s name")
          body must include("Supplier’s address")
          body must include("Is the supplier VAT registered?")
          body must include("Supplier’s VAT registration details")

          body must not include "Not provided"
          body must not include "Supplier’s business name"
        }
      }

      "for a non-vat registered individual must return OK with correct rows" in {
        given application: Application = applicationForPageLoad(Some(individualNonVatRegisteredSupplierDetailsAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, supplierDetailsCheckYourAnswersRoutePageLoad())

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check the supplier details before adding vehicles")
          body must include("Is the supplier a business or private individual?")
          body must include("Supplier’s name")
          body must include("Supplier’s address")
          body must include("Is the supplier VAT registered?")

          body must not include "Supplier’s VAT registration details"
          body must not include "Not provided"
          body must not include "Supplier’s business name"
        }
      }

      "for a vat registered business must return OK with correct rows" in {
        given application: Application = applicationForPageLoad(Some(businessVatRegisteredSupplierDetailsAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, supplierDetailsCheckYourAnswersRoutePageLoad())

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check the supplier details before adding vehicles")
          body must include("Is the supplier a business or private individual?")
          body must include("Supplier’s business name")
          body must include("Supplier’s address")
          body must include("Is the supplier VAT registered?")
          body must include("Supplier’s VAT registration details")

          body must not include "Not provided"
          body must not include "Supplier’s name"
        }
      }

      "for a non-vat registered business must return OK with correct rows" in {
        given application: Application = applicationForPageLoad(Some(businessNonVatRegisteredSupplierDetailsAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, supplierDetailsCheckYourAnswersRoutePageLoad())

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check the supplier details before adding vehicles")
          body must include("Is the supplier a business or private individual?")
          body must include("Supplier’s business name")
          body must include("Supplier’s address")
          body must include("Is the supplier VAT registered?")

          body must not include "Supplier’s VAT registration details"
          body must not include "Not provided"
          body must not include "Supplier’s name"
        }
      }

      "for a User with no answers for self supply must redirect to Unauthorised" in {
        given application: Application = applicationForPageLoad(Some(baseUserAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, supplierDetailsCheckYourAnswersRoutePageLoad())

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "for a User with no DraftId must redirect to Unauthorised" in {
        val ua = UserAnswers("id")
          .set(VehicleFromEuPage, true)
          .success
          .value
          .set(AllSuppliersQuery, Map("1" -> Json.obj()))
          .success
          .value
        given application: Application = applicationForPageLoad(Some(ua))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, supplierDetailsCheckYourAnswersRoutePageLoad())

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "for a User with no answer for IQ1 must redirect to Unauthorised" in {
        val ua = UserAnswers("id")
          .set(DraftVersionIdPage, 1L)
          .success
          .value
          .set(DraftIdPage, DraftId("DRAFT-001"))
          .success
          .value
          .set(AllSuppliersQuery, Map("1" -> Json.obj()))
          .success
          .value
        given application: Application = applicationForPageLoad(Some(ua))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, supplierDetailsCheckYourAnswersRoutePageLoad())

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "for a User with an invalid supplier number must redirect to Unauthorised" in {
        val ua = UserAnswers("id")
          .set(DraftVersionIdPage, 1L)
          .success
          .value
          .set(DraftIdPage, DraftId("DRAFT-001"))
          .success
          .value
          .set(VehicleFromEuPage, true)
          .success
          .value
        given application: Application = applicationForPageLoad(Some(ua))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, supplierDetailsCheckYourAnswersRoutePageLoad())

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

    }

    "onSubmit" - {

      // TODO: change correct downstream redirect once AVD2.0 is built
      "when succeeds must redirect to AVD2.0 for a self supplying user" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(2L)))

        given application: Application = applicationForSubmit(Some(selfSupplyPersonalDetailsAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, supplierDetailsCheckYourAnswersRouteSubmit())

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      // TODO: change correct downstream redirect once AVD2.0 is built
      "when succeeds must redirect to AVD2.0 for a supplier details" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(2L)))

        given application: Application = applicationForSubmit(Some(individualVatRegisteredSupplierDetailsAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, supplierDetailsCheckYourAnswersRouteSubmit())

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when the backend call returns an UpstreamError must redirect to Journey Recovery" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(500, "error"))))

        given application: Application = applicationForSubmit(Some(selfSupplyPersonalDetailsAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, supplierDetailsCheckYourAnswersRouteSubmit())

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when the backend call returns Forbidden must redirect to Journey Recovery" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.Forbidden)))

        given application: Application = applicationForSubmit(Some(selfSupplyPersonalDetailsAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, supplierDetailsCheckYourAnswersRouteSubmit())

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when the backend call returns NotFound must redirect to Journey Recovery" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.NotFound)))

        given application: Application = applicationForSubmit(Some(selfSupplyPersonalDetailsAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, supplierDetailsCheckYourAnswersRouteSubmit())

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised if DraftId is missing" in {
        val answersWithoutDraftId = emptyUserAnswers
          .set(VehicleFromEuPage, true)
          .success
          .value
          .set(AllSuppliersQuery, Map("1" -> Json.obj()))
          .success
          .value

        given application: Application = applicationForSubmit(Some(answersWithoutDraftId), mock[NovaImportsBackendConnector])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, supplierDetailsCheckYourAnswersRouteSubmit())

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised if IQ1 is missing" in {
        val answersWithoutDraftId = emptyUserAnswers
          .set(DraftVersionIdPage, 1L)
          .success
          .value
          .set(DraftIdPage, DraftId("DRAFT-001"))
          .success
          .value
          .set(AllSuppliersQuery, Map("1" -> Json.obj()))
          .success
          .value

        given application: Application = applicationForSubmit(Some(answersWithoutDraftId), mock[NovaImportsBackendConnector])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, supplierDetailsCheckYourAnswersRouteSubmit())

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised if supplier number is missing" in {
        val answersWithoutDraftId = emptyUserAnswers
          .set(DraftVersionIdPage, 1L)
          .success
          .value
          .set(DraftIdPage, DraftId("DRAFT-001"))
          .success
          .value
          .set(VehicleFromEuPage, true)
          .success
          .value

        given application: Application = applicationForSubmit(Some(answersWithoutDraftId), mock[NovaImportsBackendConnector])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, supplierDetailsCheckYourAnswersRouteSubmit())

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must send the correct self-supply body with the current versionId" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(2L)))

        given application: Application = applicationForSubmit(Some(selfSupplyPersonalDetailsAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, supplierDetailsCheckYourAnswersRouteSubmit())

          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER

          val bodyCaptor = ArgumentCaptor.forClass(classOf[JsObject])
          verify(connector)
            .updateDraftSection(any[DraftId], eqTo(s"supplier/${supplierNumber.value}/self-supply"), bodyCaptor.capture())(any[HeaderCarrier])
          val sentBody = bodyCaptor.getValue

          (sentBody \ "versionId").as[Long] mustEqual 1L
          (sentBody \ "areYouSelfSupplying").as[Boolean] mustEqual true
        }
      }

      "must send the correct self-supply and details body with the current versionId when providing supplier details" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(2L)))

        given application: Application = applicationForSubmit(Some(individualVatRegisteredSupplierDetailsAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, supplierDetailsCheckYourAnswersRouteSubmit())

          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER

          // Verify the first specific call happened at least once
          val bodyCaptor = ArgumentCaptor.forClass(classOf[JsObject])
          verify(connector, atLeastOnce())
            .updateDraftSection(any[DraftId], eqTo(s"supplier/${supplierNumber.value}/self-supply"), bodyCaptor.capture())(any[HeaderCarrier])
          val sentBody = bodyCaptor.getValue
          (sentBody \ "versionId").as[Long] mustEqual 1L
          (sentBody \ "areYouSelfSupplying").as[Boolean] mustEqual false

          // Verify the second specific call happened at least once
          val bodyCaptorDetails = ArgumentCaptor.forClass(classOf[JsObject])
          verify(connector, atLeastOnce())
            .updateDraftSection(any[DraftId], eqTo(s"supplier/${supplierNumber.value}/details"), bodyCaptorDetails.capture())(any[HeaderCarrier])
          val sentBodyDetails = bodyCaptorDetails.getValue
          (sentBodyDetails \ "versionId").as[Long] mustEqual 2L
          (sentBodyDetails \ "supplierBusinessOrIndividual").as[String] mustEqual "individual"
          (sentBodyDetails \ "title").as[String] mustEqual "Mr"
          (sentBodyDetails \ "firstName").as[String] mustEqual "FirstName"
          (sentBodyDetails \ "lastName").as[String] mustEqual "LastName"
          (sentBodyDetails \ "line1").as[String] mustEqual "1 Fake Street"
          (sentBodyDetails \ "postCode").as[String] mustEqual "AB12 3CD"
          (sentBodyDetails \ "country" \ "code").as[String] mustEqual "GB"
          (sentBodyDetails \ "country" \ "name").as[String] mustEqual "United Kingdom"
          (sentBodyDetails \ "isSupplierVatRegistered").as[Boolean] mustEqual true
          (sentBodyDetails \ "euStateVatReg").as[String] mustEqual "FR"
          (sentBodyDetails \ "vatRegistrationNumber").as[String] mustEqual "12345678912"

        }
      }
    }

    "onChangeAddress" - {
      "must clear the stored supplier address and supplier journey id from the session and redirect to AVD-S5.0 on change address" in {
        val answersWithJourneyId =
          individualVatRegisteredSupplierDetailsAnswers.unsafeSet(SupplierAddressJourneyIdPage(supplierNumber), "journey-123")

        val sessionRepository = stubSessionRepository()
        val application       = applicationForSubmit(Some(answersWithJourneyId), mock[NovaImportsBackendConnector], sessionRepository)

        running(application) {
          val request = FakeRequest(GET, supplierDetailsCheckYourAnswersRouteOnChangeAddress())
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.supplieraddress.routes.IsSupplierAddressInTheUKController
            .onPageLoad(supplierNumber, NormalMode)
            .url

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(sessionRepository).set(captor.capture())
          captor.getValue.get(AddressPage) mustBe None
          captor.getValue.get(SupplierAddressJourneyIdPage(supplierNumber)) mustBe None
        }
      }

      "must redirect to Unauthorised on change address if no session data is found" in {
        val application = applicationForSubmit(None, mock[NovaImportsBackendConnector])

        running(application) {
          val request = FakeRequest(GET, supplierDetailsCheckYourAnswersRouteOnChangeAddress())
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

  }
}

object SupplierDetailsCheckYourAnswersControllerSpec {

  import org.scalatest.TryValues.*

  private val supplierNumber = SupplierNumber(1)
  private val address        = Address(Seq("1 Fake Street"), Some("AB12 3CD"), Country("GB", "United Kingdom"))
  private val name           = NameDetails("Mr", "FirstName", "LastName")
  private val businessName   = "ABC Ltd"
  private val vatDetails     = VatNumberDetails("FR", "12345678912")

  private val baseUserAnswers = UserAnswers("id")
    .set(DraftVersionIdPage, 1L)
    .success
    .value
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(VehicleFromEuPage, true)
    .success
    .value
    .set(AllSuppliersQuery, Map("1" -> Json.obj()))
    .success
    .value

  // Self Supply Personal Details
  private val selfSupplyPersonalDetailsAnswers = baseUserAnswers
    .set(UsePersonalDetailsAsSupplierPage(supplierNumber), true)
    .success
    .value
    .set(AddressPage, address)
    .success
    .value
    .set(NameDetailsPage, name)
    .success
    .value

  // Self Supply Personal Details Empty
  private val selfSupplyPersonalDetailsEmptyAnswers = baseUserAnswers
    .set(UsePersonalDetailsAsSupplierPage(supplierNumber), true)
    .success
    .value

  // Self Supply Purchaser Details
  private val selfSupplyPurchaserDetailsAnswers = baseUserAnswers
    .set(UsePurchaserDetailsAsSupplierPage(supplierNumber), true)
    .success
    .value
    .set(PurchaserAddressPage, address)
    .success
    .value
    .set(PurchaserNamePage, name)
    .success
    .value

  // Self Supply Purchaser Details Empty
  private val selfSupplyPurchaserDetailsEmptyAnswers = baseUserAnswers
    .set(UsePurchaserDetailsAsSupplierPage(supplierNumber), true)
    .success
    .value

  // individual Vat Registered
  private val individualVatRegisteredSupplierDetailsAnswers = baseUserAnswers
    .set(UsePersonalDetailsAsSupplierPage(supplierNumber), false)
    .success
    .value
    .set(SupplierBusinessOrIndividualPage(supplierNumber), BusinessOrPrivateIndividual.PrivateIndividual)
    .success
    .value
    .set(SupplierNamePage(supplierNumber), name)
    .success
    .value
    .set(IsSupplierAddressInTheUkPage(supplierNumber), true)
    .success
    .value
    .set(SupplierAddressPage(supplierNumber), address)
    .success
    .value
    .set(IsSupplierVatRegisteredPage(supplierNumber), true)
    .success
    .value
    .set(SupplierVatRegistrationNumberPage(supplierNumber), vatDetails)
    .success
    .value

  // individual Non Vat Registered
  private val individualNonVatRegisteredSupplierDetailsAnswers = baseUserAnswers
    .set(UsePersonalDetailsAsSupplierPage(supplierNumber), false)
    .success
    .value
    .set(SupplierBusinessOrIndividualPage(supplierNumber), BusinessOrPrivateIndividual.PrivateIndividual)
    .success
    .value
    .set(SupplierNamePage(supplierNumber), name)
    .success
    .value
    .set(IsSupplierAddressInTheUkPage(supplierNumber), true)
    .success
    .value
    .set(SupplierAddressPage(supplierNumber), address)
    .success
    .value
    .set(IsSupplierVatRegisteredPage(supplierNumber), false)
    .success
    .value

  // Business Vat Registered
  private val businessVatRegisteredSupplierDetailsAnswers = baseUserAnswers
    .set(UsePersonalDetailsAsSupplierPage(supplierNumber), false)
    .success
    .value
    .set(SupplierBusinessOrIndividualPage(supplierNumber), BusinessOrPrivateIndividual.Business)
    .success
    .value
    .set(SupplierBusinessNamePage(supplierNumber), businessName)
    .success
    .value
    .set(IsSupplierAddressInTheUkPage(supplierNumber), true)
    .success
    .value
    .set(SupplierAddressPage(supplierNumber), address)
    .success
    .value
    .set(IsSupplierVatRegisteredPage(supplierNumber), true)
    .success
    .value
    .set(SupplierVatRegistrationNumberPage(supplierNumber), vatDetails)
    .success
    .value

  // Business Non Vat Registered
  private val businessNonVatRegisteredSupplierDetailsAnswers = baseUserAnswers
    .set(UsePersonalDetailsAsSupplierPage(supplierNumber), false)
    .success
    .value
    .set(SupplierBusinessOrIndividualPage(supplierNumber), BusinessOrPrivateIndividual.Business)
    .success
    .value
    .set(SupplierBusinessNamePage(supplierNumber), businessName)
    .success
    .value
    .set(IsSupplierAddressInTheUkPage(supplierNumber), true)
    .success
    .value
    .set(SupplierAddressPage(supplierNumber), address)
    .success
    .value
    .set(IsSupplierVatRegisteredPage(supplierNumber), false)
    .success
    .value

}
