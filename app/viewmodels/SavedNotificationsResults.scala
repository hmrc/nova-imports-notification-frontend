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

import controllers.notification.routes
import models.{DraftNotificationSummary, DraftNotifications}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.{Pagination, PaginationItem, PaginationLink}

final case class SavedNotificationsResults(drafts: Seq[DraftNotificationSummary], totalCount: Int, page: Int, pageSize: Int) {

  val totalPages: Int = math.max(1, math.ceil(totalCount.toDouble / pageSize).toInt)
  val from: Int       = if (drafts.isEmpty) 0 else (page - 1) * pageSize + 1
  val to: Int         = (page - 1) * pageSize + drafts.size

  def pagination(implicit messages: Messages): Option[Pagination] =
    Option.when(totalPages > 1) {
      Pagination(
        items = Some(pageNumbers.map {
          case Some(n) => PaginationItem(href = url(n), number = Some(n.toString), current = Some(n == page))
          case None    => PaginationItem(ellipsis = Some(true))
        }),
        previous = Option.when(page > 1)(PaginationLink(href = url(page - 1), text = Some(messages("viewSavedNotifications.pagination.previous")))),
        next = Option.when(page < totalPages)(PaginationLink(href = url(page + 1), text = Some(messages("viewSavedNotifications.pagination.next"))))
      )
    }

  // first, last and the pages either side of the current one, with ellipses for gaps
  private def pageNumbers: Seq[Option[Int]] = {
    val shown = (Seq(1, totalPages) ++ (page - 1 to page + 1)).filter(n => n >= 1 && n <= totalPages).distinct.sorted
    shown.zip(shown.drop(1)).flatMap { case (a, b) => Some(a) +: Option.when(b - a > 1)(None).toSeq } :+ Some(shown.last)
  }

  private def url(n: Int): String =
    routes.ViewSavedNotificationsController.onPageLoad(n).url
}

object SavedNotificationsResults {

  def apply(notifications: DraftNotifications, page: Int, pageSize: Int): SavedNotificationsResults =
    SavedNotificationsResults(notifications.drafts, notifications.totalCount, page, pageSize)
}
