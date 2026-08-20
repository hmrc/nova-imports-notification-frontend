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
import forms.VehicleDatesFormProvider
import models.{NormalMode, SupplierNumber, VehicleNumber}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.VehicleDatesView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VehicleDatesViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: VehicleDatesView = app.injector.instanceOf[VehicleDatesView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider = new VehicleDatesFormProvider()
  val form         = formProvider()

  private def render(): String =
    view(form, SupplierNumber(1), VehicleNumber(1), NormalMode)(request, msgs).toString

  "VehicleDatesView" - {

    "must render the correct heading" in {
      render() must include(msgs("vehicleDates.heading"))
    }

    "must render the correct page title" in {
      render() must include(msgs("vehicleDates.title"))
    }

    "must render the correct page caption" in {
      val html = render()

      html must include("govuk-caption-l")
      html must include(msgs("vehicleDates.caption"))
    }

    "must render the hint text" in {
      render() must include(msgs("vehicleDates.hint"))
    }

    "must render the purchase invoice date checkbox" in {
      render() must include(msgs("vehicleDates.checkbox.purchaseInvoiceDate"))
    }

    "must render the date of availability checkbox" in {
      render() must include(msgs("vehicleDates.checkbox.availabilityAndFirstRegistration"))
    }

    "must render the no dates checkbox after an or divider" in {
      val html = render()

      html must include("govuk-checkboxes__divider")
      html must include(msgs("vehicleDates.or"))
      html must include(msgs("vehicleDates.checkbox.noDates"))
    }

    "must make the no dates checkbox exclusive" in {
      render() must include("""data-behaviour="exclusive"""")
    }

    "must give every checkbox the same name so the exclusive behaviour can find its siblings" in {
      val names = """name="([^"]+)"""".r.findAllMatchIn(render()).map(_.group(1)).filter(_.startsWith("value")).toSeq

      names must have length 3
      names.distinct mustEqual Seq("value[]")
    }

    "must render the error summary when the form has errors" in {
      val boundForm = form.bind(Map.empty[String, String])
      val html      = view(boundForm, SupplierNumber(1), VehicleNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("vehicleDates.error.required"))
      html must include("#value_0")
    }
  }
}
