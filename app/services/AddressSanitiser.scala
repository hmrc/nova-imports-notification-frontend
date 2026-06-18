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

object AddressSanitiser {

  private val lineMaxLength     = 35
  private val postcodeMaxLength = 10

  private val disallowedInLine: Regex     = """[^a-zA-Z0-9 ,/&'"-]""".r
  private val disallowedInPostcode: Regex = "[^a-zA-Z0-9 ]".r

  def sanitise(address: Address): Address =
    Address(
      lines = address.lines.map(clean(_, disallowedInLine, lineMaxLength)).filter(_.nonEmpty),
      postcode = address.postcode.map(clean(_, disallowedInPostcode, postcodeMaxLength)),
      country = address.country
    )

  private def clean(value: String, disallowed: Regex, maxLength: Int): String =
    disallowed.replaceAllIn(value, "").take(maxLength)
}
