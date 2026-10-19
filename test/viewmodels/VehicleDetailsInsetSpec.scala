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
import models.*
import pages.sections.vehicledetails.*
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

class VehicleDetailsInsetSpec extends SpecBase {

  implicit val msgs: Messages = stubMessages()

  private val vehicleNumber = VehicleNumber(1)

  private def answersFor(vehicleType: AddVehicleType): UserAnswers =
    emptyUserAnswers.unsafeSet(AddVehicleTypePage(vehicleNumber), vehicleType)

  "VehicleDetailsInset.details" - {

    "must give the make, model, derivative, trim and body type for a car" in {
      val answers = answersFor(AddVehicleType.Car)
        .unsafeSet(AddVehicleDetailsCarPage(vehicleNumber), AddVehicleDetailsCar("Ford", "Focus", "1.0 EcoBoost", "Titanium", "Hatchback"))

      VehicleDetailsInset.details(answers, vehicleNumber).value mustEqual Seq("Ford", "Focus", "1.0 EcoBoost", "Titanium", "Hatchback")
    }

    "must give the make, model, derivative, trim and body type for a light commercial vehicle" in {
      val answers = answersFor(AddVehicleType.Lcv)
        .unsafeSet(
          AddVehicleDetailsLightCommercialPage(vehicleNumber),
          LightCommercialVehicleDetails("Ford", "Transit", "2.0 TDCi", "Trend", "Panel van")
        )

      VehicleDetailsInset.details(answers, vehicleNumber).value mustEqual Seq("Ford", "Transit", "2.0 TDCi", "Trend", "Panel van")
    }

    "must give the make and model for a heavy commercial vehicle" in {
      val answers = answersFor(AddVehicleType.Hcv)
        .unsafeSet(
          AddVehicleDetailsHeavyCommercialPage(vehicleNumber),
          AddVehicleDetailsHeavyCommercial("Volvo", "FH16", "Tractor unit", "Sleeper cab")
        )

      VehicleDetailsInset.details(answers, vehicleNumber).value mustEqual Seq("Volvo", "FH16")
    }

    "must give the make, model and derivative for a motorcycle, scooter or moped" in {
      val answers = answersFor(AddVehicleType.Motorcycle)
        .unsafeSet(
          AddVehicleDetailsMotorcyclePage(vehicleNumber),
          AddVehicleDetailsMotorcycle(
            "Honda",
            "CB500F",
            "ABS",
            "2024",
            "Naked",
            "Roadster",
            "Manual",
            MotorcycleFuelType.Petrol,
            "471"
          )
        )

      VehicleDetailsInset.details(answers, vehicleNumber).value mustEqual Seq("Honda", "CB500F", "ABS")
    }

    "must give the make of base vehicle and derivative for a motor caravan" in {
      val answers = answersFor(AddVehicleType.MotorCaravan)
        .unsafeSet(
          AddVehicleDetailsMotorCaravanPage(vehicleNumber),
          AddVehicleDetailsMotorCaravan("Swift", "Coachbuilt", "Escape", "694", "Fiat", "2.3TD")
        )

      VehicleDetailsInset.details(answers, vehicleNumber).value mustEqual Seq("Fiat", "2.3TD")
    }

    "must give the make and model for an agricultural tractor" in {
      val answers = answersFor(AddVehicleType.AgriculturalTractor)
        .unsafeSet(
          AddVehicleDetailsAgriculturalTractorPage(vehicleNumber),
          AddVehicleDetailsAgriculturalTractor("John Deere", "6R 150", "AutoPowr", "150")
        )

      VehicleDetailsInset.details(answers, vehicleNumber).value mustEqual Seq("John Deere", "6R 150")
    }

    "must give the make and model for construction, plant and machinery or a special purpose vehicle" in {
      val answers = answersFor(AddVehicleType.ContractorsPlant)
        .unsafeSet(AddVehicleDetailsContractorsPlantPage(vehicleNumber), AddVehicleDetailsContractorsPlant("JCB", "3CX", "Sitemaster"))

      VehicleDetailsInset.details(answers, vehicleNumber).value mustEqual Seq("JCB", "3CX")
    }

    "must be empty when the vehicle type has not been answered" in {
      VehicleDetailsInset.details(emptyUserAnswers, vehicleNumber) mustBe None
    }

    "must be empty when the details for the selected vehicle type have not been answered" in {
      VehicleDetailsInset.details(answersFor(AddVehicleType.Car), vehicleNumber) mustBe None
    }

    "must ignore details saved for a different vehicle type" in {
      val answers = answersFor(AddVehicleType.Hcv)
        .unsafeSet(AddVehicleDetailsCarPage(vehicleNumber), AddVehicleDetailsCar("Ford", "Focus", "1.0 EcoBoost", "Titanium", "Hatchback"))

      VehicleDetailsInset.details(answers, vehicleNumber) mustBe None
    }
  }

  "VehicleDetailsInset" - {

    "must show the label followed by the vehicle details in bold" in {
      val answers = answersFor(AddVehicleType.Hcv)
        .unsafeSet(
          AddVehicleDetailsHeavyCommercialPage(vehicleNumber),
          AddVehicleDetailsHeavyCommercial("Volvo", "FH16", "Tractor unit", "Sleeper cab")
        )

      VehicleDetailsInset(answers, vehicleNumber).value.content.asHtml.toString mustEqual
        s"${msgs("vehicleDetailsInset.label")} <b>Volvo FH16</b>"
    }

    "must escape the vehicle details" in {
      val answers = answersFor(AddVehicleType.Hcv)
        .unsafeSet(
          AddVehicleDetailsHeavyCommercialPage(vehicleNumber),
          AddVehicleDetailsHeavyCommercial("<script>", "FH16", "Tractor unit", "Sleeper cab")
        )

      VehicleDetailsInset(answers, vehicleNumber).value.content.asHtml.toString must include("<b>&lt;script&gt; FH16</b>")
    }

    "must not be shown when there are no vehicle details" in {
      VehicleDetailsInset(emptyUserAnswers, vehicleNumber) mustBe None
    }
  }
}
