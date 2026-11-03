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
import models.VinUploadError
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.UploadVinNumbersView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UploadVinNumbersViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: UploadVinNumbersView = app.injector.instanceOf[UploadVinNumbersView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private val uploadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com"
  private val fields    = Map("key" -> "11370e18-6e24-453e-b45a-76d3e32ea33d", "policy" -> "xxxxxxxx==")

  private def render(uploadError: Option[VinUploadError] = None): String =
    view(uploadUrl, fields, uploadError).toString

  "UploadVinNumbersView" - {

    "must render the AMV2.0 page title with no error prefix when there is no error" in {
      Jsoup.parse(render()).title mustEqual msgs("uploadVinNumbers.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must prefix the page title with the error prefix when upscan reported an error" in {
      val title = Jsoup.parse(render(Some(VinUploadError.NoFileSelected))).title

      title must startWith(msgs("error.title.prefix"))
      title must include(msgs("uploadVinNumbers.title"))
    }

    "must render the caption and heading" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-caption-l").text mustEqual msgs("uploadVinNumbers.caption")
      document.select("h1").text mustEqual msgs("uploadVinNumbers.heading")
    }

    "must post directly to the given upload URL as multipart form data" in {
      val document = Jsoup.parse(render())
      val form     = document.select("form")

      form.attr("action") mustEqual uploadUrl
      form.attr("enctype") mustEqual "multipart/form-data"
      form.attr("method") mustEqual "post"
    }

    "must render every upscan field as a hidden input" in {
      val document = Jsoup.parse(render())

      fields.foreach { case (name, value) =>
        document.select(s"""input[type=hidden][name=$name]""").attr("value") mustEqual value
      }
    }

    "must restrict the file picker to CSV files" in {
      Jsoup.parse(render()).getElementById("file").attr("accept") mustEqual ".csv,text/csv"
    }

    "must show no error summary when there is no upload error" in {
      Jsoup.parse(render()).select(".govuk-error-summary").isEmpty mustEqual true
    }

    "must show an error summary with the right message for each upscan error" in {
      List(
        VinUploadError.NoFileSelected -> "uploadVinNumbers.error.required",
        VinUploadError.FileTooLarge   -> "uploadVinNumbers.error.tooLarge",
        VinUploadError.UploadFailed   -> "uploadVinNumbers.error.unknown"
      ).foreach { case (error, messageKey) =>
        val document = Jsoup.parse(render(Some(error)))

        document.select(".govuk-error-summary").text must include(msgs(messageKey))
        document.select(".govuk-error-summary a").attr("href") mustEqual "#file"
      }
    }

    "must render the continue button" in {
      render() must include(msgs("site.continue"))
    }
  }
}
