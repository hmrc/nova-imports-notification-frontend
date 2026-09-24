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

package viewmodels

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.PaginationItem

class PagerSpec extends AnyFreeSpec with Matchers with OptionValues {

  private def urlForPage(p: Int): String = s"/page/$p"

  private def numbersAndEllipses(items: Seq[PaginationItem]): Seq[String] =
    items.map(i => if (i.ellipsis.contains(true)) "..." else i.number.getOrElse(""))

  "Pager.paginationFor" - {

    "must return None when there is only one page" in {
      val pageOf = PageOf(items = Seq.empty[Int], page = 1, pageSize = 10, total = 5)

      Pager.paginationFor(pageOf, urlForPage) mustBe None
    }

    "must truncate to first, a window around the current page, and last, matching the GOV.UK pattern" in {
      // 42 total pages, on page 7 -> 1 ... 6 7 8 ... 42
      val pageOf = PageOf(items = Seq.empty[Int], page = 7, pageSize = 10, total = 420)

      val pagination = Pager.paginationFor(pageOf, urlForPage).value

      numbersAndEllipses(pagination.items.value) mustBe Seq("1", "...", "6", "7", "8", "...", "42")
      pagination.items.value.find(_.number.contains("7")).value.current mustBe Some(true)
      pagination.previous.value.href mustBe urlForPage(6)
      pagination.next.value.href mustBe urlForPage(8)
    }

    "must not show a leading ellipsis when the window already starts at page 2" in {
      // total 10, current 1 -> window is [1,2], gap to 10 is more than 1, so: 1 2 ... 10
      val pageOf = PageOf(items = Seq.empty[Int], page = 1, pageSize = 10, total = 100)

      val pagination = Pager.paginationFor(pageOf, urlForPage).value

      numbersAndEllipses(pagination.items.value) mustBe Seq("1", "2", "...", "10")
      pagination.previous mustBe None
    }

    "must not show a trailing ellipsis when the window already ends at the second-to-last page" in {
      // total 10, current 10 -> window is [9,10], gap from 1 is more than 1, so: 1 ... 9 10
      val pageOf = PageOf(items = Seq.empty[Int], page = 10, pageSize = 10, total = 100)

      val pagination = Pager.paginationFor(pageOf, urlForPage).value

      numbersAndEllipses(pagination.items.value) mustBe Seq("1", "...", "9", "10")
      pagination.next mustBe None
    }

    "must not use an ellipsis to bridge a gap of exactly one page" in {
      // total 4, current 2 -> window is [1,2,3], gap to page 4 is only 1, so just list it: 1 2 3 4
      val pageOf = PageOf(items = Seq.empty[Int], page = 2, pageSize = 10, total = 40)

      val pagination = Pager.paginationFor(pageOf, urlForPage).value

      numbersAndEllipses(pagination.items.value) mustBe Seq("1", "2", "3", "4")
    }

    "must list every page without ellipses when there are few enough pages" in {
      val pageOf = PageOf(items = Seq.empty[Int], page = 2, pageSize = 10, total = 30)

      val pagination = Pager.paginationFor(pageOf, urlForPage).value

      numbersAndEllipses(pagination.items.value) mustBe Seq("1", "2", "3")
    }
  }
}
