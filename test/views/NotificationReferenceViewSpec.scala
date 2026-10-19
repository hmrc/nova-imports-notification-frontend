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
import forms.NotificationReferenceFormProvider
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
import views.html.NotificationReferenceView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class NotificationReferenceViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: NotificationReferenceView = app.injector.instanceOf[NotificationReferenceView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[String] = new NotificationReferenceFormProvider()()

  val postAction: Call =
    controllers.vehicledetails.routes.NotificationReferenceController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <b>Volvo FH16</b>")

  private def render(form: Form[String] = form, vehicleDetails: Option[InsetText] = Some(vehicleDetails)): String =
    view(form, vehicleDetails, postAction)(request, msgs).toString

  "NotificationReferenceView" - {

    "must render the AVD9.0 page title" in {
      Jsoup.parse(render()).title mustEqual msgs("notificationReference.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the vehicle details inset above the caption" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-inset-text").text mustEqual "Vehicle details: Volvo FH16"
      document.select(".govuk-inset-text + .govuk-caption-l").isEmpty mustEqual false
    }

    "must not render an inset when there are no vehicle details" in {
      Jsoup.parse(render(vehicleDetails = None)).select(".govuk-inset-text").isEmpty mustEqual true
    }

    "must render the caption, heading and paragraph" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-caption-l").text mustEqual msgs("notificationReference.caption")
      document.select("h1").text mustEqual msgs("notificationReference.heading.1")
      document.select("p.govuk-body").first.text mustEqual msgs("notificationReference.paragraph")
    }

    "must render the question as a medium label outside the h1" in {
      val document = Jsoup.parse(render())

      document.select("label.govuk-label--m").attr("for") mustEqual "value"
      document.select("label.govuk-label--m").text mustEqual msgs("notificationReference.heading.2")
      document.select("h1 label").isEmpty mustEqual true
    }

    "must render the hint text against the input" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-hint").text mustEqual msgs("notificationReference.hint")
      document.getElementById("value").attr("aria-describedby") must include("value-hint")
    }

    "must turn autocomplete off on the input" in {
      Jsoup.parse(render()).getElementById("value").attr("autocomplete") mustEqual "off"
    }

    "must post to the given submit route" in {
      Jsoup.parse(render()).select("form").attr("action") mustEqual postAction.url
    }

    "must render the continue button" in {
      render() must include(msgs("site.continue"))
    }

    "must show an error summary linking to the input when nothing is entered" in {
      val document = Jsoup.parse(render(form.bind(Map("value" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("notificationReference.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
    }

    "must fill the input with the saved notification reference" in {
      Jsoup.parse(render(form.fill("NOVA26E123456"))).getElementById("value").attr("value") mustEqual "NOVA26E123456"
    }
  }
}
