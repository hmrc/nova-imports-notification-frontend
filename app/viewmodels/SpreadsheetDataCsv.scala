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

package viewmodels

import models.responses.{SpreadsheetEuVehicle, UploadResultResponse}
import play.api.i18n.Messages

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object SpreadsheetDataCsv {

  def apply(result: UploadResultResponse)(implicit messages: Messages): Option[String] =
    result.validationType match {
      case Some("CarsEu") => Some(csv(supplierCarColumns, result.euVehicles))
      case _              => None
    }

  private type Column[A] = (String, A => String)

  private def csv[A](columns: Seq[Column[A]], vehicles: Seq[A])(implicit messages: Messages): String = {
    val headings = record(columns.map((key, _) => messages(key)))
    val rows     = vehicles.map(vehicle => record(columns.map((_, value) => value(vehicle))))
    (headings +: rows).mkString
  }

  private def supplier(field: String): String = s"checkVehicleSpreadsheetDetails.download.csv.supplier.$field"

  private val supplierCarColumns: Seq[Column[SpreadsheetEuVehicle]] = Seq(
    supplier("itemNumber")                     -> (v => number(v.itemNumber)),
    supplier("supplierBusinessPrivate")        -> (v => text(v.supplierBusinessPrivate)),
    supplier("supplierBusinessName")           -> (v => text(v.supplierBusinessName)),
    supplier("supplierTitle")                  -> (v => text(v.supplierTitle)),
    supplier("supplierFirstName")              -> (v => text(v.supplierFirstName)),
    supplier("supplierLastName")               -> (v => text(v.supplierLastName)),
    supplier("addressLine1")                   -> (v => text(v.addressLine1)),
    supplier("addressLine2")                   -> (v => text(v.addressLine2)),
    supplier("addressLine3")                   -> (v => text(v.addressLine3)),
    supplier("addressLine4")                   -> (v => text(v.addressLine4)),
    supplier("addressLine5")                   -> (v => text(v.addressLine5)),
    supplier("postcode")                       -> (v => text(v.postcode)),
    supplier("country")                        -> (v => text(v.country)),
    supplier("supplierVatRegistered")          -> (v => yesNo(v.supplierVatRegistered)),
    supplier("euMemberState")                  -> (v => text(v.euMemberState)),
    supplier("supplierVatNumber")              -> (v => text(v.supplierVatNumber)),
    supplier("knownDateFirstRegistered")       -> (v => yesNo(v.knownDateFirstRegistered)),
    supplier("countryOfFirstRegistration")     -> (v => text(v.countryOfFirstRegistration)),
    supplier("dateOfFirstRegistration")        -> (v => date(v.dateOfFirstRegistration)),
    supplier("dateMadeAvailable")              -> (v => date(v.dateMadeAvailable)),
    supplier("purchaseInvoice")                -> (v => yesNo(v.purchaseInvoice)),
    supplier("noPurchaseInvoiceReason")        -> (v => text(v.noPurchaseInvoiceReason)),
    supplier("purchaseInvoiceDate")            -> (v => date(v.purchaseInvoiceDate)),
    supplier("purchaseInvoiceNumber")          -> (v => text(v.purchaseInvoiceNumber)),
    supplier("pricePaid")                      -> (v => amount(v.pricePaid)),
    supplier("currency")                       -> (v => text(v.currency)),
    supplier("make")                           -> (v => text(v.make)),
    supplier("model")                          -> (v => text(v.model)),
    supplier("derivative")                     -> (v => text(v.derivative)),
    supplier("trim")                           -> (v => text(v.trim)),
    supplier("bodyType")                       -> (v => text(v.bodyType)),
    supplier("notificationReference")          -> (v => text(v.notificationReference)),
    supplier("vin")                            -> (v => text(v.vin)),
    supplier("currentRegistrationNumber")      -> (v => text(v.currentRegistrationNumber)),
    supplier("dateArrivedInUk")                -> (v => date(v.dateArrivedInUk)),
    supplier("mileage")                        -> (v => text(v.mileage)),
    supplier("mileageUnits")                   -> (v => text(v.mileageUnits)),
    supplier("leftOrRightHandDrive")           -> (v => text(v.leftOrRightHandDrive)),
    supplier("totalValueOfOptions")            -> (v => amount(v.totalValueOfOptions)),
    supplier("obtainedFromUnableToReclaimVat") -> (v => yesNo(v.obtainedFromUnableToReclaimVat)),
    supplier("soldUnderMarginScheme")          -> (v => yesNo(v.soldUnderMarginScheme)),
    supplier("claimingVatRelief")              -> (v => yesNo(v.claimingVatRelief)),
    supplier("reasonForClaimingRelief")        -> (v => text(v.reasonForClaimingRelief))
  )

  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private def text(value: Option[String]): String = value.getOrElse("")

  private def number(value: Option[Int]): String = value.fold("")(_.toString)

  private def amount(value: Option[BigDecimal]): String = value.fold("")(_.toString)

  private def date(value: Option[LocalDate]): String = value.fold("")(_.format(dateFormatter))

  private def yesNo(value: Option[Boolean]): String = value.fold("")(if (_) "Y" else "N")

  private def record(values: Seq[String]): String =
    s"${values.map(field).mkString(",")}\r\n"

  private def field(value: String): String =
    if (value.exists(c => c == ',' || c == '"' || c == '\r' || c == '\n')) s""""${value.replace("\"", "\"\"")}""""
    else value
}
