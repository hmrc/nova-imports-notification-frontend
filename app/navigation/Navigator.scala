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
import controllers.routes
import pages.*
import models.*
import pages.sections.initialquestions.{BusinessOrPrivatePage, PurchaserBusinessOrIndividualPage, PurchaserOrOnBehalfPage, VehicleBusinessUsePage, VehicleFromEuPage}
import pages.sections.notifierDetails.{EmailAddressPage, NameDetailsPage, PhoneNumberPage}

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: Page => (UserAnswers, NovaUserType) => Call = {
    case VehicleFromEuPage =>
      (userAnswers, userType) =>
        userType match {
          case NovaUserType.VatRegisteredOrganisation =>
            userAnswers.get(VehicleFromEuPage) match {
              case Some(_) => routes.VehicleBusinessUseController.onPageLoad(NormalMode)
              case _       => routes.JourneyRecoveryController.onPageLoad()
            }
          case NovaUserType.Agent if userAnswers.get(AgentSelectedClientPage).isDefined =>
            userAnswers.get(VehicleFromEuPage) match {
              case Some(_) => routes.AgentVehicleBusinessUseController.onPageLoad(NormalMode)
              case _       => routes.JourneyRecoveryController.onPageLoad()
            }
          case _ =>
            userAnswers.get(VehicleFromEuPage) match {
              case Some(true)  => routes.BusinessPrivateController.onPageLoad(NormalMode)
              case Some(false) => routes.VehicleOutsideEUController.onPageLoad()
              case _           => routes.JourneyRecoveryController.onPageLoad()
            }
        }
    case AboutYourDetailsPage =>
      (userAnswers, _) =>
        userAnswers.get(VehicleBusinessUsePage) match {
          case Some(true)  => routes.PhoneNumberController.onPageLoad(NormalMode)
          case Some(false) => routes.AddYourNameController.onPageLoad(NormalMode)
          case _           => routes.JourneyRecoveryController.onPageLoad()
        }
    case NameDetailsPage =>
      (_, _) => routes.PhoneNumberController.onPageLoad(NormalMode)
    case PhoneNumberPage =>
      (_, _) => routes.EmailAddressController.onPageLoad(NormalMode)
    case VehicleBusinessUsePage =>
      (_, _) => routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
    case AgentClientVehicleBusinessUsePage =>
      (_, _) => routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
    case PurchaserOrOnBehalfPage =>
      (userAnswers, _) =>
        userAnswers.get(PurchaserOrOnBehalfPage) match {
          case Some(PurchaserOrOnBehalf.Purchaser)           => routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
          case Some(PurchaserOrOnBehalf.OnBehalfOfPurchaser) => routes.PurchaserBusinessOrIndividualController.onPageLoad(NormalMode)
          case _                                             => routes.JourneyRecoveryController.onPageLoad()
        }
    case BusinessOrPrivatePage =>
      (_, _) => routes.PurchaserOrOnBehalfController.onPageLoad(NormalMode)
    case PurchaserBusinessOrIndividualPage =>
      (_, _) => routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
    case IsYourAddressInTheUkPage =>
      (userAnswers, _) =>
        userAnswers.get(IsYourAddressInTheUkPage) match {
          case Some(true)  => routes.LandingPageController.onPageLoad() // TODO: navigate to address-lookup-service - to be added later
          case Some(false) => routes.LandingPageController.onPageLoad() // TODO: navigate to AYA1.1 - to be added later
          case _           => routes.JourneyRecoveryController.onPageLoad()
        }
    case EmailAddressPage =>
      (_, _) => routes.LandingPageController.onPageLoad() // TODO: navigate to CYA2.0
    case _ => (_, _) => routes.LandingPageController.onPageLoad()
  }

  private val checkRouteMap: Page => (UserAnswers, NovaUserType) => Call = {
    case VehicleFromEuPage =>
      (userAnswers, userType) =>
        userType match {
          case NovaUserType.PrivateIndividual | NovaUserType.NonVatOrganisation =>
            userAnswers.get(VehicleFromEuPage) match {
              case Some(false) => routes.VehicleOutsideEUController.onPageLoad()
              case Some(true)  => routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
              case _           => routes.JourneyRecoveryController.onPageLoad()
            }
          case _ => routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
        }
    case VehicleBusinessUsePage | AgentClientVehicleBusinessUsePage | BusinessOrPrivatePage | PurchaserBusinessOrIndividualPage =>
      (_, _) => routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
    case PhoneNumberPage =>
      (_, _) => routes.EmailAddressController.onPageLoad(CheckMode)
    case PurchaserOrOnBehalfPage =>
      (userAnswers, _) =>
        userAnswers.get(PurchaserOrOnBehalfPage) match {
          case Some(PurchaserOrOnBehalf.Purchaser)           => routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
          case Some(PurchaserOrOnBehalf.OnBehalfOfPurchaser) => routes.PurchaserBusinessOrIndividualController.onPageLoad(CheckMode)
          case _                                             => routes.JourneyRecoveryController.onPageLoad()
        }
    case NameDetailsPage =>
      (_, _) => routes.LandingPageController.onPageLoad() // TODO: navigate to CYA2.0 - to be added later
    case EmailAddressPage =>
      (_, _) => routes.LandingPageController.onPageLoad() // TODO: navigate to CYA2.0
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
