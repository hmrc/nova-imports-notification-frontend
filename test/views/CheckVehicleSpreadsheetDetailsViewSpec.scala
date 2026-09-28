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
import controllers.vehicledetails
import models.responses.VehicleSummary
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import viewmodels.Pager
import views.html.CheckVehicleSpreadsheetDetailsView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*

class CheckVehicleSpreadsheetDetailsViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: CheckVehicleSpreadsheetDetailsView = app.injector.instanceOf[CheckVehicleSpreadsheetDetailsView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private val vehicle = VehicleSummary(itemNumber = Some(1), vin = Some("1ABCD2EO3FGI45678"), make = Some("Volkswagen"), model = Some("Golf"))

  private def urlForPage(page: Int): String = vehicledetails.routes.CheckVehicleSpreadsheetDetailsController.onPageLoad(page).url

  private def htmlFor(vehicles: Seq[VehicleSummary], page: Int = 1): String =
    view(Pager.page(vehicles, page), urlForPage).toString

  "CheckVehicleSpreadsheetDetailsView" - {

    lazy val html: String = htmlFor(Seq(vehicle))

    "must render the caption" in {
      html must include(msgs("checkVehicleSpreadsheetDetails.caption"))
    }

    "must render the table full-width as a sibling of the two-thirds column, not nested inside it" in {
      val twoThirdsOpen  = html.indexOf("govuk-grid-column-two-thirds")
      val twoThirdsClose = html.indexOf("</div>", twoThirdsOpen)
      val fullWidthOpen  = html.indexOf("govuk-grid-column-full")

      twoThirdsOpen must be >= 0
      fullWidthOpen must be >= 0
      fullWidthOpen must be > twoThirdsClose
    }

    "must render the heading as a page heading" in {
      html must include(s"""<h1 class="govuk-heading-l">${msgs("checkVehicleSpreadsheetDetails.heading")}</h1>""")
    }

    "must set the page title" in {
      html must include(s"<title>${msgs("checkVehicleSpreadsheetDetails.title")} - Notification of Vehicle Arrivals - GOV.UK")
    }

    "must render the table headers" in {
      html must include(msgs("checkVehicleSpreadsheetDetails.table.item"))
      html must include(msgs("checkVehicleSpreadsheetDetails.table.vin"))
      html must include(msgs("checkVehicleSpreadsheetDetails.table.make"))
      html must include(msgs("checkVehicleSpreadsheetDetails.table.model"))
    }

    "must render a row for each vehicle" in {
      html must include("1ABCD2EO3FGI45678")
      html must include("Volkswagen")
      html must include("Golf")
    }

    "must render blank cells for missing vehicle fields" in {
      val incomplete = VehicleSummary(itemNumber = None, vin = None, make = None, model = None)
      noException must be thrownBy htmlFor(Seq(incomplete))
    }

    "must render the table caption summarising the showing range" in {
      html must include(msgs("checkVehicleSpreadsheetDetails.table.caption", 1, 1, 1))
    }

    "must render the download notification summary link" in {
      html must include(msgs("checkVehicleSpreadsheetDetails.downloadLink"))
    }

    "must render the save and continue button" in {
      html must include(msgs("checkVehicleSpreadsheetDetails.saveAndContinue"))
    }

    "must render the update spreadsheet link" in {
      html must include(msgs("checkVehicleSpreadsheetDetails.updateSpreadsheetLink"))
    }

    "must link the update spreadsheet link to UVS5.1" in {
      val link = Jsoup.parse(html).select("a.govuk-link").asScala.find(_.text == msgs("checkVehicleSpreadsheetDetails.updateSpreadsheetLink")).value

      link.attr("href") mustEqual vehicledetails.routes.UpdateVehicleSpreadsheetController.onPageLoad().url
    }

    "must not render pagination when there is only one page of vehicles" in {
      html must not include "govuk-pagination"
    }

    "must render pagination linking back to this page when there is more than one page of vehicles" in {
      val manyVehicles = (1 to 15).map(n => vehicle.copy(itemNumber = Some(n)))
      val paginated    = htmlFor(manyVehicles, page = 1)

      paginated must include("govuk-pagination")
      paginated must include(s"""href="${urlForPage(2)}"""")
    }
  }
}
