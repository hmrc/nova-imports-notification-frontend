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

import models.{BusinessOrPrivateIndividual, Enumerable, PurchaserBusinessOrIndividual, PurchaserOrOnBehalf}
import play.api.libs.json.{Format, Json}

final case class InitialQuestions(
  bringingVehicleNI: Boolean,
  isForBusinessUse: Option[
    Boolean
  ], // not stored in formp or sent to nova but used to determine bringingVehicleBusinessNeeded for vat registered organisations
  areYouBusinessPrivate: Option[BusinessOrPrivateIndividual],
  notifyingAsPurchaser: Option[PurchaserOrOnBehalf],
  purchaserBusinessPrivate: Option[PurchaserBusinessOrIndividual],
  agentClientVehicleBusinessUse: Option[
    Boolean
  ], // not stored in formp or sent to nova but used to determine bringingVehicleBusinessNeeded for agent with client
  bringingVehicleBusiness: Option[Boolean],
  bringingVehicleBusinessNeeded: Boolean = false,
  purchasingVehiclesEuNeeded: Option[Boolean] = Some(true),
  purchaserBusinessPrivateNeeded: Boolean = true,
  sccCustomerFieldNeeded: Boolean = true,
  deregistered: Boolean = false,
  registered: Boolean = false,
  currentlyRegistered: Boolean = false
)

object InitialQuestions extends Enumerable.Implicits {
  implicit val format: Format[InitialQuestions] = Json.format[InitialQuestions]
}
