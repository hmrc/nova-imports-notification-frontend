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
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.NoVehicleDatesView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class NoVehicleDatesViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: NoVehicleDatesView = app.injector.instanceOf[NoVehicleDatesView]

  val personalTransportUnitUrl: String = "https://example.com/personal-transport-unit"

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  lazy val html: String = view(personalTransportUnitUrl).toString

  "NoVehicleDatesView" - {

    "must render the caption" in {
      html must include(msgs("noVehicleDates.caption"))
    }

    "must render the heading as a page heading" in {
      html must include(s"""<h1 class="govuk-heading-l">${msgs("noVehicleDates.heading")}</h1>""")
    }

    "must set the page title" in {
      html must include(s"<title>${msgs("noVehicleDates.title")} - Notification of Vehicle Arrivals - GOV.UK")
    }

    "must render the explanatory paragraph" in {
      html must include(msgs("noVehicleDates.paragraph.1"))
    }

    "must render the Personal Transport Unit link inside inset text" in {
      html must include("govuk-inset-text")
      html must include(personalTransportUnitUrl)
      html must include(msgs("noVehicleDates.link.text"))
    }

    "must open the Personal Transport Unit link in a new tab accessibly" in {
      html must include("""target="_blank"""")
      html must include("""rel="noopener noreferrer"""")
      html must include("(opens in new tab)")
    }

    "must not render a form or continue button" in {
      html must not include "govuk-button"
    }
  }
}
