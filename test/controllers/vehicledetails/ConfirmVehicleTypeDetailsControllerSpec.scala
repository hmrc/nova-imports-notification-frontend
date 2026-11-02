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
import connectors.{NovaImportsBackendConnector, UpdateSectionError}
import controllers.{routes, vehicledetails}
import models.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.vehicledetails.*
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ConfirmVehicleTypeDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val s = SupplierNumber(1)
  private val i = ImportNumber(1)
  private val v = VehicleNumber(1)

  private lazy val supplierRoute = vehicledetails.routes.ConfirmVehicleTypeDetailsController.supplierOnPageLoad(s, v).url
  private lazy val importRoute   = vehicledetails.routes.ConfirmVehicleTypeDetailsController.importOnPageLoad(i, v).url

  private val supplierBase: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(DraftVersionIdPage, 3L)
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))

  private val importBase: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(DraftVersionIdPage, 3L)
    .unsafeSet(VehicleFromEuPage, false)
    .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A")))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1)))

  private val carDetails     = AddVehicleDetailsCar("Land Rover", "Discovery 4", "3.0TD", "XS", "station wagon")
  private val lcvDetails     = LightCommercialVehicleDetails("Ford", "Transit", "2.0 TDCi", "L2 H2", "panel van")
  private val hcvDetails     = AddVehicleDetailsHeavyCommercial("Renault Trucks", "Magnum", "midlift axle tractor", "sleeper cab")
  private val caravanDetails = AddVehicleDetailsMotorCaravan("Swift", "Coachbuilt", "Voyager", "Voyager 685FB 4 berth", "Fiat", "2.3TD")

  private val carSupplier: UserAnswers =
    supplierBase.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Car).unsafeSet(AddVehicleDetailsCarPage(v), carDetails)

  private val carImport: UserAnswers =
    importBase.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Car).unsafeSet(AddVehicleDetailsCarPage(v), carDetails)

  private val lcvSupplier: UserAnswers =
    supplierBase.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Lcv).unsafeSet(AddVehicleDetailsLightCommercialPage(v), lcvDetails)

  private val hcvSupplier: UserAnswers =
    supplierBase.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Hcv).unsafeSet(AddVehicleDetailsHeavyCommercialPage(v), hcvDetails)

  private val caravanSupplier: UserAnswers =
    supplierBase.unsafeSet(AddVehicleTypePage(v), AddVehicleType.MotorCaravan).unsafeSet(AddVehicleDetailsMotorCaravanPage(v), caravanDetails)

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

  "ConfirmVehicleTypeDetailsController" - {

    "supplierOnPageLoad" - {

      "must return OK with the heading, make/model/derivative/trim/body type rows and a Confirm details button posting to the supplier submit" in {
        val app = application(Some(carSupplier))

        running(app) {
          val result = route(app, FakeRequest(GET, supplierRoute)).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include(messages(app)("confirmVehicleTypeDetails.heading"))
          body must include(messages(app)("site.confirmDetails"))
          body must include(s"""action="${vehicledetails.routes.ConfirmVehicleTypeDetailsController.supplierOnSubmit(s, v).url}"""")

          body must include(messages(app)("confirmVehicleTypeDetails.make.label"))
          body must include("Land Rover")
          body must include(messages(app)("confirmVehicleTypeDetails.trim.label"))
          body must include("XS")
          body must include(vehicledetails.routes.AddVehicleDetailsCarController.supplierOnPageLoad(s, v, CheckMode).url)
        }
      }

      "must show the hcvType value under the Body type label and a Cab type row, with no Trim or Derivative rows, for a heavy commercial vehicle" in {
        val app = application(Some(hcvSupplier))

        running(app) {
          val body = contentAsString(route(app, FakeRequest(GET, supplierRoute)).value)

          body must include(messages(app)("confirmVehicleTypeDetails.bodyType.label"))
          body must include("midlift axle tractor")
          body must include(messages(app)("confirmVehicleTypeDetails.cabType.label"))
          body must include("sleeper cab")
          body must not include messages(app)("confirmVehicleTypeDetails.trim.label")
        }
      }

      "must show the caravan make, model name or number and version rows for a motor caravan" in {
        val app = application(Some(caravanSupplier))

        running(app) {
          val body = contentAsString(route(app, FakeRequest(GET, supplierRoute)).value)

          body must include(messages(app)("confirmVehicleTypeDetails.make.label"))
          body must include("Swift")
          body must include(messages(app)("confirmVehicleTypeDetails.modelNameNumber.label"))
          body must include("Voyager")
          body must include(messages(app)("confirmVehicleTypeDetails.makeOfBaseVehicle.label"))
          body must include("Fiat")
        }
      }

      "must redirect to Unauthorised when draftId is missing" in {
        mustBeUnauthorised(application(Some(carSupplier.remove(DraftIdPage).success.value)), supplierRoute)
      }

      "must redirect to Unauthorised when the vehicle was not brought from the EU" in {
        mustBeUnauthorised(application(Some(carSupplier.unsafeSet(VehicleFromEuPage, false))), supplierRoute)
      }

      "must redirect to Unauthorised when the vehicle does not belong to the supplier" in {
        mustBeUnauthorised(
          application(Some(carSupplier)),
          vehicledetails.routes.ConfirmVehicleTypeDetailsController.supplierOnPageLoad(SupplierNumber(2), v).url
        )
      }

      "must redirect to Unauthorised when the vehicle type is missing" in {
        mustBeUnauthorised(application(Some(carSupplier.remove(AddVehicleTypePage(v)).success.value)), supplierRoute)
      }

      "must redirect to Unauthorised when the vehicle type details for the selected vehicle type are missing" in {
        mustBeUnauthorised(application(Some(carSupplier.remove(AddVehicleDetailsCarPage(v)).success.value)), supplierRoute)
      }
    }

    "importOnPageLoad" - {

      "must return OK and post to the import submit" in {
        val app = application(Some(carImport))

        running(app) {
          val result = route(app, FakeRequest(GET, importRoute)).value

          status(result) mustEqual OK
          contentAsString(result) must include(s"""action="${vehicledetails.routes.ConfirmVehicleTypeDetailsController.importOnSubmit(i, v).url}"""")
        }
      }

      "must redirect to Unauthorised when the vehicle was brought from the EU" in {
        mustBeUnauthorised(application(Some(carImport.unsafeSet(VehicleFromEuPage, true))), importRoute)
      }

      "must redirect to Unauthorised when the vehicle does not belong to the import" in {
        mustBeUnauthorised(
          application(Some(carImport)),
          vehicledetails.routes.ConfirmVehicleTypeDetailsController.importOnPageLoad(ImportNumber(2), v).url
        )
      }
    }

    "supplierOnSubmit" - {

      "must call F4 with the car fields, save the new version and redirect" in {
        val connector  = successfulConnector
        val repository = stubSessionRepository(carSupplier)
        val app        = application(Some(carSupplier), connector, repository)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url

          capturedBody(connector, "supplier/1/vehicle/1/details") mustEqual Json.obj(
            "make"       -> "Land Rover",
            "model"      -> "Discovery 4",
            "derivative" -> "3.0TD",
            "trim"       -> "XS",
            "bodyType"   -> "station wagon",
            "versionId"  -> 3L
          )
          verify(repository).setPage(any(), eqTo(DraftVersionIdPage), eqTo(4L))(any())
        }
      }

      "must redirect to Notification reference AVD9.0 when amending a submitted notification" in {
        val connector  = successfulConnector
        val answers    = carSupplier.unsafeSet(AmendSubmittedNotificationPage, true)
        val repository = stubSessionRepository(answers)
        val app        = application(Some(answers), connector, repository)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            vehicledetails.routes.NotificationReferenceController.supplierOnPageLoad(s, v, NormalMode).url
        }
      }

      "must call F4 with the light commercial vehicle body type under both bodyType and lcvBodyType" in {
        val connector = successfulConnector
        val app       = application(Some(lcvSupplier), connector, stubSessionRepository(lcvSupplier))

        running(app) {
          status(route(app, FakeRequest(POST, supplierRoute)).value) mustEqual SEE_OTHER

          capturedBody(connector, "supplier/1/vehicle/1/details") mustEqual Json.obj(
            "make"        -> "Ford",
            "model"       -> "Transit",
            "derivative"  -> "2.0 TDCi",
            "trim"        -> "L2 H2",
            "bodyType"    -> "panel van",
            "lcvBodyType" -> "panel van",
            "versionId"   -> 3L
          )
        }
      }

      "must call F4 with the hcvType and cabType fields for a heavy commercial vehicle" in {
        val connector = successfulConnector
        val app       = application(Some(hcvSupplier), connector, stubSessionRepository(hcvSupplier))

        running(app) {
          status(route(app, FakeRequest(POST, supplierRoute)).value) mustEqual SEE_OTHER

          capturedBody(connector, "supplier/1/vehicle/1/details") mustEqual Json.obj(
            "make"      -> "Renault Trucks",
            "model"     -> "Magnum",
            "hcvType"   -> "midlift axle tractor",
            "cabType"   -> "sleeper cab",
            "versionId" -> 3L
          )
        }
      }

      "must call F4 with the caravan-specific field names for a motor caravan" in {
        val connector = successfulConnector
        val app       = application(Some(caravanSupplier), connector, stubSessionRepository(caravanSupplier))

        running(app) {
          status(route(app, FakeRequest(POST, supplierRoute)).value) mustEqual SEE_OTHER

          capturedBody(connector, "supplier/1/vehicle/1/details") mustEqual Json.obj(
            "caravanMake"       -> "Swift",
            "modelNameNumber"   -> "Voyager",
            "caravanVersion"    -> "Voyager 685FB 4 berth",
            "caravanBody"       -> "Coachbuilt",
            "makeOfBaseVehicle" -> "Fiat",
            "derivative"        -> "2.3TD",
            "versionId"         -> 3L
          )
        }
      }

      "must call F4 with caravanBody N/A when no motor caravan body type was given" in {
        val answers =
          caravanSupplier.unsafeSet(AddVehicleDetailsMotorCaravanPage(v), caravanDetails.copy(bodyType = AddVehicleDetailsMotorCaravan.NoBodyType))
        val connector = successfulConnector
        val app       = application(Some(answers), connector, stubSessionRepository(answers))

        running(app) {
          status(route(app, FakeRequest(POST, supplierRoute)).value) mustEqual SEE_OTHER

          capturedBody(connector, "supplier/1/vehicle/1/details") mustEqual Json.obj(
            "caravanMake"       -> "Swift",
            "modelNameNumber"   -> "Voyager",
            "caravanVersion"    -> "Voyager 685FB 4 berth",
            "caravanBody"       -> "N/A",
            "makeOfBaseVehicle" -> "Fiat",
            "derivative"        -> "2.3TD",
            "versionId"         -> 3L
          )
        }
      }

      "must redirect to There is a problem when F4 fails" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(500, "error"))))
        val repository = mock[SessionRepository]
        val app        = application(Some(carSupplier), connector, repository)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(repository, never()).setPage(any(), any(), any())(any())
        }
      }

      "must redirect to There is a problem without calling F4 when the version id is missing" in {
        val connector = mock[NovaImportsBackendConnector]
        val app       = application(Some(carSupplier.remove(DraftVersionIdPage).success.value), connector)

        running(app) {
          val result = route(app, FakeRequest(POST, supplierRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(connector, never()).updateDraftSection(any(), any(), any())(any[HeaderCarrier])
        }
      }
    }

    "importOnSubmit" - {

      "must call F4 for the import vehicle details section and redirect" in {
        val connector = successfulConnector
        val app       = application(Some(carImport), connector, stubSessionRepository(carImport))

        running(app) {
          val result = route(app, FakeRequest(POST, importRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url

          capturedBody(connector, "import/1/vehicle/1/details") mustEqual Json.obj(
            "make"       -> "Land Rover",
            "model"      -> "Discovery 4",
            "derivative" -> "3.0TD",
            "trim"       -> "XS",
            "bodyType"   -> "station wagon",
            "versionId"  -> 3L
          )
        }
      }

      "must redirect to Notification reference AVD9.0 when amending a submitted notification" in {
        val connector  = successfulConnector
        val answers    = carImport.unsafeSet(AmendSubmittedNotificationPage, true)
        val repository = stubSessionRepository(answers)
        val app        = application(Some(answers), connector, repository)

        running(app) {
          val result = route(app, FakeRequest(POST, importRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            vehicledetails.routes.NotificationReferenceController.importOnPageLoad(i, v, NormalMode).url
        }
      }

      "must redirect to There is a problem when F4 fails" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(500, "error"))))
        val app = application(Some(carImport), connector)

        running(app) {
          val result = route(app, FakeRequest(POST, importRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
