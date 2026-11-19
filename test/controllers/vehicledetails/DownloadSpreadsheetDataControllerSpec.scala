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
import connectors.{GetUploadResultError, NovaImportsBackendConnector}
import controllers.actions.*
import controllers.{routes, vehicledetails}
import models.responses.{SpreadsheetNonEuVehicle, UploadResultResponse, VehicleSummary}
import models.{AgentSelectedClient, DraftId, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.{AgentSelectedClientPage, DraftIdPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.SpreadsheetDataCsv

import java.time.LocalDate
import scala.concurrent.Future

class DownloadSpreadsheetDataControllerSpec extends SpecBase with MockitoSugar {

  private lazy val downloadRoute = vehicledetails.routes.DownloadSpreadsheetDataController.download().url

  private val draftId = DraftId("DRAFT-001")

  private val importAnswers: UserAnswers =
    emptyUserAnswers.unsafeSet(DraftIdPage, draftId).unsafeSet(VehicleFromEuPage, false)

  private val vehicle = SpreadsheetNonEuVehicle(
    itemNumber = Some(1),
    importEntryNumber = Some("123-123456A"),
    importEntryDate = Some(LocalDate.of(2026, 3, 30)),
    knownDateFirstRegistered = Some(true),
    countryOfFirstRegistration = Some("FR"),
    dateOfFirstRegistration = Some(LocalDate.of(2010, 1, 1)),
    make = Some("Make"),
    model = Some("Model"),
    derivative = Some("Derivative"),
    trim = Some("Trim"),
    bodyType = Some("Body type"),
    notificationReference = Some("NOVA00E000000"),
    vin = Some("1"),
    dateArrivedInUk = Some(LocalDate.of(2026, 3, 1)),
    commodityCode = Some("1234"),
    mileage = Some("10000"),
    mileageUnits = Some("MILES"),
    leftOrRightHandDrive = Some("RHD"),
    pricePaid = Some(BigDecimal("100")),
    currency = Some("USD"),
    claimingVatRelief = Some(false),
    reasonForClaimingRelief = Some("Reason")
  )

  private def validatedNonEuResult(vehicles: Seq[SpreadsheetNonEuVehicle], validationType: Option[String] = Some("CarsNonEu")) =
    UploadResultResponse(
      fileStatus = "VALIDATED",
      validationType = validationType,
      vehicles = vehicles.map(v => VehicleSummary(v.itemNumber, v.vin, v.make, v.model)),
      euVehicles = Seq.empty,
      nonEuVehicles = vehicles,
      agriculturalTractorEuVehicles = Seq.empty,
      agriculturalTractorNonEuVehicles = Seq.empty,
      motorCaravansEuVehicles = Seq.empty,
      motorCaravansNonEuVehicles = Seq.empty,
      heavyCommercialEuVehicles = List.empty,
      heavyCommercialNonEuVehicles = List.empty,
      motorcyclesEuVehicles = Seq.empty,
      motorcyclesNonEuVehicles = Seq.empty,
      constructionVehiclesEuVehicles = Seq.empty,
      constructionVehiclesNonEuVehicles = Seq.empty,
      errors = Seq.empty
    )

  private def connectorReturning(result: Either[GetUploadResultError, UploadResultResponse]): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getUploadResult(any[DraftId])(using any[HeaderCarrier])).thenReturn(Future.successful(result))
    connector
  }

  private def applicationFor(
    standardIdentifier: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers],
    connector: NovaImportsBackendConnector
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(standardIdentifier),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[NovaImportsBackendConnector].toInstance(connector)
      )
      .build()

  "DownloadSpreadsheetDataController" - {

    "must be served from /check-vehicle-spreadsheet-details/download" in {
      given app: Application =
        applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers), connectorReturning(Right(validatedNonEuResult(Seq(vehicle)))))

      running(app) {
        downloadRoute mustEqual "/nova-imports/check-vehicle-spreadsheet-details/download"
      }
    }

    "must fetch the upload result and return it as a CSV attachment named nova-summary.csv" in {
      val uploadResult       = validatedNonEuResult(Seq(vehicle))
      val connector          = connectorReturning(Right(uploadResult))
      given app: Application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers), connector)

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual OK
        contentType(result).value mustEqual "text/csv"
        charset(result).value mustEqual "utf-8"
        header(CONTENT_DISPOSITION, result).value mustEqual "attachment; filename=\"nova-summary.csv\""
        contentAsString(result) mustEqual SpreadsheetDataCsv(uploadResult)(using messages(app)).value
        verify(connector).getUploadResult(eqTo(draftId))(using any[HeaderCarrier])
      }
    }

    "must redirect to the journey recovery page when the upload result cannot be fetched" in {
      given app: Application =
        applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers), connectorReturning(Left(GetUploadResultError.NotFound)))

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the journey recovery page when the backend cannot be reached" in {
      val connector = mock[NovaImportsBackendConnector]
      when(connector.getUploadResult(any[DraftId])(using any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("connection refused")))
      given app: Application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers), connector)

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the journey recovery page when the spreadsheet has not been validated" in {
      val connector          = connectorReturning(Right(validatedNonEuResult(Seq(vehicle)).copy(fileStatus = "VALIDATION_FAILED")))
      given app: Application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers), connector)

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the journey recovery page when the vehicle type has no matching CSV headings" in {
      val connector          = connectorReturning(Right(validatedNonEuResult(Seq(vehicle), validationType = Some("Unknown"))))
      given app: Application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers), connector)

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return OK when an agent who has selected a client downloads the CSV" in {
      val answers            = importAnswers.unsafeSet(AgentSelectedClientPage, AgentSelectedClient("700011916", Some("Company 1 Ltd")))
      given app: Application =
        applicationFor(classOf[FakeAgentIdentifierAction], Some(answers), connectorReturning(Right(validatedNonEuResult(Seq(vehicle)))))

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual OK
      }
    }

    "must return OK when a VAT-registered organisation downloads the CSV" in {
      given app: Application =
        applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers), connectorReturning(Right(validatedNonEuResult(Seq(vehicle)))))

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Unauthorised for an agent who has not selected a client" in {
      val connector          = connectorReturning(Right(validatedNonEuResult(Seq(vehicle))))
      given app: Application = applicationFor(classOf[FakeAgentIdentifierAction], Some(importAnswers), connector)

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        verify(connector, never).getUploadResult(any[DraftId])(using any[HeaderCarrier])
      }
    }

    "must redirect to Unauthorised for a private individual" in {
      val connector          = connectorReturning(Right(validatedNonEuResult(Seq(vehicle))))
      given app: Application = applicationFor(classOf[FakeIdentifierAction], Some(importAnswers), connector)

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        verify(connector, never).getUploadResult(any[DraftId])(using any[HeaderCarrier])
      }
    }

    "must redirect to Unauthorised for a non-VAT organisation" in {
      val connector          = connectorReturning(Right(validatedNonEuResult(Seq(vehicle))))
      given app: Application = applicationFor(classOf[FakeOrganisationIdentifierAction], Some(importAnswers), connector)

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        verify(connector, never).getUploadResult(any[DraftId])(using any[HeaderCarrier])
      }
    }

    "must redirect to Unauthorised when the draft id is missing" in {
      val answersWithoutDraft = emptyUserAnswers.unsafeSet(VehicleFromEuPage, false)
      val connector           = connectorReturning(Right(validatedNonEuResult(Seq(vehicle))))
      given app: Application  = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answersWithoutDraft), connector)

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        verify(connector, never).getUploadResult(any[DraftId])(using any[HeaderCarrier])
      }
    }

    "must redirect to Unauthorised when IQ1.0 has not been answered" in {
      val answersWithoutVehicleFromEu = emptyUserAnswers.unsafeSet(DraftIdPage, draftId)
      val connector                   = connectorReturning(Right(validatedNonEuResult(Seq(vehicle))))
      given app: Application          = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answersWithoutVehicleFromEu), connector)

      running(app) {
        given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, downloadRoute)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        verify(connector, never).getUploadResult(any[DraftId])(using any[HeaderCarrier])
      }
    }
  }
}
