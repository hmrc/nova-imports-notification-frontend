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
import forms.IsYourAddressInTheUkFormProvider
import models.NormalMode
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.IsYourAddressInTheUkView

class IsYourAddressInTheUkViewSpec extends SpecBase with Matchers {

  val formProvider = new IsYourAddressInTheUkFormProvider()
  val form         = formProvider()

  "IsYourAddressInTheUkView" - {

    "must render the correct heading" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("isYourAddressInTheUk.heading"))
    }

    "must render the correct page title" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("isYourAddressInTheUk.title"))
    }

    "must render the 'Add your address' caption" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("isYourAddressInTheUk.caption"))
    }

    "must render the Yes radio option" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("site.yes"))
    }

    "must render the No radio option" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("site.no"))
    }

    "must render the Continue button" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("site.continue"))
    }

    "must render the error summary when the form has errors" in new Setup {
      val boundForm    = form.bind(Map("value" -> ""))
      val html: String = view(boundForm, NormalMode)(request, msgs).toString

      html must include(msgs("isYourAddressInTheUk.error.required"))
    }

    "must post to the IsYourAddressInTheUk submit URL" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(s"""action="${controllers.routes.IsYourAddressInTheUkController.onSubmit(NormalMode).url}"""")
    }

    "must render the same content via the render method" in new Setup {
      val html: String = view.render(form, NormalMode, request, msgs).toString

      html must include(msgs("isYourAddressInTheUk.heading"))
    }

    "must render the same content via the f method" in new Setup {
      val html: String = view.f(form, NormalMode)(request, msgs).toString

      html must include(msgs("isYourAddressInTheUk.heading"))
    }

    "must return itself via the ref method" in new Setup {
      view.ref mustBe view
    }
  }

  trait Setup {
    val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
    implicit val request: Request[?] = FakeRequest()
    implicit val msgs: Messages      = messages(app)

    val view: IsYourAddressInTheUkView = app.injector.instanceOf[IsYourAddressInTheUkView]
  }
}
