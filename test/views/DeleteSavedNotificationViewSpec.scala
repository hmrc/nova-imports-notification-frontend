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
import forms.DeleteSavedNotificationFormProvider
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.DeleteSavedNotificationView

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class DeleteSavedNotificationViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: DeleteSavedNotificationView = app.injector.instanceOf[DeleteSavedNotificationView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form = new DeleteSavedNotificationFormProvider()()

  private def render(purchaserName: Option[String], boundForm: play.api.data.Form[Boolean] = form) =
    Jsoup.parse(view(boundForm, "12345", purchaserName, LocalDate.of(2026, 3, 12)).toString)

  "DeleteSavedNotificationView" - {

    "must render Delete notification as a h1" in {
      render(Some("Purchaser Company 1 Ltd")).select("h1").text mustEqual "Delete notification"
    }

    "must render the purchaser name and 12 March 2026 in the inset text" in {
      render(Some("Purchaser Company 1 Ltd")).select(".govuk-inset-text").text mustEqual
        "Purchaser name: Purchaser Company 1 Ltd Date created: 12 March 2026"
    }

    "must render Not provided in the inset text when there is no purchaser name" in {
      render(None).select(".govuk-inset-text b").first.text mustEqual "Not provided"
    }

    "must render the error message when nothing is selected" in {
      render(None, form.bind(Map("value" -> ""))).select(".govuk-error-message").text must include(
        "Select yes if you want to delete this notification"
      )
    }
  }
}
