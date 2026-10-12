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
import models.{AddVehicleDetailsMotorCaravan, VehicleNumber}
import pages.sections.vehicledetails.AddVehicleDetailsMotorCaravanPage

class AddVehicleDetailsMotorCaravanPageSpec extends SpecBase {

  "AddVehicleDetailsMotorCaravanPage" - {

    "must store the motor caravan details under the vehicle's details" in {
      val addVehicleDetailsMotorCaravan = AddVehicleDetailsMotorCaravan(
        make = "Swift",
        bodyType = "a coachbuilt",
        model = "Voyager",
        version = "Voyager 685FB 4 berth",
        makeOfBaseVehicle = "Fiat",
        derivative = "2.3TD"
      )
      val answers = emptyUserAnswers.unsafeSet(AddVehicleDetailsMotorCaravanPage(VehicleNumber(2)), addVehicleDetailsMotorCaravan)

      (answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsMotorCaravan" \ "make").as[String] mustBe "Swift"
      (answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsMotorCaravan" \ "bodyType").as[String] mustBe "a coachbuilt"
      (answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsMotorCaravan" \ "model").as[String] mustBe "Voyager"
      (answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsMotorCaravan" \ "version").as[String] mustBe "Voyager 685FB 4 berth"
      (answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsMotorCaravan" \ "makeOfBaseVehicle").as[String] mustBe "Fiat"
      (answers.data \ "vehicles" \ "2" \ "details" \ "addVehicleDetailsMotorCaravan" \ "derivative").as[String] mustBe "2.3TD"
    }
  }
}
