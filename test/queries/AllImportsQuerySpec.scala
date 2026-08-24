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

class AllImportsQuerySpec extends SpecBase {

  "AllImportsQuery" - {

    "must store the imports correctly keyed by their number" in {
      val imports = Map(
        "1" -> Json.obj("importEntryNumber" -> "123456789A"),
        "2" -> Json.obj("importEntryNumber" -> "123488789A")
      )

      val answers = emptyUserAnswers.unsafeSet(AllImportsQuery, imports)

      (answers.data \ "imports" \ "1" \ "importEntryNumber").as[String] mustBe "123456789A"
      (answers.data \ "imports" \ "2" \ "importEntryNumber").as[String] mustBe "123488789A"
    }

    "must read back all the imports" in {
      val imports = Map(
        "3" -> Json.obj("importEntryNumber" -> "111111111A"),
        "4" -> Json.obj("importEntryNumber" -> "222222222A")
      )

      val answers = emptyUserAnswers.unsafeSet(AllImportsQuery, imports)

      answers.get(AllImportsQuery).value.keys mustBe Set("3", "4")
    }

    "must read back none when the user has no imports" in {
      emptyUserAnswers.get(AllImportsQuery) mustBe None
    }
  }
}
