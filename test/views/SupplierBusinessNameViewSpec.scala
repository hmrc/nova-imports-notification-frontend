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
import forms.SupplierBusinessNameFormProvider
import models.{NormalMode, SupplierNumber}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.SupplierBusinessNameView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SupplierBusinessNameViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: SupplierBusinessNameView = app.injector.instanceOf[SupplierBusinessNameView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider = new SupplierBusinessNameFormProvider()
  val form         = formProvider()

  "SupplierBusinessNameView" - {

    "must render the page title" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("supplierBusinessName.title"))
    }

    "must render the caption" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("supplierBusinessName.caption"))
    }

    "must render the heading as the input label" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("supplierBusinessName.heading"))
      html must include("govuk-label-wrapper")
      html must include("govuk-label--l")
    }

    "must render the input with no hint and no width modifier" in {
      val html = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must not include "govuk-hint"
      html must not include "govuk-input--width"
    }

    "must render the Continue button" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("site.continue"))
    }

    "must post to the supplier number given in the URL" in {
      val html: String = view(form, SupplierNumber(3), NormalMode)(request, msgs).toString

      html must include(controllers.routes.SupplierBusinessNameController.onSubmit(SupplierNumber(3), NormalMode).url)
    }

    "must render the error summary when the form has errors" in {
      val boundForm    = form.bind(Map("value" -> ""))
      val html: String = view(boundForm, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include("govuk-error-summary")
      html must include(msgs("supplierBusinessName.error.required"))
    }

    "must render the same content via the render method" in {
      val html: String = view.render(form, SupplierNumber(1), NormalMode, request, msgs).toString

      html must include(msgs("supplierBusinessName.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("supplierBusinessName.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
