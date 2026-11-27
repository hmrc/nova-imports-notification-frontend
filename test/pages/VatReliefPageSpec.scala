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

package pages

import base.SpecBase
import models.{VatReliefReason, VehicleNumber}
import pages.sections.vehicledetails.{VatReliefPage, VatReliefReasonPage}

class VatReliefPageSpec extends SpecBase {

  private val v = VehicleNumber(1)

  "VatReliefPage" - {

    "cleanup" - {

      "must remove the reason for claiming relief when the answer is changed to No" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(VatReliefPage(v), true)
          .unsafeSet(VatReliefReasonPage(v), VatReliefReason.ModifiedForDisabledPassenger)

        val result = userAnswers.unsafeSet(VatReliefPage(v), false)

        result.get(VatReliefReasonPage(v)) mustBe None
      }

      "must keep the reason for claiming relief when the answer is re-set to Yes" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(VatReliefReasonPage(v), VatReliefReason.ModifiedForDisabledPassenger)

        val result = userAnswers.unsafeSet(VatReliefPage(v), true)

        result.get(VatReliefReasonPage(v)) mustBe Some(VatReliefReason.ModifiedForDisabledPassenger)
      }

      "must not affect another vehicle's reason for claiming relief" in {
        val other       = VehicleNumber(2)
        val userAnswers = emptyUserAnswers
          .unsafeSet(VatReliefPage(other), true)
          .unsafeSet(VatReliefReasonPage(other), VatReliefReason.InheritedGoods)
          .unsafeSet(VatReliefPage(v), true)
          .unsafeSet(VatReliefReasonPage(v), VatReliefReason.ModifiedForDisabledPassenger)

        val result = userAnswers.unsafeSet(VatReliefPage(v), false)

        result.get(VatReliefReasonPage(other)) mustBe Some(VatReliefReason.InheritedGoods)
      }
    }
  }
}
