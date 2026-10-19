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

import play.api.libs.json.{Format, Json}

final case class VehicleTypeDetails(
  make: Option[String] = None,
  model: Option[String] = None,
  derivative: Option[String] = None,
  trim: Option[String] = None,
  bodyType: Option[String] = None,
  lcvBodyType: Option[String] = None,
  seriesModel: Option[String] = None,
  versionDerivative: Option[String] = None,
  brakeHorsePower: Option[String] = None,
  caravanMake: Option[String] = None,
  modelNameNumber: Option[String] = None,
  caravanVersion: Option[String] = None,
  caravanBody: Option[String] = None,
  makeOfBaseVehicle: Option[String] = None,
  hcvType: Option[String] = None,
  cabType: Option[String] = None,
  motorcycleVersion: Option[String] = None,
  motorcycleType: Option[String] = None,
  motorcycleStyle: Option[String] = None,
  transmission: Option[String] = None,
  fuelType: Option[String] = None,
  engineSize: Option[String] = None
)

object VehicleTypeDetails {
  implicit val format: Format[VehicleTypeDetails] = Json.format[VehicleTypeDetails]
}
