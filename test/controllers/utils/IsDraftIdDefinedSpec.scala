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

import models.{DraftId, UserAnswers}
import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.DraftIdPage

class IsDraftIdDefinedSpec extends SpecBase with Matchers {

  "IsDraftIdDefined" - {

    "must return true when a DraftId is present in user answers" in {
      val answers = UserAnswers("id").set(DraftIdPage, DraftId("DRAFT-001")).success.value
      IsDraftIdDefined(answers) mustBe true
    }

    "must return false when no DraftId is present in user answers" in {
      val answers = UserAnswers("id")
      IsDraftIdDefined(answers) mustBe false
    }
  }
}
