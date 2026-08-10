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
import forms.VehicleFromEuFormProvider
import models.NormalMode
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.VehicleFromEuView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VehicleFromEuViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: VehicleFromEuView = app.injector.instanceOf[VehicleFromEuView]
  val form: Form[Boolean]     = new VehicleFromEuFormProvider()()
  val importingUrl: String    = "https://example.com/importing-vehicles"
  val euCountriesUrl: String  = "https://example.com/eu-eea"

  def render(f: Form[Boolean] = form): String =
    view(f, NormalMode, importingUrl, euCountriesUrl)(request, msgs).toString

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "VehicleFromEuView" - {

    "must render the page title" in {
      render() must include(msgs("vehicleFromEu.title"))
    }

    "must render a standalone level-one heading styled as a large heading" in {
      val html = render()
      html must include("""<h1 class="govuk-heading-l">""")
      html must include(msgs("vehicleFromEu.title"))
    }

    "must render the initial questions caption" in {
      val html = render()
      html must include("""<span class="govuk-caption-l">""")
      html must include(msgs("vehicleFromEu.caption"))
    }

    "must render the first paragraph" in {
      render() must include(msgs("vehicleFromEu.paragraph.1"))
    }

    "must render the second paragraph with a new-tab link to importing vehicles" in {
      val html = render()
      html must include(importingUrl)
      html must include("""target="_blank"""")
      html must include("""class="govuk-link"""")
      html must include(msgs("vehicleFromEu.paragraph.2.linkText"))
    }

    "must render the third paragraph with a new-tab link to the EU countries list" in {
      val html = render()
      html must include(euCountriesUrl)
      html must include("""target="_blank"""")
      html must include(msgs("vehicleFromEu.paragraph.3.linkText"))
    }

    "must place the full stop immediately after each link, with no space inside the link" in {
      val html = render()
      html must include(s"""href="$importingUrl">${msgs("vehicleFromEu.paragraph.2.linkText")} (opens in new tab)</a>.""")
      html must include(s"""href="$euCountriesUrl">${msgs("vehicleFromEu.paragraph.3.linkText")} (opens in new tab)</a>.""")
    }

    "must render the radio question as a level-two heading inside a medium legend" in {
      val html = render()
      html must include("""govuk-fieldset__legend--m""")
      html must include("""<h2 class="govuk-fieldset__heading">""")
      html must include(msgs("vehicleFromEu.heading"))
    }

    "must render exactly one level-one heading on the page" in {
      "<h1".r.findAllMatchIn(render()).size mustBe 1
    }

    "must render the yes and no radio options and the continue button" in {
      val html = render()
      html must include(msgs("site.yes"))
      html must include(msgs("site.no"))
      html must include(msgs("site.continue"))
    }

    "must render the required error message when the form has an empty submission" in {
      render(form.bind(Map("value" -> ""))) must include(msgs("vehicleFromEu.error.required"))
    }

    "must render the same content via the render method" in {
      view.render(form, NormalMode, importingUrl, euCountriesUrl, request, msgs).toString must include(
        msgs("vehicleFromEu.title")
      )
    }

    "must render the same content via the f method" in {
      view.f(form, NormalMode, importingUrl, euCountriesUrl)(request, msgs).toString must include(
        msgs("vehicleFromEu.title")
      )
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }

}
