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
import views.html.LoadingClientListView

class LoadingClientListViewSpec extends SpecBase with Matchers {

  "LoadingClientListView" - {

    "must render the correct heading" in new Setup {
      val html: String = view.apply().toString

      html must include(msgs("loadingClientList.heading"))
    }

    "must render the correct page title" in new Setup {
      val html: String = view.apply().toString

      html must include(msgs("loadingClientList.title"))
    }

    "must render the paragraph" in new Setup {
      val html: String = view.apply().toString

      html must include(msgs("loadingClientList.paragraph"))
    }

    "must render the warning message" in new Setup {
      val html: String = view.apply().toString

      html must include(msgs("loadingClientList.warning"))
    }

    "must render the same content via the render method" in new Setup {
      val html: String = view.render(request, msgs).toString

      html must include(msgs("loadingClientList.heading"))
    }

    "must render the same content via the f method" in new Setup {
      val html: String = view.f()(request, msgs).toString

      html must include(msgs("loadingClientList.heading"))
    }

    "must return itself via the ref method" in new Setup {
      view.ref mustBe view
    }
  }

  trait Setup {
    val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
    implicit val request: Request[?] = FakeRequest()
    implicit val msgs: Messages      = messages(app)

    val view: LoadingClientListView = app.injector.instanceOf[LoadingClientListView]
  }
}
