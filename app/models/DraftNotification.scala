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

import play.api.libs.json.*

final case class DraftNotificationSection(data: Option[JsObject])

object DraftNotificationSection {
  implicit val format: OFormat[DraftNotificationSection] = Json.format[DraftNotificationSection]
}

// `lastUpdatedDate` is documented in the spec but the backend may omit it for drafts
final case class DraftNotification(
  draftId: String,
  createdDate: String,
  lastUpdatedDate: Option[String],
  sections: Map[String, DraftNotificationSection]
)

object DraftNotification {
  implicit val format: OFormat[DraftNotification] = Json.format[DraftNotification]

  object SectionId {
    val Introduction: String       = "introduction"
    val InitialQuestions: String   = "initialQuestions"
    val NotifierDetails: String    = "notifierDetails"
    val NotifierAddress: String    = "notifierAddress"
    val PurchaserDetails: String   = "purchaserDetails"
    val PurchaserAddress: String   = "purchaserAddress"
    val ImportDetails: String      = "importDetails"
    val SupplierSelfSupply: String = "supplierSelfSupply"
    val Vehicles: String           = "vehicles"
    val Declaration: String        = "declaration"
  }
}
