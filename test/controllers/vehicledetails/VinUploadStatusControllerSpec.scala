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
import connectors.{DeleteVinUploadError, GetFileUploadSummaryError, NovaImportsBackendConnector}
import controllers.actions.*
import controllers.{routes, vehicledetails}
import models.responses.{DeleteFileUploadResponse, GetFileUploadSummaryResponse}
import models.{DraftId, ImportNumber, NormalMode, NumberOfVehicles, OneMultiVehicle, SupplierNumber, UserAnswers, VehicleNumber}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.NumberOfVehiclesPage
import pages.DraftIdPage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import scala.concurrent.Future

class VinUploadStatusControllerSpec extends SpecBase with MockitoSugar {

  private val s = SupplierNumber(1)
  private val i = ImportNumber(1)
  private val v = VehicleNumber(1)

  private lazy val supplierRoute       = vehicledetails.routes.VinUploadStatusController.supplierOnPageLoad(s, v).url
  private lazy val supplierRemoveRoute = vehicledetails.routes.VinUploadStatusController.supplierOnPageLoad(s, v, remove = true).url
  private lazy val importRoute         = vehicledetails.routes.VinUploadStatusController.importOnPageLoad(i, v).url
  private lazy val importRemoveRoute   = vehicledetails.routes.VinUploadStatusController.importOnPageLoad(i, v, remove = true).url

  private val draftId = DraftId("DRAFT-001")

