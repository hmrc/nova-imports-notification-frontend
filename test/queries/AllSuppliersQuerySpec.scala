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

class AllSuppliersQuerySpec extends SpecBase {

  "AllSuppliersQuery" - {

    "must store the suppliers keyed by their number" in {
      val suppliers = Map(
        "1" -> Json.obj("areYouSelfSupplying" -> false, "details" -> Json.obj("supplierBusinessName" -> "Test Ltd")),
        "2" -> Json.obj("areYouSelfSupplying" -> true)
      )

      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, suppliers)

      (answers.data \ "suppliers" \ "1" \ "details" \ "supplierBusinessName").as[String] mustBe "Test Ltd"
      (answers.data \ "suppliers" \ "2" \ "areYouSelfSupplying").as[Boolean] mustBe true
    }

    "must read back all the suppliers" in {
      val suppliers = Map(
        "2" -> Json.obj("supplierBusinessName" -> "Second Co Ltd"),
        "5" -> Json.obj("supplierBusinessName" -> "Fifth Co Ltd")
      )

      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, suppliers)

      answers.get(AllSuppliersQuery).value.keys mustBe Set("2", "5")
    }

    "must read back none when the user has no suppliers" in {
      emptyUserAnswers.get(AllSuppliersQuery) mustBe None
    }
  }
}
