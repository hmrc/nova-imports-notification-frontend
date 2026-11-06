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
import forms.LeftOrRightHandDriveFormProvider
import models.{LeftOrRightHandDrive, NormalMode, SupplierNumber, VehicleNumber}
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
import views.html.LeftOrRightHandDriveView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class LeftOrRightHandDriveViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: LeftOrRightHandDriveView = app.injector.instanceOf[LeftOrRightHandDriveView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form: Form[LeftOrRightHandDrive] = new LeftOrRightHandDriveFormProvider()()

  val postAction: Call =
    controllers.vehicledetails.routes.LeftOrRightHandDriveController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <b>Make Model BodyType</b>")

  private def render(form: Form[LeftOrRightHandDrive] = form, vehicleDetails: Option[InsetText] = Some(vehicleDetails)): String =
    view(form, vehicleDetails, postAction)(request, msgs).toString

  "LeftOrRightHandDriveView" - {

    "must render the correct page title" in {
      Jsoup.parse(render()).title mustEqual msgs("leftOrRightHandDrive.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the vehicle details inset above the caption" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-inset-text").text mustEqual "Vehicle details: Make Model BodyType"
      document.select(".govuk-inset-text + .govuk-caption-l").isEmpty mustEqual false
    }

    "must not render an inset when there are no vehicle details" in {
      Jsoup.parse(render(vehicleDetails = None)).select(".govuk-inset-text").isEmpty mustEqual true
    }

    "must render the caption and the question as a large legend with h1" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-caption-l").text mustEqual msgs("leftOrRightHandDrive.caption")
      document.select("legend.govuk-fieldset__legend--l h1").text mustEqual msgs("leftOrRightHandDrive.heading")
    }

    "must render the hint" in {
      Jsoup.parse(render()).select(".govuk-hint").text mustEqual msgs("leftOrRightHandDrive.hint")
    }

    "must render the left-hand drive and right-hand drive radios" in {
      val document = Jsoup.parse(render())

      document.getElementById("value").attr("value") mustEqual "LHD"
      document.getElementById("value-2").attr("value") mustEqual "RHD"
      document.select("label[for=value]").text mustEqual msgs("leftOrRightHandDrive.radio.leftHandDrive")
      document.select("label[for=value-2]").text mustEqual msgs("leftOrRightHandDrive.radio.rightHandDrive")
    }

    "must post to the submit URL" in {
      Jsoup.parse(render()).select("form").attr("action") mustEqual postAction.url
    }

    "must turn autocomplete off on the form" in {
      Jsoup.parse(render()).select("form").attr("autocomplete") mustEqual "off"
    }

    "must render the Continue button" in {
      render() must include(msgs("site.continue"))
    }

    "must show an error summary message when nothing is selected" in {
      val document = Jsoup.parse(render(form.bind(Map("value" -> ""))))

      document.select(".govuk-error-summary").text must include(msgs("leftOrRightHandDrive.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
    }

    "must pre-fill the right-hand drive radio when right-hand drive was previously selected" in {
      val document = Jsoup.parse(render(form.fill(LeftOrRightHandDrive.RHD)))

      document.getElementById("value-2").hasAttr("checked") mustEqual true
      document.getElementById("value").hasAttr("checked") mustEqual false
    }
  }
}
