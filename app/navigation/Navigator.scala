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
          case _ =>
            userAnswers.get(VehicleFromEuPage) match {
              case Some(true)  => routes.BusinessPrivateController.onPageLoad(NormalMode)
              case Some(false) => routes.VehicleOutsideEUController.onPageLoad()
              case _           => routes.JourneyRecoveryController.onPageLoad()
            }
        }
    case VehicleBusinessUsePage =>
      (_, _) => routes.IndexController.onPageLoad() // TODO: navigate to next page - to be added later
    case PurchaserOrOnBehalfPage =>
      (userAnswers, _) =>
        userAnswers.get(PurchaserOrOnBehalfPage) match {
          case Some(PurchaserOrOnBehalf.Purchaser)           => routes.IndexController.onPageLoad() // TODO: navigate to SS1 - to be added later
          case Some(PurchaserOrOnBehalf.OnBehalfOfPurchaser) => routes.PurchaserBusinessOrIndividualController.onPageLoad(NormalMode)
          case _                                             => routes.JourneyRecoveryController.onPageLoad()
        }
    case BusinessPrivatePage =>
      (_, _) => routes.PurchaserOrOnBehalfController.onPageLoad(NormalMode)
    case PurchaserBusinessOrIndividualPage =>
      (_, _) => routes.IndexController.onPageLoad() // TODO: navigate to SS2 - to be added later
    case _ => (_, _) => routes.IndexController.onPageLoad()
  }

  private val checkRouteMap: Page => (UserAnswers, NovaUserType) => Call = { case _ =>
    (_, _) => routes.CheckYourAnswersController.onPageLoad()
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, userType: NovaUserType): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers, userType)
    case CheckMode =>
      checkRouteMap(page)(userAnswers, userType)
  }
}
