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

enum AddVehicleDetails(val jsonValue: String) {
  case BySupplier extends AddVehicleDetails("bySupplier")
  case BySpreadsheet extends AddVehicleDetails("bySpreadsheet")

  override def toString: String = jsonValue
}

object AddVehicleDetails extends Enumerable.Implicits {
  given Enumerable[AddVehicleDetails] = Enumerable(
    BySupplier.jsonValue    -> BySupplier,
    BySpreadsheet.jsonValue -> BySpreadsheet
  )
}
