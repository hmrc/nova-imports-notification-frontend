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
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.JourneyRecoveryStartAgainView

class JourneyRecoveryStartAgainViewSpec extends SpecBase with Matchers {
  "JourneyRecoveryStartAgainView" - {
    "must render the correct title" in new Setup {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must include(msgs("journeyRecovery.startAgain.title"))
    }

    "must render the correct heading" in new Setup {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must include("govuk-heading-l")
      html must include(msgs("journeyRecovery.startAgain.heading"))
    }

    "must render the first paragraph" in new Setup {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must include("govuk-body")
      html must include(msgs("journeyRecovery.startAgain.paragraph.1"))
    }

    "must render the second paragraph" in new Setup {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must include("govuk-body")
      html must include(msgs("journeyRecovery.startAgain.paragraph.2"))
    }

    "must render the support link" in new Setup {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must include(msgs("journeyRecovery.startAgain.link.text"))
      html must include("govuk-link")
      html must include(technicalSupportUrl)
    }

    "must open the link in a new tab" in new Setup {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must include("target=\"_blank\"")
      html must include("rel=\"noreferrer noopener\"")
    }

    "must render the back link" in new Setup {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must include("govuk-back-link")
    }

    "must not render a button" in new Setup {
      val html: String = view(technicalSupportUrl)(request, msgs).toString

      html must not include "govuk-button"
    }

    "must render the same content via the render method" in new Setup {
      val html: String = view.render(technicalSupportUrl, request, msgs).toString

      html must include(msgs("journeyRecovery.startAgain.heading"))
    }

    "must render the same content via the f method" in new Setup {
      val html: String = view.f(technicalSupportUrl)(request, msgs).toString

      html must include(msgs("journeyRecovery.startAgain.heading"))
    }

    "must return itself via the ref method" in new Setup {
      view.ref mustBe view
    }
  }

  trait Setup {
    val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
    implicit val request: Request[?] = FakeRequest()
    implicit val msgs: Messages      = messages(app)

    val technicalSupportUrl: String         = "https://www.gov.uk/find-hmrc-contacts/technical-support-with-hmrc-online-services"
    val view: JourneyRecoveryStartAgainView = app.injector.instanceOf[JourneyRecoveryStartAgainView]
  }
}
