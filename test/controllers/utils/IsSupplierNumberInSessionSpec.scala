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

package controllers.utils

import models.{SupplierNumber, UserAnswers}
import base.SpecBase
import org.scalatest.matchers.must.Matchers
import pages.sections.supplierDetails.SupplierNumberPage

class IsSupplierNumberInSessionSpec extends SpecBase with Matchers {

  "IsSupplierNumberInSession" - {

    "must return true when the supplier number is one the user has in session" in {
      val answers = UserAnswers("id").set(SupplierNumberPage, 3).success.value
      IsSupplierNumberInSession(answers, SupplierNumber(3)) mustBe true
    }

    "must return false when the supplier number belongs to somebody else" in {
      val answers = UserAnswers("id").set(SupplierNumberPage, 3).success.value
      IsSupplierNumberInSession(answers, SupplierNumber(4)) mustBe false
    }

    "must return false when the user has no suppliers in session yet" in {
      IsSupplierNumberInSession(UserAnswers("id"), SupplierNumber(1)) mustBe false
    }
  }
}
