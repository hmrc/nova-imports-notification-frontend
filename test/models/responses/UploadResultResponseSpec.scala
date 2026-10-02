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

package models.responses

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

import java.time.LocalDate

class UploadResultResponseSpec extends AnyFreeSpec with Matchers {

  "UploadResultResponse.reads" - {

    "must read the summary and full Eu vehicle details for a CarsEu response" in {
      val json = Json.parse(
        """{
          |  "fileStatus": "VALIDATED",
          |  "validationType": "CarsEu",
          |  "data": {
          |    "vehicles": [
          |      {
          |        "itemNumber": 1,
          |        "vin": "1ABCD2EO3FGI45678",
          |        "make": "Volkswagen",
          |        "model": "Golf",
          |        "supplierBusinessPrivate": "business",
          |        "supplierBusinessName": "Autohaus Berlin",
          |        "addressLine1": "12 Hauptstrasse",
          |        "country": "DE",
          |        "supplierVatRegistered": true,
          |        "euMemberState": "DE",
          |        "supplierVatNumber": "811569869",
          |        "purchaseInvoice": true,
          |        "purchaseInvoiceNumber": "INV-4471",
          |        "purchaseInvoiceDate": "2026-07-01",
          |        "pricePaid": "18500.00",
          |        "currency": "EUR",
          |        "derivative": "Life",
          |        "trim": "TSI 130",
          |        "bodyType": "Hatchback",
          |        "dateArrivedInUk": "2026-07-20",
          |        "mileage": "21500",
          |        "mileageUnits": "km",
          |        "leftOrRightHandDrive": "LHD",
          |        "totalValueOfOptions": "1250.50",
          |        "obtainedFromUnableToReclaimVat": false,
          |        "soldUnderMarginScheme": false,
          |        "claimingVatRelief": false
          |      }
          |    ]
          |  },
          |  "errors": []
          |}""".stripMargin
      )

      val result = json.as[UploadResultResponse]

      result.fileStatus mustBe "VALIDATED"
      result.validationType mustBe Some("CarsEu")
      result.vehicles must have size 1
      result.vehicles.head.vin mustBe Some("1ABCD2EO3FGI45678")
      result.euVehicles must have size 1

      val vehicle = result.euVehicles.head
      vehicle.make mustBe Some("Volkswagen")
      vehicle.supplierBusinessName mustBe Some("Autohaus Berlin")
      vehicle.pricePaid mustBe Some(BigDecimal("18500.00"))
      vehicle.totalValueOfOptions mustBe Some(BigDecimal("1250.50"))
    }

    "must leave euVehicles empty and read the full NonEu vehicle details for a CarsNonEu response" in {
      val json = Json.parse(
        """{
          |  "fileStatus": "VALIDATED",
          |  "validationType": "CarsNonEu",
          |  "data": {
          |    "vehicles": [
          |      {
          |        "itemNumber": 1,
          |        "vin": "1ABCD2EO3FGI45678",
          |        "make": "Ford",
          |        "model": "Focus",
          |        "derivative": "Titanium",
          |        "trim": "X",
          |        "bodyType": "Hatchback",
          |        "importEntryNumber": "12GB3456ACD789EF21",
          |        "importEntryDate": "2026-03-30",
          |        "knownDateFirstRegistered": true,
          |        "dateOfFirstRegistration": "2010-01-01",
          |        "dateArrivedInUk": "2026-03-01",
          |        "mileage": "30000",
          |        "mileageUnits": "MILES",
          |        "leftOrRightHandDrive": "RHD",
          |        "pricePaid": "18500.00",
          |        "currency": "GBP",
          |        "commodityCode": "8703239019",
          |        "claimingVatRelief": false
          |      }
          |    ]
          |  },
          |  "errors": []
          |}""".stripMargin
      )

      val result = json.as[UploadResultResponse]

      result.validationType mustBe Some("CarsNonEu")
      result.vehicles must have size 1
      result.vehicles.head.make mustBe Some("Ford")
      result.euVehicles mustBe Seq.empty
      result.nonEuVehicles must have size 1

      val vehicle = result.nonEuVehicles.head
      vehicle.make mustBe Some("Ford")
      vehicle.importEntryNumber mustBe Some("12GB3456ACD789EF21")
      vehicle.commodityCode mustBe Some("8703239019")
      vehicle.mileage mustBe Some("30000")
      vehicle.pricePaid mustBe Some(BigDecimal("18500.00"))
    }

    "must route vehicles by validationType, not by which shape happens to validate" in {
      val vehicleJson =
        """{
          |  "itemNumber": 1,
          |  "vin": "1ABCD2EO3FGI45678",
          |  "make": "Ford",
          |  "model": "Focus",
          |  "derivative": "Titanium",
          |  "trim": "X",
          |  "bodyType": "Hatchback",
          |  "dateArrivedInUk": "2026-03-01",
          |  "mileage": "30000",
          |  "mileageUnits": "MILES",
          |  "leftOrRightHandDrive": "RHD",
          |  "pricePaid": "18500.00",
          |  "currency": "GBP",
          |  "claimingVatRelief": false
          |}""".stripMargin

      def responseWith(validationType: String) =
        Json.parse(s"""{"fileStatus":"VALIDATED","validationType":"$validationType","data":{"vehicles":[$vehicleJson]},"errors":[]}""")

      val euResult    = responseWith("CarsEu").as[UploadResultResponse]
      val nonEuResult = responseWith("CarsNonEu").as[UploadResultResponse]

      euResult.euVehicles must have size 1
      euResult.nonEuVehicles mustBe Seq.empty

      nonEuResult.euVehicles mustBe Seq.empty
      nonEuResult.nonEuVehicles must have size 1
    }

    "must default validationType, vehicles, euVehicles and nonEuVehicles when data is absent" in {
      val json = Json.parse("""{"fileStatus":"VERIFYING","errors":[]}""")

      val result = json.as[UploadResultResponse]

      result.validationType mustBe None
      result.vehicles mustBe Seq.empty
      result.euVehicles mustBe Seq.empty
      result.nonEuVehicles mustBe Seq.empty
    }

    "must read agriculturalTractorEuVehicles for an AgriculturalTractorsEu response and leave the other lists empty" in {
      val json = Json.parse(
        """{
          |  "fileStatus": "VALIDATED",
          |  "validationType": "AgriculturalTractorsEu",
          |  "data": {
          |    "vehicles": [
          |      {
          |        "itemNumber": 1,
          |        "vin": "1",
          |        "make": "Make",
          |        "seriesModel": "Series (model)",
          |        "versionDerivative": "Version (derivative)",
          |        "brakeHorsepower": "154",
          |        "supplierBusinessPrivate": "business",
          |        "addressLine1": "Address 1",
          |        "supplierVatRegistered": false,
          |        "purchaseInvoice": true,
          |        "pricePaid": "1202",
          |        "currency": "USD",
          |        "dateArrivedInUk": "2026-10-10",
          |        "mileage": "100",
          |        "mileageUnits": "miles",
          |        "leftOrRightHandDrive": "RHD",
          |        "totalValueOfOptions": "100",
          |        "obtainedFromUnableToReclaimVat": false,
          |        "soldUnderMarginScheme": false,
          |        "claimingVatRelief": false
          |      }
          |    ]
          |  },
          |  "errors": []
          |}""".stripMargin
      )

      val result = json.as[UploadResultResponse]

      result.validationType mustBe Some("AgriculturalTractorsEu")
      result.euVehicles mustBe Seq.empty
      result.nonEuVehicles mustBe Seq.empty
      result.agriculturalTractorNonEuVehicles mustBe Seq.empty
      result.agriculturalTractorEuVehicles must have size 1
      result.vehicles                      must have size 1
      result.vehicles.head.make mustBe Some("Make")
      result.vehicles.head.model mustBe Some("Series (model)")

      val vehicle = result.agriculturalTractorEuVehicles.head
      vehicle.make mustBe Some("Make")
      vehicle.seriesModel mustBe Some("Series (model)")
      vehicle.versionDerivative mustBe Some("Version (derivative)")
      vehicle.brakeHorsepower mustBe Some("154")
      vehicle.pricePaid mustBe Some(BigDecimal("1202"))
    }

    "must read agriculturalTractorNonEuVehicles for an AgriculturalTractorsNonEu response and leave the other lists empty" in {
      val json = Json.parse(
        """{
          |  "fileStatus": "VALIDATED",
          |  "validationType": "AgriculturalTractorsNonEu",
          |  "data": {
          |    "vehicles": [
          |      {
          |        "itemNumber": 1,
          |        "vin": "1",
          |        "make": "Make",
          |        "seriesModel": "Series (model)",
          |        "versionDerivative": "Version (derivative)",
          |        "brakeHorsepower": "100",
          |        "importEntryNumber": "123-123456A",
          |        "importEntryDate": "2026-10-10",
          |        "knownDateFirstRegistered": false,
          |        "dateArrivedInUk": "2026-10-10",
          |        "commodityCode": "1234",
          |        "mileage": "10",
          |        "mileageUnits": "miles",
          |        "leftOrRightHandDrive": "RHD",
          |        "pricePaid": "1201",
          |        "currency": "USD",
          |        "claimingVatRelief": false
          |      }
          |    ]
          |  },
          |  "errors": []
          |}""".stripMargin
      )

      val result = json.as[UploadResultResponse]

      result.validationType mustBe Some("AgriculturalTractorsNonEu")
      result.euVehicles mustBe Seq.empty
      result.nonEuVehicles mustBe Seq.empty
      result.agriculturalTractorEuVehicles mustBe Seq.empty
      result.agriculturalTractorNonEuVehicles must have size 1
      result.vehicles                         must have size 1
      result.vehicles.head.make mustBe Some("Make")
      result.vehicles.head.model mustBe Some("Series (model)")

      val vehicle = result.agriculturalTractorNonEuVehicles.head
      vehicle.make mustBe Some("Make")
      vehicle.seriesModel mustBe Some("Series (model)")
      vehicle.versionDerivative mustBe Some("Version (derivative)")
      vehicle.brakeHorsepower mustBe Some("100")
      vehicle.importEntryNumber mustBe Some("123-123456A")
    }

    "must read motorCaravansEuVehicles for a MotorCaravansEu response and leave the other lists empty" in {
      val json = Json.parse(
        """{
          |  "fileStatus": "VALIDATED",
          |  "validationType": "MotorCaravansEu",
          |  "data": {
          |    "vehicles": [
          |      {
          |        "itemNumber": 1,
          |        "vin": "1",
          |        "caravanMake": "Make of motor caravan",
          |        "modelNameNumber": "Motor caravan body",
          |        "caravanVersion": "Model name/number",
          |        "caravanBody": "Motor caravan version",
          |        "makeOfBaseVehicle": "Make of base vehicle",
          |        "derivative": "Derivative",
          |        "supplierBusinessPrivate": "business",
          |        "addressLine1": "Address 1",
          |        "supplierVatRegistered": false,
          |        "purchaseInvoice": true,
          |        "pricePaid": "100",
          |        "currency": "USD",
          |        "dateArrivedInUk": "2026-10-10",
          |        "mileage": "100",
          |        "mileageUnits": "miles",
          |        "leftOrRightHandDrive": "LHD",
          |        "totalValueOfOptions": "100",
          |        "obtainedFromUnableToReclaimVat": false,
          |        "soldUnderMarginScheme": false,
          |        "claimingVatRelief": false
          |      }
          |    ]
          |  },
          |  "errors": []
          |}""".stripMargin
      )

      val result = json.as[UploadResultResponse]

      result.validationType mustBe Some("MotorCaravansEu")
      result.euVehicles mustBe Seq.empty
      result.nonEuVehicles mustBe Seq.empty
      result.motorCaravansNonEuVehicles mustBe Seq.empty
      result.motorCaravansEuVehicles must have size 1
      result.vehicles                must have size 1
      result.vehicles.head.make mustBe Some("Make of motor caravan")
      result.vehicles.head.model mustBe Some("Motor caravan body")

      val vehicle = result.motorCaravansEuVehicles.head
      vehicle.caravanMake mustBe Some("Make of motor caravan")
      vehicle.modelNameNumber mustBe Some("Motor caravan body")
      vehicle.caravanVersion mustBe Some("Model name/number")
      vehicle.caravanBody mustBe Some("Motor caravan version")
      vehicle.makeOfBaseVehicle mustBe Some("Make of base vehicle")
      vehicle.derivative mustBe Some("Derivative")
      vehicle.pricePaid mustBe Some(BigDecimal("100"))
    }

    "must read motorCaravansNonEuVehicles for a MotorCaravansNonEu response and leave the other lists empty" in {
      val json = Json.parse(
        """{
          |  "fileStatus": "VALIDATED",
          |  "validationType": "MotorCaravansNonEu",
          |  "data": {
          |    "vehicles": [
          |      {
          |        "itemNumber": 1,
          |        "vin": "1",
          |        "caravanMake": "Make of motor caravan",
          |        "modelNameNumber": "Motor caravan body",
          |        "caravanVersion": "Model name / number",
          |        "caravanBody": "Motor caravan version",
          |        "makeOfBaseVehicle": "Make of base vehicle",
          |        "derivative": "Derivative",
          |        "importEntryNumber": "123-123456A",
          |        "importEntryDate": "2026-10-10",
          |        "knownDateFirstRegistered": false,
          |        "dateArrivedInUk": "2026-10-10",
          |        "commodityCode": "1234",
          |        "mileage": "154",
          |        "mileageUnits": "miles",
          |        "leftOrRightHandDrive": "RHD",
          |        "pricePaid": "125.00",
          |        "currency": "USD",
          |        "claimingVatRelief": false
          |      }
          |    ]
          |  },
          |  "errors": []
          |}""".stripMargin
      )

      val result = json.as[UploadResultResponse]

      result.validationType mustBe Some("MotorCaravansNonEu")
      result.euVehicles mustBe Seq.empty
      result.nonEuVehicles mustBe Seq.empty
      result.motorCaravansEuVehicles mustBe Seq.empty
      result.motorCaravansNonEuVehicles must have size 1
      result.vehicles                   must have size 1
      result.vehicles.head.make mustBe Some("Make of motor caravan")
      result.vehicles.head.model mustBe Some("Motor caravan body")

      val vehicle = result.motorCaravansNonEuVehicles.head
      vehicle.caravanMake mustBe Some("Make of motor caravan")
      vehicle.modelNameNumber mustBe Some("Motor caravan body")
      vehicle.caravanVersion mustBe Some("Model name / number")
      vehicle.caravanBody mustBe Some("Motor caravan version")
      vehicle.makeOfBaseVehicle mustBe Some("Make of base vehicle")
      vehicle.derivative mustBe Some("Derivative")
      vehicle.importEntryNumber mustBe Some("123-123456A")
    }

    "must read heavyCommercialEuVehicles for a HeavyCommercialVehiclesEu response and leave the other lists empty" in {
      val json = Json.parse(
        """{
          |  "fileStatus": "VALIDATED",
          |  "validationType": "HeavyCommercialVehiclesEu",
          |  "data": {
          |    "vehicles": [
          |      {
          |        "itemNumber": 1,
          |        "vin": "1",
          |        "make": "Make",
          |        "model": "Model",
          |        "heavyCommercialVehicleType": "Heavy commercial vehicle type",
          |        "cabType": "Cab type",
          |        "supplierBusinessPrivate": "business",
          |        "addressLine1": "address 1",
          |        "supplierVatRegistered": false,
          |        "purchaseInvoice": true,
          |        "purchaseInvoiceDate": "2026-01-01",
          |        "purchaseInvoiceNumber": "1",
          |        "pricePaid": "10.00",
          |        "currency": "USD",
          |        "dateArrivedInUk": "2026-01-01",
          |        "mileage": "10",
          |        "mileageUnits": "miles",
          |        "leftOrRightHandDrive": "LHD",
          |        "totalValueOfOptions": "10.00",
          |        "obtainedFromUnableToReclaimVat": false,
          |        "soldUnderMarginScheme": false,
          |        "claimingVatRelief": false
          |      }
          |    ]
          |  },
          |  "errors": []
          |}""".stripMargin
      )

      val result = json.as[UploadResultResponse]

      result.validationType mustBe Some("HeavyCommercialVehiclesEu")
      result.euVehicles mustBe List.empty
      result.nonEuVehicles mustBe List.empty
      result.agriculturalTractorEuVehicles mustBe List.empty
      result.motorCaravansEuVehicles mustBe List.empty
      result.heavyCommercialNonEuVehicles mustBe List.empty
      result.heavyCommercialEuVehicles must have size 1
      result.vehicles                  must have size 1
      result.vehicles.head.make mustBe Some("Make")
      result.vehicles.head.model mustBe Some("Model")

      val vehicle = result.heavyCommercialEuVehicles.head
      vehicle.make mustBe Some("Make")
      vehicle.model mustBe Some("Model")
      vehicle.heavyCommercialVehicleType mustBe Some("Heavy commercial vehicle type")
      vehicle.cabType mustBe Some("Cab type")
      vehicle.pricePaid mustBe Some(BigDecimal("10.00"))
      vehicle.dateArrivedInUk mustBe Some(LocalDate.of(2026, 1, 1))
    }

    "must read heavyCommercialNonEuVehicles for a HeavyCommercialVehiclesNonEu response and leave the other lists empty" in {
      val json = Json.parse(
        """{
          |  "fileStatus": "VALIDATED",
          |  "validationType": "HeavyCommercialVehiclesNonEu",
          |  "data": {
          |    "vehicles": [
          |      {
          |        "itemNumber": 1,
          |        "importEntryNumber": "123-123456A",
          |        "importEntryDate": "2026-01-01",
          |        "knownDateFirstRegistered": false,
          |        "make": "Make",
          |        "model": "Model",
          |        "heavyCommercialVehicleType": "Heavy commercial vehicle type",
          |        "cabType": "Cab type",
          |        "vin": "1",
          |        "dateArrivedInUk": "2026-01-01",
          |        "commodityCode": "1234",
          |        "mileage": "10",
          |        "mileageUnits": "km",
          |        "leftOrRightHandDrive": "LHD",
          |        "pricePaid": "10.00",
          |        "currency": "USD",
          |        "claimingVatRelief": false
          |      }
          |    ]
          |  },
          |  "errors": []
          |}""".stripMargin
      )

      val result = json.as[UploadResultResponse]

      result.validationType mustBe Some("HeavyCommercialVehiclesNonEu")
      result.euVehicles mustBe List.empty
      result.nonEuVehicles mustBe List.empty
      result.agriculturalTractorNonEuVehicles mustBe List.empty
      result.motorCaravansNonEuVehicles mustBe List.empty
      result.heavyCommercialEuVehicles mustBe List.empty
      result.heavyCommercialNonEuVehicles must have size 1
      result.vehicles                     must have size 1
      result.vehicles.head.make mustBe Some("Make")
      result.vehicles.head.model mustBe Some("Model")

      val vehicle = result.heavyCommercialNonEuVehicles.head
      vehicle.importEntryNumber mustBe Some("123-123456A")
      vehicle.commodityCode mustBe Some("1234")
      vehicle.heavyCommercialVehicleType mustBe Some("Heavy commercial vehicle type")
      vehicle.cabType mustBe Some("Cab type")
    }
  }
}
