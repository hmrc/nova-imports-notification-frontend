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
import forms.BusinessNameFormProvider
import models.NormalMode
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.BusinessNameView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class BusinessNameViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: BusinessNameView = app.injector.instanceOf[BusinessNameView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider = new BusinessNameFormProvider()
  val form         = formProvider()

  "BusinessNameView" - {

    "must render the page title" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("businessName.title"))
    }

    "must render the caption" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("businessName.caption"))
    }

    "must render the heading" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("businessName.heading"))
    }

    "must render the Continue button" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("site.continue"))
    }

    "must render the error summary when the form has errors" in {
      val boundForm    = form.bind(Map("value" -> ""))
      val html: String = view(boundForm, NormalMode)(request, msgs).toString

      html must include("govuk-error-summary")
      html must include(msgs("businessName.error.required"))
    }

    "must render the same content via the render method" in {
      val html: String = view.render(form, NormalMode, request, msgs).toString

      html must include(msgs("businessName.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(form, NormalMode)(request, msgs).toString

      html must include(msgs("businessName.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
