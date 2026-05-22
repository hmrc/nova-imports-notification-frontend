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
        navigator.nextPage(UnknownPage, NormalMode, userAnswers, NovaUserType.PrivateIndividual) mustBe routes.LandingPageController.onPageLoad()
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

        "must go from AboutYourDetailsPage to correct next screen when OQ1.0 was answered yes" in {
          // TODO: update to navigate to AYD1.2 when implemented
          val ua = userAnswers.set(VehicleBusinessUsePage, true).success.value
          navigator.nextPage(AboutYourDetailsPage, NormalMode, ua, NovaUserType.VatRegisteredOrganisation) mustBe routes.LandingPageController
            .onPageLoad()
        }

        "must go from AboutYourDetailsPage to correct next screen when OQ1.0 was answered no" in {
          // TODO: update to navigate to AYD1.1 when implemented
          val ua = userAnswers.set(VehicleBusinessUsePage, false).success.value
          navigator.nextPage(AboutYourDetailsPage, NormalMode, ua, NovaUserType.VatRegisteredOrganisation) mustBe routes.LandingPageController
            .onPageLoad()
        }

        "must go from AboutYourDetailsPage to JourneyRecovery when OQ1.0 has not been answered" in {
          navigator.nextPage(
            AboutYourDetailsPage,
            NormalMode,
            userAnswers,
            NovaUserType.VatRegisteredOrganisation
          ) mustBe routes.JourneyRecoveryController
            .onPageLoad()
        }
      }

      "for an Agent with a selected client" - {

        val sampleClient = AgentSelectedClient(vrn = "GB123456789", name = Some("Acme Ltd"))

        val answersWithClient = userAnswers.set(AgentSelectedClientPage, sampleClient).success.value

        "must go from VehicleFromEuPage to AgentVehicleBusinessUseController when Yes is selected" in {
          val ua = answersWithClient.set(VehicleFromEuPage, true).success.value
          navigator.nextPage(VehicleFromEuPage, NormalMode, ua, NovaUserType.Agent) mustBe routes.AgentVehicleBusinessUseController
            .onPageLoad(NormalMode)
        }

        "must go from VehicleFromEuPage to AgentVehicleBusinessUseController when No is selected" in {
          val ua = answersWithClient.set(VehicleFromEuPage, false).success.value
          navigator.nextPage(VehicleFromEuPage, NormalMode, ua, NovaUserType.Agent) mustBe routes.AgentVehicleBusinessUseController
            .onPageLoad(NormalMode)
        }

        "must go from VehicleFromEuPage to JourneyRecovery when no answer is found" in {
          navigator.nextPage(VehicleFromEuPage, NormalMode, answersWithClient, NovaUserType.Agent) mustBe routes.JourneyRecoveryController
            .onPageLoad()
        }
      }

      "for an Agent without a selected client" - {

        "must go from VehicleFromEuPage to BusinessPrivateController when Yes is selected" in {
          val ua = userAnswers.set(VehicleFromEuPage, true).success.value
          navigator.nextPage(VehicleFromEuPage, NormalMode, ua, NovaUserType.Agent) mustBe routes.BusinessPrivateController.onPageLoad(NormalMode)
        }

        "must go from VehicleFromEuPage to VehicleOutsideEUController when No is selected" in {
          val ua = userAnswers.set(VehicleFromEuPage, false).success.value
          navigator.nextPage(VehicleFromEuPage, NormalMode, ua, NovaUserType.Agent) mustBe routes.VehicleOutsideEUController.onPageLoad()
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

      "must go from VehicleBusinessUsePage OQ1.0 to InitialQuestionsCheckYourAnswersController" in {
        navigator.nextPage(
          VehicleBusinessUsePage,
          NormalMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from AgentVehicleBusinessUsePage AQ1.0 to InitialQuestionsCheckYourAnswersController" in {
        navigator.nextPage(
          AgentVehicleBusinessUsePage,
          NormalMode,
          userAnswers,
          NovaUserType.Agent
        ) mustBe routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from BusinessPrivatePage IQ2.0 to PurchaserOrOnBehalfController" in {
        navigator.nextPage(BusinessPrivatePage, NormalMode, userAnswers, NovaUserType.PrivateIndividual) mustBe routes.PurchaserOrOnBehalfController
          .onPageLoad(NormalMode)
      }

      "must go from PurchaserOrOnBehalfPage IQ3.0 to InitialQuestionsCheckYourAnswersController when Purchaser is selected" in {
        val ua = userAnswers.set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser).success.value
        navigator.nextPage(
          PurchaserOrOnBehalfPage,
          NormalMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
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

      "must go from IsYourAddressInTheUkPage to the next screen when Yes is selected" in {
        // TODO: update to navigate to UK address-lookup-service when implemented
        val ua = userAnswers.set(IsYourAddressInTheUkPage, true).success.value
        navigator.nextPage(IsYourAddressInTheUkPage, NormalMode, ua, NovaUserType.PrivateIndividual) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from IsYourAddressInTheUkPage to the next screen when No is selected" in {
        // TODO: update to navigate to AYA1.1 when implemented
        val ua = userAnswers.set(IsYourAddressInTheUkPage, false).success.value
        navigator.nextPage(IsYourAddressInTheUkPage, NormalMode, ua, NovaUserType.PrivateIndividual) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from IsYourAddressInTheUkPage to JourneyRecovery when no answer is found" in {
        navigator.nextPage(IsYourAddressInTheUkPage, NormalMode, userAnswers, NovaUserType.PrivateIndividual) mustBe routes.JourneyRecoveryController
          .onPageLoad()
      }

    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to LandingPageController" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, userAnswers, NovaUserType.PrivateIndividual) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from VehicleFromEuPage to InitialQuestionsCheckYourAnswersController" in {
        navigator.nextPage(
          VehicleFromEuPage,
          CheckMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe routes.InitialQuestionsCheckYourAnswersController
          .onPageLoad()
      }

      "must go from BusinessPrivatePage to InitialQuestionsCheckYourAnswers" in {
        navigator.nextPage(
          BusinessPrivatePage,
          CheckMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from PurchaserOrOnBehalfPage IQ3.0 to InitialQuestionsCheckYourAnswersController when Purchaser is selected" in {
        val ua = userAnswers.set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.Purchaser).success.value
        navigator.nextPage(
          PurchaserOrOnBehalfPage,
          CheckMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from PurchaserOrOnBehalfPage IQ3.0 to PurchaserBusinessOrIndividualController IQ3.1 in CheckMode when OnBehalfOfPurchaser is selected" in {
        val ua = userAnswers.set(PurchaserOrOnBehalfPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser).success.value
        navigator.nextPage(
          PurchaserOrOnBehalfPage,
          CheckMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe routes.PurchaserBusinessOrIndividualController.onPageLoad(CheckMode)
      }

      "must go from PurchaserOrOnBehalfPage IQ3.0 to JourneyRecovery when no answer is found" in {
        navigator.nextPage(
          PurchaserOrOnBehalfPage,
          CheckMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from PurchaserBusinessOrIndividualPage IQ3.1 to InitialQuestionsCheckYourAnswersController" in {
        navigator.nextPage(
          PurchaserBusinessOrIndividualPage,
          CheckMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from VehicleBusinessUsePage to InitialQuestionsCheckYourAnswersController" in {
        navigator.nextPage(
          VehicleBusinessUsePage,
          CheckMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from AgentVehicleBusinessUsePage AQ1.0 to InitialQuestionsCheckYourAnswers" in {
        navigator.nextPage(
          AgentVehicleBusinessUsePage,
          CheckMode,
          userAnswers,
          NovaUserType.Agent
        ) mustBe routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from IsYourAddressInTheUkPage to LandingPageController" in {
        // TODO: route to AYA check your answers when implemented
        val ua = userAnswers.set(IsYourAddressInTheUkPage, true).success.value
        navigator.nextPage(IsYourAddressInTheUkPage, CheckMode, ua, NovaUserType.PrivateIndividual) mustBe routes.LandingPageController.onPageLoad()
      }
    }
  }
}
