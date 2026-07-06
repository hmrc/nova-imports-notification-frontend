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
import views.html.PageNotFoundView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PageNotFoundViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val technicalSupportUrl: String = "https://www.gov.uk/find-hmrc-contacts/technical-support-with-hmrc-online-services"
  val view: PageNotFoundView      = app.injector.instanceOf[PageNotFoundView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "PageNotFoundView" - {
    "must render the correct title" in {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must include(msgs("pageNotFound.title"))
    }

    "must render the correct heading" in {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must include("govuk-heading-l")
      html must include(msgs("pageNotFound.heading"))
    }

    "must render the first paragraph" in {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must include("govuk-body")
      html must include(msgs("pageNotFound.paragraph.1"))
    }

    "must render the second paragraph" in {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must include("govuk-body")
      html must include(msgs("pageNotFound.paragraph.2"))
    }

    "must render the third paragraph with the support link" in {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must include(msgs("pageNotFound.paragraph.3.start"))
      html must include(msgs("pageNotFound.paragraph.3.end"))
      html must include(msgs("pageNotFound.link.text"))
      html must include("govuk-link")
      html must include(technicalSupportUrl)
    }

    "must open the support link in a new tab" in {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must include("target=\"_blank\"")
      html must include("rel=\"noreferrer noopener\"")
    }

    "must render a back link" in {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must include("govuk-back-link")
    }

    "must render the same content via the render method" in {
      val html: String = view.render(technicalSupportUrl, request, msgs).toString

      html must include(msgs("pageNotFound.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(technicalSupportUrl)(request, msgs).toString

      html must include(msgs("pageNotFound.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }

}
