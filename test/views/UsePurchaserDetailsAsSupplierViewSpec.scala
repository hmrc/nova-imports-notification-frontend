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
import controllers.supplierdetails
import forms.UsePurchaserDetailsAsSupplierFormProvider
import models.{Address, Country, NormalMode, SupplierNumber, UserAnswers}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import pages.sections.purchaserdetails.PurchaserBusinessNamePage
import pages.sections.purchaseraddress.PurchaserAddressPage
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.checkAnswers.SupplierPurchaserDetailsSummary
import views.html.UsePurchaserDetailsAsSupplierView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UsePurchaserDetailsAsSupplierViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: UsePurchaserDetailsAsSupplierView = app.injector.instanceOf[UsePurchaserDetailsAsSupplierView]

  private val answersWithDetails: UserAnswers =
    emptyUserAnswers
      .unsafeSet(PurchaserBusinessNamePage, "ABC Ltd")
      .unsafeSet(PurchaserAddressPage, Address(Seq("23, North Road", "East London", "London"), Some("ER45 6UI"), Country("GB", "United Kingdom")))

  val purchaserDetails: SummaryList = SupplierPurchaserDetailsSummary.fromSession(answersWithDetails)

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider = new UsePurchaserDetailsAsSupplierFormProvider()
  val form         = formProvider()

  "UsePurchaserDetailsAsSupplierView" - {

    "must render the correct heading" in {
      val html: String = view(form, SupplierNumber(1), NormalMode, purchaserDetails)(request, msgs).toString

      html must include(msgs("usePurchaserDetailsAsSupplier.heading"))
    }

    "must render the correct page title" in {
      val html: String = view(form, SupplierNumber(1), NormalMode, purchaserDetails)(request, msgs).toString

      html must include(msgs("usePurchaserDetailsAsSupplier.title"))
    }

    "must render the 'Add vehicle details' caption" in {
      val html: String = view(form, SupplierNumber(1), NormalMode, purchaserDetails)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("usePurchaserDetailsAsSupplier.caption"))
    }

    "must render the intro paragraph" in {
      val html: String = view(form, SupplierNumber(1), NormalMode, purchaserDetails)(request, msgs).toString

      html must include(msgs("usePurchaserDetailsAsSupplier.paragraph.1"))
    }

    "must render the purchaser's details (name and address) from the session" in {
      val html: String = view(form, SupplierNumber(1), NormalMode, purchaserDetails)(request, msgs).toString

      html must include(msgs("usePurchaserDetailsAsSupplier.purchaserDetails"))
      html must include(msgs("usePurchaserDetailsAsSupplier.name"))
      html must include(msgs("usePurchaserDetailsAsSupplier.address"))
      html must include("ABC Ltd")
      html must include("23, North Road")
      html must include("ER45 6UI")
    }

    "must render the radio question and both option hints" in {
      val html: String = view(form, SupplierNumber(1), NormalMode, purchaserDetails)(request, msgs).toString

      html must include(msgs("usePurchaserDetailsAsSupplier.question"))
      html must include(msgs("usePurchaserDetailsAsSupplier.yes.hint"))
      html must include(msgs("usePurchaserDetailsAsSupplier.no.hint"))
    }

    "must render the Yes and No radio options" in {
      val html: String = view(form, SupplierNumber(1), NormalMode, purchaserDetails)(request, msgs).toString

      html must include(msgs("site.yes"))
      html must include(msgs("site.no"))
    }

    "must render the Continue button" in {
      val html: String = view(form, SupplierNumber(1), NormalMode, purchaserDetails)(request, msgs).toString

      html must include(msgs("site.continue"))
    }

    "must render the error summary when the form has errors" in {
      val boundForm    = form.bind(Map("value" -> ""))
      val html: String = view(boundForm, SupplierNumber(1), NormalMode, purchaserDetails)(request, msgs).toString

      html must include(msgs("usePurchaserDetailsAsSupplier.error.required"))
    }

    "must post to the UsePurchaserDetailsAsSupplier submit URL" in {
      val html: String = view(form, SupplierNumber(1), NormalMode, purchaserDetails)(request, msgs).toString

      html must include(s"""action="${supplierdetails.routes.UsePurchaserDetailsAsSupplierController.onSubmit(SupplierNumber(1), NormalMode).url}"""")
    }

    "must render the same content via the render method" in {
      val html: String = view.render(form, SupplierNumber(1), NormalMode, purchaserDetails, request, msgs).toString

      html must include(msgs("usePurchaserDetailsAsSupplier.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(form, SupplierNumber(1), NormalMode, purchaserDetails)(request, msgs).toString

      html must include(msgs("usePurchaserDetailsAsSupplier.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
