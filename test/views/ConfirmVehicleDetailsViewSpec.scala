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
import models.{AddVehicleType, CheckMode, Country, Currency, ImportNumber, SupplierNumber, UserAnswers, VehicleDates, VehicleNumber}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import pages.sections.vehicledetails.*
import play.api.Application
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import queries.AllVehiclesQuery
import viewmodels.checkAnswers.ConfirmVehicleDetailsHelper
import views.html.ConfirmVehicleDetailsView

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*

class ConfirmVehicleDetailsViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: ConfirmVehicleDetailsView = app.injector.instanceOf[ConfirmVehicleDetailsView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  private val s          = SupplierNumber(1)
  private val i          = ImportNumber(1)
  private val v          = VehicleNumber(1)
  private val countries  = Seq(Country("FR", "France"))
  private val currencies = Seq(Currency("BGN", "Bulgarian lev"))
  private val submit     = routes.ConfirmVehicleDetailsController.supplierOnSubmit(s, v)

  private val bothDates: UserAnswers = emptyUserAnswers
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
    .unsafeSet(VehicleDatesPage(s, v), Set(VehicleDates.PurchaseInvoiceDate, VehicleDates.FirstRegistration, VehicleDates.MadeAvailable))
    .unsafeSet(DateOfFirstRegistrationPage(v), LocalDate.of(2026, 3, 1))
    .unsafeSet(CountryOfFirstRegistrationPage(v), "FR")
    .unsafeSet(DateOfAvailabilityPage(s, v), LocalDate.of(2026, 3, 27))
    .unsafeSet(PurchaseInvoiceDatePage(s, v), LocalDate.of(2026, 3, 20))
    .unsafeSet(PurchaseInvoiceNumberPage(s, v), "INV-001")
    .unsafeSet(NoPurchaseInvoiceReasonPage(s, v), "Stale reason")
    .unsafeSet(TotalAmountPaidPage(v), "45000")
    .unsafeSet(PaymentCurrencyPage(v), "BGN")
    .unsafeSet(AddVehicleTypePage(v), AddVehicleType.Car)

  private def supplierDoc(answers: UserAnswers): Document =
    Jsoup.parse(view(ConfirmVehicleDetailsHelper.supplierSummaryList(answers, s, v, countries, currencies), submit).toString)

  private val importAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DateOfFirstRegistrationKnownPage(v), true)
    .unsafeSet(DateOfFirstRegistrationPage(v), LocalDate.of(2026, 3, 1))
    .unsafeSet(CountryOfFirstRegistrationPage(v), "FR")
    .unsafeSet(AddVehicleTypePage(v), AddVehicleType.Hcv)

  private def importDocFor(answers: UserAnswers): Document =
    Jsoup.parse(
      view(
        ConfirmVehicleDetailsHelper.importSummaryList(answers, i, v, countries),
        routes.ConfirmVehicleDetailsController.importOnSubmit(i, v)
      ).toString
    )

  private def keys(doc: Document): Seq[String] = doc.select(".govuk-summary-list__key").eachText.asScala.toSeq

  private def valueFor(doc: Document, key: String): String =
    doc
      .select(".govuk-summary-list__row")
      .asScala
      .find(_.select(".govuk-summary-list__key").text == key)
      .value
      .select(".govuk-summary-list__value")
      .text

  private def changeLinkFor(doc: Document, key: String) =
    doc
      .select(".govuk-summary-list__row")
      .asScala
      .find(_.select(".govuk-summary-list__key").text == key)
      .value
      .select(".govuk-summary-list__actions a")

  private def label(field: String): String = msgs(s"confirmVehicleDetails.$field.label")

  "ConfirmVehicleDetailsView" - {

    val doc = supplierDoc(bothDates)

    "must render the page title, caption and heading" in {
      doc.title must startWith(msgs("confirmVehicleDetails.title"))
      doc.select("span.govuk-caption-l").text mustEqual msgs("confirmVehicleDetails.caption")
      doc.select("h1").text mustEqual msgs("confirmVehicleDetails.heading")
    }

    "must render the Confirm details button posting to the given call" in {
      doc.select("form").attr("action") mustEqual submit.url
      doc.select("form button.govuk-button").text mustEqual msgs("site.confirmDetails")
    }

    "must render the rows in the agreed order when both date types are selected, without the reason for no purchase invoice" in {
      keys(doc) mustEqual Seq(
        label("vehicleDates"),
        label("dateOfFirstRegistration"),
        label("countryOfFirstRegistration"),
        label("dateOfAvailability"),
        label("purchaseInvoiceDate"),
        label("purchaseInvoiceNumber"),
        label("totalAmountPaid"),
        label("currency"),
        label("vehicleType")
      )
    }

    "must render the selected dates on separate lines" in {
      doc.select(".govuk-summary-list__value").first.html.split("<br>").map(_.trim).toSeq mustEqual Seq(
        msgs("confirmVehicleDetails.vehicleDates.firstRegistered"),
        msgs("confirmVehicleDetails.vehicleDates.madeAvailable"),
        msgs("confirmVehicleDetails.vehicleDates.purchaseInvoiceDate")
      )
    }

    "must render dates as dd/mm/yyyy and the country by name" in {
      valueFor(doc, label("dateOfFirstRegistration")) mustEqual "01/03/2026"
      valueFor(doc, label("dateOfAvailability")) mustEqual "27/03/2026"
      valueFor(doc, label("purchaseInvoiceDate")) mustEqual "20/03/2026"
      valueFor(doc, label("countryOfFirstRegistration")) mustEqual "France"
    }

    "must render the currency by name and code, and the vehicle type by its AVD8.0 label" in {
      valueFor(doc, label("currency")) mustEqual "Bulgarian lev (BGN)"
      valueFor(doc, label("vehicleType")) mustEqual msgs("addVehicleType.radio.car")
    }

    "must render each vehicle type by its AVD8.0 label" in {
      Seq(
        AddVehicleType.Car                 -> "addVehicleType.radio.car",
        AddVehicleType.Lcv                 -> "addVehicleType.radio.lcv",
        AddVehicleType.Hcv                 -> "addVehicleType.radio.hcv",
        AddVehicleType.Motorcycle          -> "addVehicleType.radio.motorcycle",
        AddVehicleType.MotorCaravan        -> "addVehicleType.radio.caravan",
        AddVehicleType.AgriculturalTractor -> "addVehicleType.radio.tractor",
        AddVehicleType.ContractorsPlant    -> "addVehicleType.radio.plant"
      ).foreach { case (vehicleType, key) =>
        valueFor(supplierDoc(bothDates.unsafeSet(AddVehicleTypePage(v), vehicleType)), label("vehicleType")) mustEqual msgs(key)
      }
    }

    "must render each Change link in check mode with its visually hidden text" in {
      val expected = Seq(
        "vehicleDates"               -> routes.VehicleDatesController.onPageLoad(s, v, CheckMode),
        "dateOfFirstRegistration"    -> routes.DateOfFirstRegistrationController.supplierOnPageLoad(s, v, CheckMode),
        "countryOfFirstRegistration" -> routes.CountryOfFirstRegistrationController.supplierOnPageLoad(s, v, CheckMode),
        "dateOfAvailability"         -> routes.DateOfAvailabilityController.onPageLoad(s, v, CheckMode),
        "purchaseInvoiceDate"        -> routes.PurchaseInvoiceDateController.onPageLoad(s, v, CheckMode),
        "purchaseInvoiceNumber"      -> routes.PurchaseInvoiceNumberController.onPageLoad(s, v, CheckMode),
        "totalAmountPaid"            -> routes.TotalAmountPaidController.onPageLoadSupplier(s, v, CheckMode),
        "currency"                   -> routes.PaymentCurrencyController.supplierOnPageLoad(s, v, CheckMode),
        "vehicleType"                -> routes.AddVehicleTypeController.supplierOnPageLoad(s, v, CheckMode)
      )

      expected.foreach { case (field, call) =>
        val link = changeLinkFor(doc, label(field))
        link.attr("href") mustEqual call.url
        link.select(".govuk-visually-hidden").text mustEqual msgs(s"confirmVehicleDetails.$field.change.hidden")
        link.text mustEqual s"${msgs("site.change")} ${msgs(s"confirmVehicleDetails.$field.change.hidden")}"
      }
    }

    "must show only the purchase invoice rows when only the purchase invoice date is selected" in {
      val invoiceDoc = supplierDoc(bothDates.unsafeSet(VehicleDatesPage(s, v), Set(VehicleDates.PurchaseInvoiceDate)))

      keys(invoiceDoc) mustEqual Seq(
        label("vehicleDates"),
        label("purchaseInvoiceDate"),
        label("purchaseInvoiceNumber"),
        label("totalAmountPaid"),
        label("currency"),
        label("vehicleType")
      )
      valueFor(invoiceDoc, label("vehicleDates")) mustEqual msgs("confirmVehicleDetails.vehicleDates.purchaseInvoiceDate")
    }

    "must show the reason for no purchase invoice when only the availability and first registration dates are selected" in {
      val availabilityDoc = supplierDoc(bothDates.unsafeSet(VehicleDatesPage(s, v), Set(VehicleDates.FirstRegistration, VehicleDates.MadeAvailable)))

      keys(availabilityDoc) mustEqual Seq(
        label("vehicleDates"),
        label("dateOfFirstRegistration"),
        label("countryOfFirstRegistration"),
        label("dateOfAvailability"),
        label("noPurchaseInvoiceReason"),
        label("totalAmountPaid"),
        label("currency"),
        label("vehicleType")
      )
      changeLinkFor(availabilityDoc, label("noPurchaseInvoiceReason")).attr("href") mustEqual
        routes.NoPurchaseInvoiceReasonController.onPageLoad(s, v, CheckMode).url
    }

    "must show the first registration and purchase invoice rows, without the date of availability or reason, when those two dates are selected" in {
      val doc = supplierDoc(bothDates.unsafeSet(VehicleDatesPage(s, v), Set(VehicleDates.FirstRegistration, VehicleDates.PurchaseInvoiceDate)))

      keys(doc) mustEqual Seq(
        label("vehicleDates"),
        label("dateOfFirstRegistration"),
        label("countryOfFirstRegistration"),
        label("purchaseInvoiceDate"),
        label("purchaseInvoiceNumber"),
        label("totalAmountPaid"),
        label("currency"),
        label("vehicleType")
      )
      doc.select(".govuk-summary-list__value").first.html.split("<br>").map(_.trim).toSeq mustEqual Seq(
        msgs("confirmVehicleDetails.vehicleDates.firstRegistered"),
        msgs("confirmVehicleDetails.vehicleDates.purchaseInvoiceDate")
      )
    }

    "must show the date of availability and purchase invoice rows, without first registration or the reason, when those two dates are selected" in {
      val doc = supplierDoc(bothDates.unsafeSet(VehicleDatesPage(s, v), Set(VehicleDates.MadeAvailable, VehicleDates.PurchaseInvoiceDate)))

      keys(doc) mustEqual Seq(
        label("vehicleDates"),
        label("dateOfAvailability"),
        label("purchaseInvoiceDate"),
        label("purchaseInvoiceNumber"),
        label("totalAmountPaid"),
        label("currency"),
        label("vehicleType")
      )
    }

    "must escape user-entered values" in {
      val escapedDoc = supplierDoc(bothDates.unsafeSet(PurchaseInvoiceNumberPage(s, v), "<b>INV</b>"))

      valueFor(escapedDoc, label("purchaseInvoiceNumber")) mustEqual "<b>INV</b>"
      escapedDoc.select(".govuk-summary-list__value b").size mustEqual 0
    }

    "must show whether the date of first registration is known, the date and country of first registration and the vehicle type for an import vehicle, with import Change links" in {
      val importDoc = importDocFor(importAnswers)

      keys(importDoc) mustEqual Seq(
        label("dateOfFirstRegistrationKnown"),
        label("dateOfFirstRegistration"),
        label("countryOfFirstRegistration"),
        label("vehicleType")
      )
      valueFor(importDoc, label("dateOfFirstRegistrationKnown")) mustEqual msgs("site.yes")
      changeLinkFor(importDoc, label("dateOfFirstRegistrationKnown")).attr("href") mustEqual
        controllers.routes.LandingPageController.onPageLoad().url
      changeLinkFor(importDoc, label("dateOfFirstRegistrationKnown")).select(".govuk-visually-hidden").text mustEqual
        msgs("confirmVehicleDetails.dateOfFirstRegistrationKnown.change.hidden")
      changeLinkFor(importDoc, label("dateOfFirstRegistration")).attr("href") mustEqual
        routes.DateOfFirstRegistrationController.importOnPageLoad(i, v, CheckMode).url
      changeLinkFor(importDoc, label("countryOfFirstRegistration")).attr("href") mustEqual
        routes.CountryOfFirstRegistrationController.importOnPageLoad(i, v, CheckMode).url
      changeLinkFor(importDoc, label("vehicleType")).attr("href") mustEqual
        routes.AddVehicleTypeController.importOnPageLoad(i, v, CheckMode).url
    }

    "must hide the date and country of first registration for an import vehicle when the date of first registration is not known" in {
      val importDoc = importDocFor(importAnswers.unsafeSet(DateOfFirstRegistrationKnownPage(v), false))

      keys(importDoc) mustEqual Seq(label("dateOfFirstRegistrationKnown"), label("vehicleType"))
      valueFor(importDoc, label("dateOfFirstRegistrationKnown")) mustEqual msgs("site.no")
    }

    "must render the same content via the render and f methods" in {
      val list = ConfirmVehicleDetailsHelper.supplierSummaryList(bothDates, s, v, countries, currencies)

      view.render(list, submit, request, msgs).toString must include(msgs("confirmVehicleDetails.heading"))
      view.f(list, submit)(request, msgs).toString      must include(msgs("confirmVehicleDetails.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
