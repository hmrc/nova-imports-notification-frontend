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
import views.html.LandingPageAgentView

class LandingPageAgentViewSpec extends SpecBase with Matchers {

  private val traderName = "ABC Consultancy"

  "LandingPageAgentView" - {

    "must render the correct page title and H1 heading" in new Setup {
      val html: String = view(Some(traderName), hasDraftNotifications = false).toString

      html must include(msgs("landingPage.agent.title"))
      html must include(msgs("landingPage.agent.heading"))
    }

    "must render the trader name caption when supplied" in new Setup {
      val html: String = view(Some(traderName), hasDraftNotifications = false).toString

      html must include("""class="govuk-caption-m"""")
      html must include(traderName)
    }

    "must omit the trader name caption when not supplied" in new Setup {
      val html: String = view(None, hasDraftNotifications = false).toString

      html must not include traderName
      html must not include """class="govuk-caption-m""""
    }

    "must render the intro body" in new Setup {
      val html: String = view(Some(traderName), hasDraftNotifications = false).toString

      html must include(msgs("landingPage.agent.body"))
    }

    "must render the Create a new notification link routing to StartController" in new Setup {
      val html: String = view(Some(traderName), hasDraftNotifications = false).toString

      html must include(msgs("landingPage.agent.create.link"))
      html must include(msgs("landingPage.agent.create.body"))
      html must include(controllers.routes.StartController.start().url)
    }

    "must render the Update a submitted notification link routing to StartController" in new Setup {
      val html: String = view(Some(traderName), hasDraftNotifications = false).toString

      html must include(msgs("landingPage.agent.update.link"))
      html must include(msgs("landingPage.agent.update.body"))
      html must include(controllers.routes.StartController.start().url)
    }

    "must render the empty saved-notification copy when the user has no drafts" in new Setup {
      val html: String = view(Some(traderName), hasDraftNotifications = false).toString

      html must include(msgs("landingPage.agent.saved.heading"))
      html must include(msgs("landingPage.agent.saved.body.empty"))
      html must not include msgs("landingPage.agent.saved.body.has")
    }

    "must render the has-drafts saved-notification copy when the user has drafts" in new Setup {
      val html: String = view(Some(traderName), hasDraftNotifications = true).toString

      html must include(msgs("landingPage.agent.saved.heading"))
      html must include(msgs("landingPage.agent.saved.body.has"))
      html must not include msgs("landingPage.agent.saved.body.empty")
    }

    "must render the Manage your clients link routing to LoadingClientListController (CS1.0)" in new Setup {
      val html: String = view(Some(traderName), hasDraftNotifications = false).toString

      html must include(msgs("landingPage.agent.clients.link"))
      html must include(msgs("landingPage.agent.clients.body"))
      html must include(controllers.routes.LoadingClientListController.onPageLoad().url)
    }

    "must render the same content via the render method" in new Setup {
      val html: String = view.render(Some(traderName), false, request, msgs).toString

      html must include(msgs("landingPage.agent.heading"))
    }

    "must render the same content via the f method" in new Setup {
      val html: String = view.f(Some(traderName), false)(request, msgs).toString

      html must include(msgs("landingPage.agent.heading"))
    }

    "must return itself via the ref method" in new Setup {
      view.ref mustBe view
    }
  }

  trait Setup {
    val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
    implicit val request: Request[?] = FakeRequest()
    implicit val msgs: Messages      = messages(app)

    val view: LandingPageAgentView = app.injector.instanceOf[LandingPageAgentView]
  }
}
