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

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

final case class ValidationError(field: String, error: String, itemNumber: Option[Int] = None) {
  def label: String = ValidationError.labelFor(field)
}

object ValidationError {
  implicit val reads: Reads[ValidationError] = Json.reads[ValidationError]

  private val labels: Map[String, String] = Map(
    "knownDateFirstRegistered"       -> "Do you know the date the vehicle was first registered for road use?",
    "countryOfFirstRegistration"     -> "Country of first registration",
    "dateOfFirstRegistration"        -> "Date of first registration",
    "vin"                            -> "Vehicle identification number (VIN)",
    "dateArrivedInUk"                -> "Date arrived in UK",
    "mileage"                        -> "Mileage",
    "mileageUnits"                   -> "Mileage unit",
    "leftOrRightHandDrive"           -> "Is the vehicle left-hand drive (LHD) or right-hand drive (RHD)?",
    "pricePaid"                      -> "Price paid for vehicle (including all options)",
    "currency"                       -> "Currency used",
    "claimingVatRelief"              -> "Are you claiming relief?",
    "reasonForClaimingRelief"        -> "Reason for claiming relief",
    "notificationReference"          -> "Notification reference",
    "supplierBusinessPrivate"        -> "Is the supplier a business or private individual?",
    "supplierBusinessName"           -> "Supplier business name",
    "supplierTitle"                  -> "Supplier title",
    "supplierFirstName"              -> "Supplier first name",
    "supplierLastName"               -> "Supplier last name",
    "addressLine1"                   -> "Address line 1",
    "addressLine2"                   -> "Address line 2",
    "addressLine3"                   -> "Address line 3",
    "addressLine4"                   -> "Address line 4",
    "addressLine5"                   -> "Address line 5",
    "postcode"                       -> "Postcode",
    "country"                        -> "Country",
    "supplierVatRegistered"          -> "Is the supplier VAT registered?",
    "euMemberState"                  -> "EU member state in which the supplier is VAT registered",
    "supplierVatNumber"              -> "VAT registration number",
    "dateMadeAvailable"              -> "Date the vehicle was made available to you",
    "purchaseInvoice"                -> "Do you have a purchase invoice for this vehicle?",
    "noPurchaseInvoiceReason"        -> "If you do not have a purchase invoice, please give a reason",
    "purchaseInvoiceDate"            -> "Purchase invoice date",
    "purchaseInvoiceNumber"          -> "Purchase invoice number",
    "currentRegistrationNumber"      -> "Current vehicle registration number",
    "totalValueOfOptions"            -> "Total value of options",
    "obtainedFromUnableToReclaimVat" -> "Was the vehicle obtained from a business who was unable to reclaim the input VAT on purchase?",
    "soldUnderMarginScheme"          -> "Was the vehicle sold to you by a VAT registered dealer under the Margin Scheme?",
    "importEntryNumber"              -> "Import entry number",
    "importEntryDate"                -> "Import entry date",
    "commodityCode"                  -> "Commodity code",
    "make"                           -> "Make",
    "model"                          -> "Model",
    "derivative"                     -> "Derivative",
    "trim"                           -> "Trim",
    "bodyType"                       -> "Body type"
  )

  private val indexSuffix = "\\[\\d+\\]$".r

  def labelFor(fieldKey: String): String = {
    val base = indexSuffix.replaceFirstIn(fieldKey, "")
    labels.getOrElse(base, base)
  }
}

final case class VehicleSummary(itemNumber: Option[Int], vin: Option[String], make: Option[String], model: Option[String])

object VehicleSummary {
  implicit val reads: Reads[VehicleSummary] = Json.reads[VehicleSummary]
}

final case class UploadResultResponse(fileStatus: String, vehicles: Seq[VehicleSummary], errors: Seq[ValidationError])

object UploadResultResponse {
  implicit val reads: Reads[UploadResultResponse] = (
    (JsPath \ "fileStatus").read[String] and
      (JsPath \ "data" \ "vehicles").readNullable[Seq[VehicleSummary]].map(_.getOrElse(Seq.empty)) and
      (JsPath \ "errors").readNullable[Seq[ValidationError]].map(_.getOrElse(Seq.empty))
  )(UploadResultResponse.apply)
}
