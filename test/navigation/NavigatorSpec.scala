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
import controllers.{initialquestions, notifierdetails, purchaserdetails, routes, supplieraddress, supplierdetails, vehicledetails}
import pages.*
import models.*
import pages.sections.initialquestions.{AgentClientVehicleBusinessUsePage, BusinessOrPrivatePage, NotifyingAsPurchaserPage, PurchaserBusinessOrIndividualPage, VehicleBusinessUsePage, VehicleFromEuPage}
import pages.sections.notifierdetails.{AboutYourDetailsPage, BusinessNamePage, EmailAddressPage, NameDetailsPage, PhoneNumberPage}
import pages.sections.vehicledetails.{AddImportVehicleDetailsPage, AddVehicleDetailsPage}
import pages.sections.purchaserdetails.{PurchaserBusinessNamePage, PurchaserNamePage}
import pages.sections.supplierdetails.{IsSupplierVatRegisteredPage, SupplierBusinessNamePage, SupplierBusinessOrIndividualPage, SupplierNamePage, SupplierVatRegistrationNumberPage, UsePersonalDetailsAsSupplierPage, UsePurchaserDetailsAsSupplierPage}
import pages.sections.purchaseraddress.IsPurchaserAddressInTheUkPage
import pages.sections.vehicledetails.{PurchaseInvoiceDatePage, VehicleDatesPage}

