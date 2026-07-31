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
import forms.UsePersonalDetailsAsSupplierFormProvider
import models.{Address, Country, NormalMode, UserAnswers}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import pages.sections.notifieraddress.AddressPage
import pages.sections.notifierDetails.BusinessNamePage
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.checkAnswers.SupplierPersonalDetailsSummary
import views.html.UsePersonalDetailsAsSupplierView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UsePersonalDetailsAsSupplierViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: UsePersonalDetailsAsSupplierView = app.injector.instanceOf[UsePersonalDetailsAsSupplierView]
  val vatNotice728Url: String                = app.injector.instanceOf[FrontendAppConfig].vatNotice728Url

  private val answersWithDetails: UserAnswers =
    emptyUserAnswers
      .unsafeSet(BusinessNamePage, "ABC Ltd")
      .unsafeSet(AddressPage, Address(Seq("23, North Road", "East London", "London"), Some("ER45 6UI"), Country("GB", "United Kingdom")))

  val personalDetails: SummaryList = SupplierPersonalDetailsSummary.summaryList(answersWithDetails)

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider = new UsePersonalDetailsAsSupplierFormProvider()
  val form         = formProvider()

  "UsePersonalDetailsAsSupplierView" - {

    "must render the correct heading" in {
      val html: String = view(form, NormalMode, personalDetails, vatNotice728Url)(request, msgs).toString

      html must include(msgs("usePersonalDetailsAsSupplier.heading"))
    }

    "must render the correct page title" in {
      val html: String = view(form, NormalMode, personalDetails, vatNotice728Url)(request, msgs).toString

      html must include(msgs("usePersonalDetailsAsSupplier.title"))
    }

    "must render the 'Add vehicle details' caption" in {
      val html: String = view(form, NormalMode, personalDetails, vatNotice728Url)(request, msgs).toString

      html must include("govuk-caption-l")
      html must include(msgs("usePersonalDetailsAsSupplier.caption"))
    }

    "must render the self-supply guidance paragraphs and bullet list" in {
      val html: String = view(form, NormalMode, personalDetails, vatNotice728Url)(request, msgs).toString

      html must include(msgs("usePersonalDetailsAsSupplier.paragraph.1"))
      html must include(msgs("usePersonalDetailsAsSupplier.paragraph.2"))
      html must include(msgs("usePersonalDetailsAsSupplier.bullet.1"))
      html must include(msgs("usePersonalDetailsAsSupplier.bullet.2"))
      html must include(msgs("usePersonalDetailsAsSupplier.bullet.3"))
    }

    "must render the VAT Notice 728 link to GOV.UK" in {
      val html: String = view(form, NormalMode, personalDetails, vatNotice728Url)(request, msgs).toString

      html must include(msgs("usePersonalDetailsAsSupplier.bullet.4.linkText"))
      html must include(vatNotice728Url)
    }

    "must render the notifier's personal details (name and address) from the session" in {
      val html: String = view(form, NormalMode, personalDetails, vatNotice728Url)(request, msgs).toString

      html must include(msgs("usePersonalDetailsAsSupplier.personalDetails"))
      html must include(msgs("usePersonalDetailsAsSupplier.name"))
      html must include(msgs("usePersonalDetailsAsSupplier.addressLine.1"))
      html must include(msgs("usePersonalDetailsAsSupplier.postcode"))
      html must include("ABC Ltd")
      html must include("23, North Road")
      html must include("ER45 6UI")
    }

    "must render the radio question and both option hints" in {
      val html: String = view(form, NormalMode, personalDetails, vatNotice728Url)(request, msgs).toString

      html must include(msgs("usePersonalDetailsAsSupplier.question"))
      html must include(msgs("usePersonalDetailsAsSupplier.yes.hint"))
      html must include(msgs("usePersonalDetailsAsSupplier.no.hint"))
    }

    "must render the Yes and No radio options" in {
      val html: String = view(form, NormalMode, personalDetails, vatNotice728Url)(request, msgs).toString

      html must include(msgs("site.yes"))
      html must include(msgs("site.no"))
    }

    "must render the Continue button" in {
      val html: String = view(form, NormalMode, personalDetails, vatNotice728Url)(request, msgs).toString

      html must include(msgs("site.continue"))
    }

    "must render the error summary when the form has errors" in {
      val boundForm    = form.bind(Map("value" -> ""))
      val html: String = view(boundForm, NormalMode, personalDetails, vatNotice728Url)(request, msgs).toString

      html must include(msgs("usePersonalDetailsAsSupplier.error.required"))
    }

    "must post to the UsePersonalDetailsAsSupplier submit URL" in {
      val html: String = view(form, NormalMode, personalDetails, vatNotice728Url)(request, msgs).toString

      html must include(s"""action="${controllers.routes.UsePersonalDetailsAsSupplierController.onSubmit(NormalMode).url}"""")
    }

    "must render the same content via the render method" in {
      val html: String = view.render(form, NormalMode, personalDetails, vatNotice728Url, request, msgs).toString

      html must include(msgs("usePersonalDetailsAsSupplier.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(form, NormalMode, personalDetails, vatNotice728Url)(request, msgs).toString

      html must include(msgs("usePersonalDetailsAsSupplier.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
