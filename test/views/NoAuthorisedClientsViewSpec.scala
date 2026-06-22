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
import views.html.NoAuthorisedClientsView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class NoAuthorisedClientsViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: NoAuthorisedClientsView = app.injector.instanceOf[NoAuthorisedClientsView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private val hmrcOnlineUrl   = "https://example.com/hmrc-online"
  private val oaaUrl          = "https://example.com/oaa"
  private val placeholderHome = controllers.routes.LandingPageController.onPageLoad().url

  "NoAuthorisedClientsView" - {

    "must render the correct page title" in {
      val html: String = view.apply(hmrcOnlineUrl, oaaUrl).toString

      html must include(msgs("noAuthorisedClients.title"))
    }

    "must render the H1 heading" in {
      val html: String = view.apply(hmrcOnlineUrl, oaaUrl).toString

      html must include(msgs("noAuthorisedClients.heading"))
    }

    "must render both introductory body paragraphs" in {
      val html: String = view.apply(hmrcOnlineUrl, oaaUrl).toString

      html must include(msgs("noAuthorisedClients.body.1"))
      html must include(msgs("noAuthorisedClients.body.2"))
    }

    "must render the 'What you need to do' H2" in {
      val html: String = view.apply(hmrcOnlineUrl, oaaUrl).toString

      html must include(msgs("noAuthorisedClients.whatToDo.heading"))
    }

    "must render the 'You can either:' intro" in {
      val html: String = view.apply(hmrcOnlineUrl, oaaUrl).toString

      html must include(msgs("noAuthorisedClients.whatToDo.body"))
    }

    "must render the HMRC online account list item with its link text and URL" in {
      val html: String = view.apply(hmrcOnlineUrl, oaaUrl).toString

      html must include(msgs("noAuthorisedClients.list.hmrcOnlineAccount.link"))
      html must include(hmrcOnlineUrl)
    }

    "must render the Online Agent Authorisation list item with its link text and URL" in {
      val html: String = view.apply(hmrcOnlineUrl, oaaUrl).toString

      html must include(msgs("noAuthorisedClients.list.onlineAgentAuthorisation.link"))
      html must include(oaaUrl)
    }

    "must render both external links with target=_blank and rel=noopener noreferrer" in {
      val html: String = view.apply(hmrcOnlineUrl, oaaUrl).toString

      html must include("""target="_blank"""")
      html must include("""rel="noopener noreferrer"""")
    }

    "must render the 5 working days timing paragraph" in {
      val html: String = view.apply(hmrcOnlineUrl, oaaUrl).toString

      html must include(msgs("noAuthorisedClients.timing"))
    }

    "must render the Return to home button linking to the Index page" in {
      val html: String = view.apply(hmrcOnlineUrl, oaaUrl).toString

      html must include(msgs("noAuthorisedClients.returnToHome"))
      html must include(placeholderHome)
    }

    "must render the Return to home button as a secondary button to match the prototype" in {
      val html: String = view.apply(hmrcOnlineUrl, oaaUrl).toString

      html must include("govuk-button--secondary")
    }

    "must render the same content via the render method" in {
      val html: String = view.render(hmrcOnlineUrl, oaaUrl, request, msgs).toString

      html must include(msgs("noAuthorisedClients.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(hmrcOnlineUrl, oaaUrl)(request, msgs).toString

      html must include(msgs("noAuthorisedClients.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }

}