import java.time.LocalDate

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
          navigator.nextPage(
            VehicleFromEuPage,
            NormalMode,
            ua,
            NovaUserType.PrivateIndividual
          ) mustBe initialquestions.routes.BusinessPrivateController.onPageLoad(
            NormalMode
          )
        }

        "must go from VehicleFromEuPage to VehicleOutsideEUController when No is selected" in {
          val ua = userAnswers.set(VehicleFromEuPage, false).success.value
          navigator.nextPage(
            VehicleFromEuPage,
            NormalMode,
            ua,
            NovaUserType.PrivateIndividual
          ) mustBe initialquestions.routes.VehicleOutsideEUController.onPageLoad()
        }

        "must go from VehicleFromEuPage to JourneyRecovery when no answer is found" in {
          navigator.nextPage(VehicleFromEuPage, NormalMode, userAnswers, NovaUserType.PrivateIndividual) mustBe routes.JourneyRecoveryController
            .onPageLoad()
        }
      }

      "for a VatRegisteredOrganisation" - {

        "must go from VehicleFromEuPage to VehicleBusinessUseController when Yes is selected" in {
          val ua = userAnswers.set(VehicleFromEuPage, true).success.value
          navigator.nextPage(
            VehicleFromEuPage,
            NormalMode,
            ua,
            NovaUserType.VatRegisteredOrganisation
          ) mustBe initialquestions.routes.VehicleBusinessUseController
            .onPageLoad(NormalMode)
        }

        "must go from VehicleFromEuPage to VehicleBusinessUseController when No is selected" in {
          val ua = userAnswers.set(VehicleFromEuPage, false).success.value
          navigator.nextPage(
            VehicleFromEuPage,
            NormalMode,
            ua,
            NovaUserType.VatRegisteredOrganisation
          ) mustBe initialquestions.routes.VehicleBusinessUseController
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
          navigator.nextPage(
            AboutYourDetailsPage,
            NormalMode,
            ua,
            NovaUserType.VatRegisteredOrganisation
          ) mustBe notifierdetails.routes.PhoneNumberController
            .onPageLoad(NormalMode)
        }

        "must go from AboutYourDetailsPage to correct next screen AddYourNamePage when OQ1.0 was answered no" in {
          val ua = userAnswers.set(VehicleBusinessUsePage, false).success.value
          navigator.nextPage(
            AboutYourDetailsPage,
            NormalMode,
            ua,
            NovaUserType.VatRegisteredOrganisation
          ) mustBe notifierdetails.routes.AddYourNameController
            .onPageLoad(NormalMode)
        }

        "must go from AddYourNamePage to PhoneNumberController (AYD1.2)" in {
          val ua = userAnswers.set(NameDetailsPage, NameDetails("Mr", "John", "Smith")).success.value
          navigator.nextPage(
            NameDetailsPage,
            NormalMode,
            ua,
            NovaUserType.VatRegisteredOrganisation
          ) mustBe notifierdetails.routes.PhoneNumberController
            .onPageLoad(NormalMode)
        }

        "must go from BusinessNamePage (AYD1.4) to PhoneNumberController (AYD1.2)" in {
          val ua = userAnswers.set(BusinessNamePage, "Acme Trading Co Ltd").success.value
          navigator.nextPage(BusinessNamePage, NormalMode, ua, NovaUserType.NonVatOrganisation) mustBe notifierdetails.routes.PhoneNumberController
            .onPageLoad(NormalMode)
        }

        "must go from PurchaserNamePage (APD1.0) to the purchaser details check your answers page (CYA4.0)" in {
          val ua = userAnswers.set(PurchaserNamePage, NameDetails("Mr", "John", "Smith")).success.value
          navigator.nextPage(
            PurchaserNamePage,
            NormalMode,
            ua,
            NovaUserType.PrivateIndividual
          ) mustBe purchaserdetails.routes.PurchaserDetailsCheckYourAnswersController
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
          navigator.nextPage(VehicleFromEuPage, NormalMode, ua, NovaUserType.Agent) mustBe initialquestions.routes.AgentVehicleBusinessUseController
            .onPageLoad(NormalMode)
        }

        "must go from VehicleFromEuPage to AgentVehicleBusinessUseController when No is selected" in {
          val ua = answersWithClient.set(VehicleFromEuPage, false).success.value
          navigator.nextPage(VehicleFromEuPage, NormalMode, ua, NovaUserType.Agent) mustBe initialquestions.routes.AgentVehicleBusinessUseController
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
          navigator.nextPage(
            VehicleFromEuPage,
            NormalMode,
            ua,
            NovaUserType.Agent
          ) mustBe initialquestions.routes.BusinessPrivateController.onPageLoad(NormalMode)
        }

        "must go from VehicleFromEuPage to BusinessPrivateController when No is selected" in {
          val ua = userAnswers.set(VehicleFromEuPage, false).success.value
          navigator.nextPage(
            VehicleFromEuPage,
            NormalMode,
            ua,
            NovaUserType.Agent
          ) mustBe initialquestions.routes.BusinessPrivateController.onPageLoad(NormalMode)
        }
      }

      "for a NonVatOrganisation" - {

        "must go from VehicleFromEuPage to BusinessPrivateController when Yes is selected" in {
          val ua = userAnswers.set(VehicleFromEuPage, true).success.value
          navigator.nextPage(
            VehicleFromEuPage,
            NormalMode,
            ua,
            NovaUserType.NonVatOrganisation
          ) mustBe initialquestions.routes.BusinessPrivateController.onPageLoad(
            NormalMode
          )
        }

        "must go from VehicleFromEuPage to VehicleOutsideEUController when No is selected" in {
          val ua = userAnswers.set(VehicleFromEuPage, false).success.value
          navigator.nextPage(
            VehicleFromEuPage,
            NormalMode,
            ua,
            NovaUserType.NonVatOrganisation
          ) mustBe initialquestions.routes.VehicleOutsideEUController
            .onPageLoad()
        }
      }

      "must go from VehicleBusinessUsePage OQ1.0 to InitialQuestionsCheckYourAnswersController" in {
        navigator.nextPage(
          VehicleBusinessUsePage,
          NormalMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from AgentVehicleBusinessUsePage AQ1.0 to InitialQuestionsCheckYourAnswersController" in {
        navigator.nextPage(
          AgentClientVehicleBusinessUsePage,
          NormalMode,
          userAnswers,
          NovaUserType.Agent
        ) mustBe initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from BusinessPrivatePage IQ2.0 to PurchaserOrOnBehalfController" in {
        navigator.nextPage(
          BusinessOrPrivatePage,
          NormalMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe initialquestions.routes.PurchaserOrOnBehalfController
          .onPageLoad(NormalMode)
      }

      "must go from PurchaserOrOnBehalfPage IQ3.0 to InitialQuestionsCheckYourAnswersController when Purchaser is selected" in {
        val ua = userAnswers.set(NotifyingAsPurchaserPage, PurchaserOrOnBehalf.Purchaser).success.value
        navigator.nextPage(
          NotifyingAsPurchaserPage,
          NormalMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from PurchaserOrOnBehalfPage to PurchaserBusinessOrIndividualController when OnBehalfOfPurchaser is selected" in {
        val ua = userAnswers.set(NotifyingAsPurchaserPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser).success.value
        navigator.nextPage(
          NotifyingAsPurchaserPage,
          NormalMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe initialquestions.routes.PurchaserBusinessOrIndividualController.onPageLoad(NormalMode)
      }

      "must go from PurchaserOrOnBehalfPage to JourneyRecovery when no answer is found" in {
        navigator.nextPage(NotifyingAsPurchaserPage, NormalMode, userAnswers, NovaUserType.PrivateIndividual) mustBe routes.JourneyRecoveryController
          .onPageLoad()
      }

      "must go from PurchaserBusinessOrIndividualPage to InitialQuestionsCheckYourAnswersController" in {
        val ua = userAnswers.set(PurchaserBusinessOrIndividualPage, PurchaserBusinessOrIndividual.NonVatRegisteredBusiness).success.value
        navigator.nextPage(
          PurchaserBusinessOrIndividualPage,
          NormalMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from PhoneNumberPage to EmailAddressController (AYD1.3)" in {
        val ua = userAnswers.set(PhoneNumberPage, ContactNumbers(Some("01632 960 001"), None)).success.value
        navigator.nextPage(
          PhoneNumberPage,
          NormalMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe notifierdetails.routes.EmailAddressController
          .onPageLoad(NormalMode)
      }

      "must go from EmailAddressPage to CYA2.0 - YourDetails check your answers" in {
        navigator.nextPage(
          EmailAddressPage,
          NormalMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe notifierdetails.routes.YourDetailsCheckYourAnswersController
          .onPageLoad()
      }

      "must go from AddVehicleDetailsPage to the upload vehicle spreadsheet page when the user chose to upload a spreadsheet" in {
        val ua = userAnswers.set(AddVehicleDetailsPage, AddVehicleDetails.BySpreadsheet).success.value
        navigator.nextPage(
          AddVehicleDetailsPage,
          NormalMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe vehicledetails.routes.UploadVehicleSpreadsheetController.onPageLoad()
      }

      "must go from AddVehicleDetailsPage AVD1.0 to JourneyRecovery when no answer is found" in {
        navigator.nextPage(
          AddVehicleDetailsPage,
          NormalMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from AddImportVehicleDetailsPage AVD1.1 to LandingPage when ByImportEntryNumber is selected" in {
        // TODO: navigate to the import entry number flow when built
        val ua = userAnswers.set(AddImportVehicleDetailsPage, AddImportVehicleDetails.ByImportEntryNumber).success.value
        navigator.nextPage(
          AddImportVehicleDetailsPage,
          NormalMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from AddImportVehicleDetailsPage to the upload vehicle spreadsheet page when the user chose to upload a spreadsheet" in {
        val ua = userAnswers.set(AddImportVehicleDetailsPage, AddImportVehicleDetails.BySpreadsheet).success.value
        navigator.nextPage(
          AddImportVehicleDetailsPage,
          NormalMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe vehicledetails.routes.UploadVehicleSpreadsheetController.onPageLoad()
      }

      "must go from AddImportVehicleDetailsPage AVD1.1 to JourneyRecovery when no answer is found" in {
        navigator.nextPage(
          AddImportVehicleDetailsPage,
          NormalMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from UsePersonalDetailsAsSupplierPage AVD-S1.0 to LandingPage when Yes is selected" in {
        // TODO: navigate to CYA3.0 when implemented
        val ua = userAnswers.set(UsePersonalDetailsAsSupplierPage(SupplierNumber(1)), true).success.value
        navigator.nextPage(
          UsePersonalDetailsAsSupplierPage(SupplierNumber(1)),
          NormalMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from UsePersonalDetailsAsSupplierPage AVD-S1.0 to SupplierBusinessOrIndividual AVD-S2.0 when No is selected" in {
        val ua = userAnswers.set(UsePersonalDetailsAsSupplierPage(SupplierNumber(2)), false).success.value
        navigator.nextPage(
          UsePersonalDetailsAsSupplierPage(SupplierNumber(2)),
          NormalMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe supplierdetails.routes.SupplierBusinessOrIndividualController.onPageLoad(SupplierNumber(2), NormalMode)
      }

      "must go from UsePersonalDetailsAsSupplierPage AVD-S1.0 to JourneyRecovery when no answer is found" in {
        navigator.nextPage(
          UsePersonalDetailsAsSupplierPage(SupplierNumber(1)),
          NormalMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from UsePurchaserDetailsAsSupplierPage AVD-S1.1 to LandingPage when Yes is selected" in {
        // TODO: navigate to CYA3.0 when implemented
        val ua = userAnswers.set(UsePurchaserDetailsAsSupplierPage(SupplierNumber(1)), true).success.value
        navigator.nextPage(
          UsePurchaserDetailsAsSupplierPage(SupplierNumber(1)),
          NormalMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from UsePurchaserDetailsAsSupplierPage AVD-S1.1 to SupplierBusinessOrIndividual AVD-S2.0 when No is selected" in {
        val ua = userAnswers.set(UsePurchaserDetailsAsSupplierPage(SupplierNumber(2)), false).success.value
        navigator.nextPage(
          UsePurchaserDetailsAsSupplierPage(SupplierNumber(2)),
          NormalMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe supplierdetails.routes.SupplierBusinessOrIndividualController.onPageLoad(SupplierNumber(2), NormalMode)
      }

      "must go from UsePurchaserDetailsAsSupplierPage AVD-S1.1 to JourneyRecovery when no answer is found" in {
        navigator.nextPage(
          UsePurchaserDetailsAsSupplierPage(SupplierNumber(1)),
          NormalMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from IsSupplierVatRegisteredPage AVD-S8.0 to CheckSupplierDetails AVD-S9 when No is selected" in {
        val ua = userAnswers.set(IsSupplierVatRegisteredPage(SupplierNumber(2)), false).success.value
        // TODO: navigate to AVD-S9a when implemented
        navigator.nextPage(
          IsSupplierVatRegisteredPage(SupplierNumber(2)),
          NormalMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from IsSupplierVatRegisteredPage AVD-S8.0 to SupplierVatRegistrationDetails AVD-S8.1 when Yes is selected" in {
        val ua = userAnswers.set(IsSupplierVatRegisteredPage(SupplierNumber(2)), true).success.value
        navigator.nextPage(
          IsSupplierVatRegisteredPage(SupplierNumber(2)),
          NormalMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe supplierdetails.routes.SupplierVatRegistrationDetailsController.onPageLoad(SupplierNumber(2), NormalMode)
      }

      "must go from IsSupplierVatRegisteredPage AVD-S8.0 to JourneyRecovery when no answer is found" in {
        navigator.nextPage(
          IsSupplierVatRegisteredPage(SupplierNumber(1)),
          NormalMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from SupplierVatRegistrationNumberPage AVD-S8.1 to CheckSupplierDetails AVD-S9 when continue is pressed" in {
        val ua = userAnswers.set(SupplierVatRegistrationNumberPage(SupplierNumber(2)), VatNumberDetails("FR", "AA123456789")).success.value
        // TODO: navigate to AVD-S9 when implemented
        navigator.nextPage(
          SupplierVatRegistrationNumberPage(SupplierNumber(2)),
          NormalMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from SupplierVatRegistrationNumberPage AVD-S8.1 to JourneyRecovery when no answer is found" in {
        navigator.nextPage(
          SupplierVatRegistrationNumberPage(SupplierNumber(1)),
          NormalMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from VehicleDatesPage AVD3.0 to PurchaseInvoiceDate AVD4.0 for the supplier and vehicle in the URL" in {
        val ua = userAnswers.set(VehicleDatesPage(SupplierNumber(2), VehicleNumber(3)), Set(VehicleDates.PurchaseInvoiceDate)).success.value
        navigator.nextPage(
          VehicleDatesPage(SupplierNumber(2), VehicleNumber(3)),
          NormalMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe vehicledetails.routes.PurchaseInvoiceDateController.onPageLoad(SupplierNumber(2), VehicleNumber(3), NormalMode)
      }

      "must go from PurchaseInvoiceDatePage AVD4.0 to PurchaseInvoiceNumber AVD4.1 when a date is entered" in {
        val ua = userAnswers.set(PurchaseInvoiceDatePage(VehicleNumber(1)), LocalDate.of(2026, 3, 27)).success.value
        navigator.nextPage(
          PurchaseInvoiceDatePage(VehicleNumber(1)),
          NormalMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe routes.LandingPageController.onPageLoad() // TODO: update when AVD4.1 is built
      }

      "must go from PurchaseInvoiceDatePage AVD4.0 to JourneyRecovery when no answer is found" in {
        navigator.nextPage(
          PurchaseInvoiceDatePage(VehicleNumber(1)),
          NormalMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from VehicleDatesPage AVD3.0 to DateOfAvailability AVD5.0 when only the date of availability is selected" in {
        val ua =
          userAnswers.set(VehicleDatesPage(SupplierNumber(1), VehicleNumber(1)), Set(VehicleDates.AvailabilityAndFirstRegistration)).success.value
        navigator.nextPage(
          VehicleDatesPage(SupplierNumber(1), VehicleNumber(1)),
          NormalMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from VehicleDatesPage AVD3.0 to PurchaseInvoiceDate AVD4.0 when both dates are selected" in {
        val ua = userAnswers
          .set(
            VehicleDatesPage(SupplierNumber(1), VehicleNumber(1)),
            Set(VehicleDates.PurchaseInvoiceDate, VehicleDates.AvailabilityAndFirstRegistration)
          )
          .success
          .value
        navigator.nextPage(
          VehicleDatesPage(SupplierNumber(1), VehicleNumber(1)),
          NormalMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe vehicledetails.routes.PurchaseInvoiceDateController.onPageLoad(SupplierNumber(1), VehicleNumber(1), NormalMode)
      }

      "must go from VehicleDatesPage AVD3.0 to NoVehicleDates AVD3.1 when no dates are held" in {
        val ua = userAnswers.set(VehicleDatesPage(SupplierNumber(1), VehicleNumber(1)), Set(VehicleDates.NoDates)).success.value
        navigator.nextPage(VehicleDatesPage(SupplierNumber(1), VehicleNumber(1)), NormalMode, ua, NovaUserType.PrivateIndividual) mustBe
          vehicledetails.routes.NoVehicleDatesController.onPageLoad(SupplierNumber(1), VehicleNumber(1))
      }

      "must go from VehicleDatesPage AVD3.0 to JourneyRecovery when no answer is found" in {
        navigator.nextPage(
          VehicleDatesPage(SupplierNumber(1), VehicleNumber(1)),
          NormalMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from PurchaserBusinessNamePage to the purchaser details check your answers page" in {
        navigator.nextPage(
          PurchaserBusinessNamePage,
          NormalMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe purchaserdetails.routes.PurchaserDetailsCheckYourAnswersController.onPageLoad()
      }

      "must go from SupplierBusinessOrIndividualPage to the supplier business name page when Business is selected" in {
        val ua = userAnswers
          .set(SupplierBusinessOrIndividualPage(SupplierNumber(2)), BusinessOrPrivateIndividual.Business)
          .success
          .value
        navigator.nextPage(
          SupplierBusinessOrIndividualPage(SupplierNumber(2)),
          NormalMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe supplierdetails.routes.SupplierBusinessNameController.onPageLoad(SupplierNumber(2), NormalMode)
      }

      "must go from SupplierBusinessOrIndividualPage AVD-S2.0 to SupplierName AVD-S4.0 when PrivateIndividual is selected" in {
        val ua = userAnswers
          .set(SupplierBusinessOrIndividualPage(SupplierNumber(2)), BusinessOrPrivateIndividual.PrivateIndividual)
          .success
          .value
        navigator.nextPage(
          SupplierBusinessOrIndividualPage(SupplierNumber(2)),
          NormalMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe supplierdetails.routes.SupplierNameController.onPageLoad(SupplierNumber(2), NormalMode)
      }

      "must go from SupplierBusinessOrIndividualPage to the next page for the supplier it was answered for" in {
        val ua = userAnswers
          .set(SupplierBusinessOrIndividualPage(SupplierNumber(1)), BusinessOrPrivateIndividual.Business)
          .success
          .value
          .set(SupplierBusinessOrIndividualPage(SupplierNumber(3)), BusinessOrPrivateIndividual.PrivateIndividual)
          .success
          .value
        navigator.nextPage(
          SupplierBusinessOrIndividualPage(SupplierNumber(3)),
          NormalMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe supplierdetails.routes.SupplierNameController.onPageLoad(SupplierNumber(3), NormalMode)
      }

      "must go from SupplierNamePage AVD-S4.0 to the supplier address page AVD-S5.0" in {
        navigator.nextPage(
          SupplierNamePage(SupplierNumber(1)),
          NormalMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe supplieraddress.routes.IsSupplierAddressInTheUKController.onPageLoad(SupplierNumber(1), NormalMode)
      }

      "must go from SupplierBusinessNamePage to the supplier address page" in {
        navigator.nextPage(
          SupplierBusinessNamePage(SupplierNumber(1)),
          NormalMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe supplieraddress.routes.IsSupplierAddressInTheUKController.onPageLoad(SupplierNumber(1), NormalMode)
      }

      "must go from SupplierBusinessNamePage AVD-S3.0 to the supplier address page AVD-S5.0 for supplier 3" in {
        navigator.nextPage(
          SupplierBusinessNamePage(SupplierNumber(3)),
          NormalMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe supplieraddress.routes.IsSupplierAddressInTheUKController.onPageLoad(SupplierNumber(3), NormalMode)
      }

      "must go from SupplierBusinessOrIndividualPage AVD-S2.0 to JourneyRecovery when no answer is found" in {
        navigator.nextPage(
          SupplierBusinessOrIndividualPage(SupplierNumber(1)),
          NormalMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from IsPurchaserAddressInTheUkPage to the UK address journey when the purchaser address is in the UK" in {
        val ua = userAnswers.set(IsPurchaserAddressInTheUkPage, true).success.value
        navigator.nextPage(
          IsPurchaserAddressInTheUkPage,
          NormalMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe routes.LandingPageController.onPageLoad() // TODO: assert APA2.0 route when built
      }

      "must go from IsPurchaserAddressInTheUkPage to the international address journey when the purchaser address is not in the UK" in {
        val ua = userAnswers.set(IsPurchaserAddressInTheUkPage, false).success.value
        navigator.nextPage(
          IsPurchaserAddressInTheUkPage,
          NormalMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe routes.LandingPageController.onPageLoad() // TODO: assert APA1.2 route when built
      }

      "must go from IsPurchaserAddressInTheUkPage to JourneyRecovery when no answer is found" in {
        navigator.nextPage(
          IsPurchaserAddressInTheUkPage,
          NormalMode,
          userAnswers,
          NovaUserType.PrivateIndividual
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
        ) mustBe initialquestions.routes.InitialQuestionsCheckYourAnswersController
          .onPageLoad()
      }

      "must go from VehicleFromEuPage to VehicleOutsideEUController when No is selected for PrivateIndividual" in {
        val ua = userAnswers.set(VehicleFromEuPage, false).success.value
        navigator.nextPage(
          VehicleFromEuPage,
          CheckMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe initialquestions.routes.VehicleOutsideEUController.onPageLoad()
      }

      "must go from VehicleFromEuPage to VehicleOutsideEUController when No is selected for NonVatOrganisation" in {
        val ua = userAnswers.set(VehicleFromEuPage, false).success.value
        navigator.nextPage(
          VehicleFromEuPage,
          CheckMode,
          ua,
          NovaUserType.NonVatOrganisation
        ) mustBe initialquestions.routes.VehicleOutsideEUController.onPageLoad()
      }

      "must go from BusinessPrivatePage to InitialQuestionsCheckYourAnswers" in {
        navigator.nextPage(
          BusinessOrPrivatePage,
          CheckMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from PurchaserOrOnBehalfPage IQ3.0 to InitialQuestionsCheckYourAnswersController when Purchaser is selected" in {
        val ua = userAnswers.set(NotifyingAsPurchaserPage, PurchaserOrOnBehalf.Purchaser).success.value
        navigator.nextPage(
          NotifyingAsPurchaserPage,
          CheckMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from PurchaserOrOnBehalfPage IQ3.0 to PurchaserBusinessOrIndividualController IQ3.1 in CheckMode when OnBehalfOfPurchaser is selected" in {
        val ua = userAnswers.set(NotifyingAsPurchaserPage, PurchaserOrOnBehalf.OnBehalfOfPurchaser).success.value
        navigator.nextPage(
          NotifyingAsPurchaserPage,
          CheckMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe initialquestions.routes.PurchaserBusinessOrIndividualController.onPageLoad(CheckMode)
      }

      "must go from PurchaserOrOnBehalfPage IQ3.0 to JourneyRecovery when no answer is found" in {
        navigator.nextPage(
          NotifyingAsPurchaserPage,
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
        ) mustBe initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from SupplierBusinessOrIndividualPage AVD-S2.0 to LandingPage" in {
        // TODO: navigate to AVD-S9.0 supplier-details CYA when implemented
        navigator.nextPage(
          SupplierBusinessOrIndividualPage(SupplierNumber(1)),
          CheckMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from SupplierNamePage AVD-S4.0 to LandingPage" in {
        // TODO: navigate to AVD-S9.0 supplier-details CYA when implemented
        navigator.nextPage(
          SupplierNamePage(SupplierNumber(1)),
          CheckMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from VehicleDatesPage AVD3.0 to LandingPage" in {
        val ua = userAnswers.set(VehicleDatesPage(SupplierNumber(1), VehicleNumber(1)), Set(VehicleDates.PurchaseInvoiceDate)).success.value
        navigator.nextPage(
          VehicleDatesPage(SupplierNumber(1), VehicleNumber(1)),
          CheckMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.LandingPageController.onPageLoad()
      }

      "must go from SupplierBusinessNamePage to the supplier details check your answers page" in {
        navigator.nextPage(
          SupplierBusinessNamePage(SupplierNumber(1)),
          CheckMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe routes.LandingPageController.onPageLoad() // TODO: navigate to AVD-S9.0 supplier-details CYA when built
      }

      "must go from VehicleBusinessUsePage to InitialQuestionsCheckYourAnswersController" in {
        navigator.nextPage(
          VehicleBusinessUsePage,
          CheckMode,
          userAnswers,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from AgentVehicleBusinessUsePage AQ1.0 to InitialQuestionsCheckYourAnswers" in {
        navigator.nextPage(
          AgentClientVehicleBusinessUsePage,
          CheckMode,
          userAnswers,
          NovaUserType.Agent
        ) mustBe initialquestions.routes.InitialQuestionsCheckYourAnswersController.onPageLoad()
      }

      "must go from AddYourNamePage to YourDetailsCheckYourAnswersController in CheckMode" in {
        val ua = userAnswers.set(NameDetailsPage, NameDetails("Mr", "John", "Smith")).success.value
        navigator.nextPage(
          NameDetailsPage,
          CheckMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe notifierdetails.routes.YourDetailsCheckYourAnswersController
          .onPageLoad()
      }

      "must go from BusinessNamePage (AYD1.4) to YourDetailsCheckYourAnswersController in CheckMode" in {
        val ua = userAnswers.set(BusinessNamePage, "Acme Trading Co Ltd").success.value
        navigator.nextPage(
          BusinessNamePage,
          CheckMode,
          ua,
          NovaUserType.NonVatOrganisation
        ) mustBe notifierdetails.routes.YourDetailsCheckYourAnswersController
          .onPageLoad()
      }

      "must go from PurchaserNamePage (APD1.0) to the purchaser details check your answers page (CYA4.0) in CheckMode" in {
        val ua = userAnswers.set(PurchaserNamePage, NameDetails("Mr", "John", "Smith")).success.value
        navigator.nextPage(
          PurchaserNamePage,
          CheckMode,
          ua,
          NovaUserType.PrivateIndividual
        ) mustBe purchaserdetails.routes.PurchaserDetailsCheckYourAnswersController
          .onPageLoad()
      }

      "must go from EmailAddressPage to YourDetailsCheckYourAnswersController" in {
        navigator.nextPage(
          EmailAddressPage,
          CheckMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe notifierdetails.routes.YourDetailsCheckYourAnswersController
          .onPageLoad()
      }

      "must go from PhoneNumberPage to YourDetailsCheckYourAnswersController in CheckMode" in {
        val ua = userAnswers.set(PhoneNumberPage, ContactNumbers(Some("01632 960 001"), None)).success.value
        navigator.nextPage(
          PhoneNumberPage,
          CheckMode,
          ua,
          NovaUserType.VatRegisteredOrganisation
        ) mustBe notifierdetails.routes.YourDetailsCheckYourAnswersController
          .onPageLoad()
      }

      "must go from PurchaserBusinessNamePage to the purchaser details check your answers page" in {
        navigator.nextPage(
          PurchaserBusinessNamePage,
          CheckMode,
          userAnswers,
          NovaUserType.PrivateIndividual
        ) mustBe purchaserdetails.routes.PurchaserDetailsCheckYourAnswersController.onPageLoad()
      }
    }
  }
}
