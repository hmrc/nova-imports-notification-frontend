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

package viewmodels

import base.SpecBase
import models.responses.{SpreadsheetAgriculturalTractorNonEuVehicle, SpreadsheetHeavyCommercialEuVehicle, SpreadsheetMotorCaravansNonEuVehicle, SpreadsheetMotorcyclesNonEuVehicle, SpreadsheetNonEuVehicle, UploadResultResponse, VehicleSummary}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.LocalDate

class SpreadsheetDataCsvSpec extends SpecBase {

  private implicit val messages: Messages = stubMessages()

  private val importCarHeadings = Seq(
    "itemNumber",
    "importEntryNumber",
    "importEntryDate",
    "dateRoadUseKnown",
    "countryOfFirstRegistration",
    "dateOfFirstRegistration",
    "make",
    "model",
    "derivative",
    "trim",
    "bodyType",
    "notificationReference",
    "vehicleIdNumber",
    "dateArrivedInUk",
    "commodityCode",
    "mileage",
    "mileageUnits",
    "leftOrRightHand",
    "pricePaidForVehicle",
    "importCurrencyUsed",
    "areYouClaimingRelief",
    "reasonForClaimingRelief"
  ).map(field => s"checkVehicleSpreadsheetDetails.download.csv.$field").mkString("", ",", "\r\n")

  private val importLcvHeadings = Seq(
    "itemNumber",
    "importEntryNumber",
    "importEntryDate",
    "dateRoadUseKnown",
    "countryOfFirstRegistration",
    "dateOfFirstRegistration",
    "make",
    "model",
    "derivative",
    "trim",
    "lcvBodyType",
    "notificationReference",
    "vehicleIdNumber",
    "dateArrivedInUk",
    "commodityCode",
    "mileage",
    "mileageUnits",
    "leftOrRightHand",
    "pricePaidForVehicle",
    "importCurrencyUsed",
    "areYouClaimingRelief",
    "reasonForClaimingRelief"
  ).map(field => s"checkVehicleSpreadsheetDetails.download.csv.$field").mkString("", ",", "\r\n")

  private val supplierHeavyCommercialVehicleHeadings = Seq(
    "itemNumber",
    "supplierBusinessIndividual",
    "supplierBusinessName",
    "supplierTitle",
    "supplierFirstName",
    "supplierLastName",
    "addressLine1",
    "addressLine2",
    "addressLine3",
    "addressLine4",
    "addressLine5",
    "postcode",
    "country",
    "isSupplierVatReg",
    "euStateVatReg",
    "vatRegistrationNumber",
    "dateRoadUseKnown",
    "countryOfFirstRegistration",
    "dateOfFirstRegistration",
    "dateMadeAvailableYou",
    "doYouHaveAPurchaseInvoice",
    "noPurchaseInvoiceReason",
    "purchaseInvoiceDate",
    "purchaseInvoiceNumber",
    "pricePaidForVehicle",
    "currencyUsed",
    "make",
    "model",
    "hcvType",
    "cabType",
    "notificationReference",
    "vehicleIdNumber",
    "currentRegistrationNumber",
    "dateArrivedInUk",
    "mileage",
    "mileageUnits",
    "leftOrRightHand",
    "totalValueOfOptions",
    "businessUnableToReclaimVat",
    "vehicleSoldUnderMarginScheme",
    "areYouClaimingRelief",
    "reasonForClaimingRelief"
  ).map(field => s"checkVehicleSpreadsheetDetails.download.csv.$field").mkString("", ",", "\r\n")

  private val importMotorCaravanHeadings = Seq(
    "itemNumber",
    "importEntryNumber",
    "importEntryDate",
    "dateRoadUseKnown",
    "countryOfFirstRegistration",
    "dateOfFirstRegistration",
    "caravanMake",
    "caravanVersion",
    "caravanBody",
    "modelNameNumber",
    "makeOfBaseVehicle",
    "derivative",
    "notificationReference",
    "vehicleIdNumber",
    "dateArrivedInUk",
    "commodityCode",
    "mileage",
    "mileageUnits",
    "leftOrRightHand",
    "pricePaidForVehicle",
    "importCurrencyUsed",
    "areYouClaimingRelief",
    "reasonForClaimingRelief"
  ).map(field => s"checkVehicleSpreadsheetDetails.download.csv.$field").mkString("", ",", "\r\n")

  private val importMotorcycleHeadings = Seq(
    "itemNumber",
    "importEntryNumber",
    "importEntryDate",
    "dateRoadUseKnown",
    "countryOfFirstRegistration",
    "dateOfFirstRegistration",
    "make",
    "model",
    "derivative",
    "motorcycleVersion",
    "motorcycleType",
    "motorcycleStyle",
    "transmission",
    "fuelType",
    "engineSize",
    "notificationReference",
    "vehicleIdNumber",
    "dateArrivedInUk",
    "commodityCode",
    "mileage",
    "mileageUnits",
    "leftOrRightHand",
    "pricePaidForVehicle",
    "importCurrencyUsed",
    "areYouClaimingRelief",
    "reasonForClaimingRelief"
  ).map(field => s"checkVehicleSpreadsheetDetails.download.csv.$field").mkString("", ",", "\r\n")

