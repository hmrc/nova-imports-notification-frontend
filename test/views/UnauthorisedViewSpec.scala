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
import views.html.UnauthorisedView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UnauthorisedViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val helpdeskUrl: String    = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/online-services-helpdesk"
  val view: UnauthorisedView = app.injector.instanceOf[UnauthorisedView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "UnauthorisedView" - {
    "must render the correct title" in {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include(msgs("unauthorised.title"))
    }

    "must render the correct heading" in {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include("govuk-heading-l")
      html must include(msgs("unauthorised.heading"))
    }

    "must render the first paragraph" in {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include("govuk-body")
      html must include(msgs("unauthorised.paragraph.1"))
    }

    "must render the second paragraph" in {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include("govuk-body")
      html must include(msgs("unauthorised.paragraph.2"))
    }

    "must render the support link" in {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include("govuk-body")
      html must include(msgs("unauthorised.link.text"))
      html must include("govuk-link")
      html must include(helpdeskUrl)
    }

    "must open the link in a new tab" in {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include("target=\"_blank\"")
    }

    "must render the 'Return to home' button" in {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include("govuk-link")
      html must include(msgs("unauthorised.returnHome"))
    }

    "must render the same content via the render method" in {
      val html: String = view.render(helpdeskUrl, request, msgs).toString

      html must include(msgs("unauthorised.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(helpdeskUrl)(request, msgs).toString

      html must include(msgs("unauthorised.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }

}
