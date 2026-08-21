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

package queries

import base.SpecBase
import play.api.libs.json.Json

class AllVehiclesQuerySpec extends SpecBase {

  "AllVehiclesQuery" - {

    "must store the vehicles keyed by their number, with the supplierNumber they belong to" in {
      val vehicles = Map(
        "1" -> Json.obj("supplierNumber" -> 1, "vin" -> "WBA0001", "make" -> "BMW"),
        "2" -> Json.obj("supplierNumber" -> 2, "vin" -> "WVW0002", "make" -> "VW")
      )

      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, vehicles)

      (answers.data \ "vehicles" \ "1" \ "supplierNumber").as[Int] mustBe 1
      (answers.data \ "vehicles" \ "2" \ "vin").as[String] mustBe "WVW0002"
    }

    "must store a vehicle with the importNumber it belongs to" in {
      val vehicles = Map("1" -> Json.obj("importNumber" -> 1, "vin" -> "WAU0001", "make" -> "Audi"))

      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, vehicles)

      (answers.data \ "vehicles" \ "1" \ "importNumber").as[Int] mustBe 1
    }

    "must read back all the vehicles" in {
      val vehicles = Map(
        "1" -> Json.obj("supplierNumber" -> 1, "vin" -> "WBA0001"),
        "3" -> Json.obj("supplierNumber" -> 1, "vin" -> "WBA0003"),
        "7" -> Json.obj("supplierNumber" -> 2, "vin" -> "WVW0007")
      )

      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, vehicles)

      answers.get(AllVehiclesQuery).value.size mustBe 3
    }

    "must read back none when the user has no vehicles" in {
      emptyUserAnswers.get(AllVehiclesQuery) mustBe None
    }
  }
}
