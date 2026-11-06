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
import models.{CheckMode, Currency, ImportNumber, MileageUnit, OneMultiVehicle, SupplierNumber, UserAnswers, VehicleNumber}
import pages.ValidatedVinListPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.vehicledetails.*
import play.api.i18n.Messages
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow, Value}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object ConfirmAdditionalVehicleDetailsHelper {

  private val prefix = "confirmAdditionalVehicleDetails"

  final case class ChangeLinks(
    notificationReference: Call,
    numberOfVehicles: Call,
    vehicleIdentificationNumber: Call,
    vehicleRegistrationNumberKnown: Option[Call],
    mileage: Call,
    totalAmountPaid: Call,
    currency: Call
  )

  def supplierSummaryList(answers: UserAnswers, supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, currencies: Seq[Currency])(implicit
    messages: Messages
  ): SummaryList =
    summaryList(
      answers,
      vehicleNumber,
      currencies,
      ChangeLinks(
        notificationReference = routes.NotificationReferenceController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode),
        numberOfVehicles = routes.NumberOfVehiclesController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode),
        vehicleIdentificationNumber = routes.VehicleIdentificationNumberController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode),
        vehicleRegistrationNumberKnown = Some(routes.VehicleRegistrationNumberKnownController.onPageLoad(supplierNumber, vehicleNumber, CheckMode)),
        mileage = routes.MileageController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode),
        totalAmountPaid = routes.TotalAmountPaidController.onPageLoadSupplier(supplierNumber, vehicleNumber, CheckMode),
        currency = routes.PaymentCurrencyController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
      )
    )

  def importSummaryList(answers: UserAnswers, importNumber: ImportNumber, vehicleNumber: VehicleNumber, currencies: Seq[Currency])(implicit
    messages: Messages
  ): SummaryList =
    summaryList(
      answers,
      vehicleNumber,
      currencies,
      ChangeLinks(
        notificationReference = routes.NotificationReferenceController.importOnPageLoad(importNumber, vehicleNumber, CheckMode),
        numberOfVehicles = routes.NumberOfVehiclesController.importOnPageLoad(importNumber, vehicleNumber, CheckMode),
        vehicleIdentificationNumber = routes.VehicleIdentificationNumberController.importOnPageLoad(importNumber, vehicleNumber, CheckMode),
        vehicleRegistrationNumberKnown = None,
        mileage = routes.MileageController.importOnPageLoad(importNumber, vehicleNumber, CheckMode),
        totalAmountPaid = routes.TotalAmountPaidController.onPageLoadImport(importNumber, vehicleNumber, CheckMode),
        currency = routes.PaymentCurrencyController.importOnPageLoad(importNumber, vehicleNumber, CheckMode)
      )
    )

  private def summaryList(answers: UserAnswers, vehicleNumber: VehicleNumber, currencies: Seq[Currency], links: ChangeLinks)(implicit
    messages: Messages
  ): SummaryList = {
    val numberOfVehicles = answers.get(NumberOfVehiclesPage(vehicleNumber)).map(_.oneMultiVehicle)
    val oneVehicle       = numberOfVehicles.contains(OneMultiVehicle.One)
    val vehicleFromEu    = answers.get(VehicleFromEuPage)

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
        answers
          .get(MileagePage(vehicleNumber))
          .map(mileage => row("mileage", textValue(s"${mileage.mileage} ${mileageUnitText(mileage.mileageUnits)}"), change(links.mileage))),
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
