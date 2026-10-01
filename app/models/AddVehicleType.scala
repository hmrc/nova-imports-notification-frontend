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

package models

enum AddVehicleType(val jsonValue: String) {
  case AgriculturalTractor extends AddVehicleType("AGRICULTURAL_TRACTOR")
  case Car extends AddVehicleType("CAR")
  case ContractorsPlant extends AddVehicleType("CONTRACTORS_PLANT")
  case Hcv extends AddVehicleType("HCV")
  case Lcv extends AddVehicleType("LCV")
  case Motorcycle extends AddVehicleType("MOTORCYCLE")
  case MotorCaravan extends AddVehicleType("MOTOR_CARAVAN")

  override def toString: String = jsonValue
}

object AddVehicleType extends Enumerable.Implicits {
  given Enumerable[AddVehicleType] = Enumerable(
    AgriculturalTractor.jsonValue -> AgriculturalTractor,
    Car.jsonValue                 -> Car,
    ContractorsPlant.jsonValue    -> ContractorsPlant,
    Hcv.jsonValue                 -> Hcv,
    Lcv.jsonValue                 -> Lcv,
    Motorcycle.jsonValue          -> Motorcycle,
    MotorCaravan.jsonValue        -> MotorCaravan
  )
}
