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
import forms.PurchaserOrOnBehalfFormProvider
import models.NormalMode
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.PurchaserOrOnBehalfView

class PurchaserOrOnBehalfViewSpec extends SpecBase with Matchers {

  val formProvider = new PurchaserOrOnBehalfFormProvider()
  val form         = formProvider()

  "PurchaserOrOnBehalfView" - {

    "must render the correct heading" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("purchaserOrOnBehalf.heading"))
    }

    "must render the correct page title" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("purchaserOrOnBehalf.title"))
    }

    "must render the hint text" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("purchaserOrOnBehalf.hint"))
    }

    "must render the purchaser radio option" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("purchaserOrOnBehalf.radio.purchaser"))
    }

    "must render the on behalf of purchaser radio option" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("purchaserOrOnBehalf.radio.behalfOfPurchaser"))
    }

    "must render the error summary when the form has errors" in new Setup {
      val boundForm    = form.bind(Map("value" -> ""))
      val html: String = view(boundForm, NormalMode)(request, msgs).toString

      html must include(msgs("purchaserOrOnBehalf.error.required"))
    }

    "must render the same content via the render method" in new Setup {
      val html: String = view.render(form, NormalMode, request, msgs).toString

      html must include(msgs("purchaserOrOnBehalf.heading"))
    }

    "must render the same content via the f method" in new Setup {
      val html: String = view.f(form, NormalMode)(request, msgs).toString

      html must include(msgs("purchaserOrOnBehalf.heading"))
    }

    "must return itself via the ref method" in new Setup {
      view.ref mustBe view
    }
  }

  trait Setup {
    val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
    implicit val request: Request[?] = FakeRequest()
    implicit val msgs: Messages      = messages(app)

    val view: PurchaserOrOnBehalfView = app.injector.instanceOf[PurchaserOrOnBehalfView]
  }
}
