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
import models.SpreadsheetUploadError
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.UploadVehicleSpreadsheetView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UploadVehicleSpreadsheetViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: UploadVehicleSpreadsheetView = app.injector.instanceOf[UploadVehicleSpreadsheetView]

  private val uploadUrl      = "https://bucketName.s3.eu-west-2.amazonaws.com"
  private val fields         = Map("key" -> "11370e18-6e24-453e-b45a-76d3e32ea33d", "policy" -> "xxxxxxxx==")
  private val spreadsheetUrl = "https://www.gov.uk/guidance/telling-hmrc-youre-importing-multiple-vehicles#spreadsheets-for-different-vehicle-types"

  private def render(uploadError: Option[SpreadsheetUploadError] = None): String =
    view(uploadUrl, fields, spreadsheetUrl, uploadError)(request, msgs).toString.replaceAll("\\s+", " ")

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "UploadVehicleSpreadsheetView" - {

    "must render the page title" in {
      render() must include(msgs("uploadVehicleSpreadsheet.title"))
    }

    "must render the caption above the heading" in {
      render() must include(s"""<span class="govuk-caption-l">${msgs("uploadVehicleSpreadsheet.caption")}</span>""")
    }

    "must render the heading as a large h1" in {
      render() must include(s"""<h1 class="govuk-heading-l">${msgs("uploadVehicleSpreadsheet.heading")}</h1>""")
    }

    "must render the paragraph with a new tab link to the spreadsheet guidance" in {
      val html = render()
      html must include("Add details for up to 100 vehicles of the same type in one notification.")
      html must include(msgs("uploadVehicleSpreadsheet.useCorrectSpreadsheet.linkText"))
      html must include(s"""href="$spreadsheetUrl"""")
      html must include("(opens in new tab)")
    }

    "must post the upload directly to the upscan upload url as multipart form data" in {
      val html = render()
      html must include(s"""action="$uploadUrl"""")
      html must include("""method="post"""")
      html must include("""enctype="multipart/form-data"""")
    }

    "must not carry a csrf token that the upscan upload policy would reject" in {
      render() must not include "csrfToken"
    }

    "must render a hidden input for every signed upscan field" in {
      val html = render()
      html must include("""<input type="hidden" name="key" value="11370e18-6e24-453e-b45a-76d3e32ea33d">""")
      html must include("""<input type="hidden" name="policy" value="xxxxxxxx==">""")
    }

    "must render the file input after every hidden field so the upload is the last part of the request" in {
      val html = render()
      html.indexOf("""name="file"""") must be > html.lastIndexOf("""type="hidden"""")
    }

    "must name the file input file without leaning on browser-native validation" in {
      val html = render()
      html must include("""<input class="govuk-file-upload" id="file" name="file" type="file"""")
      html must not include "required="
    }

    "must render the enhanced file upload wrapper" in {
      val html = render()
      html must include("govuk-file-upload-wrapper")
      html must include("""data-module="govuk-file-upload"""")
    }

    "must render the label at normal size rather than as the page heading" in {
      val html = render()
      html must include(msgs("uploadVehicleSpreadsheet.label"))
      html must include("govuk-label")
      html must not include "govuk-label--l"
    }

    "must render the hint" in {
      render() must include(msgs("uploadVehicleSpreadsheet.hint"))
    }

    "must render the continue button" in {
      render() must include(msgs("site.continue"))
    }

    "must render no error summary and no error styling when the page was not reached from an upload failure" in {
      val html = render()
      html must not include "govuk-error-summary"
      html must not include "govuk-form-group--error"
      html must not include "govuk-file-upload--error"
      html must not include msgs("error.summary.title")
    }

    "must render an error summary linked to the file input when upscan reported a failure" in {
      val html = render(Some(SpreadsheetUploadError.NoFileSelected))
      html must include("govuk-error-summary")
      html must include(msgs("error.summary.title"))
      html must include(s"""<a href="#file">${msgs("uploadVehicleSpreadsheet.error.required")}</a>""")
    }

    "must render the inline error message with the visually hidden prefix screen readers announce" in {
      val html = render(Some(SpreadsheetUploadError.NoFileSelected))
      html must include("""<p id="file-error" class="govuk-error-message" >""")
      html must include(s"""<span class="govuk-visually-hidden">${msgs("error.prefix")}:</span>""")
      html must include(msgs("uploadVehicleSpreadsheet.error.required"))
    }

    "must apply the error styling to the form group and the file input" in {
      val html = render(Some(SpreadsheetUploadError.NoFileSelected))
      html must include("govuk-form-group--error")
      html must include("govuk-file-upload--error")
    }

    "must describe the file input by the error only while an error is present" in {
      render(Some(SpreadsheetUploadError.NoFileSelected)) must include("file-error")
      render()                                            must not include "file-error"
    }

    "must render the copy Confluence specifies for each upload error upscan can report" in {
      render(Some(SpreadsheetUploadError.NoFileSelected)) must include("Select a file")
      render(Some(SpreadsheetUploadError.FileTooLarge))   must include("The selected file must be smaller than 1MB")
      render(Some(SpreadsheetUploadError.UploadFailed))   must include("The selected file could not be uploaded. Try again.")
    }

    "must prefix the page title so the error is announced in the browser tab" in {
      render(Some(SpreadsheetUploadError.NoFileSelected)) must include(
        s"""<title>${msgs("error.title.prefix")} ${msgs("uploadVehicleSpreadsheet.title")}"""
      )
      render() must include(s"""<title>${msgs("uploadVehicleSpreadsheet.title")}""")
    }
  }
}
