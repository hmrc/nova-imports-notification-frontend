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

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

final case class EUCountry(code: String, name: Option[String], euLeavingDate: Option[String]) {

  def hasLeftEU(): Boolean = {
    euLeavingDate
      .flatMap { dateStr =>
        Try(LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))).toOption
          .map(_.isBefore(LocalDate.now))
      }
      .getOrElse(false)
  }

  def toCountry() = {
    Country(code, name)
  }

}

object EUCountry {
  implicit val format: OFormat[EUCountry] = Json.format[EUCountry]

  def apply(code: String, name: String): EUCountry = EUCountry(code, Some(name), None)

}
