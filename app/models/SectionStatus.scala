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

// The backend only ever returns `completed` or `not-yet-saved`.
// The `Incomplete` UI tag is a frontend-only overlay applied at render time
// to any section that is not `completed`.
enum SectionStatus {
  case NotYetSaved
  case Completed
}

object SectionStatus {

  private val byWire: Map[String, SectionStatus] = Map(
    "not-yet-saved" -> NotYetSaved,
    "completed"     -> Completed
  )

  private val toWire: Map[SectionStatus, String] = byWire.map(_.swap)

  implicit val format: Format[SectionStatus] = Format(
    Reads {
      case JsString(s) =>
        byWire.get(s).map(JsSuccess(_)).getOrElse(JsError(s"Unknown section status: $s"))
      case _ => JsError("Section status must be a JSON string")
    },
    Writes(status => JsString(toWire(status)))
  )
}
