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
import connectors.{NovaImportsBackendConnector, UpdateSectionError}
import controllers.actions.*
import controllers.{routes, vehicledetails}
import models.{AddVehicleType, AgentSelectedClient, CheckMode, DraftId, ImportNumber, NormalMode, SupplierNumber, UserAnswers, VehicleDates, VehicleNumber}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.*
import pages.{AgentSelectedClientPage, DraftIdPage, DraftVersionIdPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

class ConfirmVehicleDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val s = SupplierNumber(1)
  private val i = ImportNumber(1)
  private val v = VehicleNumber(1)

  private lazy val supplierRoute = vehicledetails.routes.ConfirmVehicleDetailsController.supplierOnPageLoad(s, v).url
  private lazy val importRoute   = vehicledetails.routes.ConfirmVehicleDetailsController.importOnPageLoad(i, v).url

  private val supplierBase: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(DraftVersionIdPage, 3L)
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
    .unsafeSet(PaymentCurrencyPage(v), "EUR")
    .unsafeSet(AddVehicleTypePage(v), AddVehicleType.Car)

  private val invoiceOnly: UserAnswers = supplierBase
    .unsafeSet(VehicleDatesPage(s, v), Set(VehicleDates.PurchaseInvoiceDate))
    .unsafeSet(PurchaseInvoiceDatePage(s, v), LocalDate.of(2026, 3, 27))
    .unsafeSet(PurchaseInvoiceNumberPage(s, v), "INV-001")
    .unsafeSet(TotalAmountPaidPage(v), "45000")

  private val availabilityOnly: UserAnswers = supplierBase
    .unsafeSet(VehicleDatesPage(s, v), Set(VehicleDates.FirstRegistration, VehicleDates.MadeAvailable))
    .unsafeSet(DateOfAvailabilityPage(s, v), LocalDate.of(2026, 3, 27))
    .unsafeSet(DateOfFirstRegistrationPage(v), LocalDate.of(2026, 3, 1))
    .unsafeSet(CountryOfFirstRegistrationPage(v), "FR")
    .unsafeSet(NoPurchaseInvoiceReasonPage(s, v), "No invoice was issued")
    .unsafeSet(TotalAmountPaidPage(v), "45000")

  private val importComplete: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(DraftVersionIdPage, 3L)
    .unsafeSet(VehicleFromEuPage, false)
    .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A")))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1)))
    .unsafeSet(DateOfFirstRegistrationKnownPage(v), true)
    .unsafeSet(DateOfFirstRegistrationPage(v), LocalDate.of(2026, 3, 1))
    .unsafeSet(CountryOfFirstRegistrationPage(v), "FR")
    .unsafeSet(AddVehicleTypePage(v), AddVehicleType.Hcv)

  private def stubSessionRepository(answers: UserAnswers): SessionRepository = {
    val repository = mock[SessionRepository]
    when(repository.set(any())).thenReturn(Future.successful(true))
    when(repository.setPage(any(), any(), any())(any())).thenReturn(Future.successful(answers))
    repository
  }

  private def application(
    answers: Option[UserAnswers],
    connector: NovaImportsBackendConnector = mock[NovaImportsBackendConnector],
    repository: SessionRepository = mock[SessionRepository]
  ): Application =
    applicationBuilder(answers)
      .overrides(
        bind[NovaImportsBackendConnector].toInstance(connector),
        bind[SessionRepository].toInstance(repository)
      )
      .build()

  private def applicationFor(standardIdentifier: Class[? <: IdentifierAction], answers: UserAnswers): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(standardIdentifier),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(answers)))
      )
      .build()

  private def mustBeUnauthorised(app: Application, url: String, method: String = GET): Unit =
    running(app) {
      val result = route(app, FakeRequest(method, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }

  private def successfulConnector: NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(Right(4L)))
    connector
  }

  private def capturedBody(connector: NovaImportsBackendConnector, sectionId: String): JsObject = {
    val captor = ArgumentCaptor.forClass(classOf[JsObject])
    verify(connector).updateDraftSection(eqTo(DraftId("DRAFT-001")), eqTo(sectionId), captor.capture())(any[HeaderCarrier])
    captor.getValue
  }

  "ConfirmVehicleDetailsController" - {

    "supplierOnPageLoad" - {

      "must return OK with the heading and a Confirm details button posting to the supplier submit" in {
        val app = application(Some(invoiceOnly))

        running(app) {
          val result = route(app, FakeRequest(GET, supplierRoute)).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include(messages(app)("confirmVehicleDetails.heading"))
          body must include(messages(app)("site.confirmDetails"))
          body must include(s"""action="${vehicledetails.routes.ConfirmVehicleDetailsController.supplierOnSubmit(s, v).url}"""")
        }
      }

      "must show the currency and vehicle type rows with change links to AVD7.1 and AVD8.0 in check mode" in {
        val app = application(Some(invoiceOnly))

        running(app) {
          val body = contentAsString(route(app, FakeRequest(GET, supplierRoute)).value)

          body must include(messages(app)("confirmVehicleDetails.currency.label"))
          body must include("Euro (EUR)")
          body must include(messages(app)("confirmVehicleDetails.vehicleType.label"))
          body must include(messages(app)("addVehicleType.radio.car"))
          body must include(vehicledetails.routes.PaymentCurrencyController.supplierOnPageLoad(s, v, CheckMode).url)
          body must include(vehicledetails.routes.AddVehicleTypeController.supplierOnPageLoad(s, v, CheckMode).url)
        }
      }

      "must return OK for an agent who has selected a client" in {
        val answers = invoiceOnly.unsafeSet(AgentSelectedClientPage, AgentSelectedClient("700011916", Some("Client Co")))
        val app     = applicationFor(classOf[FakeAgentIdentifierAction], answers)

        running(app) {
          status(route(app, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must return OK for a private individual" in {
        val app = applicationFor(classOf[FakeIdentifierAction], invoiceOnly)

        running(app) {
          status(route(app, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must redirect to Unauthorised when draftId is missing" in {
        mustBeUnauthorised(application(Some(invoiceOnly.remove(DraftIdPage).success.value)), supplierRoute)
      }

      "must redirect to Unauthorised when the vehicle was not brought from the EU" in {
        mustBeUnauthorised(application(Some(invoiceOnly.unsafeSet(VehicleFromEuPage, false))), supplierRoute)
      }

      "must redirect to Unauthorised when the vehicle does not belong to the supplier" in {
        mustBeUnauthorised(
          application(Some(invoiceOnly)),
          vehicledetails.routes.ConfirmVehicleDetailsController.supplierOnPageLoad(SupplierNumber(2), v).url
        )
      }

      "must redirect to Unauthorised when the dates question has not been answered" in {
        mustBeUnauthorised(application(Some(invoiceOnly.remove(VehicleDatesPage(s, v)).success.value)), supplierRoute)
      }

      "must redirect to Unauthorised when no dates is selected" in {
        mustBeUnauthorised(application(Some(invoiceOnly.unsafeSet(VehicleDatesPage(s, v), Set(VehicleDates.NoDates)))), supplierRoute)
      }

      "must redirect to Unauthorised when the purchase invoice number is missing" in {
        mustBeUnauthorised(application(Some(invoiceOnly.remove(PurchaseInvoiceNumberPage(s, v)).success.value)), supplierRoute)
      }

      "must redirect to Unauthorised when the reason for no purchase invoice is missing" in {
        mustBeUnauthorised(application(Some(availabilityOnly.remove(NoPurchaseInvoiceReasonPage(s, v)).success.value)), supplierRoute)
      }

      "must redirect to Unauthorised when the country of first registration is missing" in {
        mustBeUnauthorised(application(Some(availabilityOnly.remove(CountryOfFirstRegistrationPage(v)).success.value)), supplierRoute)
      }

      "must redirect to Unauthorised when the total amount paid is missing" in {
        mustBeUnauthorised(application(Some(invoiceOnly.remove(TotalAmountPaidPage(v)).success.value)), supplierRoute)
      }

      "must redirect to Unauthorised when the currency is missing" in {
        mustBeUnauthorised(application(Some(invoiceOnly.remove(PaymentCurrencyPage(v)).success.value)), supplierRoute)
      }

      "must redirect to Unauthorised when the vehicle type is missing" in {
        mustBeUnauthorised(application(Some(invoiceOnly.remove(AddVehicleTypePage(v)).success.value)), supplierRoute)
      }
    }

    "importOnPageLoad" - {

      "must return OK and post to the import submit" in {
        val app = application(Some(importComplete))

        running(app) {
          val result = route(app, FakeRequest(GET, importRoute)).value

          status(result) mustEqual OK
          contentAsString(result) must include(s"""action="${vehicledetails.routes.ConfirmVehicleDetailsController.importOnSubmit(i, v).url}"""")
        }
      }

      "must show the vehicle type row with a change link to AVD8.0 and no currency row" in {
        val app = application(Some(importComplete))

        running(app) {
          val body = contentAsString(route(app, FakeRequest(GET, importRoute)).value)

          body must include(messages(app)("addVehicleType.radio.hcv"))
          body must include(vehicledetails.routes.AddVehicleTypeController.importOnPageLoad(i, v, CheckMode).url)
          body must not include messages(app)("confirmVehicleDetails.currency.label")
        }
      }

      "must redirect to Unauthorised when the vehicle was brought from the EU" in {
        mustBeUnauthorised(application(Some(importComplete.unsafeSet(VehicleFromEuPage, true))), importRoute)
      }

      "must redirect to Unauthorised when the vehicle does not belong to the import" in {
        mustBeUnauthorised(
          application(Some(importComplete)),
          vehicledetails.routes.ConfirmVehicleDetailsController.importOnPageLoad(ImportNumber(2), v).url
        )
      }

      "must redirect to Unauthorised when the country of first registration is missing" in {
        mustBeUnauthorised(application(Some(importComplete.remove(CountryOfFirstRegistrationPage(v)).success.value)), importRoute)
      }

      "must redirect to Unauthorised when the date of first registration is missing" in {
        mustBeUnauthorised(application(Some(importComplete.remove(DateOfFirstRegistrationPage(v)).success.value)), importRoute)
      }

      "must redirect to Unauthorised when whether the date of first registration is known has not been answered" in {
        mustBeUnauthorised(application(Some(importComplete.remove(DateOfFirstRegistrationKnownPage(v)).success.value)), importRoute)
      }

      "must return OK without the date and country of first registration when the date of first registration is not known" in {
        val answers = importComplete
          .unsafeSet(DateOfFirstRegistrationKnownPage(v), false)
          .remove(DateOfFirstRegistrationPage(v))
          .success
          .value
          .remove(CountryOfFirstRegistrationPage(v))
          .success
          .value
        val app = application(Some(answers))

        running(app) {
          status(route(app, FakeRequest(GET, importRoute)).value) mustEqual OK
        }
      }

      "must redirect to Unauthorised when the vehicle type is missing" in {
        mustBeUnauthorised(application(Some(importComplete.remove(AddVehicleTypePage(v)).success.value)), importRoute)
      }

      "must redirect to Unauthorised when the supplier url is used for an import vehicle" in {
        mustBeUnauthorised(application(Some(importComplete)), supplierRoute)
      }
    }

    "supplierOnSubmit" - {

      "must call F4 for the supplier vehicle type section, save the new version and redirect to AVD8.1 for a car" in {
        val connector  = successfulConnector
        val repository = stubSessionRepository(invoiceOnly)
        val app        = application(Some(invoiceOnly), connector, repository)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual vehicledetails.routes.AddVehicleDetailsCarController
            .supplierOnPageLoad(s, v, NormalMode)
            .url

          capturedBody(connector, "supplier/1/vehicle/1/type") mustEqual Json.obj(
            "vehicleType"               -> "CAR",
            "doYouHaveAPurchaseInvoice" -> true,
            "currencyUsed"              -> "EUR",
            "dateRoadUseKnown"          -> false,
            "purchaseInvoiceNumber"     -> "INV-001",
            "purchaseInvoiceDate"       -> "27/03/2026",
            "pricePaidForVehicle"       -> "45000",
            "fromSupplier"              -> true,
            "versionId"                 -> 3L
          )
          verify(repository).setPage(any(), eqTo(DraftVersionIdPage), eqTo(4L))(any())
        }
      }

      "must not send stale purchase invoice answers when the purchase invoice date is no longer selected" in {
        val answers = availabilityOnly
          .unsafeSet(PurchaseInvoiceDatePage(s, v), LocalDate.of(2026, 1, 1))
          .unsafeSet(PurchaseInvoiceNumberPage(s, v), "STALE")
        val connector = successfulConnector
        val app       = application(Some(answers), connector, stubSessionRepository(answers))

        running(app) {
          status(route(app, FakeRequest(POST, supplierRoute)).value) mustEqual SEE_OTHER

          capturedBody(connector, "supplier/1/vehicle/1/type") mustEqual Json.obj(
            "vehicleType"                 -> "CAR",
            "doYouHaveAPurchaseInvoice"   -> false,
            "dateRoadUseKnown"            -> true,
            "currencyUsed"                -> "EUR",
            "pricePaidForVehicle"         -> "45000",
            "dateMadeAvailableYou"        -> "27/03/2026",
            "dateOfFirstRegistration"     -> "01/03/2026",
            "countryOfFirstRegistration"  -> "FR",
            "noPurchaserInvoiceReasonMax" -> "No invoice was issued",
            "fromSupplier"                -> true,
            "versionId"                   -> 3L
          )
        }
      }

      "must send the availability and first registration answers but not a stale reason for no purchase invoice when both date types are selected" in {
        val answers = availabilityOnly
          .unsafeSet(VehicleDatesPage(s, v), Set(VehicleDates.PurchaseInvoiceDate, VehicleDates.FirstRegistration, VehicleDates.MadeAvailable))
          .unsafeSet(PurchaseInvoiceDatePage(s, v), LocalDate.of(2026, 3, 20))
          .unsafeSet(PurchaseInvoiceNumberPage(s, v), "INV-001")
        val connector = successfulConnector
        val app       = application(Some(answers), connector, stubSessionRepository(answers))

        running(app) {
          status(route(app, FakeRequest(POST, supplierRoute)).value) mustEqual SEE_OTHER

          capturedBody(connector, "supplier/1/vehicle/1/type") mustEqual Json.obj(
            "vehicleType"                -> "CAR",
            "doYouHaveAPurchaseInvoice"  -> true,
            "dateRoadUseKnown"           -> true,
            "currencyUsed"               -> "EUR",
            "purchaseInvoiceNumber"      -> "INV-001",
            "purchaseInvoiceDate"        -> "20/03/2026",
            "pricePaidForVehicle"        -> "45000",
            "dateMadeAvailableYou"       -> "27/03/2026",
            "dateOfFirstRegistration"    -> "01/03/2026",
            "countryOfFirstRegistration" -> "FR",
            "fromSupplier"               -> true,
            "versionId"                  -> 3L
          )
        }
      }

      "must send first registration but not availability or a reason when only first registration and purchase invoice dates are selected" in {
        val answers = availabilityOnly
          .unsafeSet(VehicleDatesPage(s, v), Set(VehicleDates.FirstRegistration, VehicleDates.PurchaseInvoiceDate))
          .unsafeSet(PurchaseInvoiceDatePage(s, v), LocalDate.of(2026, 3, 20))
          .unsafeSet(PurchaseInvoiceNumberPage(s, v), "INV-001")
        val connector = successfulConnector
        val app       = application(Some(answers), connector, stubSessionRepository(answers))

        running(app) {
          status(route(app, FakeRequest(POST, supplierRoute)).value) mustEqual SEE_OTHER

          capturedBody(connector, "supplier/1/vehicle/1/type") mustEqual Json.obj(
            "vehicleType"                -> "CAR",
            "doYouHaveAPurchaseInvoice"  -> true,
            "dateRoadUseKnown"           -> true,
            "currencyUsed"               -> "EUR",
            "purchaseInvoiceNumber"      -> "INV-001",
            "purchaseInvoiceDate"        -> "20/03/2026",
            "pricePaidForVehicle"        -> "45000",
            "dateOfFirstRegistration"    -> "01/03/2026",
            "countryOfFirstRegistration" -> "FR",
            "fromSupplier"               -> true,
            "versionId"                  -> 3L
          )
        }
      }

      "must send availability but not first registration when only made available and purchase invoice dates are selected" in {
        val answers = availabilityOnly
          .unsafeSet(VehicleDatesPage(s, v), Set(VehicleDates.MadeAvailable, VehicleDates.PurchaseInvoiceDate))
          .unsafeSet(PurchaseInvoiceDatePage(s, v), LocalDate.of(2026, 3, 20))
          .unsafeSet(PurchaseInvoiceNumberPage(s, v), "INV-001")
        val connector = successfulConnector
        val app       = application(Some(answers), connector, stubSessionRepository(answers))

        running(app) {
          status(route(app, FakeRequest(POST, supplierRoute)).value) mustEqual SEE_OTHER

          capturedBody(connector, "supplier/1/vehicle/1/type") mustEqual Json.obj(
            "vehicleType"               -> "CAR",
            "doYouHaveAPurchaseInvoice" -> true,
            "dateRoadUseKnown"          -> false,
            "currencyUsed"              -> "EUR",
            "purchaseInvoiceNumber"     -> "INV-001",
            "purchaseInvoiceDate"       -> "20/03/2026",
            "pricePaidForVehicle"       -> "45000",
            "dateMadeAvailableYou"      -> "27/03/2026",
            "fromSupplier"              -> true,
            "versionId"                 -> 3L
          )
        }
      }

      "must redirect to There is a problem when F4 fails" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(500, "error"))))
        val repository = mock[SessionRepository]
        val app        = application(Some(invoiceOnly), connector, repository)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(repository, never()).setPage(any(), any(), any())(any())
        }
      }

      "must redirect to There is a problem without calling F4 when the version id is missing" in {
        val connector = mock[NovaImportsBackendConnector]
        val app       = application(Some(invoiceOnly.remove(DraftVersionIdPage).success.value), connector)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(connector, never()).updateDraftSection(any(), any(), any())(any[HeaderCarrier])
        }
      }

      "must redirect to Unauthorised when required answers are missing" in {
        mustBeUnauthorised(application(Some(invoiceOnly.remove(TotalAmountPaidPage(v)).success.value)), supplierRoute, POST)
      }
    }

    "importOnSubmit" - {

      "must call F4 for the import vehicle type section and redirect to the AVD8.3 placeholder for a heavy commercial vehicle" in {
        val connector = successfulConnector
        val app       = application(Some(importComplete), connector, stubSessionRepository(importComplete))

        running(app) {
          val result = route(app, FakeRequest(POST, importRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url

          capturedBody(connector, "import/1/vehicle/1/type") mustEqual Json.obj(
            "vehicleType"                -> "HCV",
            "dateRoadUseKnown"           -> true,
            "dateOfFirstRegistration"    -> "01/03/2026",
            "countryOfFirstRegistration" -> "FR",
            "fromSupplier"               -> false,
            "versionId"                  -> 3L
          )
        }
      }

      "must not send a stale date or country of first registration when the date of first registration is not known" in {
        val answers   = importComplete.unsafeSet(DateOfFirstRegistrationKnownPage(v), false)
        val connector = successfulConnector
        val app       = application(Some(answers), connector, stubSessionRepository(answers))

        running(app) {
          status(route(app, FakeRequest(POST, importRoute)).value) mustEqual SEE_OTHER

          capturedBody(connector, "import/1/vehicle/1/type") mustEqual Json.obj(
            "vehicleType"      -> "HCV",
            "dateRoadUseKnown" -> false,
            "fromSupplier"     -> false,
            "versionId"        -> 3L
          )
        }
      }

      "must redirect to There is a problem when F4 fails" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(500, "error"))))
        val app = application(Some(importComplete), connector)

        running(app) {
          val result = route(app, FakeRequest(POST, importRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
