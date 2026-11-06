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
import forms.TotalValueOfOptionsFormProvider
import models.{NormalMode, SupplierNumber, VehicleNumber}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText
import viewmodels.govuk.inset.*
import views.html.TotalValueOfOptionsView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class TotalValueOfOptionsViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: TotalValueOfOptionsView = app.injector.instanceOf[TotalValueOfOptionsView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[String] = new TotalValueOfOptionsFormProvider()()

  val postAction: Call =
    controllers.vehicledetails.routes.TotalValueOfOptionsController.onSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <b>Volvo FH16</b>")

  private def render(form: Form[String] = form, vehicleDetails: Option[InsetText] = Some(vehicleDetails)): String =
    view(form, vehicleDetails, postAction)(request, msgs).toString

  "TotalValueOfOptionsView" - {

    "must render the AVD10.7 page title" in {
      Jsoup.parse(render()).title mustEqual msgs("totalValueOfOptions.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the vehicle details inset above the caption" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-inset-text").text mustEqual "Vehicle details: Volvo FH16"
      document.select(".govuk-inset-text").hasClass("govuk-!-margin-top-0") mustEqual true
      document.select(".govuk-inset-text + .govuk-caption-l").isEmpty mustEqual false
    }

    "must not render an inset when there are no vehicle details" in {
      Jsoup.parse(render(vehicleDetails = None)).select(".govuk-inset-text").isEmpty mustEqual true
    }

    "must render the caption, heading, paragraphs and examples of optional extras" in {
      val document   = Jsoup.parse(render())
      val paragraphs = document.select("h1 ~ p.govuk-body")

      document.select(".govuk-caption-l").text mustEqual msgs("totalValueOfOptions.caption")
      document.select("h1").text mustEqual msgs("totalValueOfOptions.heading.1")
      paragraphs.get(0).text mustEqual msgs("totalValueOfOptions.paragraph.1")
      paragraphs.get(1).text mustEqual msgs("totalValueOfOptions.paragraph.2")
      document.select("ul.govuk-list--bullet li").eachText mustEqual java.util.List.of(
        msgs("totalValueOfOptions.bullet.1"),
        msgs("totalValueOfOptions.bullet.2"),
        msgs("totalValueOfOptions.bullet.3")
      )
    }

    "must render the question as a medium label with the hint against the input" in {
      val document = Jsoup.parse(render())

      document.select("label.govuk-label--m").attr("for") mustEqual "value"
      document.select("label.govuk-label--m").text mustEqual msgs("totalValueOfOptions.heading.2")
      document.select(".govuk-hint").text mustEqual msgs("totalValueOfOptions.hint")
      document.getElementById("value").attr("aria-describedby") must include("value-hint")
    }

    "must turn autocomplete off on the input" in {
      Jsoup.parse(render()).getElementById("value").attr("autocomplete") mustEqual "off"
    }

    "must post to the given submit route" in {
      val form = Jsoup.parse(render()).select("form")

      form.attr("action") mustEqual postAction.url
      form.hasClass("govuk-!-margin-bottom-8") mustEqual true
    }

    "must render a back link" in {
      render() must include("govuk-back-link")
    }

    "must show an error summary linking to the input when nothing is entered" in {
      val document = Jsoup.parse(render(form.bind(Map("value" -> ""))))

      document.title                               must startWith(msgs("error.title.prefix"))
      document.select(".govuk-error-summary").text must include(msgs("totalValueOfOptions.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
    }

    "must fill the input with the saved value" in {
      Jsoup.parse(render(form.fill("1500"))).getElementById("value").attr("value") mustEqual "1500"
    }
  }
}
