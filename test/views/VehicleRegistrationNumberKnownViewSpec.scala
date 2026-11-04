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
import forms.VehicleRegistrationNumberKnownFormProvider
import models.{NormalMode, SupplierNumber, VehicleNumber}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText
import viewmodels.govuk.inset.*
import views.html.VehicleRegistrationNumberKnownView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*

class VehicleRegistrationNumberKnownViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: VehicleRegistrationNumberKnownView = app.injector.instanceOf[VehicleRegistrationNumberKnownView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[Boolean] = new VehicleRegistrationNumberKnownFormProvider()()

  val postAction: Call =
    controllers.vehicledetails.routes.VehicleRegistrationNumberKnownController.onSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <b>Volvo FH16</b>")

  private def doc(form: Form[Boolean] = form, vehicleDetails: Option[InsetText] = Some(vehicleDetails)): Document =
    Jsoup.parse(view(form, vehicleDetails, postAction)(request, msgs).toString)

  "VehicleRegistrationNumberKnownView" - {

    "must render the AVD10.2 page title" in {
      doc().title mustEqual msgs("vehicleRegistrationNumberKnown.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the vehicle details inset above the caption" in {
      val document = doc()

      document.select(".govuk-inset-text").text mustEqual "Vehicle details: Volvo FH16"
      document.select(".govuk-inset-text + .govuk-caption-l").isEmpty mustEqual false
    }

    "must not render an inset when there are no vehicle details" in {
      doc(vehicleDetails = None).select(".govuk-inset-text").isEmpty mustEqual true
    }

    "must render the caption" in {
      doc().select("span.govuk-caption-l").text mustEqual msgs("vehicleRegistrationNumberKnown.caption")
    }

    "must render the heading as the legend of the radios" in {
      doc().select("legend h1").text mustEqual msgs("vehicleRegistrationNumberKnown.heading")
    }

    "must render Yes and No radio options" in {
      doc().select(".govuk-radios__label").asScala.map(_.text).toSeq mustEqual Seq(msgs("site.yes"), msgs("site.no"))
    }

    "must post to the given submit route" in {
      doc().select("form").attr("action") mustEqual postAction.url
    }

    "must render the continue button" in {
      doc().select("button.govuk-button").text mustEqual msgs("site.continue")
    }

    "must show an error summary linking to the first radio when nothing is selected" in {
      val document = doc(form.bind(Map("value" -> "")))

      document.select(".govuk-error-summary").text must include(msgs("vehicleRegistrationNumberKnown.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
      document.getElementById("value").attr("type") mustEqual "radio"
    }

    "must check the saved answer" in {
      doc(form.fill(false)).getElementById("value-no").hasAttr("checked") mustEqual true
    }
  }
}
