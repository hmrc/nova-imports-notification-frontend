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

package navigation

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import controllers.{initialquestions, notifierdetails, purchaserdetails, routes, supplieraddress, supplierdetails}
import pages.*
import models.*
import pages.sections.initialquestions.{AgentClientVehicleBusinessUsePage, BusinessOrPrivatePage, PurchaserBusinessOrIndividualPage, PurchaserOrOnBehalfPage, VehicleBusinessUsePage, VehicleFromEuPage}
import pages.sections.notifierdetails.{AboutYourDetailsPage, BusinessNamePage, EmailAddressPage, NameDetailsPage, PhoneNumberPage}
import pages.sections.vehicledetails.{AddImportVehicleDetailsPage, AddVehicleDetailsPage}
import pages.sections.notifieraddress.IsYourAddressInTheUkPage
import pages.sections.purchaseraddress.IsPurchaserAddressInTheUkPage
import pages.sections.purchaserdetails.{PurchaserBusinessNamePage, PurchaserNamePage}
import pages.sections.supplierdetails.{IsSupplierVatRegisteredPage, SupplierBusinessNamePage, SupplierBusinessOrIndividualPage, SupplierNamePage, SupplierVatRegistrationNumberPage, UsePersonalDetailsAsSupplierPage, UsePurchaserDetailsAsSupplierPage}
import pages.sections.supplieraddress.IsSupplierAddressInTheUkPage
import pages.sections.vehicledetails.VehicleDatesPage

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: Page => (UserAnswers, NovaUserType) => Call = {
    case VehicleFromEuPage =>
      (userAnswers, userType) =>
        userType match {
          case NovaUserType.VatRegisteredOrganisation =>
            userAnswers.get(VehicleFromEuPage) match {
              case Some(_) => initialquestions.routes.VehicleBusinessUseController.onPageLoad(NormalMode)
              case _       => routes.JourneyRecoveryController.onPageLoad()
            }
          case NovaUserType.Agent if userAnswers.get(AgentSelectedClientPage).isDefined =>
            userAnswers.get(VehicleFromEuPage) match {
              case Some(_) => initialquestions.routes.AgentVehicleBusinessUseController.onPageLoad(NormalMode)
              case _       => routes.JourneyRecoveryController.onPageLoad()
            }
          case _ =>
            userAnswers.get(VehicleFromEuPage) match {
              case Some(true)  => initialquestions.routes.BusinessPrivateController.onPageLoad(NormalMode)
              case Some(false) => initialquestions.routes.VehicleOutsideEUController.onPageLoad()
              case _           => routes.JourneyRecoveryController.onPageLoad()
            }
        }
    case AboutYourDetailsPage =>
      (userAnswers, _) =>
        userAnswers.get(VehicleBusinessUsePage) match {
          case Some(true)  => notifierdetails.routes.PhoneNumberController.onPageLoad(NormalMode)
          case Some(false) => notifierdetails.routes.AddYourNameController.onPageLoad(NormalMode)
          case _           => routes.JourneyRecoveryController.onPageLoad()
        }
    case NameDetailsPage =>
      (_, _) => notifierdetails.routes.PhoneNumberController.onPageLoad(NormalMode)
    case BusinessNamePage =>
      (_, _) => notifierdetails.routes.PhoneNumberController.onPageLoad(NormalMode)
    case PurchaserNamePage =>
      (_, _) => purchaserdetails.routes.PurchaserDetailsCheckYourAnswersController.onPageLoad()
    case PhoneNumberPage =>
      (_, _) => notifierdetails.routes.EmailAddressController.onPageLoad(NormalMode)
    case VehicleBusinessUsePage =>
      (_, _) => initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
    case AgentClientVehicleBusinessUsePage =>
      (_, _) => initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
    case PurchaserOrOnBehalfPage =>
      (userAnswers, _) =>
        userAnswers.get(PurchaserOrOnBehalfPage) match {
          case Some(PurchaserOrOnBehalf.Purchaser)           => initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
          case Some(PurchaserOrOnBehalf.OnBehalfOfPurchaser) => initialquestions.routes.PurchaserBusinessOrIndividualController.onPageLoad(NormalMode)
          case _                                             => routes.JourneyRecoveryController.onPageLoad()
        }
    case BusinessOrPrivatePage =>
      (_, _) => initialquestions.routes.PurchaserOrOnBehalfController.onPageLoad(NormalMode)
    case PurchaserBusinessOrIndividualPage =>
      (_, _) => initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
    case AddVehicleDetailsPage =>
      // adding by supplier is routed by the controller with the newly allocated supplierNumber set up
      (userAnswers, _) =>
        userAnswers.get(AddVehicleDetailsPage) match {
          case Some(AddVehicleDetails.BySpreadsheet) =>
            routes.LandingPageController.onPageLoad() // TODO: navigate to spreadsheet upload flow when built
          case _ => routes.JourneyRecoveryController.onPageLoad()
        }
    case AddImportVehicleDetailsPage =>
      (userAnswers, _) =>
        userAnswers.get(AddImportVehicleDetailsPage) match {
          // AVD1.1 continue navigation is TBC - both options land on a placeholder until those screens are built.
          case Some(AddImportVehicleDetails.ByImportEntryNumber) =>
            routes.LandingPageController.onPageLoad() // TODO: navigate to the import entry number flow when built
          case Some(AddImportVehicleDetails.BySpreadsheet) =>
            routes.LandingPageController.onPageLoad() // TODO: navigate to spreadsheet upload flow when built
          case _ => routes.JourneyRecoveryController.onPageLoad()
        }
    case page: UsePersonalDetailsAsSupplierPage =>
      (userAnswers, _) =>
        userAnswers.get(page) match {
          case Some(true)  => routes.LandingPageController.onPageLoad() // TODO: navigate to CYA3.0 when built
          case Some(false) =>
            supplierdetails.routes.SupplierBusinessOrIndividualController.onPageLoad(page.supplierNumber, NormalMode)
          case _ => routes.JourneyRecoveryController.onPageLoad()
        }
    case page: UsePurchaserDetailsAsSupplierPage =>
      (userAnswers, _) =>
        userAnswers.get(page) match {
          case Some(true)  => routes.LandingPageController.onPageLoad() // TODO: navigate to CYA3.0 when built
          case Some(false) =>
            supplierdetails.routes.SupplierBusinessOrIndividualController.onPageLoad(page.supplierNumber, NormalMode)
          case _ => routes.JourneyRecoveryController.onPageLoad()
        }
    case IsYourAddressInTheUkPage =>
      (userAnswers, _) =>
        userAnswers.get(IsYourAddressInTheUkPage) match {
          case Some(true)  => routes.LandingPageController.onPageLoad() // TODO: navigate to address-lookup-service - to be added later
          case Some(false) => routes.LandingPageController.onPageLoad() // TODO: navigate to AYA1.1 - to be added later
          case _           => routes.JourneyRecoveryController.onPageLoad()
        }
    case EmailAddressPage =>
      (_, _) => notifierdetails.routes.YourDetailsCheckYourAnswersController.onPageLoad()
    case PurchaserBusinessNamePage =>
      (_, _) => purchaserdetails.routes.PurchaserDetailsCheckYourAnswersController.onPageLoad()
    case page: SupplierBusinessOrIndividualPage =>
      (userAnswers, _) =>
        userAnswers.get(page) match {
          case Some(BusinessOrPrivateIndividual.Business) =>
            supplierdetails.routes.SupplierBusinessNameController.onPageLoad(page.supplierNumber, NormalMode)
          case Some(BusinessOrPrivateIndividual.PrivateIndividual) =>
            supplierdetails.routes.SupplierNameController.onPageLoad(page.supplierNumber, NormalMode)
          case _ =>
            routes.JourneyRecoveryController.onPageLoad()
        }
    case page: SupplierNamePage =>
      (_, _) =>
        supplieraddress.routes.IsSupplierAddressInTheUKController
          .onPageLoad(page.supplierNumber, NormalMode)
    case page: SupplierBusinessNamePage =>
      (_, _) =>
        supplieraddress.routes.IsSupplierAddressInTheUKController
          .onPageLoad(page.supplierNumber, NormalMode)
    case IsPurchaserAddressInTheUkPage =>
      (userAnswers, _) =>
        userAnswers.get(IsPurchaserAddressInTheUkPage) match {
          case Some(true)  => routes.LandingPageController.onPageLoad() // TODO: navigate to APA2.0 when built
          case Some(false) => routes.LandingPageController.onPageLoad() // TODO: navigate to APA1.2 when built
          case _           => routes.JourneyRecoveryController.onPageLoad()
        }
    case page: IsSupplierVatRegisteredPage =>
      (userAnswers, _) =>
        userAnswers.get(page) match {
          case Some(true) =>
            supplierdetails.routes.SupplierVatRegistrationDetailsController
              .onPageLoad(page.supplierNumber, NormalMode)
          case Some(false) =>
            routes.LandingPageController.onPageLoad() // TODO: navigate to AVD-S9 when built (page variant depends on if individual or organisation)
          case _ => routes.JourneyRecoveryController.onPageLoad()
        }
    case page: SupplierVatRegistrationNumberPage =>
      (userAnswers, _) =>
        userAnswers.get(page) match {
          case Some(vatNumberDetails) =>
            routes.LandingPageController.onPageLoad() // TODO: navigate to AVD-S9 when built (page variant depends on if individual or organisation)
          case _ => routes.JourneyRecoveryController.onPageLoad()
        }
    case page: VehicleDatesPage =>
      (userAnswers, _) =>
        userAnswers.get(page) match {
          case Some(dates) if dates.contains(VehicleDates.PurchaseInvoiceDate) =>
            routes.LandingPageController.onPageLoad() // TODO: navigate to AVD4.0 when built
          case Some(dates) if dates.contains(VehicleDates.AvailabilityAndFirstRegistration) =>
            routes.LandingPageController.onPageLoad() // TODO: navigate to AVD5.0 when built
          case Some(dates) if dates.contains(VehicleDates.NoDates) =>
            routes.LandingPageController.onPageLoad() // TODO: navigate to AVD3.1 when built
          case _ => routes.JourneyRecoveryController.onPageLoad()
        }
    case _ => (_, _) => routes.LandingPageController.onPageLoad()
  }

  private val checkRouteMap: Page => (UserAnswers, NovaUserType) => Call = {
    case VehicleFromEuPage =>
      (userAnswers, userType) =>
        userType match {
          case NovaUserType.PrivateIndividual | NovaUserType.NonVatOrganisation =>
            userAnswers.get(VehicleFromEuPage) match {
              case Some(false) => initialquestions.routes.VehicleOutsideEUController.onPageLoad()
              case Some(true)  => initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
              case _           => routes.JourneyRecoveryController.onPageLoad()
            }
          case _ => initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
        }
    case VehicleBusinessUsePage | AgentClientVehicleBusinessUsePage | BusinessOrPrivatePage | PurchaserBusinessOrIndividualPage =>
      (_, _) => initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
    case PurchaserOrOnBehalfPage =>
      (userAnswers, _) =>
        userAnswers.get(PurchaserOrOnBehalfPage) match {
          case Some(PurchaserOrOnBehalf.Purchaser)           => initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
          case Some(PurchaserOrOnBehalf.OnBehalfOfPurchaser) => initialquestions.routes.PurchaserBusinessOrIndividualController.onPageLoad(CheckMode)
          case _                                             => routes.JourneyRecoveryController.onPageLoad()
        }
    case NameDetailsPage =>
      (_, _) => notifierdetails.routes.YourDetailsCheckYourAnswersController.onPageLoad()
    case BusinessNamePage =>
      (_, _) => notifierdetails.routes.YourDetailsCheckYourAnswersController.onPageLoad()
    case PurchaserNamePage =>
      (_, _) => purchaserdetails.routes.PurchaserDetailsCheckYourAnswersController.onPageLoad()
    case PhoneNumberPage =>
      (_, _) => notifierdetails.routes.YourDetailsCheckYourAnswersController.onPageLoad()
    case EmailAddressPage =>
      (_, _) => notifierdetails.routes.YourDetailsCheckYourAnswersController.onPageLoad()
    case PurchaserBusinessNamePage =>
      (_, _) => purchaserdetails.routes.PurchaserDetailsCheckYourAnswersController.onPageLoad()
    case _: SupplierNamePage | _: IsSupplierAddressInTheUkPage | _: IsSupplierVatRegisteredPage | _: SupplierBusinessNamePage |
        _: SupplierBusinessOrIndividualPage | _: SupplierVatRegistrationNumberPage =>
      (_, _) => routes.LandingPageController.onPageLoad() // TODO: navigate to AVD-S9.0 CYA when built
    case _: VehicleDatesPage =>
      (_, _) => routes.LandingPageController.onPageLoad() // TODO: navigate to the vehicle details CYA when built
    case _ =>
      (_, _) => routes.LandingPageController.onPageLoad()
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, userType: NovaUserType): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers, userType)
    case CheckMode =>
      checkRouteMap(page)(userAnswers, userType)
  }
}
