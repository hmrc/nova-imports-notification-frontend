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
import connectors.{GetDraftNotificationError, NovaImportsBackendConnector, UpdateSectionError}
import controllers.{routes, vehicledetails}
import models.{AddVehicleDetailsCar, AddVehicleType, DraftId, DraftNotification, DraftNotificationSection, ImportNumber, NumberOfVehicles, OneMultiVehicle, SupplierNumber, UserAnswers, VehicleNumber}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.*
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

class CheckVinNumbersControllerSpec extends SpecBase with MockitoSugar {

  private val s = SupplierNumber(1)
  private val i = ImportNumber(1)
  private val v = VehicleNumber(1)

  private lazy val supplierRoute = vehicledetails.routes.CheckVinNumbersController.supplierOnPageLoad(s, v, page = 1).url
  private lazy val importRoute   = vehicledetails.routes.CheckVinNumbersController.importOnPageLoad(i, v, page = 1).url

  private val draftId    = DraftId("DRAFT-001")
  private val carDetails = AddVehicleDetailsCar("Land Rover", "Discovery 4", "3.0TD", "XS", "station wagon")
  private val vins       = Seq("VIN1", "VIN2")

  private val supplierBase: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, draftId)
    .unsafeSet(DraftVersionIdPage, 3L)
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
    .unsafeSet(NumberOfVehiclesPage(v), NumberOfVehicles(OneMultiVehicle.More, Some(2)))
    .unsafeSet(AddVehicleTypePage(v), AddVehicleType.Car)
    .unsafeSet(AddVehicleDetailsCarPage(v), carDetails)

  private val importBase: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, draftId)
    .unsafeSet(DraftVersionIdPage, 3L)
    .unsafeSet(VehicleFromEuPage, false)
    .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A")))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1)))
    .unsafeSet(NumberOfVehiclesPage(v), NumberOfVehicles(OneMultiVehicle.More, Some(2)))
    .unsafeSet(AddVehicleTypePage(v), AddVehicleType.Car)
    .unsafeSet(AddVehicleDetailsCarPage(v), carDetails)

  private val supplierWithVins: UserAnswers = supplierBase.unsafeSet(ValidatedVinListPage, vins)
  private val importWithVins: UserAnswers   = importBase.unsafeSet(ValidatedVinListPage, vins)

  private def baseVehicleSections(
    journey: String,
    groupNumber: Int,
    vehicleNumber: Int,
    additionalInformation: Boolean
  ): Map[String, DraftNotificationSection] = {
    val sections = Map(
      s"$journey/$groupNumber/vehicle/$vehicleNumber/type"    -> DraftNotificationSection(Some(Json.obj("vehicleType" -> "CAR"))),
      s"$journey/$groupNumber/vehicle/$vehicleNumber/details" -> DraftNotificationSection(
        Some(Json.obj("make" -> "Land Rover", "model" -> "Discovery 4"))
      )
    )
    if (additionalInformation)
      sections + (s"$journey/$groupNumber/vehicle/$vehicleNumber/additional-information" -> DraftNotificationSection(
        Some(Json.obj("vehicleIdNumber" -> "OLD"))
      ))
    else sections
  }

  private def draftNotification(sections: Map[String, DraftNotificationSection]): DraftNotification =
    DraftNotification(draftId = draftId.value, createdDate = "2026-01-01", lastUpdatedDate = None, sections = sections, versionId = Some(3L))

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

  private def successfulConnector(sections: Map[String, DraftNotificationSection]): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getDraftNotification(any[DraftId])(using any[HeaderCarrier])).thenReturn(Future.successful(Right(draftNotification(sections))))
    when(connector.updateDraftSection(any[DraftId], any[String], any[JsObject])(using any[HeaderCarrier])).thenReturn(Future.successful(Right(4L)))
    when(connector.replaceVehicleSections(any[DraftId], any[Map[String, JsObject]], any[Long])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(5L)))
    connector
  }

  "CheckVinNumbersController.supplierOnPageLoad" - {

    "must return OK with the saved VINs, paginated, and the vehicle type label" in {
      val app = application(Some(supplierWithVins))

      running(app) {
        val result = route(app, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include("VIN1")
        contentAsString(result) must include("VIN2")
        contentAsString(result) must include(messages(app)("addVehicleType.radio.car"))
        contentAsString(result) must include(s"""action="${vehicledetails.routes.CheckVinNumbersController.supplierOnSubmit(s, v).url}"""")
        contentAsString(result) must include(
          s"""href="${vehicledetails.routes.VehicleIdentificationNumbersController.supplierOnPageLoad(s, v).url}""""
        )
      }
    }

    "must redirect to Journey Recovery when there is no validated VIN list in session" in {
      val app = application(Some(supplierBase))

      running(app) {
        val result = route(app, FakeRequest(GET, supplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when the vehicle does not belong to the supplier" in {
      val app = application(Some(supplierWithVins))

      running(app) {
        val result =
          route(app, FakeRequest(GET, vehicledetails.routes.CheckVinNumbersController.supplierOnPageLoad(SupplierNumber(2), v, page = 1).url)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }

  "CheckVinNumbersController.importOnPageLoad" - {

    "must return OK" in {
      val app = application(Some(importWithVins))

      running(app) {
        val result = route(app, FakeRequest(GET, importRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include(s"""href="${vehicledetails.routes.VehicleIdentificationNumbersController.importOnPageLoad(i, v).url}"""")
      }
    }
  }

  "CheckVinNumbersController.supplierOnSubmit" - {

    "must save the multiple-vehicles section, fan the base vehicle out to one section per VIN, and redirect" in {
      val connector  = successfulConnector(baseVehicleSections("supplier", 1, 1, additionalInformation = false))
      val repository = stubSessionRepository(supplierWithVins)
      val app        = application(Some(supplierWithVins), connector, repository)

      running(app) {
        val result = route(app, FakeRequest(POST, vehicledetails.routes.CheckVinNumbersController.supplierOnSubmit(s, v).url)).value

        status(result) mustEqual SEE_OTHER

        val multipleVehiclesCaptor = ArgumentCaptor.forClass(classOf[JsObject])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("supplier/1/vehicle/1/multiple-vehicles"), multipleVehiclesCaptor.capture())(using
          any[HeaderCarrier]
        )
        multipleVehiclesCaptor.getValue mustEqual Json.obj("numberOfVehiclesToMultiply" -> 2, "versionId" -> 3L)

        val sectionsCaptor = ArgumentCaptor.forClass(classOf[Map[String, JsObject]])
        verify(connector).replaceVehicleSections(eqTo(draftId), sectionsCaptor.capture(), eqTo(4L))(using any[HeaderCarrier])
        val sections = sectionsCaptor.getValue
        sections.keySet mustEqual Set(
          "supplier/1/vehicle/1/type",
          "supplier/1/vehicle/1/details",
          "supplier/1/vehicle/1/additional-information",
          "supplier/1/vehicle/2/type",
          "supplier/1/vehicle/2/details",
          "supplier/1/vehicle/2/additional-information"
        )
        (sections("supplier/1/vehicle/1/additional-information") \ "vehicleIdNumber").as[String] mustEqual "VIN1"
        (sections("supplier/1/vehicle/1/additional-information") \ "confirmVehicleIdNumber").as[String] mustEqual "VIN1"
        (sections("supplier/1/vehicle/1/additional-information") \ "oneOrMultipleVehicls").as[String] mustEqual "Multiple"
        (sections("supplier/1/vehicle/2/additional-information") \ "vehicleIdNumber").as[String] mustEqual "VIN2"
        sections("supplier/1/vehicle/1/type") mustEqual Json.obj("vehicleType" -> "CAR")

        val answersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(repository).setPage(answersCaptor.capture(), eqTo(DraftVersionIdPage), eqTo(5L))(any())
        answersCaptor.getValue.get(ValidatedVinListPage) mustEqual Some(vins)
      }
    }

    "must carry forward the supplier's own details and any other group untouched, not just the fanned-out vehicle" in {
      val existingSections = baseVehicleSections("supplier", 1, 1, additionalInformation = false) ++ Map(
        "supplier/1/details"        -> DraftNotificationSection(Some(Json.obj("supplierBusinessName" -> "EuroBike Imports SARL"))),
        "supplier/2/details"        -> DraftNotificationSection(Some(Json.obj("supplierBusinessName" -> "Other Supplier"))),
        "supplier/2/vehicle/3/type" -> DraftNotificationSection(Some(Json.obj("vehicleType" -> "LCV"))),
        "import/1/details"          -> DraftNotificationSection(Some(Json.obj("importEntryNumber" -> "123456789A")))
      )
      val connector  = successfulConnector(existingSections)
      val repository = stubSessionRepository(supplierWithVins)
      val app        = application(Some(supplierWithVins), connector, repository)

      running(app) {
        val result = route(app, FakeRequest(POST, vehicledetails.routes.CheckVinNumbersController.supplierOnSubmit(s, v).url)).value

        status(result) mustEqual SEE_OTHER

        val sectionsCaptor = ArgumentCaptor.forClass(classOf[Map[String, JsObject]])
        verify(connector).replaceVehicleSections(eqTo(draftId), sectionsCaptor.capture(), any[Long])(using any[HeaderCarrier])
        val sections = sectionsCaptor.getValue

        sections("supplier/1/details") mustEqual Json.obj("supplierBusinessName" -> "EuroBike Imports SARL")
        sections("supplier/2/details") mustEqual Json.obj("supplierBusinessName" -> "Other Supplier")
        sections("supplier/2/vehicle/3/type") mustEqual Json.obj("vehicleType" -> "LCV")
        sections.keySet must not contain "import/1/details"
      }
    }

    "must still fan out correctly when the base vehicle has no additional-information section yet" in {
      val connector  = successfulConnector(baseVehicleSections("supplier", 1, 1, additionalInformation = false))
      val repository = stubSessionRepository(supplierWithVins)
      val app        = application(Some(supplierWithVins), connector, repository)

      running(app) {
        val result = route(app, FakeRequest(POST, vehicledetails.routes.CheckVinNumbersController.supplierOnSubmit(s, v).url)).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "must redirect to There is a problem when the base vehicle sections cannot be found" in {
      val connector  = successfulConnector(Map.empty)
      val repository = mock[SessionRepository]
      val app        = application(Some(supplierWithVins), connector, repository)

      running(app) {
        val result = route(app, FakeRequest(POST, vehicledetails.routes.CheckVinNumbersController.supplierOnSubmit(s, v).url)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(connector, never).replaceVehicleSections(any[DraftId], any[Map[String, JsObject]], any[Long])(using any[HeaderCarrier])
      }
    }

    "must redirect to There is a problem when saving the multiple-vehicles section fails" in {
      val connector = mock[NovaImportsBackendConnector]
      when(connector.getDraftNotification(any[DraftId])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(draftNotification(baseVehicleSections("supplier", 1, 1, additionalInformation = false)))))
      when(connector.updateDraftSection(any[DraftId], any[String], any[JsObject])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(500, "boom"))))
      val app = application(Some(supplierWithVins), connector)

      running(app) {
        val result = route(app, FakeRequest(POST, vehicledetails.routes.CheckVinNumbersController.supplierOnSubmit(s, v).url)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(connector, never).replaceVehicleSections(any[DraftId], any[Map[String, JsObject]], any[Long])(using any[HeaderCarrier])
      }
    }

    "must redirect to There is a problem when saving the fanned-out sections fails" in {
      val connector = mock[NovaImportsBackendConnector]
      when(connector.getDraftNotification(any[DraftId])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(draftNotification(baseVehicleSections("supplier", 1, 1, additionalInformation = false)))))
      when(connector.updateDraftSection(any[DraftId], any[String], any[JsObject])(using any[HeaderCarrier])).thenReturn(Future.successful(Right(4L)))
      when(connector.replaceVehicleSections(any[DraftId], any[Map[String, JsObject]], any[Long])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(500, "boom"))))
      val app = application(Some(supplierWithVins), connector)

      running(app) {
        val result = route(app, FakeRequest(POST, vehicledetails.routes.CheckVinNumbersController.supplierOnSubmit(s, v).url)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to There is a problem when retrieving the draft notification fails" in {
      val connector = mock[NovaImportsBackendConnector]
      when(connector.getDraftNotification(any[DraftId])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(GetDraftNotificationError.UpstreamError(502, "boom"))))
      val app = application(Some(supplierWithVins), connector)

      running(app) {
        val result = route(app, FakeRequest(POST, vehicledetails.routes.CheckVinNumbersController.supplierOnSubmit(s, v).url)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to There is a problem without calling the backend when there is no validated VIN list in session" in {
      val connector = mock[NovaImportsBackendConnector]
      val app       = application(Some(supplierBase), connector)

      running(app) {
        val result = route(app, FakeRequest(POST, vehicledetails.routes.CheckVinNumbersController.supplierOnSubmit(s, v).url)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(connector, never).getDraftNotification(any[DraftId])(using any[HeaderCarrier])
      }
    }
  }

  "CheckVinNumbersController.importOnSubmit" - {

    "must save the multiple-vehicles section under the import path and fan out, then redirect" in {
      val connector  = successfulConnector(baseVehicleSections("import", 1, 1, additionalInformation = false))
      val repository = stubSessionRepository(importWithVins)
      val app        = application(Some(importWithVins), connector, repository)

      running(app) {
        val result = route(app, FakeRequest(POST, vehicledetails.routes.CheckVinNumbersController.importOnSubmit(i, v).url)).value

        status(result) mustEqual SEE_OTHER
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("import/1/vehicle/1/multiple-vehicles"), any[JsObject])(using any[HeaderCarrier])

        val sectionsCaptor = ArgumentCaptor.forClass(classOf[Map[String, JsObject]])
        verify(connector).replaceVehicleSections(eqTo(draftId), sectionsCaptor.capture(), any[Long])(using any[HeaderCarrier])
        (sectionsCaptor.getValue.apply("import/1/vehicle/1/additional-information") \ "confirmVehicleIdNumber").as[String] mustEqual "VIN1"
        (sectionsCaptor.getValue.apply("import/1/vehicle/1/additional-information") \ "oneOrMultipleVehicls").as[String] mustEqual "Multiple"
      }
    }
  }
}
