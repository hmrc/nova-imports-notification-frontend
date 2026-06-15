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

package models.draftsections

import models.AddYourName
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class YourDetailsSpec extends AnyFreeSpec with Matchers {

  "YourDetails.writes" - {

    "does not include the name field when it has no value" in {
      val model = YourDetails(
        name = None,
        phoneNumber = "01632 960 001",
        emailAddress = "name@example.com"
      )

      Json.toJson(model) mustEqual Json.parse(
        """{"phoneNumber":"01632 960 001","emailAddress":"name@example.com"}"""
      )
    }

    "includes the name field when it has a value" in {
      val model = YourDetails(
        name = Some(AddYourName("Mr", "John", "Smith")),
        phoneNumber = "01632 960 001",
        emailAddress = "name@example.com"
      )

      Json.toJson(model) mustEqual Json.parse(
        """{
          |  "name":{"title":"Mr","firstName":"John","lastName":"Smith"},
          |  "phoneNumber":"01632 960 001",
          |  "emailAddress":"name@example.com"
          |}""".stripMargin
      )
    }
  }
}
