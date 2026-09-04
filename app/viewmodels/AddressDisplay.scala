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

import java.util.Locale

final case class AddressLines(line1: Option[String], line2: Option[String], line3: Option[String], line4: Option[String])

object AddressLines {

  def from(lines: Seq[String]): AddressLines =
    AddressLines(lines.lift(0), lines.lift(1), lines.lift(2), lines.lift(3))
}

object AddressDisplay {

  private val unitedKingdomCode = "GB"

  private val isoCountryCodes: Set[String] = Locale.getISOCountries.toSet

  private def countryName(country: Country): String = {
    val stored = country.name.trim
    if (stored.nonEmpty) stored
    else if (isoCountryCodes.contains(country.code)) new Locale("", country.code).getDisplayCountry(Locale.UK)
    else country.code
  }

  def lines(address: Address): List[String] =
    (address.lines.toList ++ address.postcode.toList).flatMap(populated) ++ overseasCountryName(address.country)

  def paddedLines(
    lines: AddressLines,
    postcode: Option[String],
    country: Option[Country],
    notProvided: String
  ): List[String] = {
    val addressLines = List(lines.line1, lines.line2, lines.line3, lines.line4).map(_.flatMap(populated))
    val postcodeLine = postcode.flatMap(populated)
    val countryLine  = country.flatMap(overseasCountryName)

    if ((addressLines :+ postcodeLine :+ countryLine).forall(_.isEmpty)) Nil
    else {
      val padded             = addressLines.take(2).map(_.orElse(Some(notProvided))) ++ addressLines.drop(2)
      val postcodeAndCountry = countryLine.fold(List(postcodeLine.orElse(Some(notProvided))))(name => List(postcodeLine, Some(name)))

      (padded ++ postcodeAndCountry).flatten
    }
  }

  private def overseasCountryName(country: Country): Option[String] =
    Option.when(country.code != unitedKingdomCode)(countryName(country)).flatMap(populated)

  private def populated(value: String): Option[String] = Some(value.trim).filter(_.nonEmpty)
}
