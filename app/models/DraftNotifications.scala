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

final case class DraftNotificationSummary(
  draftId: String,
  purchaserName: Option[String],
  purchaseLocation: Option[String],
  numberOfVehicles: Option[Int],
  createdDate: LocalDate
)

object DraftNotificationSummary {
  implicit val format: OFormat[DraftNotificationSummary] = Json.format[DraftNotificationSummary]
}

final case class DraftNotifications(drafts: Seq[DraftNotificationSummary], totalCount: Int, page: Int, pageSize: Int)

object DraftNotifications {
  implicit val format: OFormat[DraftNotifications] = Json.format[DraftNotifications]
}
