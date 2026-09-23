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

package models.responses

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class GetFileUploadSummaryResponseSpec extends AnyFreeSpec with Matchers {

  "GetFileUploadSummaryResponse.reads" - {

    "must read a fileStatus with no failureDetails or fileName" in {
      val json = Json.parse("""{"fileStatus":"VALIDATING"}""")

      json.as[GetFileUploadSummaryResponse] mustBe GetFileUploadSummaryResponse("VALIDATING", None, None)
    }

    "must read the failureReason out of a nested failureDetails object" in {
      val json = Json.parse("""{"fileStatus":"VERIFICATION_FAILED","failureDetails":{"failureReason":"QUARANTINE","message":"virus found"}}""")

      json.as[GetFileUploadSummaryResponse] mustBe GetFileUploadSummaryResponse("VERIFICATION_FAILED", Some("QUARANTINE"), None)
    }

    "must read the fileName when present" in {
      val json = Json.parse("""{"fileStatus":"VALIDATING","fileName":"car_spreadsheet.ods"}""")

      json.as[GetFileUploadSummaryResponse] mustBe GetFileUploadSummaryResponse("VALIDATING", None, Some("car_spreadsheet.ods"))
    }
  }
}
