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
import forms.RemoveClientFormProvider
import models.ClientSummary
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.RemoveClientView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class RemoveClientViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: RemoveClientView = app.injector.instanceOf[RemoveClientView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val form = new RemoveClientFormProvider()()

  private val client = ClientSummary("Client Co", "700000001")

  private def render(boundForm: Form[Boolean] = form, client: ClientSummary = client) =
    Jsoup.parse(view(boundForm, client).toString)

  "RemoveClientView" - {

    "must render the RC1.0 page title" in {
      render().title mustEqual "Remove client - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render Remove client as a h1" in {
      render().select("h1").text mustEqual "Remove client"
    }

    "must render the paragraphs either side of the inset" in {
      val paragraphs = render().select("p.govuk-body")

      paragraphs.get(0).text mustEqual msgs("removeClient.paragraph.1")
      paragraphs.get(1).text mustEqual msgs("removeClient.paragraph.2")
    }

    "must render the client name and VRN in the inset text" in {
      render().select(".govuk-inset-text").text mustEqual "Client name: Client Co VAT registration number: 700000001"
    }

    "must escape the client name" in {
      render(client = ClientSummary("<script>", "700000001")).select(".govuk-inset-text b").first.text mustEqual "<script>"
    }

    "must ask the question as a medium legend with Yes and No" in {
      val document = render()

      document.select("legend.govuk-fieldset__legend--m").text mustEqual "Are you sure you want to remove this client?"
      document.select(".govuk-radios__label").eachText mustEqual java.util.List.of("Yes", "No")
    }

    "must post to the remove client route for the client" in {
      render().select("form").attr("action") mustEqual
        controllers.clientselection.routes.RemoveClientController.onSubmit("700000001").url
    }

    "must show the error summary and prefix the title when nothing is selected" in {
      val document = render(form.bind(Map("value" -> "")))

      document.title                               must startWith(msgs("error.title.prefix"))
      document.select(".govuk-error-summary").text must include("Select yes if you want to remove this client")
      document.select(".govuk-error-message").text must include("Select yes if you want to remove this client")
    }
  }
}
