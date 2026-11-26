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
import controllers.vehicledetails.routes
import forms.AdditionalVehicleFormProvider
import models.{SupplierNumber, VehicleNumber, VehicleVinSummary}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import viewmodels.Pager
import views.html.VehiclesBoughtFromSupplierView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VehiclesBoughtFromSupplierViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: VehiclesBoughtFromSupplierView = app.injector.instanceOf[VehiclesBoughtFromSupplierView]

  val personalTransportUnitUrl: String = "https://example.com/personal-transport-unit"

  val formProvider = new AdditionalVehicleFormProvider()
  val form         = formProvider()

  val pageUrl = (pageNumber: Int) => controllers.vehicledetails.routes.VehiclesBoughtFromSupplierController.onPageLoad(SupplierNumber(1)).url

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private val testVinSummaries: Seq[VehicleVinSummary] =
    Seq.tabulate(15)(i => VehicleVinSummary(VehicleNumber(i + 1), s"Vin ${i + 1}", "model", "make", "bodyType"))
  private def render(supplierName: Option[String] = None, vinSummarySeq: Seq[VehicleVinSummary] = Seq.empty): String =
    view(form, Pager.page(vinSummarySeq, 1), pageUrl, supplierName, SupplierNumber(1), personalTransportUnitUrl).toString

  "VehiclesBoughtFromSupplierView" - {

    "must render the caption" in {
      render() must include(msgs("vehiclesBoughtFromSupplier.caption"))
    }

    "must render the supplier name in the heading" in {
      render(Some("ABC Ltd")) must include("""<h1 class="govuk-heading-l">Vehicles bought from ABC Ltd</h1>""")
    }

    "must render the fallback heading when there is no supplier name" in {
      render() must include(msgs("vehiclesBoughtFromSupplier.heading.noSupplierName"))
    }

    "must escape a supplier name containing markup" in {
      val html = render(Some("<script>alert(1)</script>"))

      html must not include "<script>alert(1)</script>"
      html must include("&lt;script&gt;")
    }

    "must render both date bullets in a bulleted list" in {
      val html = render()

      html must include("""<ul class="govuk-list govuk-list--bullet">""")
      html must include(msgs("vehiclesBoughtFromSupplier.bullet.1"))
      html must include(msgs("vehiclesBoughtFromSupplier.bullet.2"))
    }

    "must render the text with a link to the Personal Transport Unit" in {
      val html = render()

      html must include(personalTransportUnitUrl)
      html must include("""target="_blank"""")
    }

    "must render the where to find these dates section" in {
      val html = render()

      html must include("govuk-details")
      html must include("govuk-details__summary")
      html must include("govuk-details__summary-text")
      html must include(msgs("vehiclesBoughtFromSupplier.whereToFindDates.heading"))
      html must include(msgs("vehiclesBoughtFromSupplier.whereToFindDates.paragraph.1"))
    }

    "must render introductory paragraph" in {
      val html = render()

      html must include(msgs("vehiclesBoughtFromSupplier.paragraph.1"))
    }

    "must render the Add vehicle button as a submit button" in {
      val html = render().replaceAll("\\s+", " ")

      html must include(
        s"""<button type="submit" class="govuk-button" data-module="govuk-button"> ${msgs("vehiclesBoughtFromSupplier.addVehicle")} </button>"""
      )
    }

    "must post the form back to this page" in {
      val html = render()

      html must include(s"""action="${routes.VehiclesBoughtFromSupplierController.onSubmit(SupplierNumber(1)).url}"""")
    }

    "must set the page title" in {
      render() must include("<title>Vehicles bought from this supplier - Notification of Vehicle Arrivals - GOV.UK")
    }
  }

  "VehiclesBoughtFromSupplierView - with vehicle list" - {
    "must render the caption" in {
      render(None, testVinSummaries) must include(msgs("vehiclesBoughtFromSupplier.caption"))
    }

    "must render the supplier name in the heading" in {
      render(Some("ABC Ltd"), testVinSummaries) must include("""<h1 class="govuk-heading-l">Vehicles bought from ABC Ltd</h1>""")
    }

    "must render the fallback heading when there is no supplier name" in {
      render(None, testVinSummaries) must include(msgs("vehiclesBoughtFromSupplier.heading.noSupplierName"))
    }

    "must escape a supplier name containing markup" in {
      val html = render(Some("<script>alert(1)</script>"), testVinSummaries)

      html must not include "<script>alert(1)</script>"
      html must include("&lt;script&gt;")
    }

    "must render both date bullets in a bulleted list" in {
      val html = render(None, testVinSummaries)

      html must include("""<ul class="govuk-list govuk-list--bullet">""")
      html must include(msgs("vehiclesBoughtFromSupplier.bullet.1"))
      html must include(msgs("vehiclesBoughtFromSupplier.bullet.2"))
    }

    "must render the text with a link to the Personal Transport Unit" in {
      val html = render(None, testVinSummaries)

      html must include(personalTransportUnitUrl)
      html must include("""target="_blank"""")
    }

    "must render the where to find these dates section" in {
      val html = render(None, testVinSummaries)

      html must include("govuk-details")
      html must include("govuk-details__summary")
      html must include("govuk-details__summary-text")
      html must include(msgs("vehiclesBoughtFromSupplier.whereToFindDates.heading"))
      html must include(msgs("vehiclesBoughtFromSupplier.whereToFindDates.paragraph.1"))
    }

    "must render introductory paragraph" in {
      val html = render(None, testVinSummaries)

      html must include(msgs("vehiclesBoughtFromSupplier.paragraph.1"))
    }

    "must render the Continue button as a submit button" in {
      val html = render(None, testVinSummaries).replaceAll("\\s+", " ")

      html must include(s"""<button type="submit" class="govuk-button" data-module="govuk-button"> ${msgs("site.continue")} </button>""")
    }

    "must post the form back to this page" in {
      val html = render(None, testVinSummaries)

      html must include(s"""action="${routes.VehiclesBoughtFromSupplierController.onSubmit(SupplierNumber(1)).url}"""")
    }

    "must set the page title" in {
      render(None, testVinSummaries) must include("<title>Vehicles bought from this supplier - Notification of Vehicle Arrivals - GOV.UK")
    }

    "must render the table title" in {
      val html = render(None, testVinSummaries).replaceAll("\\s+", " ")

      html must include(s"""<h1 class="govuk-heading-m">${msgs("vehiclesBoughtFromSupplier.heading.2")}</h1>""")
    }

    "must render the table and content" in {
      val html = render(None, testVinSummaries).replaceAll("\\s+", " ")

      html must include(s"""<table class="govuk-table govuk-table--small-text-until-tablet">""")
      html must include(s"""<caption class="govuk-table__caption govuk-table__caption--m">Showing 1 to 10 of 15 records</caption>""")
      html must include(s"""${msgs("vehiclesBoughtFromSupplier.table.vin")}""")
      html must include(s"""${msgs("vehiclesBoughtFromSupplier.table.description")}""")
      html must include(s"""${msgs("vehiclesBoughtFromSupplier.table.actions")}""")

      // Check the first 10 rows are present with links and hidden text for Edit and Delete buttons.
      testVinSummaries.take(10).foreach { vinSummary =>
        html must include(s"""<td class="govuk-table__cell" >${vinSummary.vin}</td>""")
        html must include(s"""<td class="govuk-table__cell" >${vinSummary.make} ${vinSummary.model} ${vinSummary.bodyType}</td>""")

        val expectedEditLink       = controllers.routes.LandingPageController.onPageLoad().url
        val expectedEditHiddenText = msgs("vehiclesBoughtFromSupplier.action.edit.hidden").replace("{0}", vinSummary.vin)
        html must include(s"""<a class="govuk-link" href="$expectedEditLink">${msgs(
            "vehiclesBoughtFromSupplier.action.edit"
          )} <span class="govuk-visually-hidden"> $expectedEditHiddenText</span></a>""")

        val expectedDeleteLink =
          controllers.vehicledetails.routes.DeleteVehicleController.supplierOnPageLoad(SupplierNumber(1), vinSummary.vehicleNumber).url
        val expectedDeleteHiddenText = msgs("vehiclesBoughtFromSupplier.action.delete.hidden").replace("{0}", vinSummary.vin)
        html must include(s"""<a class="govuk-link" href="$expectedDeleteLink">${msgs(
            "vehiclesBoughtFromSupplier.action.delete"
          )} <span class="govuk-visually-hidden"> $expectedDeleteHiddenText</span></a>""")
      }
    }

    "must render the page numbers and page change links" in {
      val html = render(None, testVinSummaries).replaceAll("\\s+", " ")

      val expectedPage1Link = controllers.vehicledetails.routes.VehiclesBoughtFromSupplierController.onPageLoad(SupplierNumber(1)).url
      html must include(
        s"""<a class="govuk-link govuk-pagination__link" href="$expectedPage1Link" aria-label="Page 1" aria-current="page" > 1 </a>"""
      )
      val expectedPage2Link = controllers.vehicledetails.routes.VehiclesBoughtFromSupplierController.onPageLoad(SupplierNumber(1)).url
      html must include(s"""<a class="govuk-link govuk-pagination__link" href="$expectedPage2Link" aria-label="Page 2" > 2 </a>""")
      val expectedNextLink = controllers.vehicledetails.routes.VehiclesBoughtFromSupplierController.onPageLoad(SupplierNumber(1)).url
      html must include(s"""<a class="govuk-link govuk-pagination__link" href="$expectedNextLink" rel="next">""".stripMargin)
    }

    "must render the question and radio buttons" in {
      val html = render(None, testVinSummaries).replaceAll("\\s+", " ")

      html must include(msgs("vehiclesBoughtFromSupplier.heading.3"))
      html must include(msgs("site.yes"))
      html must include(msgs("site.no"))
    }

  }

}
