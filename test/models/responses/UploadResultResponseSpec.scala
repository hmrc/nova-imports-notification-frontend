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
  }
}