  private val supplierAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, draftId)
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
    .unsafeSet(NumberOfVehiclesPage(v), NumberOfVehicles(OneMultiVehicle.More, Some(5)))

  private val importAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, draftId)
    .unsafeSet(VehicleFromEuPage, false)
    .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A")))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1)))
    .unsafeSet(NumberOfVehiclesPage(v), NumberOfVehicles(OneMultiVehicle.More, Some(5)))

  private def connectorReturning(
    summaryResult: Either[GetFileUploadSummaryError, GetFileUploadSummaryResponse],
    deleteResult: Either[DeleteVinUploadError, DeleteFileUploadResponse] = Right(DeleteFileUploadResponse(true))
  ): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getFileUploadSummary(any[DraftId])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(summaryResult))
    when(connector.deleteVinUpload(any[DraftId])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(deleteResult))
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

  "VinUploadStatusController.supplierOnPageLoad" - {

    "must render the page as Uploading while the extraction is still in progress" in {
      val connector =
        connectorReturning(Right(GetFileUploadSummaryResponse(fileStatus = "VERIFYING", failureReason = None, fileName = None, kind = "VIN")))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include(messages(application)("vinUploadStatus.status.uploading"))
      }
    }

    "must render the page as Uploaded, with Continue enabled, once extraction is final" in {
      val connector = connectorReturning(
        Right(GetFileUploadSummaryResponse(fileStatus = "UPLOADED", failureReason = None, fileName = Some("vins.csv"), kind = "VIN"))
      )
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include(messages(application)("vinUploadStatus.status.uploaded"))
        contentAsString(result) must include("vins.csv")
        contentAsString(result) must include(
          s"""href="${vehicledetails.routes.EnterVinNumbersController.supplierOnPageLoad(s, v, NormalMode).url}""""
        )
      }
    }

    "must redirect to the quarantine error page when extraction failed due to a virus" in {
      val connector = connectorReturning(
        Right(GetFileUploadSummaryResponse(fileStatus = "VERIFICATION_FAILED", failureReason = Some("QUARANTINE"), fileName = None, kind = "VIN"))
      )
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSpreadsheetErrorQuarantineController
          .onPageLoad(from = "vin", supplierNumber = Some(1), importNumber = None, vehicleNumber = Some(1))
          .url
      }
    }

    "must redirect to the rejected error page when extraction failed due to the file type" in {
      val connector = connectorReturning(
        Right(GetFileUploadSummaryResponse(fileStatus = "VERIFICATION_FAILED", failureReason = Some("REJECTED"), fileName = None, kind = "VIN"))
      )
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSpreadsheetErrorRejectedController
          .onPageLoad(from = "vin", supplierNumber = Some(1), importNumber = None, vehicleNumber = Some(1))
          .url
      }
    }

    "must redirect to Journey Recovery when the in-progress upload is a different kind (spreadsheet)" in {
      val connector = connectorReturning(
        Right(GetFileUploadSummaryResponse(fileStatus = "VALIDATING", failureReason = None, fileName = None, kind = "SPREADSHEET"))
      )
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when the backend call fails" in {
      val connector   = connectorReturning(Left(GetFileUploadSummaryError.UpstreamError(502, "boom")))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "VinUploadStatusController.supplierOnPageLoad(remove = true)" - {

    "must delete the tracked upload and redirect" in {
      val connector =
        connectorReturning(Right(GetFileUploadSummaryResponse(fileStatus = "UPLOADED", failureReason = None, fileName = None, kind = "VIN")))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, supplierRemoveRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.VehicleIdentificationNumbersController.supplierOnPageLoad(s, v).url
        verify(connector).deleteVinUpload(eqTo(draftId))(using any[HeaderCarrier])
      }
    }

    "must not call getFileUploadSummary when removing" in {
      val connector =
        connectorReturning(Right(GetFileUploadSummaryResponse(fileStatus = "UPLOADED", failureReason = None, fileName = None, kind = "VIN")))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        route(application, FakeRequest(GET, supplierRemoveRoute)).value.futureValue

        verify(connector, never).getFileUploadSummary(any[DraftId])(using any[HeaderCarrier])
      }
    }
  }

  "VinUploadStatusController.importOnPageLoad(remove = true)" - {

    "must delete the tracked upload and redirect to VehicleIdentificationNumbersController AMV1.0" in {
      val connector =
        connectorReturning(Right(GetFileUploadSummaryResponse(fileStatus = "UPLOADED", failureReason = None, fileName = None, kind = "VIN")))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, importRemoveRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.VehicleIdentificationNumbersController.importOnPageLoad(i, v).url
        verify(connector).deleteVinUpload(eqTo(draftId))(using any[HeaderCarrier])
      }
    }
  }

  "VinUploadStatusController.importOnPageLoad" - {

    "must redirect to the import-journey error page when extraction fails" in {
      val connector = connectorReturning(
        Right(GetFileUploadSummaryResponse(fileStatus = "VERIFICATION_FAILED", failureReason = Some("QUARANTINE"), fileName = None, kind = "VIN"))
      )
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, importRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSpreadsheetErrorQuarantineController
          .onPageLoad(from = "vin", supplierNumber = None, importNumber = Some(1), vehicleNumber = Some(1))
          .url
      }
    }
  }

  "VinUploadStatusController.status" - {

    lazy val statusRoute =
      vehicledetails.routes.VinUploadStatusController
        .status(RedirectUrl(vehicledetails.routes.EnterVinNumbersController.supplierOnPageLoad(s, v, NormalMode).url))
        .url

    "must report not final while extraction is still in progress" in {
      val connector =
        connectorReturning(Right(GetFileUploadSummaryResponse(fileStatus = "VERIFYING", failureReason = None, fileName = None, kind = "VIN")))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, statusRoute)).value

        status(result) mustEqual OK
        (contentAsJson(result) \ "isFinal").as[Boolean] mustEqual false
      }
    }

    "must pass the given continueUrl straight through once extraction is final" in {
      val connector = connectorReturning(
        Right(GetFileUploadSummaryResponse(fileStatus = "UPLOADED", failureReason = None, fileName = Some("vins.csv"), kind = "VIN"))
      )
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, statusRoute)).value

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.obj(
          "statusText"  -> messages(application)("vinUploadStatus.status.uploaded"),
          "isFinal"     -> true,
          "fileName"    -> "vins.csv",
          "continueUrl" -> vehicledetails.routes.EnterVinNumbersController.supplierOnPageLoad(s, v, NormalMode).url
        )
      }
    }

    "must reject an absolute continueUrl and fall back to Journey Recovery instead of reflecting it" in {
      val connector = connectorReturning(
        Right(GetFileUploadSummaryResponse(fileStatus = "UPLOADED", failureReason = None, fileName = Some("vins.csv"), kind = "VIN"))
      )
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val unsafeRoute =
          vehicledetails.routes.VinUploadStatusController.status(RedirectUrl("https://evil.example.com/phishing")).url
        val result = route(application, FakeRequest(GET, unsafeRoute)).value

        status(result) mustEqual OK
        (contentAsJson(result) \ "continueUrl").as[String] mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return InternalServerError when the in-progress upload is a different kind" in {
      val connector = connectorReturning(
        Right(GetFileUploadSummaryResponse(fileStatus = "VALIDATING", failureReason = None, fileName = None, kind = "SPREADSHEET"))
      )
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, statusRoute)).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }

    "must return InternalServerError when the backend call fails" in {
      val connector   = connectorReturning(Left(GetFileUploadSummaryError.UpstreamError(502, "boom")))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, statusRoute)).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }
}
