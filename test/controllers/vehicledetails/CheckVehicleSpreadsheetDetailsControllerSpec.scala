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
import connectors.{GetUploadResultError, NovaImportsBackendConnector, UpdateSectionError}
import controllers.actions.*
import controllers.vehicledetails
import models.responses.{DeleteFileUploadResponse, SpreadsheetEuVehicle, SpreadsheetNonEuVehicle, UploadResultResponse, ValidationError, VehicleSummary}
import models.{DraftId, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

class CheckVehicleSpreadsheetDetailsControllerSpec extends SpecBase with MockitoSugar {

  private lazy val onSubmitRoute = vehicledetails.routes.CheckVehicleSpreadsheetDetailsController.onSubmit().url

  private val draftId    = DraftId("DRAFT-001")
  private val versionId  = 5L
  private val newVersion = 6L

  private val answers: UserAnswers =
    emptyUserAnswers
      .unsafeSet(DraftIdPage, draftId)
      .unsafeSet(VehicleFromEuPage, true)
      .unsafeSet(DraftVersionIdPage, versionId)

  private val fullVehicle = SpreadsheetEuVehicle(
    itemNumber = Some(1),
    supplierBusinessPrivate = Some("business"),
    supplierBusinessName = Some("business name"),
    supplierTitle = None,
    supplierFirstName = None,
    supplierLastName = None,
    addressLine1 = Some("Address 1"),
    addressLine2 = Some("Address 2"),
    addressLine3 = None,
    addressLine4 = None,
    addressLine5 = None,
    postcode = None,
    country = None,
    supplierVatRegistered = Some(false),
    euMemberState = None,
    supplierVatNumber = None,
    knownDateFirstRegistered = Some(false),
    purchaseInvoice = Some(true),
    purchaseInvoiceDate = Some(LocalDate.of(2026, 3, 30)),
    purchaseInvoiceNumber = Some("1"),
    pricePaid = Some(BigDecimal("100")),
    currency = Some("USD"),
    make = Some("Make"),
    model = Some("Model"),
    derivative = Some("Derivative"),
    trim = Some("Trim"),
    bodyType = Some("Body type"),
    vin = Some("123"),
    dateArrivedInUk = Some(LocalDate.of(2026, 3, 30)),
    mileage = Some("100000"),
    mileageUnits = Some("KM"),
    leftOrRightHandDrive = Some("RHD"),
    totalValueOfOptions = Some(BigDecimal("10000")),
    obtainedFromUnableToReclaimVat = Some(false),
    soldUnderMarginScheme = Some(false),
    claimingVatRelief = Some(false)
  )

  private val fullNonEuVehicle = SpreadsheetNonEuVehicle(
    itemNumber = Some(1),
    importEntryNumber = Some("123-123456A"),
    importEntryDate = Some(LocalDate.of(2026, 3, 30)),
    knownDateFirstRegistered = Some(true),
    countryOfFirstRegistration = None,
    dateOfFirstRegistration = Some(LocalDate.of(2010, 1, 1)),
    make = Some("Make"),
    model = Some("Model"),
    derivative = Some("Derivative"),
    trim = Some("Trim"),
    bodyType = Some("Light commercial vehicle body type"),
    notificationReference = None,
    vin = Some("1"),
    dateArrivedInUk = Some(LocalDate.of(2026, 3, 1)),
    commodityCode = Some("1234"),
    mileage = Some("10000"),
    mileageUnits = Some("MILES"),
    leftOrRightHandDrive = Some("RHD"),
    pricePaid = Some(BigDecimal("100")),
    currency = Some("USD"),
    claimingVatRelief = Some(false),
    reasonForClaimingRelief = None
  )

  private def validatedResult(vehicles: Seq[SpreadsheetEuVehicle], validationType: Option[String] = Some("CarsEu")) =
    UploadResultResponse(
      fileStatus = "VALIDATED",
      validationType = validationType,
      vehicles = vehicles.map(v => VehicleSummary(v.itemNumber, v.vin, v.make, v.model)),
      euVehicles = vehicles,
      nonEuVehicles = Seq.empty,
      errors = Seq.empty
    )

  private def validatedNonEuResult(vehicles: Seq[SpreadsheetNonEuVehicle], validationType: Option[String] = Some("LightCommercialVehiclesNonEu")) =
    UploadResultResponse(
      fileStatus = "VALIDATED",
      validationType = validationType,
      vehicles = vehicles.map(v => VehicleSummary(v.itemNumber, v.vin, v.make, v.model)),
      euVehicles = Seq.empty,
      nonEuVehicles = vehicles,
      errors = Seq.empty
    )

  private def stubSessionRepository(): SessionRepository = {
    val repo = mock[SessionRepository]
    when(repo.setPage(any(), any(), any())(using any())).thenReturn(Future.successful(answers))
    repo
  }

  private def stubConnector(): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.updateDraftSection(eqTo(draftId), any[String], any[JsObject])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(newVersion)))
    when(connector.deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(DeleteFileUploadResponse(success = true))))
    connector
  }

  private def applicationFor(
    userAnswers: Option[UserAnswers],
    connector: NovaImportsBackendConnector,
    sessionRepository: SessionRepository = stubSessionRepository()
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeVatTraderIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[NovaImportsBackendConnector].toInstance(connector),
        bind[SessionRepository].toInstance(sessionRepository)
      )
      .build()

  "CheckVehicleSpreadsheetDetailsController.onSubmit" - {

    "must save a supplier and vehicle section set for a single-vehicle CarsEu upload, then redirect on and delete the upload record" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle)))))

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(Some(answers), connector, sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.NotificationTaskListController.onPageLoad().url

        verify(connector).updateDraftSection(eqTo(draftId), eqTo("supplier/1/details"), any[JsObject])(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("supplier/1/vehicle/1/type"), any[JsObject])(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("supplier/1/vehicle/1/details"), any[JsObject])(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("supplier/1/vehicle/1/additional-information"), any[JsObject])(using
          any[HeaderCarrier]
        )
        verify(sessionRepository).setPage(eqTo(answers), eqTo(DraftVersionIdPage), eqTo(newVersion))(using any())
        verify(connector).deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier])
      }
    }

    "must map the vehicle details section fields directly across" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle)))))

      val application = applicationFor(Some(answers), connector)
      val captor      = ArgumentCaptor.forClass(classOf[JsObject])

      running(application) {
        route(application, FakeRequest(POST, onSubmitRoute)).value.futureValue

        verify(connector).updateDraftSection(eqTo(draftId), eqTo("supplier/1/vehicle/1/details"), captor.capture())(using any[HeaderCarrier])

        val body = captor.getValue
        (body \ "make").as[String] mustEqual "Make"
        (body \ "model").as[String] mustEqual "Model"
        (body \ "derivative").as[String] mustEqual "Derivative"
        (body \ "trim").as[String] mustEqual "Trim"
        (body \ "bodyType").as[String] mustEqual "Body type"
      }
    }

    "must map the additional information section, including the amendment flag from session, dates in dd/MM/yyyy, and the VIN as both id fields" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle)))))

      val amendmentAnswers = answers.unsafeSet(AmendSubmittedNotificationPage, true)
      val application      = applicationFor(Some(amendmentAnswers), connector)
      val captor           = ArgumentCaptor.forClass(classOf[JsObject])

      running(application) {
        route(application, FakeRequest(POST, onSubmitRoute)).value.futureValue

        verify(connector)
          .updateDraftSection(eqTo(draftId), eqTo("supplier/1/vehicle/1/additional-information"), captor.capture())(using any[HeaderCarrier])

        val body = captor.getValue
        (body \ "dateArrivedInUk").as[String] mustEqual "30/03/2026"
        (body \ "vehicleIdNumber").as[String] mustEqual "123"
        (body \ "confirmVehicleIdNumber").as[String] mustEqual "123"
        (body \ "isAmendment").as[Boolean] mustEqual true
        (body \ "mileage").as[String] mustEqual "100000"
        (body \ "totalValueOfOptions").as[String] mustEqual "10000"
      }
    }

    "must save a supplier and vehicle section set for a single-vehicle LightCommercialVehiclesEu upload" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle), validationType = Some("LightCommercialVehiclesEu")))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.NotificationTaskListController.onPageLoad().url

        verify(connector).updateDraftSection(eqTo(draftId), eqTo("supplier/1/details"), any[JsObject])(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("supplier/1/vehicle/1/type"), any[JsObject])(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("supplier/1/vehicle/1/details"), any[JsObject])(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("supplier/1/vehicle/1/additional-information"), any[JsObject])(using
          any[HeaderCarrier]
        )
      }
    }

    "must number a second vehicle as supplier 2" in {
      val secondVehicle = fullVehicle.copy(itemNumber = Some(2), make = Some("Make 2"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("supplier/1/details"), any[JsObject])(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("supplier/2/details"), any[JsObject])(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("supplier/2/vehicle/1/details"), any[JsObject])(using any[HeaderCarrier])
      }
    }

    "must save an import and vehicle section set for a single-vehicle LightCommercialVehiclesNonEu upload, then redirect on and delete the upload record" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedNonEuResult(Seq(fullNonEuVehicle)))))

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(Some(answers), connector, sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.NotificationTaskListController.onPageLoad().url

        verify(connector).updateDraftSection(eqTo(draftId), eqTo("import/1/details"), any[JsObject])(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("import/1/vehicle/1/type"), any[JsObject])(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("import/1/vehicle/1/details"), any[JsObject])(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("import/1/vehicle/1/additional-information"), any[JsObject])(using
          any[HeaderCarrier]
        )
        verify(sessionRepository).setPage(eqTo(answers), eqTo(DraftVersionIdPage), eqTo(newVersion))(using any())
        verify(connector).deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier])
      }
    }

    "must map the import details, vehicle type and vehicle details sections directly across for a LightCommercialVehiclesNonEu upload, using the lcvBodyType key" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedNonEuResult(Seq(fullNonEuVehicle)))))

      val application   = applicationFor(Some(answers), connector)
      val detailsCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      val typeCaptor    = ArgumentCaptor.forClass(classOf[JsObject])
      val vehicleCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      running(application) {
        route(application, FakeRequest(POST, onSubmitRoute)).value.futureValue

        verify(connector).updateDraftSection(eqTo(draftId), eqTo("import/1/details"), detailsCaptor.capture())(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("import/1/vehicle/1/type"), typeCaptor.capture())(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("import/1/vehicle/1/details"), vehicleCaptor.capture())(using any[HeaderCarrier])

        val importDetails = detailsCaptor.getValue
        (importDetails \ "importEntryNumber").as[String] mustEqual "123-123456A"
        (importDetails \ "importEntryDate").as[String] mustEqual "30/03/2026"

        val vehicleType = typeCaptor.getValue
        (vehicleType \ "vehicleType").as[String] mustEqual "LCV"
        (vehicleType \ "dateRoadUseKnown").as[Boolean] mustEqual true
        (vehicleType \ "dateOfFirstRegistration").as[String] mustEqual "01/01/2010"

        val vehicleDetails = vehicleCaptor.getValue
        (vehicleDetails \ "make").as[String] mustEqual "Make"
        (vehicleDetails \ "model").as[String] mustEqual "Model"
        (vehicleDetails \ "derivative").as[String] mustEqual "Derivative"
        (vehicleDetails \ "trim").as[String] mustEqual "Trim"
        (vehicleDetails \ "lcvBodyType").as[String] mustEqual "Light commercial vehicle body type"
        (vehicleDetails \ "bodyType").asOpt[String] mustBe None
      }
    }

    "must use the bodyType key instead of lcvBodyType for a CarsNonEu upload" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedNonEuResult(Seq(fullNonEuVehicle), validationType = Some("CarsNonEu")))))

      val application = applicationFor(Some(answers), connector)
      val captor      = ArgumentCaptor.forClass(classOf[JsObject])

      running(application) {
        route(application, FakeRequest(POST, onSubmitRoute)).value.futureValue

        verify(connector).updateDraftSection(eqTo(draftId), eqTo("import/1/vehicle/1/details"), captor.capture())(using any[HeaderCarrier])

        val body = captor.getValue
        (body \ "bodyType").as[String] mustEqual "Light commercial vehicle body type"
        (body \ "lcvBodyType").asOpt[String] mustBe None
      }
    }

    "must map the import vehicle additional information section, including the amendment flag from session and dates in dd/MM/yyyy" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedNonEuResult(Seq(fullNonEuVehicle)))))

      val amendmentAnswers = answers.unsafeSet(AmendSubmittedNotificationPage, true)
      val application      = applicationFor(Some(amendmentAnswers), connector)
      val captor           = ArgumentCaptor.forClass(classOf[JsObject])

      running(application) {
        route(application, FakeRequest(POST, onSubmitRoute)).value.futureValue

        verify(connector)
          .updateDraftSection(eqTo(draftId), eqTo("import/1/vehicle/1/additional-information"), captor.capture())(using any[HeaderCarrier])

        val body = captor.getValue
        (body \ "dateArrivedInUk").as[String] mustEqual "01/03/2026"
        (body \ "pricePaidForVehicleEntry").as[String] mustEqual "100"
        (body \ "leftOrRightHand").as[String] mustEqual "RHD"
        (body \ "currencyUsed").as[String] mustEqual "USD"
        (body \ "commodityCode").as[String] mustEqual "1234"
        (body \ "isAmendment").as[Boolean] mustEqual true
        (body \ "vehicleIdNumber").as[String] mustEqual "1"
        (body \ "mileage").as[String] mustEqual "10000"
        (body \ "mileageUnits").as[String] mustEqual "MILES"
        (body \ "areYouClaimingRelief").as[Boolean] mustEqual false
      }
    }

    "must number a second import vehicle as import 2" in {
      val secondVehicle = fullNonEuVehicle.copy(itemNumber = Some(2), make = Some("Make 2"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedNonEuResult(Seq(fullNonEuVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("import/1/details"), any[JsObject])(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("import/2/details"), any[JsObject])(using any[HeaderCarrier])
        verify(connector).updateDraftSection(eqTo(draftId), eqTo("import/2/vehicle/1/details"), any[JsObject])(using any[HeaderCarrier])
      }
    }

    "must stop and redirect to Journey Recovery, without saving later sections or deleting the upload, when a section save fails" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle)))))
      when(connector.updateDraftSection(eqTo(draftId), eqTo("supplier/1/details"), any[JsObject])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(newVersion)))
      when(connector.updateDraftSection(eqTo(draftId), eqTo("supplier/1/vehicle/1/type"), any[JsObject])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(502, "boom"))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(connector, never)
          .updateDraftSection(eqTo(draftId), eqTo("supplier/1/vehicle/1/details"), any[JsObject])(using any[HeaderCarrier])
        verify(connector, never)
          .updateDraftSection(eqTo(draftId), eqTo("supplier/1/vehicle/1/additional-information"), any[JsObject])(using any[HeaderCarrier])
        verify(connector, never).deleteFileUpload(any[DraftId])(using any[HeaderCarrier])
      }
    }

    "must redirect to Journey Recovery without saving anything for an unsupported validationType" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle), validationType = Some("HeavyCommercialVehiclesNonEu")))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(connector, never).updateDraftSection(any[DraftId], any[String], any[JsObject])(using any[HeaderCarrier])
        verify(connector, never).deleteFileUpload(any[DraftId])(using any[HeaderCarrier])
      }
    }

    "must redirect to the errors page without saving when validation failed" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier])).thenReturn(
        Future.successful(
          Right(
            UploadResultResponse(
              "VALIDATION_FAILED",
              Some("CarsEu"),
              Seq.empty,
              Seq.empty,
              Seq.empty,
              Seq(ValidationError("mileage", "required"))
            )
          )
        )
      )

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.CheckVehicleSpreadsheetErrorsController.onPageLoad().url
        verify(connector, never).updateDraftSection(any[DraftId], any[String], any[JsObject])(using any[HeaderCarrier])
      }
    }

    "must redirect to Journey Recovery when the versionId is missing from session" in {
      val connector             = stubConnector()
      val answersWithoutVersion = emptyUserAnswers.unsafeSet(DraftIdPage, draftId).unsafeSet(VehicleFromEuPage, true)
      val application           = applicationFor(Some(answersWithoutVersion), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(connector, never).getUploadResult(any[DraftId])(using any[HeaderCarrier])
      }
    }

    "must redirect to Journey Recovery when the backend call fails" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(GetUploadResultError.NotFound)))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
