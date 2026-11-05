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
import connectors.{CreateVinUploadTrackingError, DeleteVinUploadError, GetFileUploadSummaryError, NovaImportsBackendConnector}
import controllers.actions.*
import controllers.{routes, vehicledetails}
import models.responses.{CreateUploadTrackingResponse, DeleteFileUploadResponse, GetFileUploadSummaryResponse}
import models.{DraftId, ImportNumber, NumberOfVehicles, OneMultiVehicle, SupplierNumber, UserAnswers, VehicleNumber}
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
import views.html.UploadVinNumbersView

import scala.concurrent.Future

class UploadVinNumbersControllerSpec extends SpecBase with MockitoSugar {

  private val s = SupplierNumber(1)
  private val i = ImportNumber(1)
  private val v = VehicleNumber(1)

  private lazy val supplierRoute        = vehicledetails.routes.UploadVinNumbersController.supplierOnPageLoad(s, v, restart = false).url
  private lazy val supplierRestartRoute = vehicledetails.routes.UploadVinNumbersController.supplierOnPageLoad(s, v, restart = true).url
  private lazy val importRoute          = vehicledetails.routes.UploadVinNumbersController.importOnPageLoad(i, v, restart = false).url

  private val draftId = DraftId("DRAFT-001")

  private val uploadTracking = CreateUploadTrackingResponse(
    reference = "11370e18-6e24-453e-b45a-76d3e32ea33d",
    uploadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com",
    fields = Map("key" -> "11370e18-6e24-453e-b45a-76d3e32ea33d")
  )

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
    createResult: Either[CreateVinUploadTrackingError, CreateUploadTrackingResponse] = Right(uploadTracking),
    summaryResult: Either[GetFileUploadSummaryError, GetFileUploadSummaryResponse] = Left(GetFileUploadSummaryError.NotFound),
    deleteResult: Either[DeleteVinUploadError, DeleteFileUploadResponse] = Right(DeleteFileUploadResponse(true))
  ): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.createVinUploadTracking(any[DraftId], any[Option[Boolean]], any[String], any[String])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(createResult))
    when(connector.getFileUploadSummary(any[DraftId])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(summaryResult))
    when(connector.deleteVinUpload(any[DraftId])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(deleteResult))
    connector
  }

  private def applicationFor(
    standardIdentifier: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers],
    connector: NovaImportsBackendConnector = connectorReturning()
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

  "UploadVinNumbersController.supplierOnPageLoad" - {

    "must return OK and render the upload form from the upscan details when no upload exists yet" in {
      val connector   = connectorReturning()
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val request = FakeRequest(GET, supplierRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[UploadVinNumbersView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(uploadTracking.uploadUrl, uploadTracking.fields, None)(request, messages(application)).toString
        verify(connector).createVinUploadTracking(eqTo(draftId), eqTo(None), any[String], any[String])(using any[HeaderCarrier])
      }
    }

    "must redirect to the status page when a VIN upload is already in progress" in {
      val connector = connectorReturning(summaryResult =
        Right(GetFileUploadSummaryResponse(fileStatus = "VERIFYING", failureReason = None, fileName = None, kind = "VIN"))
      )
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.VinUploadStatusController.supplierOnPageLoad(s, v).url
        verify(connector, never).createVinUploadTracking(any[DraftId], any[Option[Boolean]], any[String], any[String])(using any[HeaderCarrier])
      }
    }

    "must render a fresh upload form, not redirect, when the in-progress upload is a different kind (spreadsheet)" in {
      val connector = connectorReturning(summaryResult =
        Right(GetFileUploadSummaryResponse(fileStatus = "VALIDATING", failureReason = None, fileName = None, kind = "SPREADSHEET"))
      )
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual OK
        verify(connector).createVinUploadTracking(eqTo(draftId), eqTo(None), any[String], any[String])(using any[HeaderCarrier])
      }
    }

    "must render the error upscan reported when it redirected the user back here" in {
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers))

      running(application) {
        val request = FakeRequest(GET, s"$supplierRoute?errorCode=EntityTooLarge")
        val result  = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include(messages(application)("uploadVinNumbers.error.tooLarge"))
      }
    }

    "must delete the existing VIN upload before starting a fresh one when restart is requested" in {
      val connector   = connectorReturning(summaryResult = Left(GetFileUploadSummaryError.NotFound))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, supplierRestartRoute)).value

        status(result) mustEqual OK
        verify(connector).deleteVinUpload(eqTo(draftId))(using any[HeaderCarrier])
      }
    }

    "must not delete anything when restart is not requested" in {
      val connector   = connectorReturning()
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK

        verify(connector, never).deleteVinUpload(any[DraftId])(using any[HeaderCarrier])
      }
    }

    "must redirect to Journey Recovery when the backend cannot start the upload" in {
      val connector   = connectorReturning(createResult = Left(CreateVinUploadTrackingError.UpstreamError(502, "upscan unavailable")))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when the vehicle was not marked as 'more than one'" in {
      val answers     = supplierAnswers.unsafeSet(NumberOfVehiclesPage(v), NumberOfVehicles(OneMultiVehicle.One, None))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answers))

      running(application) {
        val result = route(application, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when draftId is missing" in {
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers.remove(DraftIdPage).success.value))

      running(application) {
        val result = route(application, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

  }

  "UploadVinNumbersController.importOnPageLoad" - {

    "must return OK and render the upload form" in {
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers))

      running(application) {
        val result = route(application, FakeRequest(GET, importRoute)).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Unauthorised when the vehicle was brought from the EU" in {
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers.unsafeSet(VehicleFromEuPage, true)))

      running(application) {
        val result = route(application, FakeRequest(GET, importRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
