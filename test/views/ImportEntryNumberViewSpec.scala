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
import forms.ImportEntryNumberFormProvider
import models.{ImportNumber, NormalMode}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.ImportEntryNumberView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ImportEntryNumberViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: ImportEntryNumberView = app.injector.instanceOf[ImportEntryNumberView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[String] = new ImportEntryNumberFormProvider()()

  private def render(form: Form[String] = form): String =
    view(form, ImportNumber(1), NormalMode)(request, msgs).toString

  "ImportEntryNumberView" - {

    "must render the AVD-IE1.0 page title" in {
      Jsoup.parse(render()).title mustEqual msgs("importEntryNumber.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the caption, heading and paragraph" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-caption-l").text mustEqual msgs("importEntryNumber.caption")
      document.select("h1").text mustEqual msgs("importEntryNumber.heading.1")
      document.select("p.govuk-body").first.text mustEqual msgs("importEntryNumber.paragraph")
    }

    "must render the question as a medium label outside the h1" in {
      val document = Jsoup.parse(render())

      document.select("label.govuk-label--m").attr("for") mustEqual "value"
      document.select("label.govuk-label--m").text mustEqual msgs("importEntryNumber.heading.2")
      document.select("h1 label").isEmpty mustEqual true
    }

    "must render the hint text against the input" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-hint").text mustEqual msgs("importEntryNumber.hint")
      document.getElementById("value").attr("aria-describedby") must include("value-hint")
    }

    "must post to the AVD-IE1.0 submit route for import 1" in {
      Jsoup.parse(render()).select("form").attr("action") mustEqual
        controllers.vehicledetails.routes.ImportEntryNumberController.onSubmit(ImportNumber(1), NormalMode).url
    }

    "must render the continue button" in {
      render() must include(msgs("site.continue"))
    }

    "must show an error summary linking to the input when nothing is entered" in {
      val document = Jsoup.parse(render(form.bind(Map("value" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("importEntryNumber.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
    }

    "must fill the input with the saved import entry number" in {
      Jsoup.parse(render(form.fill("12GB3456ACD789EF21"))).getElementById("value").attr("value") mustEqual "12GB3456ACD789EF21"
    }
  }
}
