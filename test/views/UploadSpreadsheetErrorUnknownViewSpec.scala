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
import controllers.vehicledetails
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.UploadSpreadsheetErrorUnknownView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UploadSpreadsheetErrorUnknownViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: UploadSpreadsheetErrorUnknownView = app.injector.instanceOf[UploadSpreadsheetErrorUnknownView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  lazy val html: String = view().toString

  "UploadSpreadsheetErrorUnknownView" - {

    "must render the caption" in {
      html must include(msgs("uploadSpreadsheetErrorUnknown.caption"))
    }

    "must render the heading as a page heading" in {
      html must include(s"""<h1 class="govuk-heading-l">${msgs("uploadSpreadsheetErrorUnknown.heading")}</h1>""")
    }

    "must set the page title" in {
      html must include(s"<title>${msgs("uploadSpreadsheetErrorUnknown.title")} - Notification of Vehicle Arrivals - GOV.UK")
    }

    "must render the paragraph" in {
      html must include(msgs("uploadSpreadsheetErrorUnknown.paragraph"))
    }

    "must render the button as a link back to UVS1.0" in {
      html must include(msgs("uploadSpreadsheetErrorUnknown.buttonLabel"))
      html must include(s"""href="${vehicledetails.routes.UploadVehicleSpreadsheetController.onPageLoad().url}"""")
    }

    "must render the link back to the notification task list" in {
      html must include(msgs("uploadSpreadsheetErrorUnknown.link"))
      html must include(s"""href="${controllers.routes.NotificationTaskListController.onPageLoad().url}"""")
    }
  }
}