  private val importAgriculturalTractorHeadings = Seq(
    "itemNumber",
    "importEntryNumber",
    "importEntryDate",
    "dateRoadUseKnown",
    "countryOfFirstRegistration",
    "dateOfFirstRegistration",
    "make",
    "seriesModel",
    "versionDerivative",
    "brakeHorsePower",
    "notificationReference",
    "vehicleIdNumber",
    "dateArrivedInUk",
    "commodityCode",
    "mileage",
    "mileageUnits",
    "leftOrRightHand",
    "pricePaidForVehicle",
    "importCurrencyUsed",
    "areYouClaimingRelief",
    "reasonForClaimingRelief"
  ).map(field => s"checkVehicleSpreadsheetDetails.download.csv.$field").mkString("", ",", "\r\n")

  private val supplierHeavyCommercialVehicle = SpreadsheetHeavyCommercialEuVehicle(
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
    countryOfFirstRegistration = Some("DE"),
    dateOfFirstRegistration = Some(LocalDate.of(2010, 1, 1)),
    dateMadeAvailable = Some(LocalDate.of(2026, 3, 29)),
    purchaseInvoice = Some(true),
    noPurchaseInvoiceReason = None,
    purchaseInvoiceDate = Some(LocalDate.of(2026, 3, 30)),
    purchaseInvoiceNumber = Some("1"),
    pricePaid = Some(BigDecimal("100")),
    currency = Some("USD"),
    make = Some("Make"),
    model = Some("Model"),
    heavyCommercialVehicleType = Some("Heavy commercial vehicle type"),
    cabType = Some("Cab type"),
    notificationReference = None,
    vin = Some("123"),
    currentRegistrationNumber = Some("TEST 53R"),
    dateArrivedInUk = Some(LocalDate.of(2026, 3, 30)),
    mileage = Some("100000"),
    mileageUnits = Some("KM"),
    leftOrRightHandDrive = Some("RHD"),
    totalValueOfOptions = Some(BigDecimal("10000")),
    obtainedFromUnableToReclaimVat = Some(false),
    soldUnderMarginScheme = Some(false),
    claimingVatRelief = Some(false),
    reasonForClaimingRelief = Some("MODIFIED_FOR_DISABLED_PASSENGER")
  )

