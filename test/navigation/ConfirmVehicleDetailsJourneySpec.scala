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

package navigation

import base.SpecBase
import controllers.{routes, vehicledetails}
import models.{AddVehicleType, ImportNumber, NormalMode, SupplierNumber, UserAnswers, VehicleNumber}
import pages.sections.vehicledetails.AddVehicleTypePage
import play.api.libs.json.Json
import queries.AllVehiclesQuery

class ConfirmVehicleDetailsJourneySpec extends SpecBase {

  private val v = VehicleNumber(1)
  private val s = SupplierNumber(1)
  private val i = ImportNumber(1)

  private val supplierVehicle: UserAnswers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
  private val importVehicle: UserAnswers   = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1)))

  "confirmedRoute" - {

    "must go to AVD8.1 (AddVehicleDetailsCarController) for a car on the supplier journey" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(supplierVehicle.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Car), v) mustBe
        vehicledetails.routes.AddVehicleDetailsCarController.supplierOnPageLoad(s, v, NormalMode)
    }

    "must go to AVD8.1 (AddVehicleDetailsCarController) for a car on the import journey" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(importVehicle.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Car), v) mustBe
        vehicledetails.routes.AddVehicleDetailsCarController.importOnPageLoad(i, v, NormalMode)
    }

    "must go to JourneyRecovery for a car when the vehicle has no supplier or import" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(emptyUserAnswers.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Car), v) mustBe
        routes.JourneyRecoveryController.onPageLoad()
    }

    "must go to AVD8.2 (AddVehicleDetailsLightCommercialController) for a light commercial vehicle on the supplier journey" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(supplierVehicle.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Lcv), v) mustBe
        vehicledetails.routes.AddVehicleDetailsLightCommercialController.supplierOnPageLoad(s, v, NormalMode)
    }

    "must go to AVD8.2 (AddVehicleDetailsLightCommercialController) for a light commercial vehicle on the import journey" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(importVehicle.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Lcv), v) mustBe
        vehicledetails.routes.AddVehicleDetailsLightCommercialController.importOnPageLoad(i, v, NormalMode)
    }

    "must go to AVD8.3 (AddVehicleDetailsHeavyCommercialController) for a heavy commercial vehicle on the supplier journey" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(supplierVehicle.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Hcv), v) mustBe
        vehicledetails.routes.AddVehicleDetailsHeavyCommercialController.supplierOnPageLoad(s, v, NormalMode)
    }

    "must go to AVD8.3 (AddVehicleDetailsHeavyCommercialController) for a heavy commercial vehicle on the import journey" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(importVehicle.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Hcv), v) mustBe
        vehicledetails.routes.AddVehicleDetailsHeavyCommercialController.importOnPageLoad(i, v, NormalMode)
    }

    "must go to JourneyRecovery for a heavy commercial vehicle when the vehicle has no supplier or import" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(emptyUserAnswers.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Hcv), v) mustBe
        routes.JourneyRecoveryController.onPageLoad()
    }

    "must go to AVD8.4 (AddVehicleDetailsMotorcycleController) for a motorcycle on the supplier journey" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(supplierVehicle.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Motorcycle), v) mustBe
        vehicledetails.routes.AddVehicleDetailsMotorcycleController.supplierOnPageLoad(s, v, NormalMode)
    }

    "must go to AVD8.4 (AddVehicleDetailsMotorcycleController) for a motorcycle on the import journey" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(importVehicle.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Motorcycle), v) mustBe
        vehicledetails.routes.AddVehicleDetailsMotorcycleController.importOnPageLoad(i, v, NormalMode)
    }

    "must go to JourneyRecovery for a motorcycle when the vehicle has no supplier or import" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(emptyUserAnswers.unsafeSet(AddVehicleTypePage(v), AddVehicleType.Motorcycle), v) mustBe
        routes.JourneyRecoveryController.onPageLoad()
    }

    "must go to the AVD8.5 placeholder for a motor caravan" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(emptyUserAnswers.unsafeSet(AddVehicleTypePage(v), AddVehicleType.MotorCaravan), v) mustBe
        routes.LandingPageController.onPageLoad()
    }

    "must go to AVD8.6 (AddVehicleDetailsAgriculturalTractorController) for an agricultural tractor on the supplier journey" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(supplierVehicle.unsafeSet(AddVehicleTypePage(v), AddVehicleType.AgriculturalTractor), v) mustBe
        vehicledetails.routes.AddVehicleDetailsAgriculturalTractorController.supplierOnPageLoad(s, v, NormalMode)
    }

    "must go to AVD8.6 (AddVehicleDetailsAgriculturalTractorController) for an agricultural tractor on the import journey" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(importVehicle.unsafeSet(AddVehicleTypePage(v), AddVehicleType.AgriculturalTractor), v) mustBe
        vehicledetails.routes.AddVehicleDetailsAgriculturalTractorController.importOnPageLoad(i, v, NormalMode)
    }

    "must go to JourneyRecovery for an agricultural tractor when the vehicle has no supplier or import" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(emptyUserAnswers.unsafeSet(AddVehicleTypePage(v), AddVehicleType.AgriculturalTractor), v) mustBe
        routes.JourneyRecoveryController.onPageLoad()
    }

    "must go to the AVD8.7 placeholder for a construction, plant and machinery or special purpose vehicle" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(
        emptyUserAnswers.unsafeSet(AddVehicleTypePage(v), AddVehicleType.ContractorsPlant),
        v
      ) mustBe routes.LandingPageController.onPageLoad()
    }

    "must go to JourneyRecovery when no vehicle type has been answered" in {
      ConfirmVehicleDetailsJourney.confirmedRoute(emptyUserAnswers, v) mustBe routes.JourneyRecoveryController.onPageLoad()
    }
  }
}
