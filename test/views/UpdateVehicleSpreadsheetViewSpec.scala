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
import forms.UpdateVehicleSpreadsheetFormProvider
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.UpdateVehicleSpreadsheetView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UpdateVehicleSpreadsheetViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: UpdateVehicleSpreadsheetView = app.injector.instanceOf[UpdateVehicleSpreadsheetView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form = new UpdateVehicleSpreadsheetFormProvider()()

  private lazy val html: String = view(form)(request, msgs).toString

  "UpdateVehicleSpreadsheetView" - {

    "must render the page title" in {
      Jsoup.parse(html).title must startWith(msgs("updateVehicleSpreadsheet.title"))
    }

    "must render the heading as the fieldset legend" in {
      Jsoup.parse(html).select("legend h1").text mustEqual msgs("updateVehicleSpreadsheet.heading")
    }

    "must render the 'Add vehicle details' caption" in {
      Jsoup.parse(html).select("span.govuk-caption-l").text mustEqual msgs("updateVehicleSpreadsheet.caption")
    }

    "must render the hint and describe the fieldset with it" in {
      val doc  = Jsoup.parse(html)
      val hint = doc.select(".govuk-hint")

      hint.text mustEqual msgs("updateVehicleSpreadsheet.hint")
      doc.select("fieldset").attr("aria-describedby") must include(hint.attr("id"))
    }

    "must render the Yes and No radio options" in {
      val labels = Jsoup.parse(html).select(".govuk-radios__label")

      labels.eachText must contain theSameElementsInOrderAs Seq(msgs("site.yes"), msgs("site.no"))
    }

    "must render the Continue button" in {
      Jsoup.parse(html).select("button.govuk-button").text mustEqual msgs("site.continue")
    }

    "must render the error summary and field error when nothing is selected" in {
      val errorHtml = view(form.bind(Map("value" -> "")))(request, msgs).toString
      val doc       = Jsoup.parse(errorHtml)

      doc.select(".govuk-error-summary").text must include(msgs("updateVehicleSpreadsheet.error.required"))
      doc.select(".govuk-error-message").text must include(msgs("updateVehicleSpreadsheet.error.required"))
    }

    "must post to the UpdateVehicleSpreadsheet submit URL" in {
      Jsoup.parse(html).select("form").attr("action") mustEqual controllers.vehicledetails.routes.UpdateVehicleSpreadsheetController.onSubmit().url
    }

    "must render the same content via the render method" in {
      view.render(form, request, msgs).toString must include(msgs("updateVehicleSpreadsheet.heading"))
    }

    "must render the same content via the f method" in {
      view.f(form)(request, msgs).toString must include(msgs("updateVehicleSpreadsheet.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
