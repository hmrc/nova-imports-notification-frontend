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

import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.{Pagination, PaginationItem, PaginationLink}

final case class PageOf[A](items: Seq[A], page: Int, pageSize: Int, total: Int) {
  val totalPages: Int = math.max(1, math.ceil(total.toDouble / pageSize).toInt)
  val from: Int       = if (total == 0) 0 else (page - 1) * pageSize + 1
  val to: Int         = math.min(page * pageSize, total)
}

object Pager {

  def page[A](all: Seq[A], page: Int, pageSize: Int = 10): PageOf[A] = {
    val safePage = math.max(1, page)
    PageOf(all.slice((safePage - 1) * pageSize, safePage * pageSize), safePage, pageSize, all.size)
  }

  def paginationFor(pageOf: PageOf[?], urlForPage: Int => String): Option[Pagination] =
    Option.when(pageOf.totalPages > 1) {
      val current = pageOf.page
      val total   = pageOf.totalPages

      def item(p: Int): PaginationItem =
        PaginationItem(href = urlForPage(p), number = Some(p.toString), current = Some(p == current))

      val ellipsis: PaginationItem = PaginationItem(ellipsis = Some(true))

      val windowStart = math.max(1, current - 1)
      val windowEnd   = math.min(total, current + 1)

      val leading: Seq[PaginationItem] =
        if (windowStart <= 1) Nil
        else if (windowStart == 2) Seq(item(1))
        else Seq(item(1), ellipsis)

      val middle: Seq[PaginationItem] = (windowStart to windowEnd).map(item)

      val trailing: Seq[PaginationItem] =
        if (windowEnd >= total) Nil
        else if (windowEnd == total - 1) Seq(item(total))
        else Seq(ellipsis, item(total))

      Pagination(
        items = Some(leading ++ middle ++ trailing),
        previous = Option.when(current > 1)(PaginationLink(href = urlForPage(current - 1))),
        next = Option.when(current < total)(PaginationLink(href = urlForPage(current + 1)))
      )
    }
}
