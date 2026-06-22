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
import views.html.AboutYourDetailsView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AboutYourDetailsViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: AboutYourDetailsView = app.injector.instanceOf[AboutYourDetailsView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  "AboutYourDetailsView" - {

    "must render the correct heading" in {
      val html: String = view().toString
      html must include(msgs("aboutYourDetails.heading"))
    }

    "must render the correct page title" in {
      val html: String = view().toString
      html must include(msgs("aboutYourDetails.title"))
    }

    "must render the correct page caption" in {
      val html: String = view().toString
      html must include("govuk-caption-l")
      html must include(msgs("aboutYourDetails.caption"))
    }

    "must render the first paragraph" in {
      val html: String = view().toString
      html must include(msgs("aboutYourDetails.paragraph.1"))
    }

    "must render the second paragraph" in {
      val html: String = view().toString
      html must include(msgs("aboutYourDetails.paragraph.2"))
    }

    "must render the Continue button" in {
      val html: String = view().toString
      html must include(msgs("site.continue"))
    }

    "must render the same content via the render method" in {
      val html: String = view.render(request, msgs).toString
      html must include(msgs("aboutYourDetails.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(request, msgs).toString
      html must include(msgs("aboutYourDetails.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }

}
