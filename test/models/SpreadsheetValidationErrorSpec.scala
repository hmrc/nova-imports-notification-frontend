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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class SpreadsheetValidationErrorSpec extends AnyFreeSpec with Matchers {

  "SpreadsheetValidationError.format" - {

    "must read an item number and message" in {
      val json = Json.parse("""{"itemNumber":3,"message":"Registration number is missing"}""")

      json.as[SpreadsheetValidationError] mustBe SpreadsheetValidationError(3, "Registration number is missing")
    }

    "must round-trip through writes and reads" in {
      val error = SpreadsheetValidationError(7, "Date of availability must be a real date")

      Json.toJson(error).as[SpreadsheetValidationError] mustBe error
    }
  }
}
