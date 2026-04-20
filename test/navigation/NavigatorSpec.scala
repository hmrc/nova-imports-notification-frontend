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

import base.SpecBase
import controllers.routes
import pages.*
import models.*

class NavigatorSpec extends SpecBase {

  val navigator                = new Navigator
  val userAnswers: UserAnswers = UserAnswers("id")

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, userAnswers, NovaUserType.PrivateIndividual) mustBe routes.IndexController.onPageLoad()
      }

      "for a PrivateIndividual" - {

        "must go from VehicleFromEuPage to BusinessPrivateController when Yes is selected" in {
          val ua = userAnswers.set(VehicleFromEuPage, true).success.value
          navigator.nextPage(VehicleFromEuPage, NormalMode, ua, NovaUserType.PrivateIndividual) mustBe routes.BusinessPrivateController.onPageLoad(
            NormalMode
          )
        }

        "must go from VehicleFromEuPage to VehicleOutsideEUController when No is selected" in {
          val ua = userAnswers.set(VehicleFromEuPage, false).success.value
          navigator.nextPage(VehicleFromEuPage, NormalMode, ua, NovaUserType.PrivateIndividual) mustBe routes.VehicleOutsideEUController.onPageLoad()
        }

        "must go from VehicleFromEuPage to JourneyRecovery when no answer is found" in {
          navigator.nextPage(VehicleFromEuPage, NormalMode, userAnswers, NovaUserType.PrivateIndividual) mustBe routes.JourneyRecoveryController
            .onPageLoad()
        }
      }

      "for a VatRegisteredOrganisation" - {

        "must go from VehicleFromEuPage to VehicleBusinessUseController when Yes is selected" in {
          val ua = userAnswers.set(VehicleFromEuPage, true).success.value
          navigator.nextPage(VehicleFromEuPage, NormalMode, ua, NovaUserType.VatRegisteredOrganisation) mustBe routes.VehicleBusinessUseController
            .onPageLoad(NormalMode)
        }

        "must go from VehicleFromEuPage to VehicleBusinessUseController when No is selected" in {
          val ua = userAnswers.set(VehicleFromEuPage, false).success.value
          navigator.nextPage(VehicleFromEuPage, NormalMode, ua, NovaUserType.VatRegisteredOrganisation) mustBe routes.VehicleBusinessUseController
            .onPageLoad(NormalMode)
        }

        "must go from VehicleFromEuPage to JourneyRecovery when no answer is found" in {
          navigator.nextPage(
            VehicleFromEuPage,
            NormalMode,
            userAnswers,
            NovaUserType.VatRegisteredOrganisation
          ) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }

      "for a NonVatOrganisation" - {

        "must go from VehicleFromEuPage to BusinessPrivateController when Yes is selected" in {
          val ua = userAnswers.set(VehicleFromEuPage, true).success.value
          navigator.nextPage(VehicleFromEuPage, NormalMode, ua, NovaUserType.NonVatOrganisation) mustBe routes.BusinessPrivateController.onPageLoad(
            NormalMode
          )
        }

        "must go from VehicleFromEuPage to VehicleOutsideEUController when No is selected" in {
          val ua = userAnswers.set(VehicleFromEuPage, false).success.value
          navigator.nextPage(VehicleFromEuPage, NormalMode, ua, NovaUserType.NonVatOrganisation) mustBe routes.VehicleOutsideEUController
            .onPageLoad()
        }
      }

      "must go from VehicleBusinessUsePage to IndexController" in {
        navigator.nextPage(VehicleBusinessUsePage, NormalMode, userAnswers, NovaUserType.VatRegisteredOrganisation) mustBe routes.IndexController
          .onPageLoad()
      }

      "must go from BusinessPrivatePage to PurchaserOrOnBehalfController" in {
        navigator.nextPage(BusinessPrivatePage, NormalMode, userAnswers, NovaUserType.PrivateIndividual) mustBe routes.PurchaserOrOnBehalfController
          .onPageLoad(NormalMode)
      }

      "must go from PurchaserOrOnBehalfPage to IndexController when Purchaser is selected" in {
        val ua = userAnswers.set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser).success.value
        navigator.nextPage(PurchaserOrOnBehalfPage, NormalMode, ua, NovaUserType.PrivateIndividual) mustBe routes.IndexController.onPageLoad()
      }

      "must go from PurchaserOrOnBehalfPage to PurchaserBusinessOrIndividualController when OnBehalfOfPurchaser is selected" in {
        val ua = userAnswers.set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser).success.value
        navigator.nextPage(
          PurchaserOrOnBehalfPage,
          NormalMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe routes.PurchaserBusinessOrIndividualController.onPageLoad(NormalMode)
      }

      "must go from PurchaserOrOnBehalfPage to JourneyRecovery when no answer is found" in {
        navigator.nextPage(PurchaserOrOnBehalfPage, NormalMode, userAnswers, NovaUserType.PrivateIndividual) mustBe routes.JourneyRecoveryController
          .onPageLoad()
      }

    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, userAnswers, NovaUserType.PrivateIndividual) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from VehicleFromEuPage to CheckYourAnswers" in {
        val ua = userAnswers.set(VehicleFromEuPage, true).success.value
        navigator.nextPage(VehicleFromEuPage, CheckMode, ua, NovaUserType.PrivateIndividual) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from BusinessPrivatePage to CheckYourAnswers" in {
        navigator.nextPage(BusinessPrivatePage, CheckMode, userAnswers, NovaUserType.PrivateIndividual) mustBe routes.CheckYourAnswersController
          .onPageLoad()
      }

      "must go from PurchaserOrOnBehalfPage to CheckYourAnswers" in {
        navigator.nextPage(
          PurchaserOrOnBehalfPage,
          CheckMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from VehicleBusinessUsePage to CheckYourAnswers" in {
        navigator.nextPage(
          VehicleBusinessUsePage,
          CheckMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.CheckYourAnswersController.onPageLoad()
      }
    }
  }
}
