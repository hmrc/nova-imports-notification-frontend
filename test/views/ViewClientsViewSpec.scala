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
import controllers.clientselection.routes
import forms.ClientSearchFormProvider
import models.{ClientSearch, ClientSearchBy, ClientSummary}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import viewmodels.ClientListPage
import views.html.ViewClientsView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ViewClientsViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: ViewClientsView = app.injector.instanceOf[ViewClientsView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form       = new ClientSearchFormProvider()()
  val supportUrl = "http://support"

  val clients = Seq(ClientSummary("Client A", "700000001"), ClientSummary("Client B", "700000002"))
  val results = ClientListPage(clients, 30, 2, 10, None)

  def render(f: play.api.data.Form[ClientSearch] = form, page: Option[ClientListPage] = Some(results)): String =
    view(f, page, supportUrl)(request, msgs).toString

  "ViewClientsView" - {

    "must render the page title and heading" in {
      val html = render()

      html must include(msgs("viewClients.title"))
      html must include(msgs("viewClients.heading"))
      html must include(msgs("viewClients.body"))
    }

    "must render the search form" in {
      val html = render()

      html must include(msgs("viewClients.searchBy.label"))
      html must include(msgs("viewClients.searchBy.hint"))
      html must include(msgs("viewClients.searchBy.placeholder"))
      html must include(msgs("viewClients.searchBy.name"))
      html must include(msgs("viewClients.searchBy.vrn"))
      html must include(msgs("viewClients.search.label"))
      html must include(msgs("viewClients.searchButton"))
      html must include("govuk-button--secondary")
      html must include(msgs("viewClients.viewAll"))
      html must include(routes.ViewClientsController.onPageLoad(None, None, 1).url)
      html must include(routes.ViewClientsController.onSubmit().url)
    }

    "must select the current search type and keep the term" in {
      val html = render(form.fill(ClientSearch(ClientSearchBy.Vrn, "700000001")))

      html must include regex """<option value="vrn"\s+selected"""
      html must include("""value="700000001"""")
    }

    "must render the client table with a select action" in {
      val html = render()

      html must include("Showing <strong>11</strong> to <strong>12</strong> of <strong>30</strong> records")
      html must include(msgs("viewClients.table.name"))
      html must include(msgs("viewClients.table.vrn"))
      html must include(msgs("viewClients.table.actions"))
      html must include("Client A")
      html must include("700000001")
      html must include(routes.SelectClientController.select("700000001").url)
      html must include(s"""${msgs("viewClients.select")}<span class="govuk-visually-hidden"> Client A</span>""")
    }

    "must render pagination" in {
      val html = render()

      html must include("govuk-pagination")
      html must include(msgs("viewClients.pagination.previous"))
      html must include(msgs("viewClients.pagination.next"))
      html must include(routes.ViewClientsController.onPageLoad(None, None, 3).url)
    }

    "must render the no results message instead of the table" in {
      val html = render(page = Some(ClientListPage(Nil, 0, 1, 10, Some(ClientSearch(ClientSearchBy.Name, "Nobody")))))

      html must include(msgs("viewClients.noResults"))
      html must not include "govuk-table"
      html must not include "govuk-pagination"
    }

    "must render neither results nor the no results message when there are no results to show" in {
      val html = render(page = None)

      html must not include msgs("viewClients.noResults")
      html must not include "govuk-table"
    }

    "must not render the remove or download links until their pages exist" in {
      val html = render()

      html must not include msgs("site.remove")
      html must not include "Download client list (CSV)"
    }

    "must render the download details and the return to home link" in {
      val html = render()

      html must include(msgs("viewClients.download.summary"))
      html must include(msgs("viewClients.download.body.1"))
      html must include(msgs("viewClients.download.body.2"))
      html must include(msgs("viewClients.download.body.3"))
      html must include(msgs("viewClients.download.body.3.link"))
      html must include(supportUrl)
      html must include(msgs("viewClients.returnHome"))
      html must include(controllers.routes.LandingPageController.onPageLoad().url)
    }

    "must render a single combined error when nothing was entered" in {
      val html = render(form.bind(Map("searchBy" -> "", "search" -> "")))

      html must include(msgs("error.summary.title"))
      html must include(msgs("viewClients.error.required"))
      html must include(msgs("viewClients.searchBy.error.required"))
      html must include(msgs("viewClients.search.error.required"))
      html must include(msgs("error.title.prefix"))
    }

    "must render the field error when only the search term is missing" in {
      val html = render(form.bind(Map("searchBy" -> "name", "search" -> "")))

      html must include(msgs("viewClients.search.error.required.name"))
      html must not include msgs("viewClients.error.required")
    }

    "must render the same content via the render method" in {
      val html = view.render(form, Some(results), supportUrl, request, msgs).toString

      html must include(msgs("viewClients.heading"))
    }

    "must render the same content via the f method" in {
      val html = view.f(form, Some(results), supportUrl)(request, msgs).toString

      html must include(msgs("viewClients.heading"))
    }
  }
}
