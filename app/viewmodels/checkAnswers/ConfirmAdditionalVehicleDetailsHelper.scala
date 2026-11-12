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
import models.{CheckMode, Currency, ImportNumber, LeftOrRightHandDrive, MileageUnit, OneMultiVehicle, SupplierNumber, UserAnswers, VehicleNumber}
import pages.ValidatedVinListPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.supplierdetails.IsSupplierVatRegisteredPage
import pages.sections.vehicledetails.*
import play.api.i18n.Messages
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow, Value}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import java.time.format.DateTimeFormatter

object ConfirmAdditionalVehicleDetailsHelper {

  private val prefix     = "confirmAdditionalVehicleDetails"
  private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  final case class ChangeLinks(
    notificationReference: Call,
    numberOfVehicles: Call,
    vehicleIdentificationNumber: Call,
    vehicleRegistrationNumberKnown: Option[Call],
    currentRegistrationNumber: Option[Call],
    dateOfArrival: Call,
    commodityCode: Option[Call],
    mileage: Call,
    leftOrRightHandDrive: Call,
    totalValueOfOptions: Option[Call],
    totalAmountPaid: Call,
    currency: Call,
    couldNotClaimVat: Option[Call],
    vatMarginScheme: Option[Call]
  )

  def supplierSummaryList(answers: UserAnswers, supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, currencies: Seq[Currency])(implicit
    messages: Messages
  ): SummaryList =
    summaryList(
      answers,
      vehicleNumber,
      currencies,
      answers.get(IsSupplierVatRegisteredPage(supplierNumber)).contains(true),
      ChangeLinks(
        notificationReference = routes.NotificationReferenceController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode),
        numberOfVehicles = routes.NumberOfVehiclesController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode),
        vehicleIdentificationNumber = routes.VehicleIdentificationNumberController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode),
        vehicleRegistrationNumberKnown = Some(routes.VehicleRegistrationNumberKnownController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)),
        currentRegistrationNumber = Some(routes.VehicleRegistrationNumberController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)),
        dateOfArrival = routes.DateOfArrivalController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode),
        commodityCode = None,
        mileage = routes.MileageController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode),
        leftOrRightHandDrive = routes.LeftOrRightHandDriveController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode),
        totalValueOfOptions = Some(routes.TotalValueOfOptionsController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)),
        totalAmountPaid = routes.TotalAmountPaidController.onPageLoadSupplier(supplierNumber, vehicleNumber, CheckMode),
        currency = routes.PaymentCurrencyController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode),
        couldNotClaimVat = Some(routes.CouldNotClaimVatController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)),
        vatMarginScheme = Some(routes.VatMarginSchemeController.onPageLoad(supplierNumber, vehicleNumber, CheckMode))
      )
    )

  def importSummaryList(answers: UserAnswers, importNumber: ImportNumber, vehicleNumber: VehicleNumber, currencies: Seq[Currency])(implicit
    messages: Messages
  ): SummaryList =
    summaryList(
      answers,
      vehicleNumber,
      currencies,
      supplierVatRegistered = false,
      ChangeLinks(
        notificationReference = routes.NotificationReferenceController.importOnPageLoad(importNumber, vehicleNumber, CheckMode),
        numberOfVehicles = routes.NumberOfVehiclesController.importOnPageLoad(importNumber, vehicleNumber, CheckMode),
        vehicleIdentificationNumber = routes.VehicleIdentificationNumberController.importOnPageLoad(importNumber, vehicleNumber, CheckMode),
        vehicleRegistrationNumberKnown = None,
        currentRegistrationNumber = None,
        dateOfArrival = routes.DateOfArrivalController.importOnPageLoad(importNumber, vehicleNumber, CheckMode),
        commodityCode = Some(routes.CommodityCodeController.onPageLoad(importNumber, vehicleNumber, CheckMode)),
        mileage = routes.MileageController.importOnPageLoad(importNumber, vehicleNumber, CheckMode),
        leftOrRightHandDrive = routes.LeftOrRightHandDriveController.importOnPageLoad(importNumber, vehicleNumber, CheckMode),
        totalValueOfOptions = None,
        totalAmountPaid = routes.TotalAmountPaidController.onPageLoadImport(importNumber, vehicleNumber, CheckMode),
        currency = routes.PaymentCurrencyController.importOnPageLoad(importNumber, vehicleNumber, CheckMode),
        couldNotClaimVat = None,
        vatMarginScheme = None
      )
    )

  private def summaryList(
    answers: UserAnswers,
    vehicleNumber: VehicleNumber,
    currencies: Seq[Currency],
    supplierVatRegistered: Boolean,
    links: ChangeLinks
  )(implicit messages: Messages): SummaryList = {
    val numberOfVehicles  = answers.get(NumberOfVehiclesPage(vehicleNumber)).map(_.oneMultiVehicle)
    val oneVehicle        = numberOfVehicles.contains(OneMultiVehicle.One)
    val vehicleFromEu     = answers.get(VehicleFromEuPage)
    val registrationKnown = answers.get(VehicleRegistrationNumberKnownPage(vehicleNumber))

    def change(call: Call): Option[Call] = Option.when(oneVehicle)(call)

    SummaryListViewModel(
      rows = Seq(
        Option
          .when(answers.get(AmendSubmittedNotificationPage).contains(true))(
            answers
              .get(NotificationReferencePage(vehicleNumber))
              .map(reference => row("notificationReference", textValue(reference), change(links.notificationReference)))
          )
          .flatten,
        numberOfVehicles.map(value => row("numberOfVehicles", textValue(numberOfVehiclesText(value)), change(links.numberOfVehicles))),
        Option
          .when(oneVehicle)(
            answers
              .get(VehicleIdentificationNumberPage(vehicleNumber))
              .map(vin => row("vehicleIdentificationNumber", textValue(vin), change(links.vehicleIdentificationNumber)))
          )
          .flatten,
        Option
          .when(numberOfVehicles.contains(OneMultiVehicle.More))(
            answers.get(ValidatedVinListPage).filter(_.nonEmpty).map(vehicleIdentificationNumbersRow)
          )
          .flatten,
        Option
          .when(oneVehicle)(
            answers
              .get(VehicleRegistrationNumberKnownPage(vehicleNumber))
              .map(known => row("vehicleRegistrationNumberKnown", textValue(yesNo(known)), links.vehicleRegistrationNumberKnown.flatMap(change)))
          )
          .flatten,
        Option
          .when(oneVehicle && registrationKnown.contains(true))(
            answers
              .get(VehicleRegistrationNumberPage(vehicleNumber))
              .map(registration => row("currentRegistrationNumber", textValue(registration), links.currentRegistrationNumber.flatMap(change)))
          )
          .flatten,
        answers.get(DateOfArrivalPage(vehicleNumber)).map(_.format(dateFormat)) match {
          case Some(date)                            => Some(row("dateArrivedInUk", textValue(date), change(links.dateOfArrival)))
          case None if vehicleFromEu.contains(false) =>
            Some(row("dateArrivedInUk", textValue(messages(s"$prefix.notProvided")), change(links.dateOfArrival)))
          case None => None
        },
        Option
          .when(vehicleFromEu.contains(false))(
            answers
              .get(CommodityCodePage(vehicleNumber))
              .map(code => row("commodityCode", textValue(code), links.commodityCode.flatMap(change)))
          )
          .flatten,
        answers
          .get(MileagePage(vehicleNumber))
          .map(mileage => row("mileage", textValue(s"${mileage.mileage} ${mileageUnitText(mileage.mileageUnits)}"), change(links.mileage))),
        answers
          .get(LeftOrRightHandDrivePage(vehicleNumber))
          .map(drive => row("leftOrRightHand", textValue(leftOrRightHandDriveText(drive)), change(links.leftOrRightHandDrive))),
        Option
          .when(vehicleFromEu.contains(true))(
            answers
              .get(TotalValueOfOptionsPage(vehicleNumber))
              .map(value => row("totalValueOfOptions", textValue(value), links.totalValueOfOptions.flatMap(change)))
          )
          .flatten,
        Option
          .when(vehicleFromEu.contains(false))(
            answers.get(TotalAmountPaidPage(vehicleNumber)).map(amount => row("totalAmountPaid", textValue(amount), change(links.totalAmountPaid)))
          )
          .flatten,
        Option
          .when(vehicleFromEu.contains(false))(
            answers.get(PaymentCurrencyPage(vehicleNumber)).map { code =>
              val name = currencies.find(_.code == code).map(_.displayName).getOrElse(code)
              row("currency", textValue(name), change(links.currency))
            }
          )
          .flatten,
        Option
          .when(supplierVatRegistered)(
            answers
              .get(CouldNotClaimVatPage(vehicleNumber))
              .map(value => row("businessUnableToReclaimVat", textValue(yesNo(value)), links.couldNotClaimVat.flatMap(change)))
          )
          .flatten,
        Option
          .when(supplierVatRegistered)(
            answers
              .get(VatMarginSchemePage(vehicleNumber))
              .map(value => row("vehicleSoldUnderMarginScheme", textValue(yesNo(value)), links.vatMarginScheme.flatMap(change)))
          )
          .flatten
      ).flatten
    )
  }

  private def vehicleIdentificationNumbersRow(vins: Seq[String])(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = s"$prefix.vehicleIdentificationNumbers.label",
      value = ValueViewModel(HtmlContent(vins.map(HtmlFormat.escape(_).toString).mkString("<br>"))),
      actions = Seq.empty
    )

  private def numberOfVehiclesText(value: OneMultiVehicle)(implicit messages: Messages): String = value match {
    case OneMultiVehicle.One  => messages(s"$prefix.numberOfVehicles.one")
    case OneMultiVehicle.More => messages(s"$prefix.numberOfVehicles.more")
  }

  private def mileageUnitText(unit: MileageUnit)(implicit messages: Messages): String = unit match {
    case MileageUnit.MILES => messages("mileage.radio.miles")
    case MileageUnit.KM    => messages("mileage.radio.km")
  }

  private def leftOrRightHandDriveText(drive: LeftOrRightHandDrive)(implicit messages: Messages): String = drive match {
    case LeftOrRightHandDrive.LHD => messages("leftOrRightHandDrive.radio.leftHandDrive")
    case LeftOrRightHandDrive.RHD => messages("leftOrRightHandDrive.radio.rightHandDrive")
  }

  private def yesNo(value: Boolean)(implicit messages: Messages): String =
    if (value) messages("site.yes") else messages("site.no")

  private def row(field: String, value: Value, change: Option[Call])(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = s"$prefix.$field.label",
      value = value,
      actions = change.toSeq.map(call =>
        ActionItemViewModel("site.change", call.url)
          .withVisuallyHiddenText(messages(s"$prefix.$field.change.hidden"))
      )
    )

  private def textValue(value: String): Value = ValueViewModel(Text(value))
}
