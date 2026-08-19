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

import models.Address

import scala.util.matching.Regex

object AddressValidator {

  // Address lines: must begin with an allowed character and contain only approved characters.
  private val AddressLinePattern: Regex = "^[a-zA-Z0-9][a-zA-Z0-9 ,/&'\"-]*$".r

  // Country name: must start with alphanumeric, max 18 characters, spaces allowed after the first character.
  private val CountryNamePattern: Regex = "^[a-zA-Z0-9][a-zA-Z0-9 ]{0,17}$".r

  // UK postcode: standard GOV.UK postcode format.
  private val UkPostcodePattern: Regex = "^[A-Z]{1,2}[0-9][0-9A-Z]? [0-9][A-Z]{2}$".r

  def hasMandatoryFields(address: Address): Boolean =
    address.lines.lift(0).exists(_.trim.nonEmpty) && address.lines.lift(1).exists(_.trim.nonEmpty)

  def isValid(address: Address): Boolean =
    hasMandatoryFields(address) &&
      address.lines.forall(isValidLine) &&
      isValidCountryName(address.country.name) &&
      address.postcode.forall(isValidUkPostcode)

  def isValidLine(line: String): Boolean =
    AddressLinePattern.matches(line)

  def isValidCountryName(name: String): Boolean =
    CountryNamePattern.matches(name)

  def isValidUkPostcode(postcode: String): Boolean =
    UkPostcodePattern.matches(postcode)
}
