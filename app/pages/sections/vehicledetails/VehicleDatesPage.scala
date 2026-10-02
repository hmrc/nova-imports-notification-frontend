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

package pages.sections.vehicledetails

import models.{SupplierNumber, UserAnswers, VehicleDates, VehicleNumber}
import pages.QuestionPage
import play.api.libs.json.JsPath
import queries.Settable

import scala.util.Try

final case class VehicleDatesPage(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber) extends QuestionPage[Set[VehicleDates]] {

  override def path: JsPath = JsPath \ "vehicles" \ vehicleNumber.value.toString \ "details" \ toString

  override def toString: String = "vehicleDates"

  // Dropping a date clears whatever that date asked for, so the vehicle keeps no answers the user has gone back on.
  override def cleanup(value: Option[Set[VehicleDates]], userAnswers: UserAnswers): Try[UserAnswers] = {
    val dates = value.getOrElse(Set.empty).filterNot(_ == VehicleDates.NoDates)

    val firstRegistration = Seq(DateOfFirstRegistrationPage(vehicleNumber), CountryOfFirstRegistrationPage(vehicleNumber))
    val madeAvailable     = Seq(DateOfAvailabilityPage(supplierNumber, vehicleNumber))
    val purchaseInvoice   = Seq(PurchaseInvoiceDatePage(supplierNumber, vehicleNumber), PurchaseInvoiceNumberPage(supplierNumber, vehicleNumber))
    val noPurchaseInvoice = Seq(NoPurchaseInvoiceReasonPage(supplierNumber, vehicleNumber))

    val unanswered: Seq[Settable[?]] =
      (if (dates.contains(VehicleDates.FirstRegistration)) Nil else firstRegistration) ++
        (if (dates.contains(VehicleDates.MadeAvailable)) Nil else madeAvailable) ++
        (if (dates.contains(VehicleDates.PurchaseInvoiceDate)) noPurchaseInvoice else purchaseInvoice) ++
        (if (dates.isEmpty) noPurchaseInvoice else Nil)

    unanswered.distinct.foldLeft(Try(userAnswers))((answers, page) => answers.flatMap(_.remove(page)))
  }
}
