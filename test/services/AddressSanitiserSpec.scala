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

package services

import base.SpecBase
import models.{Address, Country}

class AddressSanitiserSpec extends SpecBase {

  private val germany = Country("DE", "Germany")

  "AddressSanitiser.sanitise" - {

    "must leave a clean address unchanged" in {
      val address = Address(
        lines = Seq("Musterstrasse 12", "OG", "Berlin"),
        postcode = Some("10115"),
        country = germany
      )
      AddressSanitiser.sanitise(address) mustBe address
    }

    "must strip disallowed characters from address lines" in {
      val address = Address(
        lines = Seq("12 High Street £$", "Reading"),
        postcode = None,
        country = germany
      )
      AddressSanitiser.sanitise(address).lines.head mustBe "12 High Street "
    }

    "must truncate address lines that exceed 35 characters" in {
      val longLine = "a" * 40
      val address  = Address(lines = Seq(longLine, "Town"), postcode = None, country = germany)
      AddressSanitiser.sanitise(address).lines.head mustBe "a" * 35
    }

    "must clean and truncate postcode when present" in {
      val address = Address(lines = Seq("Line 1"), postcode = Some("10115!XYZ123ABC"), country = germany)
      AddressSanitiser.sanitise(address).postcode mustBe Some("10115XYZ12")
    }

    "must leave postcode as None when not present" in {
      val address = Address(lines = Seq("Line 1"), postcode = None, country = germany)
      AddressSanitiser.sanitise(address).postcode mustBe None
    }

    "must leave country untouched" in {
      val address = Address(lines = Seq("Line 1"), postcode = None, country = germany)
      AddressSanitiser.sanitise(address).country mustBe germany
    }
  }
}
