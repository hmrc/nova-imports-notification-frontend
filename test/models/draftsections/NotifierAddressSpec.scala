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

import base.SpecBase
import models.{Address, Country}

class NotifierAddressSpec extends SpecBase {

  private val gb = Country("GB", "United Kingdom")
  private val de = Country("DE", "Germany")

  "NotifierAddress.fromAddress (UK)" - {

    "must map ALF lines verbatim to line1..line4 and keep postcode" in {
      val address = Address(
        lines = Seq("12 High Street", "Apartment 5", "Mayfair", "Reading"),
        postcode = Some("RE12 9GC"),
        country = gb
      )

      NotifierAddress.fromAddress(address) mustBe NotifierAddress(
        line1 = "12 High Street",
        line2 = "Apartment 5",
        line3 = Some("Mayfair"),
        line4 = Some("Reading"),
        postCode = Some("RE12 9GC"),
        country = gb
      )
    }

    "must leave line4 None when ALF returns fewer than four lines" in {
      val address = Address(lines = Seq("12 High Street", "Reading"), postcode = Some("RE12 9GC"), country = gb)
      val result  = NotifierAddress.fromAddress(address)
      result.line1 mustBe "12 High Street"
      result.line2 mustBe "Reading"
      result.line3 mustBe None
      result.line4 mustBe None
    }

    "must NOT put the country code in line4" in {
      val address = Address(lines = Seq("Sole line"), postcode = Some("AA1 1AA"), country = gb)
      val result  = NotifierAddress.fromAddress(address)
      result.line4 mustBe None
      result.country.code mustBe "GB"
    }
  }

  "NotifierAddress.fromAddress (non-UK)" - {

    "must map ALF lines verbatim to line1..line4" in {
      val address = Address(
        lines = Seq("Musterstrasse 12", "Block A", "Mitte", "Berlin"),
        postcode = Some("10115"),
        country = de
      )

      NotifierAddress.fromAddress(address) mustBe NotifierAddress(
        line1 = "Musterstrasse 12",
        line2 = "Block A",
        line3 = Some("Mitte"),
        line4 = Some("Berlin"),
        postCode = None,
        country = de
      )
    }

    "must drop the postcode (non-UK has no postcode in the F4 payload)" in {
      val address = Address(lines = Seq("Line 1", "Town"), postcode = Some("10115"), country = de)
      NotifierAddress.fromAddress(address).postCode mustBe None
    }

    "must leave line3 None when ALF returns fewer than three lines" in {
      val address = Address(lines = Seq("Musterstrasse 12", "Berlin"), postcode = None, country = de)
      val result  = NotifierAddress.fromAddress(address)
      result.line1 mustBe "Musterstrasse 12"
      result.line2 mustBe "Berlin"
      result.line3 mustBe None
      result.line4 mustBe None
    }
  }

  "NotifierAddress.fromAddress" - {

    "must include the country object in the body" in {
      val address = Address(lines = Seq("Musterstrasse 12", "Berlin"), postcode = Some("10115"), country = de)
      val json    = play.api.libs.json.Json.toJson(NotifierAddress.fromAddress(address))
      (json \ "country" \ "code").as[String] mustBe "DE"
      (json \ "country" \ "name").as[String] mustBe "Germany"
    }
  }
}
