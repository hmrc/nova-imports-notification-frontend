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
import forms.PurchaserBusinessOrIndividualFormProvider
import models.NormalMode
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.PurchaserBusinessOrIndividualView

class PurchaserBusinessOrIndividualViewSpec extends SpecBase with Matchers {

  val formProvider = new PurchaserBusinessOrIndividualFormProvider()
  val form         = formProvider()

  "PurchaserBusinessOrIndividualView" - {

    "must render the correct heading" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("purchaserBusinessOrIndividual.heading"))
    }

    "must render the correct page title" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("purchaserBusinessOrIndividual.title"))
    }

    "must render the non-VAT registered business radio option" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("purchaserBusinessOrIndividual.radio.nonVatRegisteredBusiness"))
    }

    "must render the non-VAT registered private individual radio option" in new Setup {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("purchaserBusinessOrIndividual.radio.nonVatRegisteredPrivateIndividual"))
    }

    "must render the error summary when the form has errors" in new Setup {
      val boundForm    = form.bind(Map("value" -> ""))
      val html: String = view(boundForm, NormalMode)(request, msgs).toString

      html must include(msgs("purchaserBusinessOrIndividual.error.required"))
    }
  }

  trait Setup {
    val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
    implicit val request: Request[?] = FakeRequest()
    implicit val msgs: Messages      = messages(app)

    val view: PurchaserBusinessOrIndividualView = app.injector.instanceOf[PurchaserBusinessOrIndividualView]
  }
}
