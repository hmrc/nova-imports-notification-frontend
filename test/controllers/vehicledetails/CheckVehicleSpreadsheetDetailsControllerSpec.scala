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
import connectors.{GetDraftNotificationError, GetUploadResultError, NovaImportsBackendConnector, UpdateSectionError}
import controllers.actions.*
import controllers.vehicledetails
import models.responses.{DeleteFileUploadResponse, SpreadsheetAgriculturalTractorEuVehicle, SpreadsheetAgriculturalTractorNonEuVehicle, SpreadsheetConstructionVehiclesEuVehicle, SpreadsheetConstructionVehiclesNonEuVehicle, SpreadsheetEuVehicle, SpreadsheetHeavyCommercialEuVehicle, SpreadsheetHeavyCommercialNonEuVehicle, SpreadsheetMotorCaravansEuVehicle, SpreadsheetMotorCaravansNonEuVehicle, SpreadsheetMotorcyclesEuVehicle, SpreadsheetMotorcyclesNonEuVehicle, SpreadsheetNonEuVehicle, UploadResultResponse, ValidationError, VehicleSummary}
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
import services.UserDataService
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

  private val fullAgriculturalTractorEuVehicle = SpreadsheetAgriculturalTractorEuVehicle(
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
    seriesModel = Some("Series (model)"),
    versionDerivative = Some("Version (derivative)"),
    brakeHorsepower = Some("154"),
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

  private val fullAgriculturalTractorNonEuVehicle = SpreadsheetAgriculturalTractorNonEuVehicle(
    itemNumber = Some(1),
    importEntryNumber = Some("123-123456A"),
    importEntryDate = Some(LocalDate.of(2026, 3, 30)),
    knownDateFirstRegistered = Some(true),
    countryOfFirstRegistration = None,
    dateOfFirstRegistration = Some(LocalDate.of(2010, 1, 1)),
    make = Some("Make"),
    seriesModel = Some("Series (model)"),
    versionDerivative = Some("Version (derivative)"),
    brakeHorsepower = Some("100"),
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

  private def validatedNonEuResult(vehicles: Seq[SpreadsheetNonEuVehicle], validationType: Option[String] = Some("LightCommercialVehiclesNonEu")) =
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

  private def validatedAgriculturalTractorEuResult(vehicles: Seq[SpreadsheetAgriculturalTractorEuVehicle]) =
    UploadResultResponse(
      fileStatus = "VALIDATED",
      validationType = Some("AgriculturalTractorsEu"),
      vehicles = vehicles.map(v => VehicleSummary(v.itemNumber, v.vin, v.make, None)),
      euVehicles = Seq.empty,
      nonEuVehicles = Seq.empty,
      agriculturalTractorEuVehicles = vehicles,
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

  private def validatedAgriculturalTractorNonEuResult(vehicles: Seq[SpreadsheetAgriculturalTractorNonEuVehicle]) =
    UploadResultResponse(
      fileStatus = "VALIDATED",
      validationType = Some("AgriculturalTractorsNonEu"),
      vehicles = vehicles.map(v => VehicleSummary(v.itemNumber, v.vin, v.make, None)),
      euVehicles = Seq.empty,
      nonEuVehicles = Seq.empty,
      agriculturalTractorEuVehicles = Seq.empty,
      agriculturalTractorNonEuVehicles = vehicles,
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

  private def validatedMotorCaravansEuResult(vehicles: Seq[SpreadsheetMotorCaravansEuVehicle]) =
    UploadResultResponse(
      fileStatus = "VALIDATED",
      validationType = Some("MotorCaravansEu"),
      vehicles = vehicles.map(v => VehicleSummary(v.itemNumber, v.vin, v.caravanMake, None)),
      euVehicles = Seq.empty,
      nonEuVehicles = Seq.empty,
      agriculturalTractorEuVehicles = Seq.empty,
      agriculturalTractorNonEuVehicles = Seq.empty,
      motorCaravansEuVehicles = vehicles,
      motorCaravansNonEuVehicles = Seq.empty,
      heavyCommercialEuVehicles = List.empty,
      heavyCommercialNonEuVehicles = List.empty,
      motorcyclesEuVehicles = Seq.empty,
      motorcyclesNonEuVehicles = Seq.empty,
      constructionVehiclesEuVehicles = Seq.empty,
      constructionVehiclesNonEuVehicles = Seq.empty,
      errors = Seq.empty
    )

  private def validatedMotorCaravansNonEuResult(vehicles: Seq[SpreadsheetMotorCaravansNonEuVehicle]) =
    UploadResultResponse(
      fileStatus = "VALIDATED",
      validationType = Some("MotorCaravansNonEu"),
      vehicles = vehicles.map(v => VehicleSummary(v.itemNumber, v.vin, v.caravanMake, None)),
      euVehicles = Seq.empty,
      nonEuVehicles = Seq.empty,
      agriculturalTractorEuVehicles = Seq.empty,
      agriculturalTractorNonEuVehicles = Seq.empty,
      motorCaravansEuVehicles = Seq.empty,
      motorCaravansNonEuVehicles = vehicles,
      heavyCommercialEuVehicles = List.empty,
      heavyCommercialNonEuVehicles = List.empty,
      motorcyclesEuVehicles = Seq.empty,
      motorcyclesNonEuVehicles = Seq.empty,
      constructionVehiclesEuVehicles = Seq.empty,
      constructionVehiclesNonEuVehicles = Seq.empty,
      errors = Seq.empty
    )

  private val fullMotorCaravansEuVehicle = SpreadsheetMotorCaravansEuVehicle(
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
    caravanMake = Some("Make of motor caravan"),
    modelNameNumber = Some("Motor caravan body"),
    caravanVersion = Some("Model name/number"),
    caravanBody = Some("Motor caravan version"),
    makeOfBaseVehicle = Some("Make of base vehicle"),
    derivative = Some("Derivative"),
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

  private val fullMotorCaravansNonEuVehicle = SpreadsheetMotorCaravansNonEuVehicle(
    itemNumber = Some(1),
    importEntryNumber = Some("123-123456A"),
    importEntryDate = Some(LocalDate.of(2026, 3, 30)),
    knownDateFirstRegistered = Some(true),
    countryOfFirstRegistration = None,
    dateOfFirstRegistration = Some(LocalDate.of(2010, 1, 1)),
    caravanMake = Some("Make of motor caravan"),
    modelNameNumber = Some("Motor caravan body"),
    caravanVersion = Some("Model name / number"),
    caravanBody = Some("Motor caravan version"),
    makeOfBaseVehicle = Some("Make of base vehicle"),
    derivative = Some("Derivative"),
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

  private def validatedHeavyCommercialEuResult(vehicles: List[SpreadsheetHeavyCommercialEuVehicle]) =
    UploadResultResponse(
      fileStatus = "VALIDATED",
      validationType = Some("HeavyCommercialVehiclesEu"),
      vehicles = vehicles.map(v => VehicleSummary(v.itemNumber, v.vin, v.make, v.model)),
      euVehicles = List.empty,
      nonEuVehicles = List.empty,
      agriculturalTractorEuVehicles = List.empty,
      agriculturalTractorNonEuVehicles = List.empty,
      motorCaravansEuVehicles = List.empty,
      motorCaravansNonEuVehicles = List.empty,
      heavyCommercialEuVehicles = vehicles,
      heavyCommercialNonEuVehicles = List.empty,
      motorcyclesEuVehicles = Seq.empty,
      motorcyclesNonEuVehicles = Seq.empty,
      constructionVehiclesEuVehicles = Seq.empty,
      constructionVehiclesNonEuVehicles = Seq.empty,
      errors = List.empty
    )

  private def validatedHeavyCommercialNonEuResult(vehicles: List[SpreadsheetHeavyCommercialNonEuVehicle]) =
    UploadResultResponse(
      fileStatus = "VALIDATED",
      validationType = Some("HeavyCommercialVehiclesNonEu"),
      vehicles = vehicles.map(v => VehicleSummary(v.itemNumber, v.vin, v.make, v.model)),
      euVehicles = List.empty,
      nonEuVehicles = List.empty,
      agriculturalTractorEuVehicles = List.empty,
      agriculturalTractorNonEuVehicles = List.empty,
      motorCaravansEuVehicles = List.empty,
      motorCaravansNonEuVehicles = List.empty,
      heavyCommercialEuVehicles = List.empty,
      heavyCommercialNonEuVehicles = vehicles,
      motorcyclesEuVehicles = Seq.empty,
      motorcyclesNonEuVehicles = Seq.empty,
      constructionVehiclesEuVehicles = Seq.empty,
      constructionVehiclesNonEuVehicles = Seq.empty,
      errors = List.empty
    )

  private val fullHeavyCommercialEuVehicle = SpreadsheetHeavyCommercialEuVehicle(
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
    heavyCommercialVehicleType = Some("Heavy commercial vehicle type"),
    cabType = Some("Cab type"),
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

  private val fullHeavyCommercialNonEuVehicle = SpreadsheetHeavyCommercialNonEuVehicle(
    itemNumber = Some(1),
    importEntryNumber = Some("123-123456A"),
    importEntryDate = Some(LocalDate.of(2026, 3, 30)),
    knownDateFirstRegistered = Some(true),
    countryOfFirstRegistration = None,
    dateOfFirstRegistration = Some(LocalDate.of(2010, 1, 1)),
    make = Some("Make"),
    model = Some("Model"),
    heavyCommercialVehicleType = Some("Heavy commercial vehicle type"),
    cabType = Some("Cab type"),
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

  private def validatedMotorcyclesEuResult(vehicles: Seq[SpreadsheetMotorcyclesEuVehicle]) =
    UploadResultResponse(
      fileStatus = "VALIDATED",
      validationType = Some("MotorcyclesEu"),
      vehicles = vehicles.map(v => VehicleSummary(v.itemNumber, v.vin, v.make, v.model)),
      euVehicles = Seq.empty,
      nonEuVehicles = Seq.empty,
      agriculturalTractorEuVehicles = Seq.empty,
      agriculturalTractorNonEuVehicles = Seq.empty,
      motorCaravansEuVehicles = Seq.empty,
      motorCaravansNonEuVehicles = Seq.empty,
      heavyCommercialEuVehicles = List.empty,
      heavyCommercialNonEuVehicles = List.empty,
      motorcyclesEuVehicles = vehicles,
      motorcyclesNonEuVehicles = Seq.empty,
      constructionVehiclesEuVehicles = Seq.empty,
      constructionVehiclesNonEuVehicles = Seq.empty,
      errors = Seq.empty
    )

  private def validatedMotorcyclesNonEuResult(vehicles: Seq[SpreadsheetMotorcyclesNonEuVehicle]) =
    UploadResultResponse(
      fileStatus = "VALIDATED",
      validationType = Some("MotorcyclesNonEu"),
      vehicles = vehicles.map(v => VehicleSummary(v.itemNumber, v.vin, v.make, v.model)),
      euVehicles = Seq.empty,
      nonEuVehicles = Seq.empty,
      agriculturalTractorEuVehicles = Seq.empty,
      agriculturalTractorNonEuVehicles = Seq.empty,
      motorCaravansEuVehicles = Seq.empty,
      motorCaravansNonEuVehicles = Seq.empty,
      heavyCommercialEuVehicles = List.empty,
      heavyCommercialNonEuVehicles = List.empty,
      motorcyclesEuVehicles = Seq.empty,
      motorcyclesNonEuVehicles = vehicles,
      constructionVehiclesEuVehicles = Seq.empty,
      constructionVehiclesNonEuVehicles = Seq.empty,
      errors = Seq.empty
    )

  private val fullMotorcyclesEuVehicle = SpreadsheetMotorcyclesEuVehicle(
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
    version = Some("Version"),
    motorcycleType = Some("Type of motorcycle"),
    style = Some("Style of motorcycle"),
    transmissionType = Some("Transmission type"),
    fuelType = Some("Fuel type"),
    engineSize = Some("125"),
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

  private val fullMotorcyclesNonEuVehicle = SpreadsheetMotorcyclesNonEuVehicle(
    itemNumber = Some(1),
    importEntryNumber = Some("123-123456A"),
    importEntryDate = Some(LocalDate.of(2026, 3, 30)),
    knownDateFirstRegistered = Some(true),
    countryOfFirstRegistration = None,
    dateOfFirstRegistration = Some(LocalDate.of(2010, 1, 1)),
    make = Some("Make"),
    model = Some("Model"),
    derivative = Some("Derivative"),
    version = Some("Version"),
    motorcycleType = Some("Type of motorcycle"),
    style = Some("Style of motorcycle"),
    transmissionType = Some("Transmission type"),
    fuelType = Some("Fuel type"),
    engineSize = Some("124"),
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

  private def validatedConstructionVehiclesEuResult(vehicles: Seq[SpreadsheetConstructionVehiclesEuVehicle]) =
    UploadResultResponse(
      fileStatus = "VALIDATED",
      validationType = Some("ConstructionVehiclesEu"),
      vehicles = vehicles.map(v => VehicleSummary(v.itemNumber, v.vin, v.make, None)),
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
      constructionVehiclesEuVehicles = vehicles,
      constructionVehiclesNonEuVehicles = Seq.empty,
      errors = Seq.empty
    )

  private def validatedConstructionVehiclesNonEuResult(vehicles: Seq[SpreadsheetConstructionVehiclesNonEuVehicle]) =
    UploadResultResponse(
      fileStatus = "VALIDATED",
      validationType = Some("ConstructionVehiclesNonEu"),
      vehicles = vehicles.map(v => VehicleSummary(v.itemNumber, v.vin, v.make, None)),
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
      constructionVehiclesNonEuVehicles = vehicles,
      errors = Seq.empty
    )

  private val fullConstructionVehiclesEuVehicle = SpreadsheetConstructionVehiclesEuVehicle(
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
    seriesModel = Some("Series (model)"),
    versionDerivative = Some("Version (derivative)"),
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

  private val fullConstructionVehiclesNonEuVehicle = SpreadsheetConstructionVehiclesNonEuVehicle(
    itemNumber = Some(1),
    importEntryNumber = Some("123-123456A"),
    importEntryDate = Some(LocalDate.of(2026, 3, 30)),
    knownDateFirstRegistered = Some(true),
    countryOfFirstRegistration = None,
    dateOfFirstRegistration = Some(LocalDate.of(2010, 1, 1)),
    make = Some("Make"),
    seriesModel = Some("Series (model)"),
    versionDerivative = Some("Version (derivative)"),
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

  private def stubSessionRepository(): SessionRepository = {
    val repo = mock[SessionRepository]
    when(repo.setPage(any(), any(), any())(using any())).thenReturn(Future.successful(answers))
    repo
  }

  private def stubUserDataService(): UserDataService = {
    val service = mock[UserDataService]
    when(service.retrieveAndStoreDraftNotification(any(), any(), any())(using any[HeaderCarrier])).thenReturn(Future.successful(Right(answers)))
    service
  }

  private def stubConnector(): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.replaceVehicleSections(eqTo(draftId), any[Map[String, JsObject]], any[Long])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(newVersion)))
    when(connector.deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(DeleteFileUploadResponse(success = true))))
    connector
  }

  private def applicationFor(
    userAnswers: Option[UserAnswers],
    connector: NovaImportsBackendConnector,
    sessionRepository: SessionRepository = stubSessionRepository(),
    userDataService: UserDataService = stubUserDataService()
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
        bind[SessionRepository].toInstance(sessionRepository),
        bind[UserDataService].toInstance(userDataService)
      )
      .build()

  private def captureSections(connector: NovaImportsBackendConnector): Map[String, JsObject] = {
    val captor = ArgumentCaptor.forClass(classOf[Map[String, JsObject]])
    verify(connector).replaceVehicleSections(eqTo(draftId), captor.capture(), eqTo(versionId))(using any[HeaderCarrier])
    captor.getValue
  }

  "CheckVehicleSpreadsheetDetailsController.onSubmit" - {

    "must replace vehicle sections in one call for a single-vehicle CarsEu upload, then redirect on and delete the upload record" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle)))))

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(Some(answers), connector, sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSuccessfulController.onPageLoad().url

        val sections = captureSections(connector)
        sections.keySet mustEqual Set(
          "supplier/1/details",
          "supplier/1/vehicle/1/type",
          "supplier/1/vehicle/1/details",
          "supplier/1/vehicle/1/additional-information"
        )
        verify(sessionRepository).setPage(eqTo(answers), eqTo(DraftVersionIdPage), eqTo(newVersion))(using any())
        verify(connector).deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier])
      }
    }

    "must refresh the session from the draft notification, marking the vehicles section, before redirecting to UVS6.0" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle)))))

      val userDataService = stubUserDataService()
      val application     = applicationFor(Some(answers), connector, userDataService = userDataService)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSuccessfulController.onPageLoad().url
        verify(userDataService).retrieveAndStoreDraftNotification(eqTo(draftId), eqTo(answers), any())(using any[HeaderCarrier])
      }
    }

    "must redirect to JourneyRecovery when the draft notification cannot be refreshed after saving" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle)))))

      val userDataService = mock[UserDataService]
      when(userDataService.retrieveAndStoreDraftNotification(any(), any(), any())(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(GetDraftNotificationError.UpstreamError(500, "boom"))))

      val application = applicationFor(Some(answers), connector, userDataService = userDataService)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must not refresh the draft notification when saving the vehicle sections fails" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle)))))
      when(connector.replaceVehicleSections(eqTo(draftId), any[Map[String, JsObject]], any[Long])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(500, "boom"))))

      val userDataService = stubUserDataService()
      val application     = applicationFor(Some(answers), connector, userDataService = userDataService)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(userDataService, never()).retrieveAndStoreDraftNotification(any(), any(), any())(using any[HeaderCarrier])
      }
    }

    "must map the vehicle details section fields directly across" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        route(application, FakeRequest(POST, onSubmitRoute)).value.futureValue

        val body = captureSections(connector)("supplier/1/vehicle/1/details")
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

      running(application) {
        route(application, FakeRequest(POST, onSubmitRoute)).value.futureValue

        val body = captureSections(connector)("supplier/1/vehicle/1/additional-information")
        (body \ "dateArrivedInUk").as[String] mustEqual "30/03/2026"
        (body \ "vehicleIdNumber").as[String] mustEqual "123"
        (body \ "confirmVehicleIdNumber").as[String] mustEqual "123"
        (body \ "isAmendment").as[Boolean] mustEqual true
        (body \ "mileage").as[String] mustEqual "100000"
        (body \ "totalValueOfOptions").as[String] mustEqual "10000"
      }
    }

    "must replace vehicle sections in one call for a single-vehicle LightCommercialVehiclesEu upload" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle), validationType = Some("LightCommercialVehiclesEu")))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSuccessfulController.onPageLoad().url

        val sections = captureSections(connector)
        sections.keySet mustEqual Set(
          "supplier/1/details",
          "supplier/1/vehicle/1/type",
          "supplier/1/vehicle/1/details",
          "supplier/1/vehicle/1/additional-information"
        )
      }
    }

    "must group a second vehicle under the same supplier number when the supplier details are identical, numbering the vehicle by its row position" in {
      val secondVehicle = fullVehicle.copy(itemNumber = Some(2), make = Some("Make 2"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        val keys = captureSections(connector).keySet
        keys must contain("supplier/1/details")
        keys must contain("supplier/1/vehicle/1/details")
        keys must contain("supplier/1/vehicle/2/details")
        keys must not contain "supplier/2/details"
      }
    }

    "must allocate a new supplier number for a second vehicle with different supplier details, while still numbering the vehicle by its row position" in {
      val secondVehicle = fullVehicle.copy(itemNumber = Some(2), make = Some("Make 2"), supplierBusinessName = Some("A different business"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        val keys = captureSections(connector).keySet
        keys must contain("supplier/1/details")
        keys must contain("supplier/2/details")
        keys must contain("supplier/1/vehicle/1/details")
        keys must contain("supplier/2/vehicle/2/details")
        keys must not contain "supplier/2/vehicle/1/details"
      }
    }

    "must replace vehicle sections in one call for a single-vehicle LightCommercialVehiclesNonEu upload, then redirect on and delete the upload record" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedNonEuResult(Seq(fullNonEuVehicle)))))

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(Some(answers), connector, sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSuccessfulController.onPageLoad().url

        val sections = captureSections(connector)
        sections.keySet mustEqual Set(
          "import/1/details",
          "import/1/vehicle/1/type",
          "import/1/vehicle/1/details",
          "import/1/vehicle/1/additional-information"
        )
        verify(sessionRepository).setPage(eqTo(answers), eqTo(DraftVersionIdPage), eqTo(newVersion))(using any())
        verify(connector).deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier])
      }
    }

    "must map the import details, vehicle type and vehicle details sections directly across for a LightCommercialVehiclesNonEu upload, using the lcvBodyType key" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedNonEuResult(Seq(fullNonEuVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        route(application, FakeRequest(POST, onSubmitRoute)).value.futureValue

        val sections      = captureSections(connector)
        val importDetails = sections("import/1/details")
        (importDetails \ "importEntryNumber").as[String] mustEqual "123-123456A"
        (importDetails \ "importEntryDate").as[String] mustEqual "30/03/2026"

        val vehicleType = sections("import/1/vehicle/1/type")
        (vehicleType \ "vehicleType").as[String] mustEqual "LCV"
        (vehicleType \ "dateRoadUseKnown").as[Boolean] mustEqual true
        (vehicleType \ "dateOfFirstRegistration").as[String] mustEqual "01/01/2010"

        val vehicleDetails = sections("import/1/vehicle/1/details")
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

      running(application) {
        route(application, FakeRequest(POST, onSubmitRoute)).value.futureValue

        val body = captureSections(connector)("import/1/vehicle/1/details")
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

      running(application) {
        route(application, FakeRequest(POST, onSubmitRoute)).value.futureValue

        val body = captureSections(connector)("import/1/vehicle/1/additional-information")
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

    "must group a second import vehicle under the same import number when the import details are identical, numbering the vehicle by its row position" in {
      val secondVehicle = fullNonEuVehicle.copy(itemNumber = Some(2), make = Some("Make 2"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedNonEuResult(Seq(fullNonEuVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        val keys = captureSections(connector).keySet
        keys must contain("import/1/details")
        keys must contain("import/1/vehicle/1/details")
        keys must contain("import/1/vehicle/2/details")
        keys must not contain "import/2/details"
      }
    }

    "must allocate a new import number for a second import vehicle with different import details, while still numbering the vehicle by its row position" in {
      val secondVehicle = fullNonEuVehicle.copy(itemNumber = Some(2), make = Some("Make 2"), importEntryNumber = Some("999-999999Z"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedNonEuResult(Seq(fullNonEuVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        val keys = captureSections(connector).keySet
        keys must contain("import/1/details")
        keys must contain("import/2/details")
        keys must contain("import/1/vehicle/1/details")
        keys must contain("import/2/vehicle/2/details")
        keys must not contain "import/2/vehicle/1/details"
      }
    }

    "must replace vehicle sections in one call for a single-vehicle AgriculturalTractorsEu upload, using seriesModel/versionDerivative/brakeHorsePower and the AGRICULTURAL_TRACTOR vehicleType" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedAgriculturalTractorEuResult(Seq(fullAgriculturalTractorEuVehicle)))))

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(Some(answers), connector, sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSuccessfulController.onPageLoad().url

        val sections = captureSections(connector)
        verify(sessionRepository).setPage(eqTo(answers), eqTo(DraftVersionIdPage), eqTo(newVersion))(using any())
        verify(connector).deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier])

        (sections("supplier/1/vehicle/1/type") \ "vehicleType").as[String] mustEqual "AGRICULTURAL_TRACTOR"

        val vehicleDetails = sections("supplier/1/vehicle/1/details")
        (vehicleDetails \ "make").as[String] mustEqual "Make"
        (vehicleDetails \ "seriesModel").as[String] mustEqual "Series (model)"
        (vehicleDetails \ "versionDerivative").as[String] mustEqual "Version (derivative)"
        (vehicleDetails \ "brakeHorsePower").as[String] mustEqual "154"
        (vehicleDetails \ "model").asOpt[String] mustBe None
        (vehicleDetails \ "bodyType").asOpt[String] mustBe None
      }
    }

    "must replace vehicle sections in one call for a single-vehicle AgriculturalTractorsNonEu upload, using seriesModel/versionDerivative/brakeHorsePower and the AGRICULTURAL_TRACTOR vehicleType" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedAgriculturalTractorNonEuResult(Seq(fullAgriculturalTractorNonEuVehicle)))))

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(Some(answers), connector, sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSuccessfulController.onPageLoad().url

        val sections = captureSections(connector)
        verify(sessionRepository).setPage(eqTo(answers), eqTo(DraftVersionIdPage), eqTo(newVersion))(using any())
        verify(connector).deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier])

        (sections("import/1/vehicle/1/type") \ "vehicleType").as[String] mustEqual "AGRICULTURAL_TRACTOR"

        val vehicleDetails = sections("import/1/vehicle/1/details")
        (vehicleDetails \ "make").as[String] mustEqual "Make"
        (vehicleDetails \ "seriesModel").as[String] mustEqual "Series (model)"
        (vehicleDetails \ "versionDerivative").as[String] mustEqual "Version (derivative)"
        (vehicleDetails \ "brakeHorsePower").as[String] mustEqual "100"
      }
    }

    "must group a second AgriculturalTractorsEu vehicle under the same supplier number when the supplier details are identical, numbering the vehicle by its row position" in {
      val secondVehicle = fullAgriculturalTractorEuVehicle.copy(itemNumber = Some(2), make = Some("Make 2"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedAgriculturalTractorEuResult(Seq(fullAgriculturalTractorEuVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        val keys = captureSections(connector).keySet
        keys must contain("supplier/1/details")
        keys must contain("supplier/1/vehicle/1/details")
        keys must contain("supplier/1/vehicle/2/details")
        keys must not contain "supplier/2/details"
      }
    }

    "must group a second AgriculturalTractorsNonEu vehicle under the same import number when the import details are identical, numbering the vehicle by its row position" in {
      val secondVehicle = fullAgriculturalTractorNonEuVehicle.copy(itemNumber = Some(2), make = Some("Make 2"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedAgriculturalTractorNonEuResult(Seq(fullAgriculturalTractorNonEuVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        val keys = captureSections(connector).keySet
        keys must contain("import/1/details")
        keys must contain("import/1/vehicle/1/details")
        keys must contain("import/1/vehicle/2/details")
        keys must not contain "import/2/details"
      }
    }

    "must replace vehicle sections in one call for a single-vehicle MotorCaravansEu upload, using the rotated caravan fields and the MOTOR_CARAVAN vehicleType" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedMotorCaravansEuResult(Seq(fullMotorCaravansEuVehicle)))))

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(Some(answers), connector, sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSuccessfulController.onPageLoad().url

        val sections = captureSections(connector)
        verify(sessionRepository).setPage(eqTo(answers), eqTo(DraftVersionIdPage), eqTo(newVersion))(using any())
        verify(connector).deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier])

        (sections("supplier/1/vehicle/1/type") \ "vehicleType").as[String] mustEqual "MOTOR_CARAVAN"

        val vehicleDetails = sections("supplier/1/vehicle/1/details")
        (vehicleDetails \ "caravanMake").as[String] mustEqual "Make of motor caravan"
        (vehicleDetails \ "modelNameNumber").as[String] mustEqual "Motor caravan body"
        (vehicleDetails \ "caravanVersion").as[String] mustEqual "Model name/number"
        (vehicleDetails \ "caravanBody").as[String] mustEqual "Motor caravan version"
        (vehicleDetails \ "makeOfBaseVehicle").as[String] mustEqual "Make of base vehicle"
        (vehicleDetails \ "derivative").as[String] mustEqual "Derivative"
      }
    }

    "must replace vehicle sections in one call for a single-vehicle MotorCaravansNonEu upload, using the rotated caravan fields and the MOTOR_CARAVAN vehicleType" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedMotorCaravansNonEuResult(Seq(fullMotorCaravansNonEuVehicle)))))

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(Some(answers), connector, sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSuccessfulController.onPageLoad().url

        val sections = captureSections(connector)
        verify(sessionRepository).setPage(eqTo(answers), eqTo(DraftVersionIdPage), eqTo(newVersion))(using any())
        verify(connector).deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier])

        (sections("import/1/vehicle/1/type") \ "vehicleType").as[String] mustEqual "MOTOR_CARAVAN"

        val vehicleDetails = sections("import/1/vehicle/1/details")
        (vehicleDetails \ "caravanMake").as[String] mustEqual "Make of motor caravan"
        (vehicleDetails \ "modelNameNumber").as[String] mustEqual "Motor caravan body"
        (vehicleDetails \ "caravanVersion").as[String] mustEqual "Model name / number"
        (vehicleDetails \ "caravanBody").as[String] mustEqual "Motor caravan version"
        (vehicleDetails \ "makeOfBaseVehicle").as[String] mustEqual "Make of base vehicle"
        (vehicleDetails \ "derivative").as[String] mustEqual "Derivative"
      }
    }

    "must group a second MotorCaravansEu vehicle under the same supplier number when the supplier details are identical, numbering the vehicle by its row position" in {
      val secondVehicle = fullMotorCaravansEuVehicle.copy(itemNumber = Some(2), caravanMake = Some("Make of motor caravan 2"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedMotorCaravansEuResult(Seq(fullMotorCaravansEuVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        val keys = captureSections(connector).keySet
        keys must contain("supplier/1/details")
        keys must contain("supplier/1/vehicle/1/details")
        keys must contain("supplier/1/vehicle/2/details")
        keys must not contain "supplier/2/details"
      }
    }

    "must group a second MotorCaravansNonEu vehicle under the same import number when the import details are identical, numbering the vehicle by its row position" in {
      val secondVehicle = fullMotorCaravansNonEuVehicle.copy(itemNumber = Some(2), caravanMake = Some("Make of motor caravan 2"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedMotorCaravansNonEuResult(Seq(fullMotorCaravansNonEuVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        val keys = captureSections(connector).keySet
        keys must contain("import/1/details")
        keys must contain("import/1/vehicle/1/details")
        keys must contain("import/1/vehicle/2/details")
        keys must not contain "import/2/details"
      }
    }

    "must replace vehicle sections in one call for a single-vehicle MotorcyclesEu upload, using the motorcycle-specific detail fields and the MOTORCYCLE vehicleType" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedMotorcyclesEuResult(Seq(fullMotorcyclesEuVehicle)))))

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(Some(answers), connector, sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSuccessfulController.onPageLoad().url

        val sections = captureSections(connector)
        verify(sessionRepository).setPage(eqTo(answers), eqTo(DraftVersionIdPage), eqTo(newVersion))(using any())
        verify(connector).deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier])

        (sections("supplier/1/vehicle/1/type") \ "vehicleType").as[String] mustEqual "MOTORCYCLE"

        val vehicleDetails = sections("supplier/1/vehicle/1/details")
        (vehicleDetails \ "make").as[String] mustEqual "Make"
        (vehicleDetails \ "model").as[String] mustEqual "Model"
        (vehicleDetails \ "derivative").as[String] mustEqual "Derivative"
        (vehicleDetails \ "motorcycleVersion").as[String] mustEqual "Version"
        (vehicleDetails \ "motorcycleType").as[String] mustEqual "Type of motorcycle"
        (vehicleDetails \ "motorcycleStyle").as[String] mustEqual "Style of motorcycle"
        (vehicleDetails \ "transmission").as[String] mustEqual "Transmission type"
        (vehicleDetails \ "fuelType").as[String] mustEqual "Fuel type"
        (vehicleDetails \ "engineSize").as[String] mustEqual "125"
      }
    }

    "must replace vehicle sections in one call for a single-vehicle MotorcyclesNonEu upload, using the motorcycle-specific detail fields and the MOTORCYCLE vehicleType" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedMotorcyclesNonEuResult(Seq(fullMotorcyclesNonEuVehicle)))))

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(Some(answers), connector, sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSuccessfulController.onPageLoad().url

        val sections = captureSections(connector)
        verify(sessionRepository).setPage(eqTo(answers), eqTo(DraftVersionIdPage), eqTo(newVersion))(using any())
        verify(connector).deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier])

        (sections("import/1/vehicle/1/type") \ "vehicleType").as[String] mustEqual "MOTORCYCLE"

        val vehicleDetails = sections("import/1/vehicle/1/details")
        (vehicleDetails \ "make").as[String] mustEqual "Make"
        (vehicleDetails \ "model").as[String] mustEqual "Model"
        (vehicleDetails \ "derivative").as[String] mustEqual "Derivative"
        (vehicleDetails \ "motorcycleVersion").as[String] mustEqual "Version"
        (vehicleDetails \ "motorcycleType").as[String] mustEqual "Type of motorcycle"
        (vehicleDetails \ "motorcycleStyle").as[String] mustEqual "Style of motorcycle"
        (vehicleDetails \ "transmission").as[String] mustEqual "Transmission type"
        (vehicleDetails \ "fuelType").as[String] mustEqual "Fuel type"
        (vehicleDetails \ "engineSize").as[String] mustEqual "124"
      }
    }

    "must group a second MotorcyclesEu vehicle under the same supplier number when the supplier details are identical, numbering the vehicle by its row position" in {
      val secondVehicle = fullMotorcyclesEuVehicle.copy(itemNumber = Some(2), make = Some("Make 2"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedMotorcyclesEuResult(Seq(fullMotorcyclesEuVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        val keys = captureSections(connector).keySet
        keys must contain("supplier/1/details")
        keys must contain("supplier/1/vehicle/1/details")
        keys must contain("supplier/1/vehicle/2/details")
        keys must not contain "supplier/2/details"
      }
    }

    "must group a second MotorcyclesNonEu vehicle under the same import number when the import details are identical, numbering the vehicle by its row position" in {
      val secondVehicle = fullMotorcyclesNonEuVehicle.copy(itemNumber = Some(2), make = Some("Make 2"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedMotorcyclesNonEuResult(Seq(fullMotorcyclesNonEuVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        val keys = captureSections(connector).keySet
        keys must contain("import/1/details")
        keys must contain("import/1/vehicle/1/details")
        keys must contain("import/1/vehicle/2/details")
        keys must not contain "import/2/details"
      }
    }

    "must replace vehicle sections in one call for a single-vehicle ConstructionVehiclesEu upload, using make/seriesModel/versionDerivative and the CONTRACTORS_PLANT vehicleType" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedConstructionVehiclesEuResult(Seq(fullConstructionVehiclesEuVehicle)))))

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(Some(answers), connector, sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSuccessfulController.onPageLoad().url

        val sections = captureSections(connector)
        verify(sessionRepository).setPage(eqTo(answers), eqTo(DraftVersionIdPage), eqTo(newVersion))(using any())
        verify(connector).deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier])

        (sections("supplier/1/vehicle/1/type") \ "vehicleType").as[String] mustEqual "CONTRACTORS_PLANT"

        val vehicleDetails = sections("supplier/1/vehicle/1/details")
        (vehicleDetails \ "make").as[String] mustEqual "Make"
        (vehicleDetails \ "seriesModel").as[String] mustEqual "Series (model)"
        (vehicleDetails \ "versionDerivative").as[String] mustEqual "Version (derivative)"
      }
    }

    "must replace vehicle sections in one call for a single-vehicle ConstructionVehiclesNonEu upload, using make/seriesModel/versionDerivative and the CONTRACTORS_PLANT vehicleType" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedConstructionVehiclesNonEuResult(Seq(fullConstructionVehiclesNonEuVehicle)))))

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(Some(answers), connector, sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSuccessfulController.onPageLoad().url

        val sections = captureSections(connector)
        verify(sessionRepository).setPage(eqTo(answers), eqTo(DraftVersionIdPage), eqTo(newVersion))(using any())
        verify(connector).deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier])

        (sections("import/1/vehicle/1/type") \ "vehicleType").as[String] mustEqual "CONTRACTORS_PLANT"

        val vehicleDetails = sections("import/1/vehicle/1/details")
        (vehicleDetails \ "make").as[String] mustEqual "Make"
        (vehicleDetails \ "seriesModel").as[String] mustEqual "Series (model)"
        (vehicleDetails \ "versionDerivative").as[String] mustEqual "Version (derivative)"
      }
    }

    "must group a second ConstructionVehiclesEu vehicle under the same supplier number when the supplier details are identical, numbering the vehicle by its row position" in {
      val secondVehicle = fullConstructionVehiclesEuVehicle.copy(itemNumber = Some(2), make = Some("Make 2"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedConstructionVehiclesEuResult(Seq(fullConstructionVehiclesEuVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        val keys = captureSections(connector).keySet
        keys must contain("supplier/1/details")
        keys must contain("supplier/1/vehicle/1/details")
        keys must contain("supplier/1/vehicle/2/details")
        keys must not contain "supplier/2/details"
      }
    }

    "must group a second ConstructionVehiclesNonEu vehicle under the same import number when the import details are identical, numbering the vehicle by its row position" in {
      val secondVehicle = fullConstructionVehiclesNonEuVehicle.copy(itemNumber = Some(2), make = Some("Make 2"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedConstructionVehiclesNonEuResult(Seq(fullConstructionVehiclesNonEuVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        val keys = captureSections(connector).keySet
        keys must contain("import/1/details")
        keys must contain("import/1/vehicle/1/details")
        keys must contain("import/1/vehicle/2/details")
        keys must not contain "import/2/details"
      }
    }

    "must replace vehicle sections in one call for a single-vehicle HeavyCommercialVehiclesEu upload, using make/model/hcvType/cabType and the HCV vehicleType" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedHeavyCommercialEuResult(List(fullHeavyCommercialEuVehicle)))))

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(Some(answers), connector, sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSuccessfulController.onPageLoad().url

        val sections = captureSections(connector)
        verify(sessionRepository).setPage(eqTo(answers), eqTo(DraftVersionIdPage), eqTo(newVersion))(using any())
        verify(connector).deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier])

        (sections("supplier/1/vehicle/1/type") \ "vehicleType").as[String] mustEqual "HCV"

        val vehicleDetails = sections("supplier/1/vehicle/1/details")
        (vehicleDetails \ "make").as[String] mustEqual "Make"
        (vehicleDetails \ "model").as[String] mustEqual "Model"
        (vehicleDetails \ "hcvType").as[String] mustEqual "Heavy commercial vehicle type"
        (vehicleDetails \ "cabType").as[String] mustEqual "Cab type"
        (vehicleDetails \ "heavyCommercialVehicleType").asOpt[String] mustBe None
        (vehicleDetails \ "bodyType").asOpt[String] mustBe None
      }
    }

    "must replace vehicle sections in one call for a single-vehicle HeavyCommercialVehiclesNonEu upload, using make/model/hcvType/cabType and the HCV vehicleType" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedHeavyCommercialNonEuResult(List(fullHeavyCommercialNonEuVehicle)))))

      val sessionRepository = stubSessionRepository()
      val application       = applicationFor(Some(answers), connector, sessionRepository)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSuccessfulController.onPageLoad().url

        val sections = captureSections(connector)
        verify(sessionRepository).setPage(eqTo(answers), eqTo(DraftVersionIdPage), eqTo(newVersion))(using any())
        verify(connector).deleteFileUpload(eqTo(draftId))(using any[HeaderCarrier])

        (sections("import/1/vehicle/1/type") \ "vehicleType").as[String] mustEqual "HCV"
        (sections("import/1/vehicle/1/type") \ "dateOfFirstRegistration").as[String] mustEqual "01/01/2010"

        val vehicleDetails = sections("import/1/vehicle/1/details")
        (vehicleDetails \ "make").as[String] mustEqual "Make"
        (vehicleDetails \ "model").as[String] mustEqual "Model"
        (vehicleDetails \ "hcvType").as[String] mustEqual "Heavy commercial vehicle type"
        (vehicleDetails \ "cabType").as[String] mustEqual "Cab type"
        (vehicleDetails \ "heavyCommercialVehicleType").asOpt[String] mustBe None
      }
    }

    "must group a second HeavyCommercialVehiclesEu vehicle under the same supplier number when the supplier details are identical, numbering the vehicle by its row position" in {
      val secondVehicle = fullHeavyCommercialEuVehicle.copy(itemNumber = Some(2), make = Some("Make 2"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedHeavyCommercialEuResult(List(fullHeavyCommercialEuVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        val keys = captureSections(connector).keySet
        keys must contain("supplier/1/details")
        keys must contain("supplier/1/vehicle/1/details")
        keys must contain("supplier/1/vehicle/2/details")
        keys must not contain "supplier/2/details"
      }
    }

    "must group a second HeavyCommercialVehiclesNonEu vehicle under the same import number when the import details are identical, numbering the vehicle by its row position" in {
      val secondVehicle = fullHeavyCommercialNonEuVehicle.copy(itemNumber = Some(2), make = Some("Make 2"))
      val connector     = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedHeavyCommercialNonEuResult(List(fullHeavyCommercialNonEuVehicle, secondVehicle)))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        val keys = captureSections(connector).keySet
        keys must contain("import/1/details")
        keys must contain("import/1/vehicle/1/details")
        keys must contain("import/1/vehicle/2/details")
        keys must not contain "import/2/details"
      }
    }

    "must stop and redirect to Journey Recovery, without deleting the upload, when the bulk replace call fails" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(Seq(fullVehicle)))))
      when(connector.replaceVehicleSections(eqTo(draftId), any[Map[String, JsObject]], any[Long])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(502, "boom"))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(connector, never).deleteFileUpload(any[DraftId])(using any[HeaderCarrier])
      }
    }

    "must redirect to Journey Recovery without saving anything for an unsupported validationType" in {
      val connector = stubConnector()
      when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validatedResult(List(fullVehicle), validationType = Some("SomeFutureVehicleTypeNotYetSupported")))))

      val application = applicationFor(Some(answers), connector)

      running(application) {
        val result = route(application, FakeRequest(POST, onSubmitRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(connector, never).replaceVehicleSections(any[DraftId], any[Map[String, JsObject]], any[Long])(using any[HeaderCarrier])
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
              Seq.empty,
              Seq.empty,
              Seq.empty,
              Seq.empty,
              List.empty,
              List.empty,
              Seq.empty,
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
        verify(connector, never).replaceVehicleSections(any[DraftId], any[Map[String, JsObject]], any[Long])(using any[HeaderCarrier])
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
