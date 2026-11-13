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
import forms.VatReliefReasonFormProvider
import models.{NormalMode, SupplierNumber, VatReliefReason, VehicleNumber}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText
import viewmodels.govuk.inset.*
import views.html.VatReliefReasonView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VatReliefReasonViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: VatReliefReasonView = app.injector.instanceOf[VatReliefReasonView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val supplierForm: Form[VatReliefReason] = new VatReliefReasonFormProvider()(isSupplier = true)
  val importForm: Form[VatReliefReason]   = new VatReliefReasonFormProvider()(isSupplier = false)

  val postAction: Call =
    controllers.vehicledetails.routes.VatReliefReasonController.supplierOnSubmit(SupplierNumber(1), VehicleNumber(1), NormalMode)

  val vehicleDetails: InsetText = InsetTextViewModel("Vehicle details: <strong>Volvo FH16 station wagon</strong>")

  private def render(
    form: Form[VatReliefReason] = supplierForm,
    options: Seq[VatReliefReason] = VatReliefReason.supplierOptions,
    vehicleDetails: Option[InsetText] = Some(vehicleDetails)
  ): String =
    view(form, options, vehicleDetails, postAction)(request, msgs).toString

  "VatReliefReasonView" - {

    "must render the AVD11.1 page title" in {
      Jsoup.parse(render()).title mustEqual msgs("vatReliefReason.title") + " - " + msgs("service.name") + " - " + msgs("site.govuk")
    }

    "must render the vehicle details inset above the caption" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-inset-text").text mustEqual "Vehicle details: Volvo FH16 station wagon"
      document.select(".govuk-inset-text").hasClass("govuk-!-margin-top-0") mustEqual true
    }

    "must not render an inset when there are no vehicle details" in {
      Jsoup.parse(render(vehicleDetails = None)).select(".govuk-inset-text").isEmpty mustEqual true
    }

    "must render the caption and heading" in {
      val document = Jsoup.parse(render())

      document.select(".govuk-caption-l").text mustEqual msgs("vatReliefReason.caption")
      document.select("h1").text mustEqual msgs("vatReliefReason.heading")
    }

    "must render only the two supplier-journey radio options" in {
      val document = Jsoup.parse(render())

      val labels = document.select(".govuk-radios__item label").eachText
      labels must contain theSameElementsInOrderAs Seq(msgs("vatReliefReason.radio.disabledPerson"), msgs("vatReliefReason.radio.charity"))
    }

    "must render all five import-journey radio options in order" in {
      val document = Jsoup.parse(render(form = importForm, options = VatReliefReason.importOptions))

      val labels = document.select(".govuk-radios__item label").eachText
      labels must contain theSameElementsInOrderAs Seq(
        msgs("vatReliefReason.radio.inheritedGoods"),
        msgs("vatReliefReason.radio.returnedGoods"),
        msgs("vatReliefReason.radio.transferOfResidence"),
        msgs("vatReliefReason.radio.disabledPerson"),
        msgs("vatReliefReason.radio.charity")
      )
    }

    "must post to the given submit route" in {
      val form = Jsoup.parse(render()).select("form")

      form.attr("action") mustEqual postAction.url
    }

    "must render a back link" in {
      render() must include("govuk-back-link")
    }

    "must show the supplier error summary when nothing is selected" in {
      val document = Jsoup.parse(render(form = supplierForm.bind(Map("value" -> ""))))

      document.title                               must startWith(msgs("error.title.prefix"))
      document.select(".govuk-error-summary").text must include(msgs("vatReliefReason.supplier.error.required"))
      document.select(".govuk-error-summary a").attr("href") mustEqual "#value"
    }

    "must show the import error summary when nothing is selected" in {
      val document = Jsoup.parse(render(form = importForm.bind(Map("value" -> "")), options = VatReliefReason.importOptions))

      document.select(".govuk-error-summary").text must include(msgs("vatReliefReason.import.error.required"))
    }

    "must select the saved value" in {
      Jsoup
        .parse(render(form = supplierForm.fill(VatReliefReason.SubjectToReliefForCharity)))
        .select("input[type=radio][checked]")
        .attr("value") mustEqual VatReliefReason.SubjectToReliefForCharity.toString
    }
  }
}
