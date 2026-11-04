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
import models.{NormalMode, SupplierNumber, VehicleNumber}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.VinUploadStatusView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VinUploadStatusViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: VinUploadStatusView = app.injector.instanceOf[VinUploadStatusView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private val s = SupplierNumber(1)
  private val v = VehicleNumber(1)

  private val cancelUrl              = routes.UploadVinNumbersController.supplierOnPageLoad(s, v, restart = true)
  private val removeUrl              = routes.VinUploadStatusController.supplierOnPageLoad(s, v, remove = true)
  private val statusUrl              = routes.VinUploadStatusController.status(RedirectUrl(continueUrl.url)).url
  private val refreshIntervalSeconds = 3
  private val maxPollSeconds         = 120
  private val timeoutUrl             =
    routes.UploadSpreadsheetErrorUnknownController.onPageLoad(from = "vin", supplierNumber = Some(1), importNumber = None, vehicleNumber = Some(1))

  private def continueUrl: Call = routes.EnterVinNumbersController.supplierOnPageLoad(s, v, NormalMode)

  private def htmlFor(isFinal: Boolean, fileName: Option[String] = None, continueUrl: Option[Call] = None): String =
    view(isFinal, fileName, cancelUrl, removeUrl, statusUrl, refreshIntervalSeconds, maxPollSeconds, timeoutUrl, continueUrl).toString

  "VinUploadStatusView" - {

    "must render the AMV2.1 page title" in {
      Jsoup.parse(htmlFor(isFinal = false)).title mustEqual msgs("vinUploadStatus.heading") + " - " + msgs("service.name") + " - " + msgs(
        "site.govuk"
      )
    }

    "must show the default file name placeholder when the real name is not yet known" in {
      htmlFor(isFinal = false, fileName = None) must include(msgs("vinUploadStatus.defaultFileName"))
    }

    "must show the real file name once known" in {
      htmlFor(isFinal = false, fileName = Some("vins.csv")) must include("vins.csv")
    }

    "while not final, must show the Uploading tag in yellow, Cancel, and a disabled Continue button" in {
      val html = htmlFor(isFinal = false)

      html must include(msgs("vinUploadStatus.status.uploading"))
      html must include("""class="govuk-tag  govuk-tag--yellow"""")
      html must include(msgs("site.cancel"))
      html must include(cancelUrl.url)
      html must include("""aria-disabled="true"""")
    }

    "once final, must show the Uploaded tag in green and Remove" in {
      val html = htmlFor(isFinal = true)

      html must include(msgs("vinUploadStatus.status.uploaded"))
      html must include("""class="govuk-tag  govuk-tag--green"""")
      html must include(msgs("site.remove"))
      html must include(removeUrl.url)
    }

    "when the page loads already final, must render Continue as a real link, working without JS" in {
      val html = htmlFor(isFinal = true, continueUrl = Some(continueUrl))

      html must include(s"""href="${continueUrl.url}"""")
      html must include("""class="govuk-button"""")
      html must include("""id="continueButton"""")
    }

    "must pass the status endpoint, poll timings and timeout url to the polling script as data attributes, not inline JS" in {
      val html = htmlFor(isFinal = false)

      html must include(s"""data-status-url="$statusUrl"""")
      html must include(s"""data-refresh-interval-ms="${refreshIntervalSeconds * 1000}"""")
      html must include(s"""data-max-poll-ms="${maxPollSeconds * 1000}"""")
      html must include(s"""data-timeout-url="${timeoutUrl.url.split("\\?").head}""")
      html must include(s"""data-is-final="false"""")
    }

    "must set data-is-final to true when the page loads already final" in {
      htmlFor(isFinal = true) must include("""data-is-final="true"""")
    }

    "must load the polling script as an external, versioned static file rather than inline" in {
      val html = htmlFor(isFinal = false)

      html must include("""src="""")
      html must include("vinUploadStatus")
      html must not include "function checkUploadStatus"
    }

    "while not final, must render a no-JS refresh link before the polling script in document order, so JS can remove it as soon as it runs" in {
      val html = htmlFor(isFinal = false)

      html must include(s"""id="refreshLink"""")
      html must include(msgs("vinUploadStatus.refreshPageLink"))

      html.indexOf(s"""id="refreshLink"""") must be < html.indexOf("vinUploadStatus.js")
    }

    "once final, must not render the no-JS refresh link" in {
      htmlFor(isFinal = true) must not include s"""id="refreshLink""""
    }
  }

  "the static vinUploadStatus.js polling script" - {

    val scriptSource = scala.io.Source.fromFile("public/javascripts/vinUploadStatus.js")
    val script       =
      try scriptSource.mkString
      finally scriptSource.close()

    "must remove the no-JS refresh link synchronously, not gated behind DOMContentLoaded" in {
      val removalIndex          = script.indexOf("refreshLink.remove()")
      val domContentLoadedIndex = script.indexOf("addEventListener('DOMContentLoaded'")

      removalIndex must be >= 0
      removalIndex must be < domContentLoadedIndex
    }

    "must read the Continue href from the live poll response, not a value baked in by the server" in {
      script must include("data.continueUrl")
    }

    "must read its configuration from the vinUploadStatusConfig data attributes, not from inline Twirl values" in {
      script must include("vinUploadStatusConfig")
      script must include("data-status-url")
      script must include("data-refresh-interval-ms")
      script must include("data-max-poll-ms")
      script must include("data-timeout-url")
      script must include("data-is-final")
    }
  }
}
