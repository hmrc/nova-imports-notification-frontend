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

class CreateUploadTrackingResponseSpec extends AnyFreeSpec with Matchers {

  "CreateUploadTrackingResponse.reads" - {

    "must read the upscan reference, upload url and signed fields" in {
      val json = Json.parse(
        """{
          |  "reference": "11370e18-6e24-453e-b45a-76d3e32ea33d",
          |  "uploadUrl": "https://bucketName.s3.eu-west-2.amazonaws.com",
          |  "fields": {
          |    "acl": "private",
          |    "key": "11370e18-6e24-453e-b45a-76d3e32ea33d",
          |    "policy": "xxxxxxxx=="
          |  }
          |}""".stripMargin
      )

      json.as[CreateUploadTrackingResponse] mustBe CreateUploadTrackingResponse(
        reference = "11370e18-6e24-453e-b45a-76d3e32ea33d",
        uploadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com",
        fields = Map(
          "acl"    -> "private",
          "key"    -> "11370e18-6e24-453e-b45a-76d3e32ea33d",
          "policy" -> "xxxxxxxx=="
        )
      )
    }

    "must read a response whose signed fields are empty" in {
      val json = Json.parse("""{"reference":"ref","uploadUrl":"https://s3","fields":{}}""")

      json.as[CreateUploadTrackingResponse].fields mustBe Map.empty
    }
  }
}
