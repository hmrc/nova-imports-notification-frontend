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
import models.SpreadsheetValidationError
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.UploadSpreadsheetErrorDataView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UploadSpreadsheetErrorDataViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: UploadSpreadsheetErrorDataView = app.injector.instanceOf[UploadSpreadsheetErrorDataView]

  private val validationErrors = Seq(
    SpreadsheetValidationError(1, "Registration number is missing"),
    SpreadsheetValidationError(3, "Date of availability must be a real date")
  )

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  lazy val html: String           = view(Seq.empty).toString
  lazy val htmlWithErrors: String = view(validationErrors).toString

  "UploadSpreadsheetErrorDataView" - {

    "must render the caption" in {
      html must include(msgs("uploadSpreadsheetErrorData.caption"))
    }

    "must render the heading as a page heading" in {
      html must include(s"""<h1 class="govuk-heading-l">${msgs("uploadSpreadsheetErrorData.heading")}</h1>""")
    }

    "must set the page title" in {
      html must include(s"<title>${msgs("uploadSpreadsheetErrorData.title")} - Notification of Vehicle Arrivals - GOV.UK")
    }

    "must render the paragraph" in {
      html must include(msgs("uploadSpreadsheetErrorData.paragraph"))
    }

    "must render the table headers" in {
      html must include(msgs("uploadSpreadsheetErrorData.table.itemNumber"))
      html must include(msgs("uploadSpreadsheetErrorData.table.problem"))
    }

    "must render each validation error with its item number and message" in {
      htmlWithErrors must include("1")
      htmlWithErrors must include("Registration number is missing")
      htmlWithErrors must include("3")
      htmlWithErrors must include("Date of availability must be a real date")
    }

    "must render an empty table when there are no validation errors" in {
      html must not include "Registration number is missing"
    }

    "must render the button as a link back to UVS1.0" in {
      html must include(msgs("uploadSpreadsheetErrorData.buttonLabel"))
      html must include(s"""href="${vehicledetails.routes.UploadVehicleSpreadsheetController.onPageLoad().url}"""")
    }

    "must render the link back to the notification task list" in {
      html must include(msgs("uploadSpreadsheetErrorData.link"))
      html must include(s"""href="${controllers.routes.NotificationTaskListController.onPageLoad().url}"""")
    }
  }
}
