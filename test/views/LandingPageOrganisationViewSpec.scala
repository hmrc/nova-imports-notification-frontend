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
import views.html.LandingPageOrganisationView

class LandingPageOrganisationViewSpec extends SpecBase with Matchers {

  private val traderName = "ABC LTD"
  private val vrn        = "123456789"
  private val startUrl   = controllers.routes.StartController.start().url

  "LandingPageOrganisationView" - {

    "must render the page title and heading" in new Setup {
      val html: String = view(Some(traderName), vrn, hasDraftNotifications = false).toString
      html must include(msgs("landingPage.organisation.title"))
      html must include(msgs("landingPage.organisation.heading"))
    }

    "must render the trader name caption" in new Setup {
      val html: String = view(Some(traderName), vrn, hasDraftNotifications = false).toString
      html must include(traderName)
      html must include("govuk-caption-l")
    }

    "must render the VRN caption with GB prefix" in new Setup {
      val html: String = view(Some(traderName), vrn, hasDraftNotifications = false).toString
      html must include(msgs("landingPage.organisation.vrn.caption", vrn))
      html must include(s"GB$vrn")
      html must include("govuk-caption-l")
    }

    "must not render a trader name caption when traderName is None" in new Setup {
      val html: String = view(None, vrn, hasDraftNotifications = false).toString
      html must not include traderName
      html must include(msgs("landingPage.organisation.vrn.caption", vrn))
    }

    "must render the body intro" in new Setup {
      val html: String = view(Some(traderName), vrn, hasDraftNotifications = false).toString
      html must include(msgs("landingPage.organisation.body"))
    }

    "must render the create-a-new-notification section linking to /start" in new Setup {
      val html: String = view(Some(traderName), vrn, hasDraftNotifications = false).toString
      html must include(msgs("landingPage.organisation.create.link"))
      html must include(msgs("landingPage.organisation.create.body"))
      html must include(startUrl)
    }

    "must render the update-a-submitted-notification section linking to /start" in new Setup {
      val html: String = view(Some(traderName), vrn, hasDraftNotifications = false).toString
      html must include(msgs("landingPage.organisation.update.link"))
      html must include(msgs("landingPage.organisation.update.body"))
    }

    "must render the manage-a-saved-notification heading" in new Setup {
      val html: String = view(Some(traderName), vrn, hasDraftNotifications = false).toString
      html must include(msgs("landingPage.organisation.saved.heading"))
    }

    "must render the empty saved-notifications message when hasDraftNotifications is false" in new Setup {
      val html: String = view(Some(traderName), vrn, hasDraftNotifications = false).toString
      html must include(msgs("landingPage.organisation.saved.body.empty"))
      html must not include msgs("landingPage.organisation.saved.body.has")
    }

    "must render the has-drafts saved-notifications message when hasDraftNotifications is true" in new Setup {
      val html: String = view(Some(traderName), vrn, hasDraftNotifications = true).toString
      html must include(msgs("landingPage.organisation.saved.body.has"))
      html must not include msgs("landingPage.organisation.saved.body.empty")
    }
  }

  trait Setup {
    val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
    implicit val request: Request[?] = FakeRequest()
    implicit val msgs: Messages      = messages(app)

    val view: LandingPageOrganisationView = app.injector.instanceOf[LandingPageOrganisationView]
  }
}
