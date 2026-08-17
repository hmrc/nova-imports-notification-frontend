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
import config.FrontendAppConfig
import forms.SupplierVatRegistrationDetailsFormProvider
import models.{NormalMode, SupplierNumber}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.SupplierVatRegistrationDetailsView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SupplierVatRegistrationDetailsViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: SupplierVatRegistrationDetailsView = app.injector.instanceOf[SupplierVatRegistrationDetailsView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val appConfig = app.injector.instanceOf[FrontendAppConfig]
  val formProvider = new SupplierVatRegistrationDetailsFormProvider()
  val form         = formProvider(appConfig.vrnValidationList)

  "SupplierVatRegistrationDetailsView" - {

    "must render the correct heading" in {
      val html: String = view(appConfig.vrnValidationList, false, form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("supplierVatRegistrationDetails.heading"))
    }

    "must render the correct page title" in {
      val html: String = view(appConfig.vrnValidationList, false, form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("supplierVatRegistrationDetails.title"))
    }

    "must render the correct page caption" in {
      val html: String = view(appConfig.vrnValidationList, false, form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("supplierVatRegistrationDetails.caption"))
    }

    "must render the first paragraph" in {
      val html: String = view(appConfig.vrnValidationList, false, form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include("govuk-body")
      html must include(msgs("supplierVatRegistrationDetails.paragraph.1"))
    }

    "must render the second paragraph" in {
      val html: String = view(appConfig.vrnValidationList, false, form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include("govuk-body")
      html must include(msgs("supplierVatRegistrationDetails.paragraph.2"))
    }

    "must render the country field label" in {
      val html: String = view(appConfig.vrnValidationList, false, form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("supplierVatRegistrationDetails.label.country"))
    }

    "must render the country vat number label" in {
      val html: String = view(appConfig.vrnValidationList, false, form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("supplierVatRegistrationDetails.label.vatNumber"))
    }

    "must render the Continue button" in {
      val html: String = view(appConfig.vrnValidationList, false, form, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("site.continue"))
    }

    "must render the error summary when the form has errors" in {
      val boundForm    = form.bind(Map("value" -> ""))
      val html: String = view(appConfig.vrnValidationList, false, boundForm, SupplierNumber(1), NormalMode)(request, msgs).toString

      html must include(msgs("supplierVatRegistrationDetails.country.error.required"))
      html must include(msgs("supplierVatRegistrationDetails.vatNumber.error.required"))
    }

  }

}
