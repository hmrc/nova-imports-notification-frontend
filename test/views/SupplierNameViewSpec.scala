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
import forms.SupplierNameFormProvider
import models.{NormalMode, SupplierNumber}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.SupplierNameView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SupplierNameViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: SupplierNameView = app.injector.instanceOf[SupplierNameView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider = new SupplierNameFormProvider()
  val form         = formProvider()

  "SupplierNameView" - {

    "must render the page title" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("supplierName.title"))
    }

    "must render the caption" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("supplierName.caption"))
    }

    "must render the heading" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("supplierName.heading"))
    }

    "must render the titleField" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("supplierName.titleField"))
    }

    "must render the firstName" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("supplierName.firstName"))
    }

    "must render the lastName" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("supplierName.lastName"))
    }

    "must render the Continue button" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("site.continue"))
    }

    "must switch off autocomplete on each input itself" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString.replaceAll("\\s+", " ")

      html must include("""name="title" type="text" autocomplete="off"""")
      html must include("""name="firstName" type="text" autocomplete="off"""")
      html must include("""name="lastName" type="text" autocomplete="off"""")
    }

    "must post to the supplier number given in the URL" in {
      val html: String = view(form, SupplierNumber(3), NormalMode)(request, msgs).toString

      html must include(controllers.supplierdetails.routes.SupplierNameController.onSubmit(SupplierNumber(3), NormalMode).url)
    }

    "must render the error summary when the form has errors" in {
      val boundForm    = form.bind(Map("title" -> "", "firstName" -> "", "lastName" -> ""))
      val html: String = view(boundForm, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include("govuk-error-summary")
      html must include(msgs("supplierName.titleField.error.required"))
      html must include(msgs("supplierName.firstName.error.required"))
      html must include(msgs("supplierName.lastName.error.required"))
    }

    "must render the same content via the render method" in {
      val html: String = view.render(form, SupplierNumber(1), NormalMode, request, msgs).toString

      html must include(msgs("supplierName.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("supplierName.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }

}
