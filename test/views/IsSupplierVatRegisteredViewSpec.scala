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
import forms.IsSupplierVatRegisteredFormProvider
import models.{NormalMode, SupplierNumber}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.IsSupplierVatRegisteredView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class IsSupplierVatRegisteredViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: IsSupplierVatRegisteredView = app.injector.instanceOf[IsSupplierVatRegisteredView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider = new IsSupplierVatRegisteredFormProvider()
  val form         = formProvider()

  "IsSupplierVatRegisteredView" - {

    "must render the correct heading" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("isSupplierVatRegistered.heading"))
    }

    "must render the correct page title" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("isSupplierVatRegistered.title"))
    }

    "must render the correct page caption" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("isSupplierVatRegistered.caption"))
    }

    "must render the Yes radio option" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("site.yes"))
    }

    "must render the No radio option" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("site.no"))
    }

    "must render the Continue button" in {
      val html: String = view(form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("site.continue"))
    }

    "must render the error summary when the form has errors" in {
      val boundForm    = form.bind(Map("value" -> ""))
      val html: String = view(boundForm, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("isSupplierVatRegistered.error.required"))
    }

  }

}
