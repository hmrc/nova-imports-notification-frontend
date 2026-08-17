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

class AddressValidatorSpec extends SpecBase {

  private val uk = Country("GB", "United Kingdom")

  private val validAddress = Address(
    lines = Seq("12 High Street", "Reading"),
    postcode = Some("RE12 9GC"),
    country = uk
  )

  "AddressValidator.isValidLine" - {

    "must accept a line with letters, digits and a space" in {
      AddressValidator.isValidLine("12 High Street") mustBe true
    }

    "must accept a line using every approved special character" in {
      AddressValidator.isValidLine("Flat 1, Rose & Crown/O'Brien's \"Yard\"-Side") mustBe true
    }

    "must reject an empty line" in {
      AddressValidator.isValidLine("") mustBe false
    }

    "must reject a line beginning with a non-alphanumeric character" in {
      AddressValidator.isValidLine(" 12 High Street") mustBe false
      AddressValidator.isValidLine("-12 High Street") mustBe false
    }

    "must reject a line containing a disallowed character" in {
      AddressValidator.isValidLine("12 High Street £") mustBe false
      AddressValidator.isValidLine("12 High Street!") mustBe false
    }
  }

  "AddressValidator.isValidCountryName" - {

    "must accept a country name of 18 characters or fewer" in {
      AddressValidator.isValidCountryName("United Kingdom") mustBe true
    }

    "must accept a country name of exactly 18 characters" in {
      AddressValidator.isValidCountryName("a" * 18) mustBe true
    }

    "must reject a country name longer than 18 characters" in {
      AddressValidator.isValidCountryName("United Arab Emirates") mustBe false
    }

    "must reject a country name beginning with a space" in {
      AddressValidator.isValidCountryName(" Germany") mustBe false
    }

    "must reject an empty country name" in {
      AddressValidator.isValidCountryName("") mustBe false
    }
  }

  "AddressValidator.isValidUkPostcode" - {

    "must accept a standard UK postcode" in {
      AddressValidator.isValidUkPostcode("RE12 9GC") mustBe true
    }

    "must accept a short-outward-code postcode" in {
      AddressValidator.isValidUkPostcode("W1 1AA") mustBe true
    }

    "must reject a postcode missing the internal space" in {
      AddressValidator.isValidUkPostcode("RE129GC") mustBe false
    }

    "must reject a lower-case postcode" in {
      AddressValidator.isValidUkPostcode("re12 9gc") mustBe false
    }

    "must reject a malformed postcode" in {
      AddressValidator.isValidUkPostcode("NOTAPOSTCODE") mustBe false
    }
  }

  "AddressValidator.isValid" - {

    "must accept a fully valid UK address" in {
      AddressValidator.isValid(validAddress) mustBe true
    }

    "must accept a fully valid non-UK address with no postcode" in {
      val address = validAddress.copy(postcode = None, country = Country("DE", "Germany"))
      AddressValidator.isValid(address) mustBe true
    }

    "must reject when line1 is missing" in {
      AddressValidator.isValid(validAddress.copy(lines = Seq("Reading"))) mustBe false
    }

    "must reject when an address line contains a disallowed character" in {
      AddressValidator.isValid(validAddress.copy(lines = Seq("12 High Street £", "Reading"))) mustBe false
    }

    "must reject when the country name is too long" in {
      AddressValidator.isValid(validAddress.copy(country = Country("AE", "United Arab Emirates"))) mustBe false
    }

    "must reject when the UK postcode is malformed" in {
      AddressValidator.isValid(validAddress.copy(postcode = Some("NOTAPOSTCODE"))) mustBe false
    }
  }
}
