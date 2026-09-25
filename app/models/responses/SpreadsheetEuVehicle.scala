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

package models.responses

import play.api.libs.json.{Json, Reads}

import java.time.LocalDate

// One row from a validated CarsEu or LightCommercialEu spreadsheet - the two share an identical field set at this layer
// Used only when saving the notification's vehicle/supplier sections to FormP
final case class SpreadsheetEuVehicle(
  itemNumber: Option[Int],
  supplierBusinessPrivate: Option[String],
  supplierBusinessName: Option[String],
  supplierTitle: Option[String],
  supplierFirstName: Option[String],
  supplierLastName: Option[String],
  addressLine1: Option[String],
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  addressLine5: Option[String],
  postcode: Option[String],
  country: Option[String],
  supplierVatRegistered: Option[Boolean],
  euMemberState: Option[String],
  supplierVatNumber: Option[String],
  knownDateFirstRegistered: Option[Boolean],
  purchaseInvoice: Option[Boolean],
  purchaseInvoiceDate: Option[LocalDate],
  purchaseInvoiceNumber: Option[String],
  pricePaid: Option[BigDecimal],
  currency: Option[String],
  make: Option[String],
  model: Option[String],
  derivative: Option[String],
  trim: Option[String],
  bodyType: Option[String],
  vin: Option[String],
  dateArrivedInUk: Option[LocalDate],
  mileage: Option[String],
  mileageUnits: Option[String],
  leftOrRightHandDrive: Option[String],
  totalValueOfOptions: Option[BigDecimal],
  obtainedFromUnableToReclaimVat: Option[Boolean],
  soldUnderMarginScheme: Option[Boolean],
  claimingVatRelief: Option[Boolean]
)

object SpreadsheetEuVehicle {

  // the backend writes BigDecimal fields as JSON strings (not numbers) to avoid floating point loss
  private implicit val bigDecimalReads: Reads[BigDecimal] = Reads.StringReads.map(BigDecimal(_))

  implicit val reads: Reads[SpreadsheetEuVehicle] = Json.reads[SpreadsheetEuVehicle]
}
