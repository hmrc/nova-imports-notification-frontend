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

import models.responses.{SpreadsheetAgriculturalTractorEuVehicle, SpreadsheetAgriculturalTractorNonEuVehicle, SpreadsheetConstructionVehiclesEuVehicle, SpreadsheetConstructionVehiclesNonEuVehicle, SpreadsheetEuVehicle, SpreadsheetHeavyCommercialEuVehicle, SpreadsheetHeavyCommercialNonEuVehicle, SpreadsheetMotorCaravansEuVehicle, SpreadsheetMotorCaravansNonEuVehicle, SpreadsheetMotorcyclesEuVehicle, SpreadsheetMotorcyclesNonEuVehicle, SpreadsheetNonEuVehicle, UploadResultResponse}
import play.api.i18n.Messages
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object SpreadsheetDataCsv {

  def apply(result: UploadResultResponse)(implicit messages: Messages): Option[String] =
    result.validationType match {
      case Some("CarsEu") | Some("LightCommercialVehiclesEu") =>
        Some(buildVehicleHeadingsAndRows(supplierCarAndLcvHeadingsAndValues, result.euVehicles))
      case Some("CarsNonEu")                    => Some(buildVehicleHeadingsAndRows(importCarHeadingsAndValues, result.nonEuVehicles))
      case Some("LightCommercialVehiclesNonEu") => Some(buildVehicleHeadingsAndRows(importLcvHeadingsAndValues, result.nonEuVehicles))
      case Some("AgriculturalTractorsEu")       =>
        Some(buildVehicleHeadingsAndRows(supplierAgriculturalTractorHeadingsAndValues, result.agriculturalTractorEuVehicles))
      case Some("AgriculturalTractorsNonEu") =>
        Some(buildVehicleHeadingsAndRows(importAgriculturalTractorHeadingsAndValues, result.agriculturalTractorNonEuVehicles))
      case Some("ConstructionVehiclesEu") =>
        Some(buildVehicleHeadingsAndRows(supplierConstructionVehicleHeadingsAndValues, result.constructionVehiclesEuVehicles))
      case Some("ConstructionVehiclesNonEu") =>
        Some(buildVehicleHeadingsAndRows(importConstructionVehicleHeadingsAndValues, result.constructionVehiclesNonEuVehicles))
      case Some("HeavyCommercialVehiclesEu") =>
        Some(buildVehicleHeadingsAndRows(supplierHeavyCommercialVehicleHeadingsAndValues, result.heavyCommercialEuVehicles))
      case Some("HeavyCommercialVehiclesNonEu") =>
        Some(buildVehicleHeadingsAndRows(importHeavyCommercialVehicleHeadingsAndValues, result.heavyCommercialNonEuVehicles))
      case Some("MotorCaravansEu") =>
        Some(buildVehicleHeadingsAndRows(supplierMotorCaravanHeadingsAndValues, result.motorCaravansEuVehicles))
      case Some("MotorCaravansNonEu") =>
        Some(buildVehicleHeadingsAndRows(importMotorCaravanHeadingsAndValues, result.motorCaravansNonEuVehicles))
      case Some("MotorcyclesEu") =>
        Some(buildVehicleHeadingsAndRows(supplierMotorcycleHeadingsAndValues, result.motorcyclesEuVehicles))
      case Some("MotorcyclesNonEu") =>
        Some(buildVehicleHeadingsAndRows(importMotorcycleHeadingsAndValues, result.motorcyclesNonEuVehicles))
      case _ => None
    }

  private type HeadingAndValue[A] = (String, A => String)

  private def buildVehicleHeadingsAndRows[A](headingsAndValues: Seq[HeadingAndValue[A]], vehicles: Seq[A])(implicit messages: Messages): String = {
    val headings = record(headingsAndValues.map((key, _) => messages(key)))
    val rows     = vehicles.map(vehicle => record(headingsAndValues.map((_, value) => value(vehicle))))
    (headings +: rows).mkString
  }

  private def headingKey(field: String): String = s"checkVehicleSpreadsheetDetails.download.csv.$field"

  private val supplierCarAndLcvHeadingsAndValues: Seq[HeadingAndValue[SpreadsheetEuVehicle]] = Seq(
    headingKey("itemNumber")                   -> (v => number(v.itemNumber)),
    headingKey("supplierBusinessIndividual")   -> (v => text(v.supplierBusinessPrivate)),
    headingKey("supplierBusinessName")         -> (v => text(v.supplierBusinessName)),
    headingKey("supplierTitle")                -> (v => text(v.supplierTitle)),
    headingKey("supplierFirstName")            -> (v => text(v.supplierFirstName)),
    headingKey("supplierLastName")             -> (v => text(v.supplierLastName)),
    headingKey("addressLine1")                 -> (v => text(v.addressLine1)),
    headingKey("addressLine2")                 -> (v => text(v.addressLine2)),
    headingKey("addressLine3")                 -> (v => text(v.addressLine3)),
    headingKey("addressLine4")                 -> (v => text(v.addressLine4)),
    headingKey("addressLine5")                 -> (v => text(v.addressLine5)),
    headingKey("postcode")                     -> (v => text(v.postcode)),
    headingKey("country")                      -> (v => text(v.country)),
    headingKey("isSupplierVatReg")             -> (v => yesNo(v.supplierVatRegistered)),
    headingKey("euStateVatReg")                -> (v => text(v.euMemberState)),
    headingKey("vatRegistrationNumber")        -> (v => text(v.supplierVatNumber)),
    headingKey("dateRoadUseKnown")             -> (v => yesNo(v.knownDateFirstRegistered)),
    headingKey("countryOfFirstRegistration")   -> (v => text(v.countryOfFirstRegistration)),
    headingKey("dateOfFirstRegistration")      -> (v => date(v.dateOfFirstRegistration)),
    headingKey("dateMadeAvailableYou")         -> (v => date(v.dateMadeAvailable)),
    headingKey("doYouHaveAPurchaseInvoice")    -> (v => yesNo(v.purchaseInvoice)),
    headingKey("noPurchaseInvoiceReason")      -> (v => text(v.noPurchaseInvoiceReason)),
    headingKey("purchaseInvoiceDate")          -> (v => date(v.purchaseInvoiceDate)),
    headingKey("purchaseInvoiceNumber")        -> (v => text(v.purchaseInvoiceNumber)),
    headingKey("pricePaidForVehicle")          -> (v => amount(v.pricePaid)),
    headingKey("currencyUsed")                 -> (v => text(v.currency)),
    headingKey("make")                         -> (v => text(v.make)),
    headingKey("model")                        -> (v => text(v.model)),
    headingKey("derivative")                   -> (v => text(v.derivative)),
    headingKey("trim")                         -> (v => text(v.trim)),
    headingKey("bodyType")                     -> (v => text(v.bodyType)),
    headingKey("notificationReference")        -> (v => text(v.notificationReference)),
    headingKey("vehicleIdNumber")              -> (v => text(v.vin)),
    headingKey("currentRegistrationNumber")    -> (v => text(v.currentRegistrationNumber)),
    headingKey("dateArrivedInUk")              -> (v => date(v.dateArrivedInUk)),
    headingKey("mileage")                      -> (v => text(v.mileage)),
    headingKey("mileageUnits")                 -> (v => lowerCase(v.mileageUnits)),
    headingKey("leftOrRightHand")              -> (v => text(v.leftOrRightHandDrive)),
    headingKey("totalValueOfOptions")          -> (v => amount(v.totalValueOfOptions)),
    headingKey("businessUnableToReclaimVat")   -> (v => yesNo(v.obtainedFromUnableToReclaimVat)),
    headingKey("vehicleSoldUnderMarginScheme") -> (v => yesNo(v.soldUnderMarginScheme)),
    headingKey("areYouClaimingRelief")         -> (v => yesNo(v.claimingVatRelief)),
    headingKey("reasonForClaimingRelief")      -> (v => text(v.reasonForClaimingRelief))
  )

  private val supplierHeavyCommercialVehicleHeadingsAndValues: Seq[HeadingAndValue[SpreadsheetHeavyCommercialEuVehicle]] = Seq(
    headingKey("itemNumber")                   -> (v => number(v.itemNumber)),
    headingKey("supplierBusinessIndividual")   -> (v => text(v.supplierBusinessPrivate)),
    headingKey("supplierBusinessName")         -> (v => text(v.supplierBusinessName)),
    headingKey("supplierTitle")                -> (v => text(v.supplierTitle)),
    headingKey("supplierFirstName")            -> (v => text(v.supplierFirstName)),
    headingKey("supplierLastName")             -> (v => text(v.supplierLastName)),
    headingKey("addressLine1")                 -> (v => text(v.addressLine1)),
    headingKey("addressLine2")                 -> (v => text(v.addressLine2)),
    headingKey("addressLine3")                 -> (v => text(v.addressLine3)),
    headingKey("addressLine4")                 -> (v => text(v.addressLine4)),
    headingKey("addressLine5")                 -> (v => text(v.addressLine5)),
    headingKey("postcode")                     -> (v => text(v.postcode)),
    headingKey("country")                      -> (v => text(v.country)),
    headingKey("isSupplierVatReg")             -> (v => yesNo(v.supplierVatRegistered)),
    headingKey("euStateVatReg")                -> (v => text(v.euMemberState)),
    headingKey("vatRegistrationNumber")        -> (v => text(v.supplierVatNumber)),
    headingKey("dateRoadUseKnown")             -> (v => yesNo(v.knownDateFirstRegistered)),
    headingKey("countryOfFirstRegistration")   -> (v => text(v.countryOfFirstRegistration)),
    headingKey("dateOfFirstRegistration")      -> (v => date(v.dateOfFirstRegistration)),
    headingKey("dateMadeAvailableYou")         -> (v => date(v.dateMadeAvailable)),
    headingKey("doYouHaveAPurchaseInvoice")    -> (v => yesNo(v.purchaseInvoice)),
    headingKey("noPurchaseInvoiceReason")      -> (v => text(v.noPurchaseInvoiceReason)),
    headingKey("purchaseInvoiceDate")          -> (v => date(v.purchaseInvoiceDate)),
    headingKey("purchaseInvoiceNumber")        -> (v => text(v.purchaseInvoiceNumber)),
    headingKey("pricePaidForVehicle")          -> (v => amount(v.pricePaid)),
    headingKey("currencyUsed")                 -> (v => text(v.currency)),
    headingKey("make")                         -> (v => text(v.make)),
    headingKey("model")                        -> (v => text(v.model)),
    headingKey("hcvType")                      -> (v => text(v.heavyCommercialVehicleType)),
    headingKey("cabType")                      -> (v => text(v.cabType)),
    headingKey("notificationReference")        -> (v => text(v.notificationReference)),
    headingKey("vehicleIdNumber")              -> (v => text(v.vin)),
    headingKey("currentRegistrationNumber")    -> (v => text(v.currentRegistrationNumber)),
    headingKey("dateArrivedInUk")              -> (v => date(v.dateArrivedInUk)),
    headingKey("mileage")                      -> (v => text(v.mileage)),
    headingKey("mileageUnits")                 -> (v => lowerCase(v.mileageUnits)),
    headingKey("leftOrRightHand")              -> (v => text(v.leftOrRightHandDrive)),
    headingKey("totalValueOfOptions")          -> (v => amount(v.totalValueOfOptions)),
    headingKey("businessUnableToReclaimVat")   -> (v => yesNo(v.obtainedFromUnableToReclaimVat)),
    headingKey("vehicleSoldUnderMarginScheme") -> (v => yesNo(v.soldUnderMarginScheme)),
    headingKey("areYouClaimingRelief")         -> (v => yesNo(v.claimingVatRelief)),
    headingKey("reasonForClaimingRelief")      -> (v => text(v.reasonForClaimingRelief))
  )

  private val supplierMotorCaravanHeadingsAndValues: Seq[HeadingAndValue[SpreadsheetMotorCaravansEuVehicle]] = Seq(
    headingKey("itemNumber")                   -> (v => number(v.itemNumber)),
    headingKey("supplierBusinessIndividual")   -> (v => text(v.supplierBusinessPrivate)),
    headingKey("supplierBusinessName")         -> (v => text(v.supplierBusinessName)),
    headingKey("supplierTitle")                -> (v => text(v.supplierTitle)),
    headingKey("supplierFirstName")            -> (v => text(v.supplierFirstName)),
    headingKey("supplierLastName")             -> (v => text(v.supplierLastName)),
    headingKey("addressLine1")                 -> (v => text(v.addressLine1)),
    headingKey("addressLine2")                 -> (v => text(v.addressLine2)),
    headingKey("addressLine3")                 -> (v => text(v.addressLine3)),
    headingKey("addressLine4")                 -> (v => text(v.addressLine4)),
    headingKey("addressLine5")                 -> (v => text(v.addressLine5)),
    headingKey("postcode")                     -> (v => text(v.postcode)),
    headingKey("country")                      -> (v => text(v.country)),
    headingKey("isSupplierVatReg")             -> (v => yesNo(v.supplierVatRegistered)),
    headingKey("euStateVatReg")                -> (v => text(v.euMemberState)),
    headingKey("vatRegistrationNumber")        -> (v => text(v.supplierVatNumber)),
    headingKey("dateRoadUseKnown")             -> (v => yesNo(v.knownDateFirstRegistered)),
    headingKey("countryOfFirstRegistration")   -> (v => text(v.countryOfFirstRegistration)),
    headingKey("dateOfFirstRegistration")      -> (v => date(v.dateOfFirstRegistration)),
    headingKey("dateMadeAvailableYou")         -> (v => date(v.dateMadeAvailable)),
    headingKey("doYouHaveAPurchaseInvoice")    -> (v => yesNo(v.purchaseInvoice)),
    headingKey("noPurchaseInvoiceReason")      -> (v => text(v.noPurchaseInvoiceReason)),
    headingKey("purchaseInvoiceDate")          -> (v => date(v.purchaseInvoiceDate)),
    headingKey("purchaseInvoiceNumber")        -> (v => text(v.purchaseInvoiceNumber)),
    headingKey("pricePaidForVehicle")          -> (v => amount(v.pricePaid)),
    headingKey("currencyUsed")                 -> (v => text(v.currency)),
    headingKey("caravanMake")                  -> (v => text(v.caravanMake)),
    headingKey("modelNameNumber")              -> (v => text(v.modelNameNumber)), // writes Motor caravan body value, mapped by validation
    headingKey("caravanVersion")               -> (v => text(v.caravanVersion)), // writes the Model name/number value, mapped by validation
    headingKey("caravanBody")                  -> (v => text(v.caravanBody)), // writes the Motor caravan version value, mapped by validation
    headingKey("makeOfBaseVehicle")            -> (v => text(v.makeOfBaseVehicle)),
    headingKey("derivative")                   -> (v => text(v.derivative)),
    headingKey("notificationReference")        -> (v => text(v.notificationReference)),
    headingKey("vehicleIdNumber")              -> (v => text(v.vin)),
    headingKey("currentRegistrationNumber")    -> (v => text(v.currentRegistrationNumber)),
    headingKey("dateArrivedInUk")              -> (v => date(v.dateArrivedInUk)),
    headingKey("mileage")                      -> (v => text(v.mileage)),
    headingKey("mileageUnits")                 -> (v => lowerCase(v.mileageUnits)),
    headingKey("leftOrRightHand")              -> (v => text(v.leftOrRightHandDrive)),
    headingKey("totalValueOfOptions")          -> (v => amount(v.totalValueOfOptions)),
    headingKey("businessUnableToReclaimVat")   -> (v => yesNo(v.obtainedFromUnableToReclaimVat)),
    headingKey("vehicleSoldUnderMarginScheme") -> (v => yesNo(v.soldUnderMarginScheme)),
    headingKey("areYouClaimingRelief")         -> (v => yesNo(v.claimingVatRelief)),
    headingKey("reasonForClaimingRelief")      -> (v => text(v.reasonForClaimingRelief))
  )

  private val supplierMotorcycleHeadingsAndValues: Seq[HeadingAndValue[SpreadsheetMotorcyclesEuVehicle]] = Seq(
    headingKey("itemNumber")                   -> (v => number(v.itemNumber)),
    headingKey("supplierBusinessIndividual")   -> (v => text(v.supplierBusinessPrivate)),
    headingKey("supplierBusinessName")         -> (v => text(v.supplierBusinessName)),
    headingKey("supplierTitle")                -> (v => text(v.supplierTitle)),
    headingKey("supplierFirstName")            -> (v => text(v.supplierFirstName)),
    headingKey("supplierLastName")             -> (v => text(v.supplierLastName)),
    headingKey("addressLine1")                 -> (v => text(v.addressLine1)),
    headingKey("addressLine2")                 -> (v => text(v.addressLine2)),
    headingKey("addressLine3")                 -> (v => text(v.addressLine3)),
    headingKey("addressLine4")                 -> (v => text(v.addressLine4)),
    headingKey("addressLine5")                 -> (v => text(v.addressLine5)),
    headingKey("postcode")                     -> (v => text(v.postcode)),
    headingKey("country")                      -> (v => text(v.country)),
    headingKey("isSupplierVatReg")             -> (v => yesNo(v.supplierVatRegistered)),
    headingKey("euStateVatReg")                -> (v => text(v.euMemberState)),
    headingKey("vatRegistrationNumber")        -> (v => text(v.supplierVatNumber)),
    headingKey("dateRoadUseKnown")             -> (v => yesNo(v.knownDateFirstRegistered)),
    headingKey("countryOfFirstRegistration")   -> (v => text(v.countryOfFirstRegistration)),
    headingKey("dateOfFirstRegistration")      -> (v => date(v.dateOfFirstRegistration)),
    headingKey("dateMadeAvailableYou")         -> (v => date(v.dateMadeAvailable)),
    headingKey("doYouHaveAPurchaseInvoice")    -> (v => yesNo(v.purchaseInvoice)),
    headingKey("noPurchaseInvoiceReason")      -> (v => text(v.noPurchaseInvoiceReason)),
    headingKey("purchaseInvoiceDate")          -> (v => date(v.purchaseInvoiceDate)),
    headingKey("purchaseInvoiceNumber")        -> (v => text(v.purchaseInvoiceNumber)),
    headingKey("pricePaidForVehicle")          -> (v => amount(v.pricePaid)),
    headingKey("currencyUsed")                 -> (v => text(v.currency)),
    headingKey("make")                         -> (v => text(v.make)),
    headingKey("model")                        -> (v => text(v.model)),
    headingKey("derivative")                   -> (v => text(v.derivative)),
    headingKey("motorcycleVersion")            -> (v => text(v.version)),
    headingKey("motorcycleType")               -> (v => text(v.motorcycleType)),
    headingKey("motorcycleStyle")              -> (v => text(v.style)),
    headingKey("transmission")                 -> (v => text(v.transmissionType)),
    headingKey("fuelType")                     -> (v => text(v.fuelType)),
    headingKey("engineSize")                   -> (v => text(v.engineSize)),
    headingKey("notificationReference")        -> (v => text(v.notificationReference)),
    headingKey("vehicleIdNumber")              -> (v => text(v.vin)),
    headingKey("currentRegistrationNumber")    -> (v => text(v.currentRegistrationNumber)),
    headingKey("dateArrivedInUk")              -> (v => date(v.dateArrivedInUk)),
    headingKey("mileage")                      -> (v => text(v.mileage)),
    headingKey("mileageUnits")                 -> (v => lowerCase(v.mileageUnits)),
    headingKey("leftOrRightHand")              -> (v => text(v.leftOrRightHandDrive)),
    headingKey("totalValueOfOptions")          -> (v => amount(v.totalValueOfOptions)),
    headingKey("businessUnableToReclaimVat")   -> (v => yesNo(v.obtainedFromUnableToReclaimVat)),
    headingKey("vehicleSoldUnderMarginScheme") -> (v => yesNo(v.soldUnderMarginScheme)),
    headingKey("areYouClaimingRelief")         -> (v => yesNo(v.claimingVatRelief)),
    headingKey("reasonForClaimingRelief")      -> (v => text(v.reasonForClaimingRelief))
  )

  private val supplierAgriculturalTractorHeadingsAndValues: Seq[HeadingAndValue[SpreadsheetAgriculturalTractorEuVehicle]] = Seq(
    headingKey("itemNumber")                   -> (v => number(v.itemNumber)),
    headingKey("supplierBusinessIndividual")   -> (v => text(v.supplierBusinessPrivate)),
    headingKey("supplierBusinessName")         -> (v => text(v.supplierBusinessName)),
    headingKey("supplierTitle")                -> (v => text(v.supplierTitle)),
    headingKey("supplierFirstName")            -> (v => text(v.supplierFirstName)),
    headingKey("supplierLastName")             -> (v => text(v.supplierLastName)),
    headingKey("addressLine1")                 -> (v => text(v.addressLine1)),
    headingKey("addressLine2")                 -> (v => text(v.addressLine2)),
    headingKey("addressLine3")                 -> (v => text(v.addressLine3)),
    headingKey("addressLine4")                 -> (v => text(v.addressLine4)),
    headingKey("addressLine5")                 -> (v => text(v.addressLine5)),
    headingKey("postcode")                     -> (v => text(v.postcode)),
    headingKey("country")                      -> (v => text(v.country)),
    headingKey("isSupplierVatReg")             -> (v => yesNo(v.supplierVatRegistered)),
    headingKey("euStateVatReg")                -> (v => text(v.euMemberState)),
    headingKey("vatRegistrationNumber")        -> (v => text(v.supplierVatNumber)),
    headingKey("dateRoadUseKnown")             -> (v => yesNo(v.knownDateFirstRegistered)),
    headingKey("countryOfFirstRegistration")   -> (v => text(v.countryOfFirstRegistration)),
    headingKey("dateOfFirstRegistration")      -> (v => date(v.dateOfFirstRegistration)),
    headingKey("dateMadeAvailableYou")         -> (v => date(v.dateMadeAvailable)),
    headingKey("doYouHaveAPurchaseInvoice")    -> (v => yesNo(v.purchaseInvoice)),
    headingKey("noPurchaseInvoiceReason")      -> (v => text(v.noPurchaseInvoiceReason)),
    headingKey("purchaseInvoiceDate")          -> (v => date(v.purchaseInvoiceDate)),
    headingKey("purchaseInvoiceNumber")        -> (v => text(v.purchaseInvoiceNumber)),
    headingKey("pricePaidForVehicle")          -> (v => amount(v.pricePaid)),
    headingKey("currencyUsed")                 -> (v => text(v.currency)),
    headingKey("make")                         -> (v => text(v.make)),
    headingKey("seriesModel")                  -> (v => text(v.seriesModel)),
    headingKey("versionDerivative")            -> (v => text(v.versionDerivative)),
    headingKey("brakeHorsePower")              -> (v => text(v.brakeHorsepower)),
    headingKey("notificationReference")        -> (v => text(v.notificationReference)),
    headingKey("vehicleIdNumber")              -> (v => text(v.vin)),
    headingKey("currentRegistrationNumber")    -> (v => text(v.currentRegistrationNumber)),
    headingKey("dateArrivedInUk")              -> (v => date(v.dateArrivedInUk)),
    headingKey("mileage")                      -> (v => text(v.mileage)),
    headingKey("mileageUnits")                 -> (v => lowerCase(v.mileageUnits)),
    headingKey("leftOrRightHand")              -> (v => text(v.leftOrRightHandDrive)),
    headingKey("totalValueOfOptions")          -> (v => amount(v.totalValueOfOptions)),
    headingKey("businessUnableToReclaimVat")   -> (v => yesNo(v.obtainedFromUnableToReclaimVat)),
    headingKey("vehicleSoldUnderMarginScheme") -> (v => yesNo(v.soldUnderMarginScheme)),
    headingKey("areYouClaimingRelief")         -> (v => yesNo(v.claimingVatRelief)),
    headingKey("reasonForClaimingRelief")      -> (v => text(v.reasonForClaimingRelief))
  )

  private val supplierConstructionVehicleHeadingsAndValues: Seq[HeadingAndValue[SpreadsheetConstructionVehiclesEuVehicle]] = Seq(
    headingKey("itemNumber")                   -> (v => number(v.itemNumber)),
    headingKey("supplierBusinessIndividual")   -> (v => text(v.supplierBusinessPrivate)),
    headingKey("supplierBusinessName")         -> (v => text(v.supplierBusinessName)),
    headingKey("supplierTitle")                -> (v => text(v.supplierTitle)),
    headingKey("supplierFirstName")            -> (v => text(v.supplierFirstName)),
    headingKey("supplierLastName")             -> (v => text(v.supplierLastName)),
    headingKey("addressLine1")                 -> (v => text(v.addressLine1)),
    headingKey("addressLine2")                 -> (v => text(v.addressLine2)),
    headingKey("addressLine3")                 -> (v => text(v.addressLine3)),
    headingKey("addressLine4")                 -> (v => text(v.addressLine4)),
    headingKey("addressLine5")                 -> (v => text(v.addressLine5)),
    headingKey("postcode")                     -> (v => text(v.postcode)),
    headingKey("country")                      -> (v => text(v.country)),
    headingKey("isSupplierVatReg")             -> (v => yesNo(v.supplierVatRegistered)),
    headingKey("euStateVatReg")                -> (v => text(v.euMemberState)),
    headingKey("vatRegistrationNumber")        -> (v => text(v.supplierVatNumber)),
    headingKey("dateRoadUseKnown")             -> (v => yesNo(v.knownDateFirstRegistered)),
    headingKey("countryOfFirstRegistration")   -> (v => text(v.countryOfFirstRegistration)),
    headingKey("dateOfFirstRegistration")      -> (v => date(v.dateOfFirstRegistration)),
    headingKey("dateMadeAvailableYou")         -> (v => date(v.dateMadeAvailable)),
    headingKey("doYouHaveAPurchaseInvoice")    -> (v => yesNo(v.purchaseInvoice)),
    headingKey("noPurchaseInvoiceReason")      -> (v => text(v.noPurchaseInvoiceReason)),
    headingKey("purchaseInvoiceDate")          -> (v => date(v.purchaseInvoiceDate)),
    headingKey("purchaseInvoiceNumber")        -> (v => text(v.purchaseInvoiceNumber)),
    headingKey("pricePaidForVehicle")          -> (v => amount(v.pricePaid)),
    headingKey("currencyUsed")                 -> (v => text(v.currency)),
    headingKey("make")                         -> (v => text(v.make)),
    headingKey("seriesModel")                  -> (v => text(v.seriesModel)),
    headingKey("versionDerivative")            -> (v => text(v.versionDerivative)),
    headingKey("notificationReference")        -> (v => text(v.notificationReference)),
    headingKey("vehicleIdNumber")              -> (v => text(v.vin)),
    headingKey("currentRegistrationNumber")    -> (v => text(v.currentRegistrationNumber)),
    headingKey("dateArrivedInUk")              -> (v => date(v.dateArrivedInUk)),
    headingKey("mileage")                      -> (v => text(v.mileage)),
    headingKey("mileageUnits")                 -> (v => lowerCase(v.mileageUnits)),
    headingKey("leftOrRightHand")              -> (v => text(v.leftOrRightHandDrive)),
    headingKey("totalValueOfOptions")          -> (v => amount(v.totalValueOfOptions)),
    headingKey("businessUnableToReclaimVat")   -> (v => yesNo(v.obtainedFromUnableToReclaimVat)),
    headingKey("vehicleSoldUnderMarginScheme") -> (v => yesNo(v.soldUnderMarginScheme)),
    headingKey("areYouClaimingRelief")         -> (v => yesNo(v.claimingVatRelief)),
    headingKey("reasonForClaimingRelief")      -> (v => text(v.reasonForClaimingRelief))
  )

  private val importCarHeadingsAndValues: Seq[HeadingAndValue[SpreadsheetNonEuVehicle]] = Seq(
    headingKey("itemNumber")                 -> (v => number(v.itemNumber)),
    headingKey("importEntryNumber")          -> (v => text(v.importEntryNumber)),
    headingKey("importEntryDate")            -> (v => date(v.importEntryDate)),
    headingKey("dateRoadUseKnown")           -> (v => yesNo(v.knownDateFirstRegistered)),
    headingKey("countryOfFirstRegistration") -> (v => text(v.countryOfFirstRegistration)),
    headingKey("dateOfFirstRegistration")    -> (v => date(v.dateOfFirstRegistration)),
    headingKey("make")                       -> (v => text(v.make)),
    headingKey("model")                      -> (v => text(v.model)),
    headingKey("derivative")                 -> (v => text(v.derivative)),
    headingKey("trim")                       -> (v => text(v.trim)),
    headingKey("bodyType")                   -> (v => text(v.bodyType)),
    headingKey("notificationReference")      -> (v => text(v.notificationReference)),
    headingKey("vehicleIdNumber")            -> (v => text(v.vin)),
    headingKey("dateArrivedInUk")            -> (v => date(v.dateArrivedInUk)),
    headingKey("commodityCode")              -> (v => text(v.commodityCode)),
    headingKey("mileage")                    -> (v => text(v.mileage)),
    headingKey("mileageUnits")               -> (v => lowerCase(v.mileageUnits)),
    headingKey("leftOrRightHand")            -> (v => text(v.leftOrRightHandDrive)),
    headingKey("pricePaidForVehicle")        -> (v => amount(v.pricePaid)),
    headingKey("importCurrencyUsed")         -> (v => text(v.currency)),
    headingKey("areYouClaimingRelief")       -> (v => yesNo(v.claimingVatRelief)),
    headingKey("reasonForClaimingRelief")    -> (v => text(v.reasonForClaimingRelief))
  )

  private val importLcvHeadingsAndValues: Seq[HeadingAndValue[SpreadsheetNonEuVehicle]] = Seq(
    headingKey("itemNumber")                 -> (v => number(v.itemNumber)),
    headingKey("importEntryNumber")          -> (v => text(v.importEntryNumber)),
    headingKey("importEntryDate")            -> (v => date(v.importEntryDate)),
    headingKey("dateRoadUseKnown")           -> (v => yesNo(v.knownDateFirstRegistered)),
    headingKey("countryOfFirstRegistration") -> (v => text(v.countryOfFirstRegistration)),
    headingKey("dateOfFirstRegistration")    -> (v => date(v.dateOfFirstRegistration)),
    headingKey("make")                       -> (v => text(v.make)),
    headingKey("model")                      -> (v => text(v.model)),
    headingKey("derivative")                 -> (v => text(v.derivative)),
    headingKey("trim")                       -> (v => text(v.trim)),
    headingKey("lcvBodyType")                -> (v => text(v.bodyType)),
    headingKey("notificationReference")      -> (v => text(v.notificationReference)),
    headingKey("vehicleIdNumber")            -> (v => text(v.vin)),
    headingKey("dateArrivedInUk")            -> (v => date(v.dateArrivedInUk)),
    headingKey("commodityCode")              -> (v => text(v.commodityCode)),
    headingKey("mileage")                    -> (v => text(v.mileage)),
    headingKey("mileageUnits")               -> (v => lowerCase(v.mileageUnits)),
    headingKey("leftOrRightHand")            -> (v => text(v.leftOrRightHandDrive)),
    headingKey("pricePaidForVehicle")        -> (v => amount(v.pricePaid)),
    headingKey("importCurrencyUsed")         -> (v => text(v.currency)),
    headingKey("areYouClaimingRelief")       -> (v => yesNo(v.claimingVatRelief)),
    headingKey("reasonForClaimingRelief")    -> (v => text(v.reasonForClaimingRelief))
  )

  private val importHeavyCommercialVehicleHeadingsAndValues: Seq[HeadingAndValue[SpreadsheetHeavyCommercialNonEuVehicle]] = Seq(
    headingKey("itemNumber")                 -> (v => number(v.itemNumber)),
    headingKey("importEntryNumber")          -> (v => text(v.importEntryNumber)),
    headingKey("importEntryDate")            -> (v => date(v.importEntryDate)),
    headingKey("dateRoadUseKnown")           -> (v => yesNo(v.knownDateFirstRegistered)),
    headingKey("countryOfFirstRegistration") -> (v => text(v.countryOfFirstRegistration)),
    headingKey("dateOfFirstRegistration")    -> (v => date(v.dateOfFirstRegistration)),
    headingKey("make")                       -> (v => text(v.make)),
    headingKey("model")                      -> (v => text(v.model)),
    headingKey("hcvType")                    -> (v => text(v.heavyCommercialVehicleType)),
    headingKey("cabType")                    -> (v => text(v.cabType)),
    headingKey("notificationReference")      -> (v => text(v.notificationReference)),
    headingKey("vehicleIdNumber")            -> (v => text(v.vin)),
    headingKey("dateArrivedInUk")            -> (v => date(v.dateArrivedInUk)),
    headingKey("commodityCode")              -> (v => text(v.commodityCode)),
    headingKey("mileage")                    -> (v => text(v.mileage)),
    headingKey("mileageUnits")               -> (v => lowerCase(v.mileageUnits)),
    headingKey("leftOrRightHand")            -> (v => text(v.leftOrRightHandDrive)),
    headingKey("pricePaidForVehicle")        -> (v => amount(v.pricePaid)),
    headingKey("importCurrencyUsed")         -> (v => text(v.currency)),
    headingKey("areYouClaimingRelief")       -> (v => yesNo(v.claimingVatRelief)),
    headingKey("reasonForClaimingRelief")    -> (v => text(v.reasonForClaimingRelief))
  )

  private val importMotorCaravanHeadingsAndValues: Seq[HeadingAndValue[SpreadsheetMotorCaravansNonEuVehicle]] = Seq(
    headingKey("itemNumber")                 -> (v => number(v.itemNumber)),
    headingKey("importEntryNumber")          -> (v => text(v.importEntryNumber)),
    headingKey("importEntryDate")            -> (v => date(v.importEntryDate)),
    headingKey("dateRoadUseKnown")           -> (v => yesNo(v.knownDateFirstRegistered)),
    headingKey("countryOfFirstRegistration") -> (v => text(v.countryOfFirstRegistration)),
    headingKey("dateOfFirstRegistration")    -> (v => date(v.dateOfFirstRegistration)),
    headingKey("caravanMake")                -> (v => text(v.caravanMake)),
    headingKey("caravanVersion")             -> (v => text(v.caravanVersion)), // writes the Model name / number value, mapped by validation
    headingKey("caravanBody")                -> (v => text(v.caravanBody)), // writes the Motor caravan version value, mapped by validation
    headingKey("modelNameNumber")            -> (v => text(v.modelNameNumber)), // writes the Motor caravan body value, mapped by validation
    headingKey("makeOfBaseVehicle")          -> (v => text(v.makeOfBaseVehicle)),
    headingKey("derivative")                 -> (v => text(v.derivative)),
    headingKey("notificationReference")      -> (v => text(v.notificationReference)),
    headingKey("vehicleIdNumber")            -> (v => text(v.vin)),
    headingKey("dateArrivedInUk")            -> (v => date(v.dateArrivedInUk)),
    headingKey("commodityCode")              -> (v => text(v.commodityCode)),
    headingKey("mileage")                    -> (v => text(v.mileage)),
    headingKey("mileageUnits")               -> (v => lowerCase(v.mileageUnits)),
    headingKey("leftOrRightHand")            -> (v => text(v.leftOrRightHandDrive)),
    headingKey("pricePaidForVehicle")        -> (v => amount(v.pricePaid)),
    headingKey("importCurrencyUsed")         -> (v => text(v.currency)),
    headingKey("areYouClaimingRelief")       -> (v => yesNo(v.claimingVatRelief)),
    headingKey("reasonForClaimingRelief")    -> (v => text(v.reasonForClaimingRelief))
  )

  private val importMotorcycleHeadingsAndValues: Seq[HeadingAndValue[SpreadsheetMotorcyclesNonEuVehicle]] = Seq(
    headingKey("itemNumber")                 -> (v => number(v.itemNumber)),
    headingKey("importEntryNumber")          -> (v => text(v.importEntryNumber)),
    headingKey("importEntryDate")            -> (v => date(v.importEntryDate)),
    headingKey("dateRoadUseKnown")           -> (v => yesNo(v.knownDateFirstRegistered)),
    headingKey("countryOfFirstRegistration") -> (v => text(v.countryOfFirstRegistration)),
    headingKey("dateOfFirstRegistration")    -> (v => date(v.dateOfFirstRegistration)),
    headingKey("make")                       -> (v => text(v.make)),
    headingKey("model")                      -> (v => text(v.model)),
    headingKey("derivative")                 -> (v => text(v.derivative)),
    headingKey("motorcycleVersion")          -> (v => text(v.version)),
    headingKey("motorcycleType")             -> (v => text(v.motorcycleType)),
    headingKey("motorcycleStyle")            -> (v => text(v.style)),
    headingKey("transmission")               -> (v => text(v.transmissionType)),
    headingKey("fuelType")                   -> (v => text(v.fuelType)),
    headingKey("engineSize")                 -> (v => text(v.engineSize)),
    headingKey("notificationReference")      -> (v => text(v.notificationReference)),
    headingKey("vehicleIdNumber")            -> (v => text(v.vin)),
    headingKey("dateArrivedInUk")            -> (v => date(v.dateArrivedInUk)),
    headingKey("commodityCode")              -> (v => text(v.commodityCode)),
    headingKey("mileage")                    -> (v => text(v.mileage)),
    headingKey("mileageUnits")               -> (v => lowerCase(v.mileageUnits)),
    headingKey("leftOrRightHand")            -> (v => text(v.leftOrRightHandDrive)),
    headingKey("pricePaidForVehicle")        -> (v => amount(v.pricePaid)),
    headingKey("importCurrencyUsed")         -> (v => text(v.currency)),
    headingKey("areYouClaimingRelief")       -> (v => yesNo(v.claimingVatRelief)),
    headingKey("reasonForClaimingRelief")    -> (v => text(v.reasonForClaimingRelief))
  )

  private val importAgriculturalTractorHeadingsAndValues: Seq[HeadingAndValue[SpreadsheetAgriculturalTractorNonEuVehicle]] = Seq(
    headingKey("itemNumber")                 -> (v => number(v.itemNumber)),
    headingKey("importEntryNumber")          -> (v => text(v.importEntryNumber)),
    headingKey("importEntryDate")            -> (v => date(v.importEntryDate)),
    headingKey("dateRoadUseKnown")           -> (v => yesNo(v.knownDateFirstRegistered)),
    headingKey("countryOfFirstRegistration") -> (v => text(v.countryOfFirstRegistration)),
    headingKey("dateOfFirstRegistration")    -> (v => date(v.dateOfFirstRegistration)),
    headingKey("make")                       -> (v => text(v.make)),
    headingKey("seriesModel")                -> (v => text(v.seriesModel)),
    headingKey("versionDerivative")          -> (v => text(v.versionDerivative)),
    headingKey("brakeHorsePower")            -> (v => text(v.brakeHorsepower)),
    headingKey("notificationReference")      -> (v => text(v.notificationReference)),
    headingKey("vehicleIdNumber")            -> (v => text(v.vin)),
    headingKey("dateArrivedInUk")            -> (v => date(v.dateArrivedInUk)),
    headingKey("commodityCode")              -> (v => text(v.commodityCode)),
    headingKey("mileage")                    -> (v => text(v.mileage)),
    headingKey("mileageUnits")               -> (v => lowerCase(v.mileageUnits)),
    headingKey("leftOrRightHand")            -> (v => text(v.leftOrRightHandDrive)),
    headingKey("pricePaidForVehicle")        -> (v => amount(v.pricePaid)),
    headingKey("importCurrencyUsed")         -> (v => text(v.currency)),
    headingKey("areYouClaimingRelief")       -> (v => yesNo(v.claimingVatRelief)),
    headingKey("reasonForClaimingRelief")    -> (v => text(v.reasonForClaimingRelief))
  )

  private val importConstructionVehicleHeadingsAndValues: Seq[HeadingAndValue[SpreadsheetConstructionVehiclesNonEuVehicle]] = Seq(
    headingKey("itemNumber")                 -> (v => number(v.itemNumber)),
    headingKey("importEntryNumber")          -> (v => text(v.importEntryNumber)),
    headingKey("importEntryDate")            -> (v => date(v.importEntryDate)),
    headingKey("dateRoadUseKnown")           -> (v => yesNo(v.knownDateFirstRegistered)),
    headingKey("countryOfFirstRegistration") -> (v => text(v.countryOfFirstRegistration)),
    headingKey("dateOfFirstRegistration")    -> (v => date(v.dateOfFirstRegistration)),
    headingKey("make")                       -> (v => text(v.make)),
    headingKey("seriesModel")                -> (v => text(v.seriesModel)),
    headingKey("versionDerivative")          -> (v => text(v.versionDerivative)),
    headingKey("notificationReference")      -> (v => text(v.notificationReference)),
    headingKey("vehicleIdNumber")            -> (v => text(v.vin)),
    headingKey("dateArrivedInUk")            -> (v => date(v.dateArrivedInUk)),
    headingKey("commodityCode")              -> (v => text(v.commodityCode)),
    headingKey("mileage")                    -> (v => text(v.mileage)),
    headingKey("mileageUnits")               -> (v => lowerCase(v.mileageUnits)),
    headingKey("leftOrRightHand")            -> (v => text(v.leftOrRightHandDrive)),
    headingKey("pricePaidForVehicle")        -> (v => amount(v.pricePaid)),
    headingKey("importCurrencyUsed")         -> (v => text(v.currency)),
    headingKey("areYouClaimingRelief")       -> (v => yesNo(v.claimingVatRelief)),
    headingKey("reasonForClaimingRelief")    -> (v => text(v.reasonForClaimingRelief))
  )

  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private def text(value: Option[String]): String = value.getOrElse("")

  private def lowerCase(value: Option[String]): String = value.fold("")(_.toLowerCase)

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
