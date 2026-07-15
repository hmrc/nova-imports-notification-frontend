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
import forms.SupplierBusinessOrIndividualFormProvider
import models.NormalMode
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.SupplierBusinessOrIndividualView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SupplierBusinessOrIndividualViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: SupplierBusinessOrIndividualView = app.injector.instanceOf[SupplierBusinessOrIndividualView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider = new SupplierBusinessOrIndividualFormProvider()
  val form         = formProvider()

  "SupplierBusinessOrIndividualView" - {

    "must render the correct heading" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("supplierBusinessOrIndividual.heading"))
    }

    "must render the correct page title" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("supplierBusinessOrIndividual.title"))
    }

    "must render the correct page caption" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("supplierBusinessOrIndividual.caption"))
    }

    "must render the hint text" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("supplierBusinessOrIndividual.hint"))
    }

    "must render the business radio option" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("supplierBusinessOrIndividual.radio.business"))
    }

    "must render the private individual radio option" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("supplierBusinessOrIndividual.radio.privateIndividual"))
    }

    "must render the error summary when the form has errors" in {
      val boundForm    = form.bind(Map("value" -> ""))
      val html: String = view(boundForm, NormalMode)(request, msgs).toString

      html must include(msgs("supplierBusinessOrIndividual.error.required"))
    }
  }

}
