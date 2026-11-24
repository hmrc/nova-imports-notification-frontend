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
import forms.DeleteVehicleFormProvider
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import views.html.DeleteVehicleView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class DeleteVehicleViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: DeleteVehicleView = app.injector.instanceOf[DeleteVehicleView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form       = new DeleteVehicleFormProvider()()
  val submitCall = Call("POST", "/nova-imports/supplier/1/vehicle/2/delete")

  private val vehicleDetails = Seq(
    "deleteVehicle.vin"   -> "1ABCD2E03FG456789",
    "deleteVehicle.make"  -> "Volvo",
    "deleteVehicle.model" -> "FH16"
  )

  private def render(details: Seq[(String, String)] = vehicleDetails, boundForm: Form[Boolean] = form) =
    Jsoup.parse(view(boundForm, details, submitCall).toString)

  "DeleteVehicleView" - {

    "must render the caption and Delete vehicle as a h1" in {
      val doc = render()

      doc.select(".govuk-caption-l").text mustEqual "Add vehicle details"
      doc.select("h1").text mustEqual "Delete vehicle"
    }

    "must render the VIN, make and model in the inset text" in {
      render().select(".govuk-inset-text").text mustEqual
        "Vehicle identification number: 1ABCD2E03FG456789 Make: Volvo Model: FH16"
    }

    "must escape the vehicle details" in {
      render(Seq("deleteVehicle.make" -> "<script>")).select(".govuk-inset-text b").first.text mustEqual "<script>"
    }

    "must not render the inset text when there are no vehicle details" in {
      render(Seq.empty).select(".govuk-inset-text").isEmpty mustBe true
    }

    "must render the question as a h3 above the yes and no options, with a hidden legend for screen readers" in {
      val doc = render()

      doc.select("h3.govuk-heading-m").text mustEqual "Are you sure you want to delete this vehicle?"
      doc.select("legend.govuk-visually-hidden").text mustEqual "Are you sure you want to delete this vehicle?"
      doc.select("input[type=radio]").eachAttr("value") must contain theSameElementsInOrderAs Seq("true", "false")
    }

    "must post to the submit call" in {
      render().select("form").attr("action") mustEqual submitCall.url
    }

    "must render the error message when nothing is selected" in {
      render(boundForm = form.bind(Map("value" -> ""))).select(".govuk-error-message").text must include(
        "Select yes if you want to delete this vehicle"
      )
    }
  }
}
