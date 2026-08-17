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

class SupplierAddressSpec extends SpecBase {

  private val gb = Country("GB", "United Kingdom")
  private val de = Country("DE", "Germany")

  "SupplierAddress.fromAddress" - {

    "must map ALF lines exactly from line1 to line4 and keep postcode" in {
      val address = Address(
        lines = Seq("12 High Street", "Apartment 5", "Mayfair", "Reading"),
        postcode = Some("RE12 9GC"),
        country = gb
      )

      SupplierAddress.fromAddress(address) mustBe SupplierAddress(
        line1 = "12 High Street",
        line2 = "Apartment 5",
        line3 = Some("Mayfair"),
        line4 = Some("Reading"),
        postCode = Some("RE12 9GC"),
        country = gb
      )
    }

    "must keep in the postcode for all addresses, both UK and overseas" in {
      val address = Address(lines = Seq("Musterstrasse 12", "Berlin"), postcode = Some("10115"), country = de)
      SupplierAddress.fromAddress(address).postCode mustBe Some("10115")
    }

    "must include the country object in the body" in {
      val address = Address(lines = Seq("Musterstrasse 12", "Berlin"), postcode = Some("10115"), country = de)
      val json    = play.api.libs.json.Json.toJson(SupplierAddress.fromAddress(address))
      (json \ "country" \ "code").as[String] mustBe "DE"
      (json \ "country" \ "name").as[String] mustBe "Germany"
    }
  }
}
