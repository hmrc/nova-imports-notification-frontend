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

import base.SpecBase
import controllers.notification.routes
import models.DraftNotificationSummary
import org.scalatest.BeforeAndAfterAll
import play.api.Application
import play.api.i18n.Messages

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SavedNotificationsResultsSpec extends SpecBase with BeforeAndAfterAll {

  val app: Application        = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val msgs: Messages = messages(app)

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private def drafts(n: Int) = (1 to n).map(i => DraftNotificationSummary(s"$i", None, None, None, LocalDate.of(2026, 3, 1)))

  private def url(n: Int) = routes.ViewSavedNotificationsController.onPageLoad(n).url

  "SavedNotificationsResults" - {

    "must return records 11 to 20 and 3 pages for page 2 of 30 drafts" in {
      val p = SavedNotificationsResults(drafts(10), 30, 2, 10)

      p.from mustEqual 11
      p.to mustEqual 20
      p.totalPages mustEqual 3
    }

    "must return no pagination when there is only one page" in {
      SavedNotificationsResults(drafts(10), 10, 1, 10).pagination mustBe None
    }

    "must return links to pages 1, 2 and 3, plus Previous and Next, for page 2 of 30 drafts" in {
      val pagination = SavedNotificationsResults(drafts(10), 30, 2, 10).pagination.value

      pagination.items.value.map(_.href) mustEqual Seq(url(1), url(2), url(3))
      pagination.previous.value.href mustEqual url(1)
      pagination.next.value.href mustEqual url(3)
    }
  }
}
