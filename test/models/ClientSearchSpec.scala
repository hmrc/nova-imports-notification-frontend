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

class ClientSearchSpec extends AnyFreeSpec with Matchers {

  "ClientSearch.from" - {

    "must build a name search" in {
      ClientSearch.from(Some("name"), Some(" Client ")) mustEqual Some(ClientSearch(ClientSearchBy.Name, "Client"))
    }

    "must build a VRN search" in {
      ClientSearch.from(Some("vrn"), Some("123456789")) mustEqual Some(ClientSearch(ClientSearchBy.Vrn, "123456789"))
    }

    "must be empty for an unknown search type, a blank term or a missing part" in {
      ClientSearch.from(Some("email"), Some("x")) mustBe None
      ClientSearch.from(Some("name"), Some("  ")) mustBe None
      ClientSearch.from(Some("name"), None) mustBe None
      ClientSearch.from(None, Some("x")) mustBe None
    }
  }

  "ClientListQuery.apply" - {

    "must map a VRN search onto the vrn parameter" in {
      ClientListQuery(Some(ClientSearch(ClientSearchBy.Vrn, "123456789")), 0, 10) mustEqual ClientListQuery(
        vrn = Some("123456789"),
        start = 0,
        count = 10
      )
    }

    "must map a name search onto the name parameter" in {
      ClientListQuery(Some(ClientSearch(ClientSearchBy.Name, "Client")), 10, 10) mustEqual ClientListQuery(
        name = Some("Client"),
        start = 10,
        count = 10
      )
    }

    "must carry only paging when there is no search" in {
      ClientListQuery(None, 20, 10) mustEqual ClientListQuery(start = 20, count = 10)
    }
  }
}
