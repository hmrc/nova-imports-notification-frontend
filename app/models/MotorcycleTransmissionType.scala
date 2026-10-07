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

enum MotorcycleTransmissionType(val jsonValue: String) {
  case Automatic extends MotorcycleTransmissionType("automatic")
  case Manual extends MotorcycleTransmissionType("manual")
  case SemiAutomatic extends MotorcycleTransmissionType("semiAutomatic")

  override def toString: String = jsonValue
}

object MotorcycleTransmissionType extends Enumerable.Implicits {
  given Enumerable[MotorcycleTransmissionType] = Enumerable(
    Automatic.jsonValue     -> Automatic,
    Manual.jsonValue        -> Manual,
    SemiAutomatic.jsonValue -> SemiAutomatic
  )
}
