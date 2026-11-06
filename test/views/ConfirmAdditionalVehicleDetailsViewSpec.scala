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
import models.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import pages.sections.vehicledetails.*
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import viewmodels.checkAnswers.ConfirmAdditionalVehicleDetailsHelper
import views.html.ConfirmAdditionalVehicleDetailsView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ConfirmAdditionalVehicleDetailsViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: ConfirmAdditionalVehicleDetailsView = app.injector.instanceOf[ConfirmAdditionalVehicleDetailsView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private val s      = SupplierNumber(1)
  private val v      = VehicleNumber(1)
  private val submit = routes.ConfirmAdditionalVehicleDetailsController.supplierOnSubmit(s, v)

  private val answers = emptyUserAnswers
    .unsafeSet(NumberOfVehiclesPage(v), NumberOfVehicles(OneMultiVehicle.One, None))
    .unsafeSet(MileagePage(v), Mileage(MileageUnit.MILES, "25000"))

  private val doc: Document =
    Jsoup.parse(view(ConfirmAdditionalVehicleDetailsHelper.supplierSummaryList(answers, s, v, Seq.empty), submit).toString)

  "ConfirmAdditionalVehicleDetailsView" - {

    "must have the page title" in {
      doc.title mustEqual "Check the additional vehicle details before you continue - Notification of Vehicle Arrivals - GOV.UK"
    }

    "must have the caption" in {
      doc.select("span.govuk-caption-l").text() mustEqual "Add vehicle details"
    }

    "must have the heading" in {
      doc.select("h1").text() mustEqual "Check the additional vehicle details before you continue"
    }

    "must render the answers in a summary list" in {
      doc.select(".govuk-summary-list__row").size mustEqual 2
    }

    "must have a Confirm details button submitting to the given call" in {
      doc.select("form").attr("action") mustEqual submit.url
      doc.select("form").attr("method") mustEqual "POST"
      doc.select("form button.govuk-button").text() mustEqual "Confirm details"
    }

    "must give each change link visually hidden text" in {
      doc.select(".govuk-summary-list__actions a .govuk-visually-hidden").eachText().toArray mustEqual
        Array("how many vehicles this notification is for", "mileage")
    }
  }
}
