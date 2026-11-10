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
import models.SupplierNumber
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import views.html.AddingMultipleVehiclesErrorView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AddingMultipleVehiclesErrorViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: AddingMultipleVehiclesErrorView = app.injector.instanceOf[AddingMultipleVehiclesErrorView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  // TODO: Update route to AVD12.0 when built
  val reviewVehiclesUrl: Call = controllers.vehicledetails.routes.VehiclesBoughtFromSupplierAvd12PlaceholderController.onPageLoad(SupplierNumber(1))

  lazy val document: Document = Jsoup.parse(view(reviewVehiclesUrl)(request, msgs).toString)

  "AddingMultipleVehiclesErrorView" - {

    "must render the correct page title" in {
      document.title mustEqual msgs("addingMultipleVehiclesError.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the caption" in {
      document.select(".govuk-caption-l").text mustEqual msgs("addingMultipleVehiclesError.caption")
    }

    "must render the heading as a page heading" in {
      document.select("h1.govuk-heading-l").text mustEqual msgs("addingMultipleVehiclesError.heading")
    }

    "must render the paragraph" in {
      document.select("p.govuk-body").text must include(msgs("addingMultipleVehiclesError.paragraph"))
    }

    "must render the 'go back and change your answer' bullet as text with no link" in {
      val bullet = document.select("ul.govuk-list--bullet li").get(0)

      bullet.text mustEqual msgs("addingMultipleVehiclesError.list.1")
      bullet.select("a").isEmpty mustEqual true
    }

    "must render the 'review the vehicles' link to the URL passed in" in {
      val link = document.select("ul.govuk-list--bullet li").get(1).select("a")

      link.text mustEqual msgs("addingMultipleVehiclesError.list.2.link")
      link.attr("href") mustEqual reviewVehiclesUrl.url
    }

    "must render the 'create a new notification' link to the landing page" in {
      val link = document.select("ul.govuk-list--bullet li").get(2).select("a")

      link.text mustEqual msgs("addingMultipleVehiclesError.list.3.link")
      link.attr("href") mustEqual controllers.routes.LandingPageController.onPageLoad().url
    }
  }
}
