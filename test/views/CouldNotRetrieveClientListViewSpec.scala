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
import views.html.CouldNotRetrieveClientListView

class CouldNotRetrieveClientListViewSpec extends SpecBase with Matchers {

  "CouldNotRetrieveClientListView" - {

    "must render the correct heading" in new Setup {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include(msgs("couldNotRetrieveClientList.heading"))
    }

    "must render the correct page title" in new Setup {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include(msgs("couldNotRetrieveClientList.title"))
    }

    "must render the paragraph" in new Setup {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include(msgs("couldNotRetrieveClientList.paragraph"))
    }

    "must render all bullet point reasons" in new Setup {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include(msgs("couldNotRetrieveClientList.reason1"))
      html must include(msgs("couldNotRetrieveClientList.reason2"))
      html must include(msgs("couldNotRetrieveClientList.reason3"))
    }

    "must render the full helpdesk sentence with inline link" in new Setup {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include("If the problem continues, contact the")
      html must include(msgs("couldNotRetrieveClientList.helpdesk.linkText"))
    }

    "must render the helpdesk link URL" in new Setup {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include(helpdeskUrl)
    }

    "must render the helpdesk link with target blank for opening in new tab" in new Setup {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include("target=\"_blank\"")
    }

    "must render the return to home button" in new Setup {
      val html: String = view(helpdeskUrl)(request, msgs).toString

      html must include(msgs("couldNotRetrieveClientList.returnHome"))
    }

    "must render the same content via the render method" in new Setup {
      val html: String = view.render(helpdeskUrl, request, msgs).toString

      html must include(msgs("couldNotRetrieveClientList.heading"))
    }

    "must render the same content via the f method" in new Setup {
      val html: String = view.f(helpdeskUrl)(request, msgs).toString

      html must include(msgs("couldNotRetrieveClientList.heading"))
    }

    "must return itself via the ref method" in new Setup {
      view.ref mustBe view
    }
  }

  trait Setup {
    val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
    implicit val request: Request[?] = FakeRequest()
    implicit val msgs: Messages      = messages(app)

    val helpdeskUrl: String                  = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/online-services-helpdesk"
    val view: CouldNotRetrieveClientListView = app.injector.instanceOf[CouldNotRetrieveClientListView]
  }
}
