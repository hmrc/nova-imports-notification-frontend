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

package pages

import base.SpecBase
import models.{AddVehicleDetailsMotorcycle, MotorcycleFuelType, MotorcycleTransmissionType, VehicleNumber}
import pages.sections.vehicledetails.AddVehicleDetailsMotorcyclePage

class AddVehicleDetailsMotorcyclePageSpec extends SpecBase {

  "AddVehicleDetailsMotorcyclePage" - {

    "must store the motorcycle details under the vehicle's details" in {
      val answer = AddVehicleDetailsMotorcycle(
        make = "Honda",
        model = "GL1200",
        derivative = "1200",
        motorcycleVersion = "Gold Wing Deluxe",
        motorcycleType = "a road motorcycle",
        motorcycleStyle = "a tourer",
        transmission = MotorcycleTransmissionType.SemiAutomatic,
        fuelType = MotorcycleFuelType.Hybrid,
        engineSize = "1000.12"
      )
      val answers = emptyUserAnswers.unsafeSet(AddVehicleDetailsMotorcyclePage(VehicleNumber(2)), answer)

      val details = answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsMotorcycle"

      (details \ "make").as[String] mustBe "Honda"
      (details \ "model").as[String] mustBe "GL1200"
      (details \ "derivative").as[String] mustBe "1200"
      (details \ "motorcycleVersion").as[String] mustBe "Gold Wing Deluxe"
      (details \ "motorcycleType").as[String] mustBe "a road motorcycle"
      (details \ "motorcycleStyle").as[String] mustBe "a tourer"
      (details \ "transmission").as[String] mustBe "semiAutomatic"
      (details \ "fuelType").as[String] mustBe "hybrid"
      (details \ "engineSize").as[String] mustBe "1000.12"
    }
  }
}
