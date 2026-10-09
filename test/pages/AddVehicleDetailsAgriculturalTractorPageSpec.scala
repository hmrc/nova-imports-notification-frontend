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
import models.{AddVehicleDetailsAgriculturalTractor, VehicleNumber}
import pages.sections.vehicledetails.AddVehicleDetailsAgriculturalTractorPage

class AddVehicleDetailsAgriculturalTractorPageSpec extends SpecBase {

  "AddVehicleDetailsAgriculturalTractorPage" - {

    "must store the agricultural tractor details under the vehicle's details" in {
      val addVehicleDetailsAgriculturalTractor =
        AddVehicleDetailsAgriculturalTractor(make = "John Deere", model = "SR", derivative = "5090M", brakeHorsepower = "240.75")
      val answers = emptyUserAnswers.unsafeSet(AddVehicleDetailsAgriculturalTractorPage(VehicleNumber(2)), addVehicleDetailsAgriculturalTractor)

      (answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsAgriculturalTractor" \ "make").as[String] mustBe "John Deere"
      (answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsAgriculturalTractor" \ "model").as[String] mustBe "SR"
      (answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsAgriculturalTractor" \ "derivative").as[String] mustBe "5090M"
      (answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsAgriculturalTractor" \ "brakeHorsepower").as[String] mustBe "240.75"
    }
  }
}
