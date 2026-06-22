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
import forms.PhoneNumberFormProvider
import models.NormalMode
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.PhoneNumberView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PhoneNumberViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: PhoneNumberView = app.injector.instanceOf[PhoneNumberView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider = new PhoneNumberFormProvider()
  val form         = formProvider()

  "PhoneNumberView" - {

    "must render the correct heading" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("phoneNumber.heading"))
    }

    "must render the correct page title" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("phoneNumber.title"))
    }

    "must render the 'Add your details' caption" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("phoneNumber.caption"))
      html must include("govuk-caption-l")
    }

    "must render the hint text" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("phoneNumber.hint"))
    }

    "must render an input field with type tel autocomplete and inputmode" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include("""autocomplete="tel"""")
      html must include("""inputmode="tel"""")
    }

    "must render the Continue button" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("site.continue"))
    }

    "must render the error summary with the required error when value is blank" in {
      val boundForm    = form.bind(Map("value" -> ""))
      val html: String = view(boundForm, NormalMode)(request, msgs).toString

      html must include(msgs("phoneNumber.error.required"))
    }

    "must render the error summary with the length error when value exceeds 20 characters" in {
      val boundForm    = form.bind(Map("value" -> "012345678901234567890"))
      val html: String = view(boundForm, NormalMode)(request, msgs).toString

      html must include(msgs("phoneNumber.error.length"))
    }

    "must render the error summary with the invalid error when value contains disallowed characters" in {
      val boundForm    = form.bind(Map("value" -> "020-7946-0958"))
      val html: String = view(boundForm, NormalMode)(request, msgs).toString

      html must include(msgs("phoneNumber.error.invalid"))
    }

    "must post to the PhoneNumber submit URL" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(s"""action="${controllers.routes.PhoneNumberController.onSubmit(NormalMode).url}"""")
    }

    "must render the same content via the render method" in {
      val html: String = view.render(form, NormalMode, request, msgs).toString

      html must include(msgs("phoneNumber.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(form, NormalMode)(request, msgs).toString

      html must include(msgs("phoneNumber.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }

}
