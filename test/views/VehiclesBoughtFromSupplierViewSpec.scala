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
import controllers.vehicledetails.routes
import models.SupplierNumber
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.VehiclesBoughtFromSupplierView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VehiclesBoughtFromSupplierViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: VehiclesBoughtFromSupplierView = app.injector.instanceOf[VehiclesBoughtFromSupplierView]

  val personalTransportUnitUrl: String = "https://example.com/personal-transport-unit"

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private def render(supplierName: Option[String] = None): String =
    view(supplierName, SupplierNumber(1), personalTransportUnitUrl).toString

  "VehiclesBoughtFromSupplierView" - {

    "must render the caption" in {
      render() must include(msgs("vehiclesBoughtFromSupplier.caption"))
    }

    "must render the supplier name in the heading" in {
      render(Some("ABC Ltd")) must include("""<h1 class="govuk-heading-l">Vehicles bought from ABC Ltd</h1>""")
    }

    "must render the fallback heading when there is no supplier name" in {
      render() must include(msgs("vehiclesBoughtFromSupplier.heading.noSupplierName"))
    }

    "must escape a supplier name containing markup" in {
      val html = render(Some("<script>alert(1)</script>"))

      html must not include "<script>alert(1)</script>"
      html must include("&lt;script&gt;")
    }

    "must render both date bullets in a bulleted list" in {
      val html = render()

      html must include("""<ul class="govuk-list govuk-list--bullet">""")
      html must include(msgs("vehiclesBoughtFromSupplier.bullet.1"))
      html must include(msgs("vehiclesBoughtFromSupplier.bullet.2"))
    }

    "must render the inset text with a link to the Personal Transport Unit" in {
      val html = render()

      html must include("govuk-inset-text")
      html must include(personalTransportUnitUrl)
      html must include("""target="_blank"""")
    }

    "must render the where to find these dates section" in {
      val html = render()

      html must include("""<h2 class="govuk-heading-m">""")
      html must include(msgs("vehiclesBoughtFromSupplier.whereToFindDates.heading"))
      html must include(msgs("vehiclesBoughtFromSupplier.whereToFindDates.paragraph.1"))
    }

    "must render both introductory paragraphs" in {
      val html = render()

      html must include(msgs("vehiclesBoughtFromSupplier.paragraph.1"))
      html must include(msgs("vehiclesBoughtFromSupplier.paragraph.2"))
    }

    "must render the Add vehicle button as a submit button" in {
      val html = render().replaceAll("\\s+", " ")

      html must include("""<button type="submit" class="govuk-button"""")
      html must include(msgs("vehiclesBoughtFromSupplier.addVehicle"))
    }

    "must post the form back to this page" in {
      val html = render()

      html must include(s"""action="${routes.VehiclesBoughtFromSupplierController.onSubmit(SupplierNumber(1)).url}"""")
    }

    "must set the page title" in {
      render() must include("<title>Vehicles bought from this supplier - Notification of Vehicle Arrivals - GOV.UK")
    }
  }
}
