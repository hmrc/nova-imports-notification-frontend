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

import models.{Address, Country}
import play.api.libs.json.{Format, Json}

final case class NotifierAddress(
  line1: String,
  line2: String,
  line3: Option[String],
  line4: Option[String],
  postCode: Option[String],
  country: Country
)

object NotifierAddress {

  implicit val format: Format[NotifierAddress] = Json.format[NotifierAddress]

  def fromAddress(address: Address): NotifierAddress = {
    val isUk  = address.country.code == "GB"
    val lines = address.lines.toIndexedSeq

    NotifierAddress(
      line1 = lines.lift(0).getOrElse(""),
      line2 = lines.lift(1).getOrElse(""),
      line3 = lines.lift(2),
      line4 = lines.lift(3),
      postCode = if (isUk) address.postcode else None,
      country = address.country
    )
  }
}
