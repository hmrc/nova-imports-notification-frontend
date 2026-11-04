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

import base.SpecBase
import models.DraftNotificationSection
import play.api.libs.json.Json

class VinDuplicateCheckServiceSpec extends SpecBase {

  private def sectionWithVin(vin: String): DraftNotificationSection =
    DraftNotificationSection(Some(Json.obj("vehicleIdNumber" -> vin)))

  "VinDuplicateCheckService.crossNotificationDuplicates" - {

    "find no duplicates when no other vehicle shares a candidate VIN" in {
      val sections = Map("supplier/2/vehicle/1/additional-information" -> sectionWithVin("ZZZ999"))

      VinDuplicateCheckService.crossNotificationDuplicates(sections, Seq("AAA111", "BBB222"), "supplier", 1) mustEqual Seq.empty
    }

    "find a duplicate against another supplier group" in {
      val sections = Map("supplier/2/vehicle/1/additional-information" -> sectionWithVin("AAA111"))

      VinDuplicateCheckService.crossNotificationDuplicates(sections, Seq("AAA111", "BBB222"), "supplier", 1) mustEqual Seq("AAA111")
    }

    "find a duplicate against an import group when multiplying a supplier group" in {
      val sections = Map("import/1/vehicle/1/additional-information" -> sectionWithVin("AAA111"))

      VinDuplicateCheckService.crossNotificationDuplicates(sections, Seq("AAA111"), "supplier", 1) mustEqual Seq("AAA111")
    }

    "exclude the group currently being multiplied, to avoid false positives when re-saving" in {
      val sections = Map(
        "supplier/1/vehicle/1/additional-information" -> sectionWithVin("AAA111"),
        "supplier/1/vehicle/2/additional-information" -> sectionWithVin("BBB222")
      )

      VinDuplicateCheckService.crossNotificationDuplicates(sections, Seq("AAA111", "BBB222"), "supplier", 1) mustEqual Seq.empty
    }

    "compare case-insensitively, per F26" in {
      val sections = Map("supplier/2/vehicle/1/additional-information" -> sectionWithVin("aaa111"))

      VinDuplicateCheckService.crossNotificationDuplicates(sections, Seq("AAA111"), "supplier", 1) mustEqual Seq("AAA111")
    }

    "ignore sections that are not additional-information pages" in {
      val sections = Map("supplier/2/vehicle/1/type" -> sectionWithVin("AAA111"))

      VinDuplicateCheckService.crossNotificationDuplicates(sections, Seq("AAA111"), "supplier", 1) mustEqual Seq.empty
    }

    "ignore additional-information sections with no data (deleted/not-yet-saved)" in {
      val sections = Map("supplier/2/vehicle/1/additional-information" -> DraftNotificationSection(None))

      VinDuplicateCheckService.crossNotificationDuplicates(sections, Seq("AAA111"), "supplier", 1) mustEqual Seq.empty
    }
  }
}
