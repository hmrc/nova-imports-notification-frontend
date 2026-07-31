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

class TraderInformationSpec extends AnyFreeSpec with Matchers {

  private val noDetails = TraderInformation(None, None, None, None, None, None, None)

  "name" - {

    "must use the trader name when it is present" in {
      noDetails.copy(traderName = Some("ABC LTD"), tradingName = Some("ABC Trading")).name mustBe Some("ABC LTD")
    }

    "must fall back to the trading name when there is no trader name" in {
      noDetails.copy(tradingName = Some("ABC Trading")).name mustBe Some("ABC Trading")
    }

    "must trim the name" in {
      noDetails.copy(traderName = Some("  ABC LTD  ")).name mustBe Some("ABC LTD")
    }

    "must fall back to the trading name when the trader name is present but blank" in {
      noDetails.copy(traderName = Some("   "), tradingName = Some("ABC Trading")).name mustBe Some("ABC Trading")
    }

    "must be None when there is no name" in {
      noDetails.name mustBe None
    }
  }

  "addressLines" - {

    "must return the lines in order with the postcode last" in {
      val trader = noDetails.copy(
        addressLine1 = Some("1 High Street"),
        addressLine2 = Some("Testtown"),
        addressLine3 = Some("Testshire"),
        addressLine4 = Some("England"),
        postcode = Some("TF3 4ER")
      )

      trader.addressLines mustBe Seq("1 High Street", "Testtown", "Testshire", "England", "TF3 4ER")
    }

    "must skip missing lines" in {
      noDetails.copy(addressLine1 = Some("1 High Street"), addressLine3 = Some("Testshire")).addressLines mustBe
        Seq("1 High Street", "Testshire")
    }

    "must skip blank lines" in {
      noDetails.copy(addressLine1 = Some("1 High Street"), addressLine2 = Some("  ")).addressLines mustBe Seq("1 High Street")
    }

    "must be empty when there is no address" in {
      noDetails.addressLines mustBe Seq.empty
    }
  }

  "format" - {

    "must ignore fields this journey does not display" in {
      val json = Json.parse(
        """{"vrn":"123456789","status":"Registered","traderName":"ABC LTD","addressLine1":"1 High Street",
          |"postcode":"TF3 4ER","email":"test@test.com","organisationType":"0001"}""".stripMargin
      )

      json.as[TraderInformation] mustBe noDetails.copy(
        traderName = Some("ABC LTD"),
        addressLine1 = Some("1 High Street"),
        postcode = Some("TF3 4ER")
      )
    }

    "must read a record with no fields set" in {
      Json.parse("{}").as[TraderInformation] mustBe noDetails
    }
  }
}
