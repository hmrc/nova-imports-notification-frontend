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

package models.responses

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

final case class CreateDraftResponse(draftId: String, versionId: Long)

object CreateDraftResponse {

  implicit val reads: Reads[CreateDraftResponse] = (
    (__ \ "draftId").read[String] and
      (__ \ "versionId").read[Long]
  )(CreateDraftResponse.apply _)

  implicit val writes: OWrites[CreateDraftResponse] = (
    (__ \ "draftId").write[String] and
      (__ \ "versionId").write[Long]
  )(c => (c.draftId, c.versionId))

  implicit val format: OFormat[CreateDraftResponse] = OFormat(reads, writes)
}