  private val importMotorCaravan = SpreadsheetMotorCaravansNonEuVehicle(
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

  private val importMotorcycle = SpreadsheetMotorcyclesNonEuVehicle(
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

  private val importAgriculturalTractor = SpreadsheetAgriculturalTractorNonEuVehicle(
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

  private val importCar = SpreadsheetNonEuVehicle(
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
    reasonForClaimingRelief = Some("MODIFIED_FOR_DISABLED_PASSENGER")
  )

  private def validatedResult(vehicles: Seq[SpreadsheetNonEuVehicle], validationType: Option[String] = Some("CarsNonEu")) =
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

  private def importCarCsvFor(vehicles: SpreadsheetNonEuVehicle*): Option[String] = SpreadsheetDataCsv(validatedResult(vehicles))

  "SpreadsheetDataCsv" - {

    "must write the headings followed by one row per vehicle for imported cars" in {
      importCarCsvFor(importCar, importCar.copy(itemNumber = Some(2), vin = Some("2"))).value mustEqual
        s"${importCarHeadings}1,123-123456A,30/03/2026,Y,FR,01/01/2010,Make,Model,Derivative,Trim,Body type,NOVA00E000000,1,01/03/2026,1234,10000,miles,RHD,100,USD,N,MODIFIED_FOR_DISABLED_PASSENGER\r\n" +
        "2,123-123456A,30/03/2026,Y,FR,01/01/2010,Make,Model,Derivative,Trim,Body type,NOVA00E000000,2,01/03/2026,1234,10000,miles,RHD,100,USD,N,MODIFIED_FOR_DISABLED_PASSENGER\r\n"
    }

    "must write the headings followed by one row for an imported light commercial vehicle" in {
      SpreadsheetDataCsv(validatedResult(Seq(importCar), validationType = Some("LightCommercialVehiclesNonEu"))).value mustEqual
        s"${importLcvHeadings}1,123-123456A,30/03/2026,Y,FR,01/01/2010,Make,Model,Derivative,Trim,Body type,NOVA00E000000,1,01/03/2026,1234,10000,miles,RHD,100,USD,N,MODIFIED_FOR_DISABLED_PASSENGER\r\n"
    }

    "must write the headings followed by one row for a heavy commercial vehicle bought from an EU supplier" in {
      SpreadsheetDataCsv(
        validatedResult(Nil, validationType = Some("HeavyCommercialVehiclesEu"))
          .copy(heavyCommercialEuVehicles = List(supplierHeavyCommercialVehicle))
      ).value mustEqual
        s"${supplierHeavyCommercialVehicleHeadings}1,business,business name,,,,Address 1,Address 2,,,,,,N,,,N,DE,01/01/2010,29/03/2026,Y,,30/03/2026,1,100,USD,Make,Model,Heavy commercial vehicle type,Cab type,,123,TEST 53R,30/03/2026,100000,km,RHD,10000,N,N,N,MODIFIED_FOR_DISABLED_PASSENGER\r\n"
    }

    "must write the headings followed by one row for an imported motor caravan" in {
      SpreadsheetDataCsv(
        validatedResult(Nil, validationType = Some("MotorCaravansNonEu")).copy(motorCaravansNonEuVehicles = Seq(importMotorCaravan))
      ).value mustEqual
        s"${importMotorCaravanHeadings}1,123-123456A,30/03/2026,Y,,01/01/2010,Make of motor caravan,Model name / number,Motor caravan version,Motor caravan body,Make of base vehicle,Derivative,,1,01/03/2026,1234,10000,miles,RHD,100,USD,N,\r\n"
    }

    "must write the headings followed by one row for an imported motorcycle" in {
      SpreadsheetDataCsv(
        validatedResult(Nil, validationType = Some("MotorcyclesNonEu")).copy(motorcyclesNonEuVehicles = Seq(importMotorcycle))
      ).value mustEqual
        s"${importMotorcycleHeadings}1,123-123456A,30/03/2026,Y,,01/01/2010,Make,Model,Derivative,Version,Type of motorcycle,Style of motorcycle,Transmission type,Fuel type,124,,1,01/03/2026,1234,10000,miles,RHD,100,USD,N,\r\n"
    }

    "must write the headings followed by one row for an imported agricultural tractor" in {
      SpreadsheetDataCsv(
        validatedResult(Nil, validationType = Some("AgriculturalTractorsNonEu"))
          .copy(agriculturalTractorNonEuVehicles = Seq(importAgriculturalTractor))
      ).value mustEqual
        s"${importAgriculturalTractorHeadings}1,123-123456A,30/03/2026,Y,,01/01/2010,Make,Series (model),Version (derivative),100,,1,01/03/2026,1234,10000,miles,RHD,100,USD,N,\r\n"
    }

    "must write only the headings for an empty list" in {
      importCarCsvFor().value mustEqual importCarHeadings
    }

    "must quote a field that contains a comma" in {
      importCarCsvFor(importCar.copy(make = Some("Make, Model"))).value mustEqual
        s"${importCarHeadings}1,123-123456A,30/03/2026,Y,FR,01/01/2010,\"Make, Model\",Model,Derivative,Trim,Body type,NOVA00E000000,1,01/03/2026,1234,10000,miles,RHD,100,USD,N,MODIFIED_FOR_DISABLED_PASSENGER\r\n"
    }

    "must quote a field that contains a double quote and double it" in {
      SpreadsheetDataCsv(
        validatedResult(Nil, validationType = Some("HeavyCommercialVehiclesEu"))
          .copy(heavyCommercialEuVehicles = List(supplierHeavyCommercialVehicle.copy(supplierBusinessName = Some("Company \"1\" Ltd"))))
      ).value mustEqual
        s"${supplierHeavyCommercialVehicleHeadings}1,business,\"Company \"\"1\"\" Ltd\",,,,Address 1,Address 2,,,,,,N,,,N,DE,01/01/2010,29/03/2026,Y,,30/03/2026,1,100,USD,Make,Model,Heavy commercial vehicle type,Cab type,,123,TEST 53R,30/03/2026,100000,km,RHD,10000,N,N,N,MODIFIED_FOR_DISABLED_PASSENGER\r\n"
    }

    "must leave a missing answer blank" in {
      importCarCsvFor(
        importCar.copy(itemNumber = None, importEntryDate = None, knownDateFirstRegistered = None, trim = None, pricePaid = None)
      ).value mustEqual
        s"$importCarHeadings,123-123456A,,,FR,01/01/2010,Make,Model,Derivative,,Body type,NOVA00E000000,1,01/03/2026,1234,10000,miles,RHD,,USD,N,MODIFIED_FOR_DISABLED_PASSENGER\r\n"
    }

    "must not write a CSV for an unknown vehicle type" in {
      SpreadsheetDataCsv(validatedResult(Seq(importCar), validationType = Some("Unknown"))) mustBe None
    }
  }
}
