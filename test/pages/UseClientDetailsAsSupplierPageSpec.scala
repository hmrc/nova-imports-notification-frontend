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
import models.SupplierNumber
import pages.sections.supplierdetails.{UseClientDetailsAsSupplierPage, UsePersonalDetailsAsSupplierPage, UsePurchaserDetailsAsSupplierPage}

class UseClientDetailsAsSupplierPageSpec extends SpecBase {

  "UseClientDetailsAsSupplierPage" - {

    "must store the answer against the supplier" in {
      val answers = emptyUserAnswers.unsafeSet(UseClientDetailsAsSupplierPage(SupplierNumber(2)), true)

      (answers.data \ "suppliers" \ "2" \ "useClientDetailsAsSupplier").as[Boolean] mustBe true
    }

    "must clear the personal and purchaser answers when it is answered" in {
      val answers = emptyUserAnswers
        .unsafeSet(UsePersonalDetailsAsSupplierPage(SupplierNumber(1)), true)
        .unsafeSet(UsePurchaserDetailsAsSupplierPage(SupplierNumber(1)), true)
        .unsafeSet(UseClientDetailsAsSupplierPage(SupplierNumber(1)), false)

      answers.get(UseClientDetailsAsSupplierPage(SupplierNumber(1))) mustBe Some(false)
      answers.get(UsePersonalDetailsAsSupplierPage(SupplierNumber(1))) mustBe None
      answers.get(UsePurchaserDetailsAsSupplierPage(SupplierNumber(1))) mustBe None
    }

    "must be cleared when the purchaser answer is given instead" in {
      val answers = emptyUserAnswers
        .unsafeSet(UseClientDetailsAsSupplierPage(SupplierNumber(1)), true)
        .unsafeSet(UsePurchaserDetailsAsSupplierPage(SupplierNumber(1)), true)

      answers.get(UseClientDetailsAsSupplierPage(SupplierNumber(1))) mustBe None
      answers.get(UsePurchaserDetailsAsSupplierPage(SupplierNumber(1))) mustBe Some(true)
    }

    "must only clear the same supplier's other answers" in {
      val answers = emptyUserAnswers
        .unsafeSet(UsePersonalDetailsAsSupplierPage(SupplierNumber(2)), true)
        .unsafeSet(UseClientDetailsAsSupplierPage(SupplierNumber(1)), true)

      answers.get(UsePersonalDetailsAsSupplierPage(SupplierNumber(2))) mustBe Some(true)
      answers.get(UseClientDetailsAsSupplierPage(SupplierNumber(1))) mustBe Some(true)
    }

    "must keep each supplier's answer separate" in {
      val answers = emptyUserAnswers
        .unsafeSet(UseClientDetailsAsSupplierPage(SupplierNumber(1)), true)
        .unsafeSet(UseClientDetailsAsSupplierPage(SupplierNumber(2)), false)

      answers.get(UseClientDetailsAsSupplierPage(SupplierNumber(1))) mustBe Some(true)
      answers.get(UseClientDetailsAsSupplierPage(SupplierNumber(2))) mustBe Some(false)
    }
  }
}
