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
import connectors.{NovaImportsBackendConnector, ValidateVinsError}
import controllers.{routes, vehicledetails}
import models.responses.{UploadResultResponse, ValidateVinsResponse, ValidationError}
import models.{DraftId, DraftNotification, DraftNotificationSection, ImportNumber, NormalMode, NumberOfVehicles, OneMultiVehicle, SupplierNumber, UserAnswers, VehicleNumber}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.NumberOfVehiclesPage
import pages.{DraftIdPage, DraftVersionIdPage, ValidatedVinListPage}
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class EnterVinNumbersControllerSpec extends SpecBase with MockitoSugar {

  private val s = SupplierNumber(1)
  private val i = ImportNumber(1)
  private val v = VehicleNumber(1)

  private lazy val supplierRoute = vehicledetails.routes.EnterVinNumbersController.supplierOnPageLoad(s, v, NormalMode).url
  private lazy val importRoute   = vehicledetails.routes.EnterVinNumbersController.importOnPageLoad(i, v, NormalMode).url

  private val draftId = DraftId("DRAFT-001")

  private val supplierAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, draftId)
    .unsafeSet(DraftVersionIdPage, 3L)
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
    .unsafeSet(NumberOfVehiclesPage(v), NumberOfVehicles(OneMultiVehicle.More, Some(2)))

  private val importAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, draftId)
    .unsafeSet(DraftVersionIdPage, 3L)
    .unsafeSet(VehicleFromEuPage, false)
    .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A")))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1)))
    .unsafeSet(NumberOfVehiclesPage(v), NumberOfVehicles(OneMultiVehicle.More, Some(2)))

  private def uploadResult(fileStatus: String, rawVinContent: Option[String] = None): UploadResultResponse =
    UploadResultResponse(
      fileStatus = fileStatus,
      validationType = None,
      vehicles = Seq.empty,
      euVehicles = Seq.empty,
      nonEuVehicles = Seq.empty,
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
      errors = Seq.empty,
      rawVinContent = rawVinContent,
      kind = "VIN"
    )

  private def draftNotification(sections: Map[String, DraftNotificationSection] = Map.empty): DraftNotification =
    DraftNotification(draftId = draftId.value, createdDate = "2026-01-01", lastUpdatedDate = None, sections = sections, versionId = Some(1L))

  private def stubSessionRepository: SessionRepository = {
    val repository = mock[SessionRepository]
    when(repository.set(any())).thenReturn(Future.successful(true))
    when(repository.setPage(any[UserAnswers](), any(), any())(any())).thenReturn(Future.successful(supplierAnswers))
    repository
  }

  private def application(
    answers: Option[UserAnswers],
    connector: NovaImportsBackendConnector = mock[NovaImportsBackendConnector],
    repository: SessionRepository = stubSessionRepository
  ): Application =
    applicationBuilder(answers)
      .overrides(
        bind[NovaImportsBackendConnector].toInstance(connector),
        bind[SessionRepository].toInstance(repository)
      )
      .build()

  "EnterVinNumbersController.supplierOnPageLoad" - {

    "must return OK with the empty form when nothing has been uploaded" in {
      val connector = mock[NovaImportsBackendConnector]
      when(connector.getUploadResult(any[DraftId])(using any[HeaderCarrier])).thenReturn(Future.successful(Right(uploadResult("AWAITING_UPLOAD"))))
      val app = application(Some(supplierAnswers), connector)

      running(app) {
        val result = route(app, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include(
          s"""action="${vehicledetails.routes.EnterVinNumbersController.supplierOnSubmit(s, v, NormalMode).url}""""
        )
        contentAsString(result) must include(messages(app)("enterVinNumbers.heading", 2))
      }
    }

    "must pre-fill the textarea with the uploaded VIN content once extraction has finished" in {
      val connector = mock[NovaImportsBackendConnector]
      when(connector.getUploadResult(any[DraftId])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(uploadResult("UPLOADED", Some("VIN1,VIN2")))))
      val app = application(Some(supplierAnswers), connector)

      running(app) {
        val result = route(app, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include("VIN1,VIN2")
      }
    }

    "must redirect to Unauthorised when the vehicle was not marked as 'more than one'" in {
      val answers = supplierAnswers.unsafeSet(NumberOfVehiclesPage(v), NumberOfVehicles(OneMultiVehicle.One, None))
      val app     = application(Some(answers))

      running(app) {
        val result = route(app, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }

  "EnterVinNumbersController.importOnPageLoad" - {

    "must return OK" in {
      val connector = mock[NovaImportsBackendConnector]
      when(connector.getUploadResult(any[DraftId])(using any[HeaderCarrier])).thenReturn(Future.successful(Right(uploadResult("AWAITING_UPLOAD"))))
      val app = application(Some(importAnswers), connector)

      running(app) {
        val result = route(app, FakeRequest(GET, importRoute)).value

        status(result) mustEqual OK
      }
    }
  }

  "EnterVinNumbersController.supplierOnSubmit" - {

    "must validate the VINs, check for cross-notification duplicates, save them and redirect when valid" in {
      val connector  = mock[NovaImportsBackendConnector]
      val repository = stubSessionRepository
      when(connector.validateVins(any[DraftId], any[String], any[Int])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(ValidateVinsResponse(valid = true, vins = Seq("VIN1", "VIN2"), errors = Seq.empty))))
      when(connector.getDraftNotification(any[DraftId])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(draftNotification())))
      when(connector.updateDraftSection(any[DraftId], any[String], any[JsObject])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(4L)))
      val app = application(Some(supplierAnswers), connector, repository)

      running(app) {
        val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody("enterVinNumbers" -> "VIN1,VIN2")
        val result  = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.CheckVinNumbersController.supplierOnPageLoad(s, v, page = 1).url

        val sectionCaptor = ArgumentCaptor.forClass(classOf[JsObject])
        verify(connector).updateDraftSection(
          org.mockito.ArgumentMatchers.eq(draftId),
          org.mockito.ArgumentMatchers.eq("supplier/1/vehicle/1/multiple-vins"),
          sectionCaptor.capture()
        )(using any[HeaderCarrier])
        sectionCaptor.getValue mustEqual Json.obj("multipleVins" -> "VIN1, VIN2", "versionId" -> 3L)

        val answersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(repository)
          .setPage(answersCaptor.capture(), org.mockito.ArgumentMatchers.eq(DraftVersionIdPage), org.mockito.ArgumentMatchers.eq(4L))(any())
        answersCaptor.getValue.get(ValidatedVinListPage) mustEqual Some(Seq("VIN1", "VIN2"))
      }
    }

    "must re-render with the backend's per-item errors when F25/F31 validation fails" in {
      val connector = mock[NovaImportsBackendConnector]
      when(connector.validateVins(any[DraftId], any[String], any[Int])(using any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Right(ValidateVinsResponse(valid = false, vins = Seq.empty, errors = Seq(ValidationError("enterVinNumbers", "bad format", Some(1)))))
          )
        )
      val app = application(Some(supplierAnswers), connector)

      running(app) {
        val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody("enterVinNumbers" -> "not-a-vin")
        val result  = route(app, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include("bad format")
      }
    }

    "must re-render with the duplicate error when a VIN already exists elsewhere in the notification" in {
      val connector = mock[NovaImportsBackendConnector]
      when(connector.validateVins(any[DraftId], any[String], any[Int])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(ValidateVinsResponse(valid = true, vins = Seq("VIN1", "VIN2"), errors = Seq.empty))))
      when(connector.getDraftNotification(any[DraftId])(using any[HeaderCarrier])).thenReturn(
        Future.successful(
          Right(
            draftNotification(
              Map("supplier/2/vehicle/1/additional-information" -> DraftNotificationSection(Some(Json.obj("vehicleIdNumber" -> "vin1"))))
            )
          )
        )
      )
      val app = application(Some(supplierAnswers), connector)

      running(app) {
        val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody("enterVinNumbers" -> "VIN1,VIN2")
        val result  = route(app, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(app)("enterVinNumbers.error.duplicate.crossNotification"))
      }
    }

    "must not flag a duplicate against the vehicle's own group" in {
      val connector  = mock[NovaImportsBackendConnector]
      val repository = stubSessionRepository
      when(connector.validateVins(any[DraftId], any[String], any[Int])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(ValidateVinsResponse(valid = true, vins = Seq("VIN1", "VIN2"), errors = Seq.empty))))
      when(connector.getDraftNotification(any[DraftId])(using any[HeaderCarrier])).thenReturn(
        Future.successful(
          Right(
            draftNotification(
              Map("supplier/1/vehicle/1/additional-information" -> DraftNotificationSection(Some(Json.obj("vehicleIdNumber" -> "vin1"))))
            )
          )
        )
      )
      when(connector.updateDraftSection(any[DraftId], any[String], any[JsObject])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(4L)))
      val app = application(Some(supplierAnswers), connector, repository)

      running(app) {
        val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody("enterVinNumbers" -> "VIN1,VIN2")
        val result  = route(app, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "must return BAD_REQUEST without calling the backend when the textarea is empty" in {
      val connector = mock[NovaImportsBackendConnector]
      val app       = application(Some(supplierAnswers), connector)

      running(app) {
        val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody("enterVinNumbers" -> "")
        val result  = route(app, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages(app)("enterVinNumbers.error.required"))
      }
    }

    "must redirect to Journey Recovery when validateVins fails upstream" in {
      val connector = mock[NovaImportsBackendConnector]
      when(connector.validateVins(any[DraftId], any[String], any[Int])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ValidateVinsError.UpstreamError(502, "boom"))))
      val app = application(Some(supplierAnswers), connector)

      running(app) {
        val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody("enterVinNumbers" -> "VIN1,VIN2")
        val result  = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "EnterVinNumbersController.importOnSubmit" - {

    "must save the VINs and redirect to the import journey's check page" in {
      val connector  = mock[NovaImportsBackendConnector]
      val repository = stubSessionRepository
      when(connector.validateVins(any[DraftId], any[String], any[Int])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(ValidateVinsResponse(valid = true, vins = Seq("VIN1", "VIN2"), errors = Seq.empty))))
      when(connector.getDraftNotification(any[DraftId])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(draftNotification())))
      when(connector.updateDraftSection(any[DraftId], any[String], any[JsObject])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(4L)))
      val app = application(Some(importAnswers), connector, repository)

      running(app) {
        val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody("enterVinNumbers" -> "VIN1,VIN2")
        val result  = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.CheckVinNumbersController.importOnPageLoad(i, v, page = 1).url
        verify(connector).updateDraftSection(
          org.mockito.ArgumentMatchers.eq(draftId),
          org.mockito.ArgumentMatchers.eq("import/1/vehicle/1/multiple-vins"),
          any[JsObject]
        )(using any[HeaderCarrier])
      }
    }
  }
}
