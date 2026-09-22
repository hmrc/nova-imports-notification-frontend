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
import forms.TotalAmountPaidFormProvider
import models.{ImportNumber, NormalMode, SupplierNumber, VehicleNumber}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.TotalAmountPaidView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class TotalAmountPaidViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: TotalAmountPaidView = app.injector.instanceOf[TotalAmountPaidView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[String] = new TotalAmountPaidFormProvider()()

  val supplierPostAction =
    controllers.vehicledetails.routes.TotalAmountPaidController.onSubmitSupplier(SupplierNumber(1), VehicleNumber(1), NormalMode)
  val importPostAction = controllers.vehicledetails.routes.TotalAmountPaidController.onSubmitImport(ImportNumber(1), VehicleNumber(1), NormalMode)

  private def render(form: Form[String] = form, postAction: play.api.mvc.Call = supplierPostAction): String =
    view(form, postAction)(request, msgs).toString

  "TotalAmountPaidView" - {

    "must render the correct heading as an h1" in {
      val document = Jsoup.parse(render())

      document.select("h1").text mustEqual msgs("totalAmountPaid.heading")
    }

    "must render the correct page title" in {
      Jsoup.parse(render()).title mustEqual msgs("totalAmountPaid.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the field label for the input" in {
      val document = Jsoup.parse(render())

      document.select("label").attr("for") mustEqual "value"
      document.select("label").text mustEqual msgs("totalAmountPaid.label")
    }

    "must render the hint text against the input" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-hint").text mustEqual msgs("totalAmountPaid.hint")
      document.getElementById("value").attr("aria-describedby") must include("value-hint")
    }

    "must render the correct page caption" in {
      val html = render()

      html must include("govuk-caption-l")
      html must include(msgs("totalAmountPaid.caption"))
    }

    "must render the guidance paragraphs and bullet list" in {
      val html = render()

      html must include(msgs("totalAmountPaid.paragraph.1"))
      html must include(msgs("totalAmountPaid.bullet.1"))
      html must include(msgs("totalAmountPaid.bullet.2"))
      html must include(msgs("totalAmountPaid.bullet.3"))
      html must include(msgs("totalAmountPaid.paragraph.2"))
    }

    "must render a back link" in {
      render() must include("govuk-back-link")
    }

    "must render a text input" in {
      Option(Jsoup.parse(render()).getElementById("value")) must not be None
    }

    "must render the continue button" in {
      render() must include(msgs("site.continue"))
    }

    "must post to the supplier journey submit route when rendered for the supplier journey" in {
      Jsoup.parse(render(postAction = supplierPostAction)).select("form").attr("action") mustEqual supplierPostAction.url
    }

    "must post to the import journey submit route when rendered for the import journey" in {
      Jsoup.parse(render(postAction = importPostAction)).select("form").attr("action") mustEqual importPostAction.url
    }

    "must render identical content for the supplier and import journeys other than the form action" in {
      val supplierHtml = Jsoup.parse(render(postAction = supplierPostAction))
      val importHtml   = Jsoup.parse(render(postAction = importPostAction))

      supplierHtml.select("form").removeAttr("action")
      importHtml.select("form").removeAttr("action")

      supplierHtml.body.html mustEqual importHtml.body.html
    }

    "must show an error summary linking to the input when nothing is entered" in {
      val document = Jsoup.parse(render(form.bind(Map("value" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("totalAmountPaid.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
    }

    "must show the length error when more than 14 characters are entered" in {
      render(form.bind(Map("value" -> ("1" * 15)))) must include(msgs("totalAmountPaid.error.length"))
    }

    "must show the comma/decimal error when a comma is entered" in {
      val html = render(form.bind(Map("value" -> "15,000")))

      html must include(msgs("totalAmountPaid.error.commaOrDecimalPoint"))
      Jsoup.parse(html).getElementById("value").hasClass("govuk-input--error") mustEqual true
    }

    "must show the format error when other invalid characters are entered" in {
      render(form.bind(Map("value" -> "15000abc"))) must include(msgs("totalAmountPaid.error.invalid"))
    }

    "must pre-populate the input with a previously entered value" in {
      Jsoup.parse(render(form.fill("15000"))).getElementById("value").attr("value") mustEqual "15000"
    }
  }
}
