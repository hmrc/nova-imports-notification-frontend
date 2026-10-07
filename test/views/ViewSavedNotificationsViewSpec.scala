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

package views

import base.SpecBase
import models.DraftNotificationSummary
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import viewmodels.PageOf
import views.html.ViewSavedNotificationsView

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*

class ViewSavedNotificationsViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: ViewSavedNotificationsView = app.injector.instanceOf[ViewSavedNotificationsView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val completeDraft = DraftNotificationSummary("12345", Some("Purchaser Company 1 Ltd"), Some("withinEu"), Some(3), LocalDate.of(2026, 2, 14))
  val emptyDraft    = DraftNotificationSummary("12347", None, None, None, LocalDate.of(2026, 9, 5))

  private def cellsOfRow(html: String, row: Int): Seq[String] =
    Jsoup.parse(html).select("tbody tr").get(row).select("td").eachText.asScala.toSeq

  "ViewSavedNotificationsView" - {

    "must put page 2 of 3 in the title when there are 30 drafts" in {
      val html = view(PageOf(Seq(completeDraft), 2, 10, 30)).toString

      Jsoup.parse(html).title mustEqual "Saved notifications (page 2 of 3) - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must leave the page number out of the title when there is one page" in {
      val html = view(PageOf(Seq(completeDraft, emptyDraft), 1, 10, 2)).toString

      Jsoup.parse(html).title mustEqual "Saved notifications - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the five column headings" in {
      val html = view(PageOf(Seq(completeDraft), 1, 10, 1)).toString

      Jsoup.parse(html).select("thead th").eachText.asScala.toSeq mustEqual
        Seq("Purchaser name", "Purchase location", "Number of vehicles", "Date created", "Actions")
    }

    "must render the four cells for a draft with every field" in {
      val html = view(PageOf(Seq(completeDraft), 1, 10, 1)).toString

      cellsOfRow(html, 0).take(4) mustEqual Seq("Purchaser Company 1 Ltd", "Within the EU", "3", "14 Feb 2026")
    }

    "must render Not provided for an empty draft" in {
      val html = view(PageOf(Seq(emptyDraft), 1, 10, 1)).toString

      cellsOfRow(html, 0).take(4) mustEqual Seq("Not provided", "Not provided", "Not provided", "5 Sep 2026")
    }

    "must link Continue to onContinue and Delete to the landing page" in {
      val links = Jsoup.parse(view(PageOf(Seq(completeDraft), 1, 10, 1)).toString).select("tbody tr td").last.select("a")

      links.get(0).attr("href") mustEqual controllers.notification.routes.ViewSavedNotificationsController.onContinue("12345").url
      links.get(1).attr("href") mustEqual controllers.routes.LandingPageController.onPageLoad().url
    }
  }
}
