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
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import views.html.UploadSuccessfulView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UploadSuccessfulViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: UploadSuccessfulView = app.injector.instanceOf[UploadSuccessfulView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val continueCall: Call = controllers.routes.NotificationTaskListController.onPageLoad()

  lazy val html: String = view(continueCall).toString
  lazy val doc          = Jsoup.parse(html)

  "UploadSuccessfulView" - {

    "must set the page title" in {
      html must include(s"<title>${msgs("uploadSuccessful.title")} - Notification of Vehicle Arrivals - GOV.UK")
    }

    "must render the heading as the page heading inside a confirmation panel" in {
      val heading = doc.select(".govuk-panel.govuk-panel--confirmation h1.govuk-panel__title")
      heading.size mustBe 1
      heading.text mustBe msgs("uploadSuccessful.heading")
    }

    "must render the paragraph" in {
      doc.select("p.govuk-body").eachText() must contain(msgs("uploadSuccessful.paragraph"))
    }

    "must render a back link" in {
      doc.select(".govuk-back-link").size mustBe 1
    }

    "must render the Continue button as a link to the continue call given" in {
      val continue = doc.select("a.govuk-button")
      continue.text mustBe msgs("site.continue")
      continue.attr("href") mustBe continueCall.url
    }

    "must use the copy from the ticket" in {
      msgs("uploadSuccessful.title") mustBe "Upload successful"
      msgs("uploadSuccessful.heading") mustBe "Upload successful"
      msgs("uploadSuccessful.paragraph") mustBe "You have successfully attached this vehicle spreadsheet."
    }
  }
}
