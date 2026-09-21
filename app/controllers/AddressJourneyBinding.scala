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

package controllers

import controllers.utils.IsDraftIdDefined
import models.draftsections.{NotifierAddress, PurchaserAddress, SupplierAddress}
import models.requests.DataRequest
import models.{Address, AddressJourney, BusinessOrPrivateIndividual, CheckMode, NormalMode, PurchaserOrOnBehalf, SupplierNumber}
import pages.QuestionPage
import pages.sections.initialquestions.{NotifyingAsPurchaserPage, VehicleFromEuPage}
import pages.sections.notifieraddress.{AddressJourneyIdPage, AddressPage}
import pages.sections.purchaseraddress.{PurchaserAddressJourneyIdPage, PurchaserAddressPage}
import pages.sections.supplieraddress.{SupplierAddressJourneyIdPage, SupplierAddressPage}
import pages.sections.supplierdetails.{IsSupplierVatRegisteredPage, SupplierBusinessOrIndividualPage}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import services.SupplierService

// common class to avoid duplication
final case class AddressJourneyBinding(
  addressPage: QuestionPage[Address],
  journeyIdPage: QuestionPage[String],
  sectionId: String,
  payload: Address => JsObject,
  guard: DataRequest[?] => Boolean,
  onComplete: DataRequest[?] => Call,
  addressChangedPage: Call,
  addressChangedSubmit: Call,
  changeAddressLink: Call,
  restartAt: DataRequest[?] => Call,
  messageKeyPrefix: String,
  saveAddressToFormP: Boolean
)

object AddressJourneyBinding {

  def apply(journey: AddressJourney, supplierService: SupplierService): AddressJourneyBinding = journey match {
    case AddressJourney.Notifier         => notifier
    case AddressJourney.Supplier(number) => supplier(number, supplierService)
    case AddressJourney.Purchaser        => purchaser
  }

  private val notifier: AddressJourneyBinding = AddressJourneyBinding(
    addressPage = AddressPage,
    journeyIdPage = AddressJourneyIdPage,
    sectionId = "notifier-address",
    payload = address => Json.toJson(NotifierAddress.fromAddress(address)).as[JsObject],
    guard = !_.userContext.isAgent,
    onComplete = request => routes.NotificationTaskListController.onPageLoad(),
    addressChangedPage = routes.AddressChangedController.onPageLoad(),
    addressChangedSubmit = routes.AddressChangedController.onSubmit(),
    changeAddressLink = routes.AddressChangedController.onChangeAddress(),
    restartAt = _ => notifieraddress.routes.IsYourAddressInTheUkController.onPageLoad(NormalMode),
    messageKeyPrefix = "addressChanged",
    saveAddressToFormP = true
  )

  private def supplier(number: SupplierNumber, supplierService: SupplierService): AddressJourneyBinding = AddressJourneyBinding(
    addressPage = SupplierAddressPage(number),
    journeyIdPage = SupplierAddressJourneyIdPage(number),
    sectionId = s"supplier/${number.value}/details",
    payload = address => Json.toJson(SupplierAddress.fromAddress(address)).as[JsObject],
    guard = request =>
      IsDraftIdDefined(request.userAnswers) &&
        request.userAnswers.get(VehicleFromEuPage).contains(true) &&
        request.userAnswers.get(SupplierBusinessOrIndividualPage(number)).isDefined &&
        supplierService.numberHasValues(request.userAnswers, number),
    onComplete = request =>
      if (request.userAnswers.get(IsSupplierVatRegisteredPage(number)).isDefined) {
        supplierdetails.routes.SupplierDetailsCheckYourAnswersController.onPageLoad(number)
      } else {
        supplierdetails.routes.IsSupplierVatRegisteredController.onPageLoad(number, NormalMode)
      },
    addressChangedPage = routes.AddressChangedController.supplierOnPageLoad(number),
    addressChangedSubmit = routes.AddressChangedController.supplierOnSubmit(number),
    changeAddressLink = routes.AddressChangedController.supplierOnChangeAddress(number),
    restartAt = request => supplierAlfRestart(number, request),
    messageKeyPrefix = "supplierAddressChanged",
    saveAddressToFormP = false
  )

  def supplierAlfRestart(number: SupplierNumber, request: DataRequest[?]): Call =
    request.userAnswers.get(SupplierBusinessOrIndividualPage(number)) match {
      case Some(BusinessOrPrivateIndividual.Business) =>
        supplierdetails.routes.SupplierBusinessNameController.onPageLoad(number, CheckMode)
      case _ =>
        supplierdetails.routes.SupplierNameController.onPageLoad(number, CheckMode)
    }

  private val purchaser: AddressJourneyBinding = AddressJourneyBinding(
    addressPage = PurchaserAddressPage,
    journeyIdPage = PurchaserAddressJourneyIdPage,
    sectionId = "purchaser-address",
    payload = address => Json.toJson(PurchaserAddress.fromAddress(address)).as[JsObject],
    guard = request =>
      request.userContext match {
        case ctx if ctx.isAgent => IsDraftIdDefined(request.userAnswers)
        case _                  =>
          IsDraftIdDefined(request.userAnswers) &&
          request.userAnswers.get(NotifyingAsPurchaserPage).contains(PurchaserOrOnBehalf.OnBehalfOfPurchaser)
      },
    onComplete = request => routes.NotificationTaskListController.onPageLoad(),
    addressChangedPage = routes.AddressChangedController.purchaserOnPageLoad(),
    addressChangedSubmit = routes.AddressChangedController.purchaserOnSubmit(),
    changeAddressLink = routes.AddressChangedController.purchaserOnChangeAddress(),
    restartAt = _ => purchaseraddress.routes.IsPurchaserAddressInTheUkController.onPageLoad(NormalMode),
    messageKeyPrefix = "purchaserAddressChanged",
    saveAddressToFormP = true
  )
}
