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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class NameDetailsSpec extends AnyFreeSpec with Matchers {

  "displayName" - {

    "must join the title, first name and last name with spaces" in {
      NameDetails("Mr", "John", "Smith").displayName mustBe "Mr John Smith"
    }

    "must omit an empty title" in {
      NameDetails("", "John", "Smith").displayName mustBe "John Smith"
    }

    "must omit an empty last name" in {
      NameDetails("Mr", "John", "").displayName mustBe "Mr John"
    }

    "must be empty when every part is empty" in {
      NameDetails("", "", "").displayName mustBe ""
    }
  }
}
