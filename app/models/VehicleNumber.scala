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

import play.api.mvc.PathBindable

final case class VehicleNumber(value: Int)

object VehicleNumber {

  implicit val pathBindable: PathBindable[VehicleNumber] = new PathBindable[VehicleNumber] {

    override def bind(key: String, value: String): Either[String, VehicleNumber] =
      PathBindable.bindableInt.bind(key, value).flatMap {
        case number if number >= 1 => Right(VehicleNumber(number))
        case number                => Left(s"Vehicle number must be 1 or higher, but was $number")
      }

    override def unbind(key: String, vehicleNumber: VehicleNumber): String =
      PathBindable.bindableInt.unbind(key, vehicleNumber.value)
  }
}
