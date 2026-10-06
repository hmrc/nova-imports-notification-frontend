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
import models.{AddVehicleDetailsHeavyCommercial, VehicleNumber}
import pages.sections.vehicledetails.AddVehicleDetailsHeavyCommercialPage

class AddVehicleDetailsHeavyCommercialPageSpec extends SpecBase {

  "AddVehicleDetailsHeavyCommercialPage" - {

    "must store the heavy commercial vehicle details under the vehicle's details" in {
      val addVehicleDetailsHeavyCommercial =
        AddVehicleDetailsHeavyCommercial(make = "Renault Trucks", model = "Magnum", hcvType = "midlift axle tractor", cabType = "sleeper cab")
      val answers = emptyUserAnswers.unsafeSet(AddVehicleDetailsHeavyCommercialPage(VehicleNumber(2)), addVehicleDetailsHeavyCommercial)

      (answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsHeavyCommercial" \ "make").as[String] mustBe "Renault Trucks"
      (answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsHeavyCommercial" \ "model").as[String] mustBe "Magnum"
      (answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsHeavyCommercial" \ "hcvType").as[String] mustBe "midlift axle tractor"
      (answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsHeavyCommercial" \ "cabType").as[String] mustBe "sleeper cab"
    }
  }
}
