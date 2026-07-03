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
import pages.sections.initialquestions.{BusinessOrPrivatePage, PurchaserBusinessOrIndividualPage, PurchaserOrOnBehalfPage, VehicleBusinessUsePage, VehicleFromEuPage}
import pages.sections.notifierDetails.{EmailAddressPage, NameDetailsPage, PhoneNumberPage}
import pages.sections.purchaserDetails.PurchaserNamePage

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

        "must go from AboutYourDetailsPage to PhoneNumberController (AYD1.2) when OQ1.0 was answered yes" in {
          val ua = userAnswers.set(VehicleBusinessUsePage, true).success.value
          navigator.nextPage(AboutYourDetailsPage, NormalMode, ua, NovaUserType.VatRegisteredOrganisation) mustBe routes.PhoneNumberController
            .onPageLoad(NormalMode)
        }

        "must go from AboutYourDetailsPage to correct next screen AddYourNamePage when OQ1.0 was answered no" in {
          val ua = userAnswers.set(VehicleBusinessUsePage, false).success.value
          navigator.nextPage(AboutYourDetailsPage, NormalMode, ua, NovaUserType.VatRegisteredOrganisation) mustBe routes.AddYourNameController
            .onPageLoad(NormalMode)
        }

        "must go from AddYourNamePage to PhoneNumberController (AYD1.2)" in {
          val ua = userAnswers.set(NameDetailsPage, NameDetails("Mr", "John", "Smith")).success.value
          navigator.nextPage(NameDetailsPage, NormalMode, ua, NovaUserType.VatRegisteredOrganisation) mustBe routes.PhoneNumberController
            .onPageLoad(NormalMode)
        }

        "must go from PurchaserNamePage (APD1.0) to LandingPageController until CYA4.0 is built" in {
          val ua = userAnswers.set(PurchaserNamePage, NameDetails("Mr", "John", "Smith")).success.value
          navigator.nextPage(PurchaserNamePage, NormalMode, ua, NovaUserType.PrivateIndividual) mustBe routes.LandingPageController
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
          AgentClientVehicleBusinessUsePage,
          NormalMode,
          userAnswers,
          NovaUserType.Agent
        ) mustBe routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from BusinessPrivatePage IQ2.0 to PurchaserOrOnBehalfController" in {
        navigator.nextPage(BusinessOrPrivatePage, NormalMode, userAnswers, NovaUserType.PrivateIndividual) mustBe routes.PurchaserOrOnBehalfController
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

      "must go from PhoneNumberPage to EmailAddressController (AYD1.3)" in {
        val ua = userAnswers.set(PhoneNumberPage, ContactNumbers(Some("01632 960 001"), None)).success.value
        navigator.nextPage(PhoneNumberPage, NormalMode, ua, NovaUserType.VatRegisteredOrganisation) mustBe routes.EmailAddressController
          .onPageLoad(NormalMode)
      }

      "must go from EmailAddressPage to CYA2.0 - YourDetails check your answers" in {
        navigator.nextPage(
          EmailAddressPage,
          NormalMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe routes.YourDetailsCheckYourAnswersController
          .onPageLoad()
      }

      "must go from AddVehicleDetailsPage AVD1.0 to LandingPage when BySupplier is selected" in {
        // TODO: navigate to AVD-S1.0 when implemented
        val ua = userAnswers.set(AddVehicleDetailsPage, AddVehicleDetails.BySupplier).success.value
        navigator.nextPage(
          AddVehicleDetailsPage,
          NormalMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from AddVehicleDetailsPage AVD1.0 to LandingPage when BySpreadsheet is selected" in {
        // TODO: navigate to spreadsheet upload flow when implemented
        val ua = userAnswers.set(AddVehicleDetailsPage, AddVehicleDetails.BySpreadsheet).success.value
        navigator.nextPage(
          AddVehicleDetailsPage,
          NormalMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from AddVehicleDetailsPage AVD1.0 to JourneyRecovery when no answer is found" in {
        navigator.nextPage(
          AddVehicleDetailsPage,
          NormalMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to LandingPageController" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, userAnswers, NovaUserType.PrivateIndividual) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from VehicleFromEuPage to InitialQuestionsCheckYourAnswersController if agent" in {
        navigator.nextPage(
          VehicleFromEuPage,
          CheckMode,
          userAnswers,
          NovaUserType.Agent
        ) mustBe routes.InitialQuestionsCheckYourAnswersController
          .onPageLoad()
      }

      "must go from VehicleFromEuPage to VehicleOutsideEUController when No is selected for PrivateIndividual" in {
        val ua = userAnswers.set(VehicleFromEuPage, false).success.value
        navigator.nextPage(
          VehicleFromEuPage,
          CheckMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe routes.VehicleOutsideEUController.onPageLoad()
      }

      "must go from VehicleFromEuPage to VehicleOutsideEUController when No is selected for NonVatOrganisation" in {
        val ua = userAnswers.set(VehicleFromEuPage, false).success.value
        navigator.nextPage(
          VehicleFromEuPage,
          CheckMode,
          ua,
          NovaUserType.NonVatOrganisation
        ) mustBe routes.VehicleOutsideEUController.onPageLoad()
      }

      "must go from BusinessPrivatePage to InitialQuestionsCheckYourAnswers" in {
        navigator.nextPage(
          BusinessOrPrivatePage,
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
          AgentClientVehicleBusinessUsePage,
          CheckMode,
          userAnswers,
          NovaUserType.Agent
        ) mustBe routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from AddYourNamePage to YourDetailsCheckYourAnswersController in CheckMode" in {
        val ua = userAnswers.set(NameDetailsPage, NameDetails("Mr", "John", "Smith")).success.value
        navigator.nextPage(NameDetailsPage, CheckMode, ua, NovaUserType.VatRegisteredOrganisation) mustBe routes.YourDetailsCheckYourAnswersController
          .onPageLoad()
      }

      "must go from PurchaserNamePage (APD1.0) to LandingPageController in CheckMode until CYA4.0 is built" in {
        val ua = userAnswers.set(PurchaserNamePage, NameDetails("Mr", "John", "Smith")).success.value
        navigator.nextPage(PurchaserNamePage, CheckMode, ua, NovaUserType.PrivateIndividual) mustBe routes.LandingPageController
          .onPageLoad()
      }

      "must go from EmailAddressPage to YourDetailsCheckYourAnswersController" in {
        navigator.nextPage(
          EmailAddressPage,
          CheckMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe routes.YourDetailsCheckYourAnswersController
          .onPageLoad()
      }

      "must go from PhoneNumberPage to YourDetailsCheckYourAnswersController in CheckMode" in {
        val ua = userAnswers.set(PhoneNumberPage, ContactNumbers(Some("01632 960 001"), None)).success.value
        navigator.nextPage(PhoneNumberPage, CheckMode, ua, NovaUserType.VatRegisteredOrganisation) mustBe routes.YourDetailsCheckYourAnswersController
          .onPageLoad()
      }
    }
  }
}
