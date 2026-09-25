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
import controllers.vehicledetails.routes
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.VehicleSpreadsheetUploadView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VehicleSpreadsheetUploadViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: VehicleSpreadsheetUploadView = app.injector.instanceOf[VehicleSpreadsheetUploadView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private val removeUrl              = routes.AddVehicleDetailsController.onPageLoad(models.NormalMode)
  private val statusUrl              = routes.VehicleSpreadsheetUploadController.status().url
  private val refreshIntervalSeconds = 3
  private val maxPollSeconds         = 120
  private val cancelUrl              = routes.UploadVehicleSpreadsheetController.onPageLoad().url
  private val problemUploadingUrl    = routes.UploadSpreadsheetErrorUnknownController.onPageLoad().url

  private val continueUrl = routes.CheckVehicleSpreadsheetDetailsController.onPageLoad()

  private def htmlFor(isFinal: Boolean, fileName: Option[String] = None, continueUrl: Option[play.api.mvc.Call] = None): String =
    view(isFinal, fileName, removeUrl, statusUrl, refreshIntervalSeconds, maxPollSeconds, continueUrl).toString

  "VehicleSpreadsheetUploadView" - {

    "must set the page title" in {
      htmlFor(isFinal = false) must include(s"<title>${msgs("vehicleSpreadsheetUpload.heading")} - Notification of Vehicle Arrivals - GOV.UK")
    }

    "must render the caption and heading" in {
      val html = htmlFor(isFinal = false)

      html must include(msgs("vehicleSpreadsheetUpload.caption"))
      html must include(s"""<h1 class="govuk-heading-l">${msgs("vehicleSpreadsheetUpload.heading")}</h1>""")
    }

    "must show the default file name placeholder when the real name is not yet known" in {
      htmlFor(isFinal = false, fileName = None) must include(msgs("vehicleSpreadsheetUpload.defaultFileName"))
    }

    "must show the real file name once known" in {
      htmlFor(isFinal = false, fileName = Some("car_spreadsheet.ods")) must include("car_spreadsheet.ods")
    }

    "while not final, must show the Uploading tag in yellow, Cancel, and a disabled Continue button" in {
      val html = htmlFor(isFinal = false)

      html must include(msgs("vehicleSpreadsheetUpload.status.uploading"))
      html must include("""class="govuk-tag  govuk-tag--yellow"""")
      html must include(msgs("site.cancel"))
      html must include(cancelUrl)
      html must include("""aria-disabled="true"""")
    }

    "once final, must show the Uploaded tag in green, Remove, and an enabled Continue button" in {
      val html = htmlFor(isFinal = true)

      html must include(msgs("vehicleSpreadsheetUpload.status.uploaded"))
      html must include("""class="govuk-tag  govuk-tag--green"""")
      html must include(msgs("site.remove"))
      html must include(removeUrl.url)
      html must not include """aria-disabled="true""""
    }

    "when the page loads already final (e.g. landed on directly from the task list), must wire the Continue button to navigate immediately, without waiting for a poll" in {
      val html = htmlFor(isFinal = true, continueUrl = Some(continueUrl))

      html must include(s"""data-continue-href="${continueUrl.url}"""")
      html must include("wireContinueButton()")
    }

    "must include the polling script pointing at the status endpoint" in {
      val html = htmlFor(isFinal = false)

      html must include(statusUrl)
      html must include(s"$refreshIntervalSeconds * 1000")
    }

    "must give up polling and redirect to the problem uploading page once the max poll duration is reached" in {
      val html = htmlFor(isFinal = false)

      html must include(s"$maxPollSeconds * 1000")
      html must include(problemUploadingUrl)
    }

    "while not final, must render a no-JS refresh link that the script removes immediately when JS runs" in {
      val html = htmlFor(isFinal = false)

      html must include(s"""id="refreshLink"""")
      html must include(msgs("vehicleSpreadsheetUpload.refreshPageLink"))
      html must include(routes.VehicleSpreadsheetUploadController.onPageLoad().url)
      html must include("refreshLink.remove()")

      html.indexOf(s"""id="refreshLink"""") must be < html.indexOf("refreshLink.remove()")
    }

    "once final, must not render the no-JS refresh link, since there's nothing left to wait for" in {
      htmlFor(isFinal = true) must not include s"""id="refreshLink""""
    }
  }
}
