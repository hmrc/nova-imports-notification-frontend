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

package viewmodels

import models.{Address, Country}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class AddressDisplaySpec extends AnyFreeSpec with Matchers {

  private val notProvided = "Not provided"

  ".lines" - {

    "must end a UK address with the postcode and never show the country" in {
      val address = Address(Seq("12 High Street", "Reading"), Some("RE12 9GC"), Country("GB", "United Kingdom"))

      AddressDisplay.lines(address) mustBe List("12 High Street", "Reading", "RE12 9GC")
    }

    "must show the postcode then the country for an overseas address that has a postcode" in {
      val address = Address(Seq("Musterstrasse 12", "Berlin"), Some("10115"), Country("DE", "Germany"))

      AddressDisplay.lines(address) mustBe List("Musterstrasse 12", "Berlin", "10115", "Germany")
    }

    "must omit the postcode line entirely for an overseas address that has none" in {
      val address = Address(Seq("24 Rue de Rivoli", "Paris"), None, Country("FR", "France"))

      AddressDisplay.lines(address) mustBe List("24 Rue de Rivoli", "Paris", "France")
    }

    "must omit the postcode line for an overseas address whose postcode is present but blank" in {
      val address = Address(Seq("24 Rue de Rivoli", "Paris"), Some("  "), Country("FR", "France"))

      AddressDisplay.lines(address) mustBe List("24 Rue de Rivoli", "Paris", "France")
    }

    "must trim the stored country name" in {
      val address = Address(Seq("Musterstrasse 12"), None, Country("DE", "  Germany  "))

      AddressDisplay.lines(address) mustBe List("Musterstrasse 12", "Germany")
    }

    "must resolve an empty overseas country name from the ISO code" in {
      val address = Address(Seq("Some Street", "Kabul"), None, Country("AF", ""))

      AddressDisplay.lines(address) mustBe List("Some Street", "Kabul", "Afghanistan")
    }

    "must fall back to the raw country code when it is not a resolvable ISO code" in {
      val address = Address(Seq("10 Rue de Paris"), None, Country("ZZ", ""))

      AddressDisplay.lines(address) mustBe List("10 Rue de Paris", "ZZ")
    }

    "must trim every part and drop blank ones" in {
      val address = Address(Seq(" 12 High Street ", "", "  "), Some("  RE12 9GC "), Country("GB", "United Kingdom"))

      AddressDisplay.lines(address) mustBe List("12 High Street", "RE12 9GC")
    }
  }

  ".paddedLines" - {

    "must pad an empty line 2 and an absent UK postcode with the not-provided text" in {
      val result = AddressDisplay.paddedLines(
        lines = AddressLines(line1 = Some("1 High Street"), line2 = None, line3 = None, line4 = None),
        postcode = None,
        country = None,
        notProvided = notProvided
      )

      result mustBe List("1 High Street", notProvided, notProvided)
    }

    "must show the postcode before the country for an overseas address that has a postcode" in {
      val result = AddressDisplay.paddedLines(
        lines = AddressLines(line1 = Some("10 Rue de Paris"), line2 = None, line3 = None, line4 = None),
        postcode = Some("75000"),
        country = Some(Country("FR", "France")),
        notProvided = notProvided
      )

      result mustBe List("10 Rue de Paris", notProvided, "75000", "France")
    }

    "must omit the postcode line rather than pad it when an overseas address has no postcode" in {
      val result = AddressDisplay.paddedLines(
        lines = AddressLines(line1 = Some("10 Rue de Paris"), line2 = None, line3 = None, line4 = None),
        postcode = None,
        country = Some(Country("FR", "France")),
        notProvided = notProvided
      )

      result mustBe List("10 Rue de Paris", notProvided, "France")
    }

    "must omit the postcode line rather than pad it when an overseas postcode is present but blank" in {
      val result = AddressDisplay.paddedLines(
        lines = AddressLines(line1 = Some("10 Rue de Paris"), line2 = None, line3 = None, line4 = None),
        postcode = Some(""),
        country = Some(Country("FR", "France")),
        notProvided = notProvided
      )

      result mustBe List("10 Rue de Paris", notProvided, "France")
    }

    "must treat a United Kingdom country as no country line at all" in {
      val result = AddressDisplay.paddedLines(
        lines = AddressLines(line1 = Some("12 High Street"), line2 = Some("Reading"), line3 = None, line4 = None),
        postcode = Some("RE12 9GC"),
        country = Some(Country("GB", "United Kingdom")),
        notProvided = notProvided
      )

      result mustBe List("12 High Street", "Reading", "RE12 9GC")
    }

    "must keep lines 3 and 4 when present and drop them when empty" in {
      val result = AddressDisplay.paddedLines(
        lines = AddressLines(line1 = Some("1 Arundel Mews"), line2 = Some("Sunnymede"), line3 = Some("Worthing"), line4 = None),
        postcode = Some("BN11 5RG"),
        country = None,
        notProvided = notProvided
      )

      result mustBe List("1 Arundel Mews", "Sunnymede", "Worthing", "BN11 5RG")
    }

    "must return nothing when every part is empty" in {
      val result = AddressDisplay.paddedLines(
        lines = AddressLines(line1 = None, line2 = None, line3 = None, line4 = None),
        postcode = None,
        country = None,
        notProvided = notProvided
      )

      result mustBe Nil
    }

    "must return nothing when every part is blank whitespace" in {
      val result = AddressDisplay.paddedLines(
        lines = AddressLines(line1 = Some(" "), line2 = Some(""), line3 = None, line4 = None),
        postcode = Some("  "),
        country = None,
        notProvided = notProvided
      )

      result mustBe Nil
    }
  }
}
