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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.LandingPageAgentWithClientView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*

class LandingPageAgentWithClientViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  private val agentName  = "ABC Consultancy"
  private val clientName = "BMD Logistics"
  private val clientVrn  = "123456789"

  private val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit private val request: Request[?] = FakeRequest()
  implicit private val msgs: Messages      = messages(app)

  private val view: LandingPageAgentWithClientView = app.injector.instanceOf[LandingPageAgentWithClientView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private def render(
    agentName: Option[String] = Some(agentName),
    clientName: Option[String] = Some(clientName),
    hasDraftNotifications: Boolean = false
  ): Document =
    Jsoup.parse(view(agentName, clientName, clientVrn, hasDraftNotifications).toString)

  private def linkHref(document: Document, text: String): String =
    document.select(s"a:containsOwn($text)").attr("href")

  private val beforeYouContinueUrl      = controllers.introduction.routes.BeforeYouContinueController.onPageLoad().url
  private val beforeYouContinueAmendUrl = controllers.introduction.routes.BeforeYouContinueController.onPageLoadAmend().url
  private val loadingClientListUrl      = controllers.clientselection.routes.LoadingClientListController.onPageLoad().url
  private val savedNotificationsUrl     = controllers.routes.JourneyRecoveryController.onPageLoad().url

  "LandingPageAgentWithClientView" - {

    "must render the correct page title" in {
      render().title mustEqual msgs("landingPage.agent.title") + " - " + msgs("site.govuk")
    }

    "must render the H1 heading" in {
      render().select("h1").text mustEqual msgs("landingPage.agent.heading")
    }

    "must render the agent name as the caption when supplied" in {
      render().select(".govuk-caption-l").text mustEqual agentName
    }

    "must omit the caption when the agent name is not supplied" in {
      render(agentName = None).select(".govuk-caption-l").isEmpty mustBe true
    }

    "must render the intro body" in {
      render().select("p.govuk-body").eachText.asScala must contain(msgs("landingPage.agent.body"))
    }

    "must render the Notifying on behalf of summary card title" in {
      render().select(".govuk-summary-card__title").text mustEqual msgs("landingPage.agent.client.heading")
    }

    "must render the client name and VAT registration number rows" in {
      val document = render()

      document.select(".govuk-summary-list__key").eachText.asScala.toSeq mustEqual Seq(
        msgs("landingPage.agent.client.name"),
        msgs("landingPage.agent.client.vrn")
      )
      document.select(".govuk-summary-list__value").eachText.asScala.toSeq mustEqual Seq(clientName, s"GB$clientVrn")
    }

    "must omit the client name row when the client name is not known" in {
      val document = render(clientName = None)

      document.select(".govuk-summary-list__key").eachText.asScala.toSeq mustEqual Seq(msgs("landingPage.agent.client.vrn"))
      document.select(".govuk-summary-list__value").eachText.asScala.toSeq mustEqual Seq(s"GB$clientVrn")
    }

    "must render the Change client link to CS1.0 with the client name as visually hidden text" in {
      val link = render().select(".govuk-summary-card__action a")

      link.attr("href") mustEqual loadingClientListUrl
      link.text mustEqual s"${msgs("landingPage.agent.client.change")} $clientName"
      link.select(".govuk-visually-hidden").text mustEqual clientName
    }

    "must render the Change client link without visually hidden text when the client name is not known" in {
      val link = render(clientName = None).select(".govuk-summary-card__action a")

      link.text mustEqual msgs("landingPage.agent.client.change")
      link.select(".govuk-visually-hidden").isEmpty mustBe true
    }

    "must render the Create a new notification link to BY2.0" in {
      val document = render()

      linkHref(document, msgs("landingPage.agent.create.link")) mustEqual beforeYouContinueUrl
      document.text must include(msgs("landingPage.agent.create.body"))
    }

    "must render the Update a submitted notification link to BY2.0 in amend mode" in {
      val document = render()

      linkHref(document, msgs("landingPage.agent.update.link")) mustEqual beforeYouContinueAmendUrl
      document.text must include(msgs("landingPage.agent.update.body"))
    }

    "must render Manage a saved notification as an enabled link with the has-drafts copy when the client has drafts" in {
      val document = render(hasDraftNotifications = true)

      linkHref(document, msgs("landingPage.agent.saved.heading")) mustEqual savedNotificationsUrl
      document.text must include(msgs("landingPage.agent.saved.body.has"))
      document.text must not include msgs("landingPage.agent.saved.body.empty")
      document.select("h2.app-text-secondary").isEmpty mustBe true
    }

    "must render Manage a saved notification as disabled grey text with the empty copy when the client has no drafts" in {
      val document = render(hasDraftNotifications = false)

      document.select(s"a:containsOwn(${msgs("landingPage.agent.saved.heading")})").isEmpty mustBe true
      document.select("h2.app-text-secondary").text mustEqual msgs("landingPage.agent.saved.heading")
      document.text must include(msgs("landingPage.agent.saved.body.empty"))
      document.text must not include msgs("landingPage.agent.saved.body.has")
      document.select("p.govuk-visually-hidden").text mustEqual msgs("landingPage.content.disabled.body")
    }

    "must render a back link" in {
      render().select(".govuk-back-link").isEmpty mustBe false
    }

    "must render the Manage your clients link to CS1.0" in {
      val document = render()

      linkHref(document, msgs("landingPage.agent.clients.link")) mustEqual loadingClientListUrl
      document.text must include(msgs("landingPage.agent.clients.body"))
    }

    "must render the sections in the agreed order" in {
      render().select("#main-content h2").eachText.asScala.toSeq mustEqual Seq(
        msgs("landingPage.agent.client.heading"),
        msgs("landingPage.agent.create.link"),
        msgs("landingPage.agent.update.link"),
        msgs("landingPage.agent.saved.heading"),
        msgs("landingPage.agent.clients.link")
      )
    }

    "must render the Welsh hidden disabled text when the language is Welsh" in {
      val cyMessages = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("cy")))
      val html       = view(Some(agentName), Some(clientName), clientVrn, hasDraftNotifications = false)(request, cyMessages).toString

      html must include("Hyd nes bod gennych hysbysiad wedi’i gadw, nid yw’r rhan hon o’r gwasanaeth ar gael")
    }

    "must render the same content via the render method" in {
      view.render(Some(agentName), Some(clientName), clientVrn, false, request, msgs).toString must include(msgs("landingPage.agent.heading"))
    }

    "must render the same content via the f method" in {
      view.f(Some(agentName), Some(clientName), clientVrn, false)(request, msgs).toString must include(msgs("landingPage.agent.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
