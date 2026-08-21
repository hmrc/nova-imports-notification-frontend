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
import models.{Address, AddressJourney, NormalMode, PurchaserOrOnBehalf, SupplierNumber}
import pages.QuestionPage
import pages.sections.initialquestions.PurchaserOrOnBehalfPage
import pages.sections.notifieraddress.{AddressJourneyIdPage, AddressPage}
import pages.sections.purchaseraddress.{PurchaserAddressJourneyIdPage, PurchaserAddressPage}
import pages.sections.supplieraddress.IsSupplierAddressInTheUkPage
import pages.sections.supplieraddress.{SupplierAddressJourneyIdPage, SupplierAddressPage}
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
  onComplete: Call,
  addressChangedPage: Call,
  addressChangedSubmit: Call,
  changeAddressLink: Call,
  restartAt: Call,
  messageKeyPrefix: String
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
    onComplete = routes.NotificationTaskListController.onPageLoad(),
    addressChangedPage = routes.AddressChangedController.onPageLoad(),
    addressChangedSubmit = routes.AddressChangedController.onSubmit(),
    changeAddressLink = routes.AddressChangedController.onChangeAddress(),
    restartAt = notifieraddress.routes.IsYourAddressInTheUkController.onPageLoad(NormalMode),
    messageKeyPrefix = "addressChanged"
  )

  private def supplier(number: SupplierNumber, supplierService: SupplierService): AddressJourneyBinding = AddressJourneyBinding(
    addressPage = SupplierAddressPage(number),
    journeyIdPage = SupplierAddressJourneyIdPage(number),
    sectionId = s"supplier/${number.value}/details",
    payload = address => Json.toJson(SupplierAddress.fromAddress(address)).as[JsObject],
    guard = request =>
      IsDraftIdDefined(request.userAnswers) &&
        request.userAnswers.get(IsSupplierAddressInTheUkPage(number)).isDefined &&
        supplierService.numberExists(request.userAnswers, number),
    onComplete = routes.LandingPageController.onPageLoad(), // TODO: navigate to AVD-S8.0 when built (DTR-6200)
    addressChangedPage = routes.AddressChangedController.supplierOnPageLoad(number),
    addressChangedSubmit = routes.AddressChangedController.supplierOnSubmit(number),
    changeAddressLink = routes.AddressChangedController.supplierOnChangeAddress(number),
    restartAt = supplieraddress.routes.IsSupplierAddressInTheUKController.onPageLoad(number, NormalMode),
    messageKeyPrefix = "supplierAddressChanged"
  )

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
          request.userAnswers.get(PurchaserOrOnBehalfPage).contains(PurchaserOrOnBehalf.OnBehalfOfPurchaser)
      },
    onComplete = routes.NotificationTaskListController.onPageLoad(),
    addressChangedPage = routes.AddressChangedController.purchaserOnPageLoad(),
    addressChangedSubmit = routes.AddressChangedController.purchaserOnSubmit(),
    changeAddressLink = routes.AddressChangedController.purchaserOnChangeAddress(),
    restartAt = purchaseraddress.routes.IsPurchaserAddressInTheUkController.onPageLoad(NormalMode),
    messageKeyPrefix = "purchaserAddressChanged"
  )
}
