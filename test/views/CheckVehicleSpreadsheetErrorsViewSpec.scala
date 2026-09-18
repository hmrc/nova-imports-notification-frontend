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
import models.responses.ValidationError
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import viewmodels.Pager
import views.html.CheckVehicleSpreadsheetErrorsView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class CheckVehicleSpreadsheetErrorsViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: CheckVehicleSpreadsheetErrorsView = app.injector.instanceOf[CheckVehicleSpreadsheetErrorsView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private val error = ValidationError(field = "make[0]", error = "Enter the make, for example Land Rover", itemNumber = Some(1))

  private def urlForPage(page: Int): String = vehicledetails.routes.CheckVehicleSpreadsheetErrorsController.onPageLoad(page).url

  private def htmlFor(errors: Seq[ValidationError], page: Int = 1): String =
    view(Pager.page(errors, page), urlForPage).toString

  "CheckVehicleSpreadsheetErrorsView" - {

    lazy val html: String = htmlFor(Seq(error))

    "must render the caption" in {
      html must include(msgs("spreadsheetErrors.caption"))
    }

    "must render the table full-width as a sibling of the two-thirds column, not nested inside it" in {
      val twoThirdsOpen  = html.indexOf("govuk-grid-column-two-thirds")
      val twoThirdsClose = html.indexOf("</div>", twoThirdsOpen)
      val fullWidthOpen  = html.indexOf("govuk-grid-column-full")

      twoThirdsOpen must be >= 0
      fullWidthOpen must be >= 0
      fullWidthOpen must be > twoThirdsClose
    }

    "must render the heading as a page heading" in {
      html must include(s"""<h1 class="govuk-heading-l">${msgs("spreadsheetErrors.heading")}</h1>""")
    }

    "must set the page title" in {
      html must include(s"<title>${msgs("spreadsheetErrors.title")} - Notification of Vehicle Arrivals - GOV.UK")
    }

    "must render the paragraph" in {
      html must include(msgs("spreadsheetErrors.paragraph"))
    }

    "must render the table headers" in {
      html must include(msgs("spreadsheetErrors.table.item"))
      html must include(msgs("spreadsheetErrors.table.error"))
    }

    "must render a row for each error, with the item number and error message" in {
      html must include(">1<")
      html must include("Enter the make, for example Land Rover")
    }

    "must render the field label in bold above the error message" in {
      html must include("<b>Make:</b><br>Enter the make, for example Land Rover")
    }

    "must fall back to the raw field key when it has no known label" in {
      val unknownField = error.copy(field = "somethingUnexpected[0]")
      htmlFor(Seq(unknownField)) must include("<b>somethingUnexpected:</b><br>")
    }

    "must render a blank item cell when the error has no item number" in {
      val noItemNumber = error.copy(itemNumber = None)
      noException must be thrownBy htmlFor(Seq(noItemNumber))
    }

    "must render the table caption summarising the showing range" in {
      html must include(msgs("spreadsheetErrors.table.caption", 1, 1, 1))
    }

    "must render the upload updated spreadsheet button linking to UVS1.0" in {
      html must include(msgs("spreadsheetErrors.uploadButton"))
      html must include(s"""href="${vehicledetails.routes.UploadVehicleSpreadsheetController.onPageLoad().url}"""")
    }

    "must render the link back to the notification task list" in {
      html must include(msgs("spreadsheetErrors.returnLink"))
      html must include(s"""href="${controllers.routes.NotificationTaskListController.onPageLoad().url}"""")
    }

    "must not render pagination when there is only one page of errors" in {
      html must not include "govuk-pagination"
    }

    "must render pagination linking back to this page when there is more than one page of errors" in {
      val manyErrors = (1 to 15).map(n => error.copy(itemNumber = Some(n)))
      val paginated  = htmlFor(manyErrors, page = 1)

      paginated must include("govuk-pagination")
      paginated must include(s"""href="${urlForPage(2)}"""")
    }
  }
}
