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
import models.{SupplierNumber, VehicleNumber}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import viewmodels.Pager
import views.html.CheckVinNumbersView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class CheckVinNumbersViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: CheckVinNumbersView = app.injector.instanceOf[CheckVinNumbersView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private val vins          = Seq.tabulate(12)(n => s"VIN$n")
  private val submitCall    = controllers.vehicledetails.routes.CheckVinNumbersController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1))
  private val updateLinkUrl = "/foo/update"

  private def render(page: Int = 1, vehicleType: Option[String] = Some("Car"), allVins: Seq[String] = vins): String =
    view(Pager.page(allVins, page), vehicleType, allVins.size, p => s"/page/$p", submitCall, updateLinkUrl).toString

  "CheckVinNumbersView" - {

    "must render the AMV4.0 page title without pagination wording when there is only one page" in {
      val document = Jsoup.parse(render(allVins = Seq("VIN1")))

      document.title mustEqual msgs("checkVinNumbers.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the paginated page title when there is more than one page" in {
      Jsoup.parse(render(page = 1)).title must startWith(msgs("checkVinNumbers.pagination.title", 1, 2))
    }

    "must render the caption, heading and the vehicle type/count inset" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-caption-l").text mustEqual msgs("checkVinNumbers.caption")
      document.select("h1").text mustEqual msgs("checkVinNumbers.heading")
      document.select(".govuk-inset-text").text must include("Car")
      document.select(".govuk-inset-text").text must include("12")
    }

    "must render a table row per VIN on the page with its 1-based item number" in {
      val document = Jsoup.parse(render(page = 1))
      val rows     = document.select("table tbody tr")

      rows.size mustEqual 10
      rows.get(0).select("td").get(0).text mustEqual "1"
      rows.get(0).select("td").get(1).text mustEqual "VIN0"
    }

    "must render page 2 with the remaining VINs numbered from 11" in {
      val document = Jsoup.parse(render(page = 2))
      val rows     = document.select("table tbody tr")

      rows.size mustEqual 2
      rows.get(0).select("td").get(0).text mustEqual "11"
    }

    "must render pagination links when there is more than one page" in {
      Jsoup.parse(render(page = 1)).select(".govuk-pagination").isEmpty mustEqual false
    }

    "must not render pagination when everything fits on one page" in {
      Jsoup.parse(render(allVins = Seq("VIN1"))).select(".govuk-pagination").isEmpty mustEqual true
    }

    "must post to the given submit route" in {
      Jsoup.parse(render()).select("form").attr("action") mustEqual submitCall.url
    }

    "must render 'Save and continue' as a real submit button inside the form, not a link" in {
      val document   = Jsoup.parse(render())
      val saveButton = document.select("form button, form input[type=submit]")

      saveButton.isEmpty mustEqual false
      saveButton.attr("href") mustEqual ""
    }

    "must render the update link pointing at the given url" in {
      Jsoup.parse(render()).select("div.govuk-button-group a.govuk-link").attr("href") mustEqual updateLinkUrl
    }
  }
}
