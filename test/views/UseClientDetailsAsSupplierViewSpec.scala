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
import forms.UseClientDetailsAsSupplierFormProvider
import models.{NormalMode, SupplierNumber, TraderInformation}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.checkAnswers.SupplierClientDetailsSummary
import views.html.UseClientDetailsAsSupplierView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UseClientDetailsAsSupplierViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: UseClientDetailsAsSupplierView = app.injector.instanceOf[UseClientDetailsAsSupplierView]
  val vatNotice728Url: String              = app.injector.instanceOf[FrontendAppConfig].vatNotice728Url

  private val clientAddress = TraderInformation(
    traderName = Some("ABC Ltd"),
    tradingName = None,
    addressLine1 = Some("1 Arundel Mews"),
    addressLine2 = Some("Sunnymede"),
    addressLine3 = Some("Worthing"),
    addressLine4 = Some("West Sussex"),
    postcode = Some("BN11 5RG")
  )

  val clientDetails: SummaryList         = SupplierClientDetailsSummary(Some("ABC Ltd"), Some(clientAddress))
  val detailsWithoutAddress: SummaryList = SupplierClientDetailsSummary(Some("ABC Ltd"), None)

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider = new UseClientDetailsAsSupplierFormProvider()
  val form         = formProvider()

  private def render(details: SummaryList = clientDetails): String =
    view(form, SupplierNumber(1), NormalMode, details, vatNotice728Url)(request, msgs).toString

  "UseClientDetailsAsSupplierView" - {

    "must render the page title" in {
      render() must include(msgs("useClientDetailsAsSupplier.title"))
    }

    "must render the caption" in {
      render() must include("govuk-caption-l")
      render() must include(msgs("useClientDetailsAsSupplier.caption"))
    }

    "must render the heading" in {
      render() must include(msgs("useClientDetailsAsSupplier.heading"))
    }

    "must render both paragraphs" in {
      render() must include(msgs("useClientDetailsAsSupplier.paragraph.1"))
      render() must include(msgs("useClientDetailsAsSupplier.paragraph.2"))
    }

    "must render the self-supply conditions" in {
      val html = render()

      Seq("bullet.1", "bullet.2", "bullet.3", "bullet.4").foreach { bullet =>
        html must include(msgs(s"useClientDetailsAsSupplier.$bullet"))
      }
    }

    "must link to VAT Notice 728 in a new tab" in {
      val html = render()

      html must include(msgs("useClientDetailsAsSupplier.bullet.4.linkText"))
      html must include(vatNotice728Url)
    }

    "must render the client's name and address" in {
      val html = render()

      html must include(msgs("useClientDetailsAsSupplier.clientDetails"))
      html must include("ABC Ltd")
      html must include("1 Arundel Mews")
      html must include("BN11 5RG")
    }

    "must show the address as not provided when it is unavailable" in {
      val html = render(detailsWithoutAddress)

      html must include("ABC Ltd")
      html must include(msgs("useClientDetailsAsSupplier.notProvided"))
    }

    "must render the question as the radio legend" in {
      render() must include(msgs("useClientDetailsAsSupplier.question"))
    }

    "must render a hint under each answer" in {
      val html = render()

      html must include(msgs("useClientDetailsAsSupplier.yes.hint"))
      html must include(msgs("useClientDetailsAsSupplier.no.hint"))
    }

    "must render the Continue button" in {
      render() must include(msgs("site.continue"))
    }

    "must post to the submit route" in {
      render() must include(
        controllers.supplierdetails.routes.UseClientDetailsAsSupplierController.onSubmit(SupplierNumber(1), NormalMode).url
      )
    }

    "must render the error summary when nothing is selected" in {
      val boundForm = form.bind(Map.empty[String, String])
      val html      = view(boundForm, SupplierNumber(1), NormalMode, clientDetails, vatNotice728Url)(request, msgs).toString

      html must include("govuk-error-summary")
      html must include(msgs("useClientDetailsAsSupplier.error.required"))
    }
  }
}
