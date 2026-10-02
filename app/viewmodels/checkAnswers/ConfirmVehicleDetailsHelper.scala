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

package viewmodels.checkAnswers

import controllers.vehicledetails.routes
import models.{AddVehicleType, CheckMode, Country, Currency, ImportNumber, SupplierNumber, UserAnswers, VehicleDates, VehicleNumber}
import navigation.ConfirmVehicleDetailsJourney
import pages.sections.vehicledetails.*
import play.api.i18n.Messages
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow, Value}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ConfirmVehicleDetailsHelper {

  private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  def supplierSummaryList(
    answers: UserAnswers,
    supplierNumber: SupplierNumber,
    vehicleNumber: VehicleNumber,
    countries: Seq[Country],
    currencies: Seq[Currency]
  )(implicit messages: Messages): SummaryList = {
    val dates             = answers.get(VehicleDatesPage(supplierNumber, vehicleNumber)).getOrElse(Set.empty)
    val firstRegistration = dates.contains(VehicleDates.FirstRegistration)
    val madeAvailable     = dates.contains(VehicleDates.MadeAvailable)
    val invoiceDate       = dates.contains(VehicleDates.PurchaseInvoiceDate)

    SummaryListViewModel(
      rows = Seq(
        vehicleDatesRow(dates, routes.VehicleDatesController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)),
        Option
          .when(firstRegistration)(
            dateOfFirstRegistrationRow(
              answers,
              vehicleNumber,
              routes.DateOfFirstRegistrationController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
            )
          )
          .flatten,
        Option
          .when(firstRegistration)(
            countryOfFirstRegistrationRow(
              answers,
              vehicleNumber,
              countries,
              routes.CountryOfFirstRegistrationController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
            )
          )
          .flatten,
        Option
          .when(madeAvailable)(
            answers
              .get(DateOfAvailabilityPage(supplierNumber, vehicleNumber))
              .map(date =>
                row(
                  "dateOfAvailability",
                  textValue(formatDate(date)),
                  routes.DateOfAvailabilityController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)
                )
              )
          )
          .flatten,
        Option
          .when(invoiceDate)(
            answers
              .get(PurchaseInvoiceDatePage(supplierNumber, vehicleNumber))
              .map(date =>
                row(
                  "purchaseInvoiceDate",
                  textValue(formatDate(date)),
                  routes.PurchaseInvoiceDateController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)
                )
              )
          )
          .flatten,
        Option
          .when(invoiceDate)(
            answers
              .get(PurchaseInvoiceNumberPage(supplierNumber, vehicleNumber))
              .map(number =>
                row(
                  "purchaseInvoiceNumber",
                  textValue(number),
                  routes.PurchaseInvoiceNumberController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)
                )
              )
          )
          .flatten,
        Option
          .when(!invoiceDate)(
            answers
              .get(NoPurchaseInvoiceReasonPage(supplierNumber, vehicleNumber))
              .map(reason =>
                row(
                  "noPurchaseInvoiceReason",
                  textValue(reason),
                  routes.NoPurchaseInvoiceReasonController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)
                )
              )
          )
          .flatten,
        answers
          .get(TotalAmountPaidPage(vehicleNumber))
          .map(amount =>
            row("totalAmountPaid", textValue(amount), routes.TotalAmountPaidController.onPageLoadSupplier(supplierNumber, vehicleNumber, CheckMode))
          ),
        answers
          .get(PaymentCurrencyPage(vehicleNumber))
          .map { code =>
            val name = currencies.find(_.code == code).map(_.displayName).getOrElse(code)
            row("currency", textValue(name), routes.PaymentCurrencyController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode))
          },
        vehicleTypeRow(answers, vehicleNumber, routes.AddVehicleTypeController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode))
      ).flatten
    )
  }

  def importSummaryList(answers: UserAnswers, importNumber: ImportNumber, vehicleNumber: VehicleNumber, countries: Seq[Country])(implicit
    messages: Messages
  ): SummaryList = {
    val dateOfFirstRegistrationKnown = answers.get(DateOfFirstRegistrationKnownPage(vehicleNumber))

    SummaryListViewModel(
      rows = Seq(
        dateOfFirstRegistrationKnown.map(known =>
          row(
            "dateOfFirstRegistrationKnown",
            textValue(messages(if (known) "site.yes" else "site.no")),
            ConfirmVehicleDetailsJourney.dateOfFirstRegistrationKnownChange
          )
        ),
        Option
          .when(dateOfFirstRegistrationKnown.contains(true))(
            dateOfFirstRegistrationRow(
              answers,
              vehicleNumber,
              routes.DateOfFirstRegistrationController.importOnPageLoad(importNumber, vehicleNumber, CheckMode)
            )
          )
          .flatten,
        Option
          .when(dateOfFirstRegistrationKnown.contains(true))(
            countryOfFirstRegistrationRow(
              answers,
              vehicleNumber,
              countries,
              routes.CountryOfFirstRegistrationController.importOnPageLoad(importNumber, vehicleNumber, CheckMode)
            )
          )
          .flatten,
        vehicleTypeRow(answers, vehicleNumber, routes.AddVehicleTypeController.importOnPageLoad(importNumber, vehicleNumber, CheckMode))
      ).flatten
    )
  }

  private def vehicleDatesRow(dates: Set[VehicleDates], change: Call)(implicit messages: Messages): Option[SummaryListRow] =
    Option.when(dates.nonEmpty) {
      val lines = Seq(
        VehicleDates.FirstRegistration   -> "confirmVehicleDetails.vehicleDates.firstRegistered",
        VehicleDates.MadeAvailable       -> "confirmVehicleDetails.vehicleDates.madeAvailable",
        VehicleDates.PurchaseInvoiceDate -> "confirmVehicleDetails.vehicleDates.purchaseInvoiceDate"
      ).collect { case (date, key) if dates.contains(date) => key }

      row("vehicleDates", ValueViewModel(HtmlContent(lines.map(key => HtmlFormat.escape(messages(key)).body).mkString("<br>"))), change)
    }

  private def dateOfFirstRegistrationRow(answers: UserAnswers, vehicleNumber: VehicleNumber, change: Call)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    answers.get(DateOfFirstRegistrationPage(vehicleNumber)).map(date => row("dateOfFirstRegistration", textValue(formatDate(date)), change))

  private def countryOfFirstRegistrationRow(answers: UserAnswers, vehicleNumber: VehicleNumber, countries: Seq[Country], change: Call)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    answers.get(CountryOfFirstRegistrationPage(vehicleNumber)).map { code =>
      val name = countries.find(_.code == code).flatMap(_.name).getOrElse(code)
      row("countryOfFirstRegistration", textValue(name), change)
    }

  private def vehicleTypeRow(answers: UserAnswers, vehicleNumber: VehicleNumber, change: Call)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    answers.get(AddVehicleTypePage(vehicleNumber)).map { vehicleType =>
      val key = vehicleType match {
        case AddVehicleType.AgriculturalTractor => "addVehicleType.radio.tractor"
        case AddVehicleType.Car                 => "addVehicleType.radio.car"
        case AddVehicleType.ContractorsPlant    => "addVehicleType.radio.plant"
        case AddVehicleType.Hcv                 => "addVehicleType.radio.hcv"
        case AddVehicleType.Lcv                 => "addVehicleType.radio.lcv"
        case AddVehicleType.Motorcycle          => "addVehicleType.radio.motorcycle"
        case AddVehicleType.MotorCaravan        => "addVehicleType.radio.caravan"
      }
      row("vehicleType", textValue(messages(key)), change)
    }

  private def row(field: String, value: Value, change: Call)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = s"confirmVehicleDetails.$field.label",
      value = value,
      actions = Seq(
        ActionItemViewModel("site.change", change.url)
          .withVisuallyHiddenText(messages(s"confirmVehicleDetails.$field.change.hidden"))
      )
    )

  private def textValue(value: String): Value = ValueViewModel(Text(value))

  private def formatDate(date: LocalDate): String = date.format(dateFormat)
}
