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

package controllers.vehicledetails

import base.SpecBase
import com.google.inject.name.Names
import controllers.actions.*
import controllers.{routes, vehicledetails}
import models.*
import pages.{AgentSelectedClientPage, DraftIdPage}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.vehicledetails.{AddVehicleDetailsHeavyCommercialPage, AddVehicleTypePage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import views.html.AddingMultipleVehiclesErrorView

class AddingMultipleVehiclesErrorControllerSpec extends SpecBase {

  val supplierNumber = SupplierNumber(1)
  val importNumber   = ImportNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  lazy val supplierRoute = vehicledetails.routes.AddingMultipleVehiclesErrorController.supplierOnPageLoad(supplierNumber, vehicleNumber).url
  lazy val importRoute   = vehicledetails.routes.AddingMultipleVehiclesErrorController.importOnPageLoad(importNumber, vehicleNumber).url

  private def withVehicleDetails(answers: UserAnswers): UserAnswers =
    answers
      .unsafeSet(AddVehicleTypePage(vehicleNumber), AddVehicleType.Hcv)
      .unsafeSet(AddVehicleDetailsHeavyCommercialPage(vehicleNumber), AddVehicleDetailsHeavyCommercial("Make", "Model", "HcvType", "CabType"))

  val supplierAnswers: UserAnswers = withVehicleDetails(
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(VehicleFromEuPage, true)
      .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
      .unsafeSet(
        AllVehiclesQuery,
        Map(
          "1" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true)),
          "2" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))
        )
      )
  )

  val importAnswers: UserAnswers = withVehicleDetails(
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(VehicleFromEuPage, false)
      .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123-456789A")))
      .unsafeSet(
        AllVehiclesQuery,
        Map(
          "1" -> Json.obj("importNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true)),
          "2" -> Json.obj("importNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))
        )
      )
  )

  private def applicationFor(
    identifierAction: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers]
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )
      .build()

  private def mustReturnOk(application: Application, url: String) =
    running(application) {
      status(route(application, FakeRequest(GET, url)).value) mustEqual OK
    }

  private def mustBeUnauthorised(application: Application, url: String) =
    running(application) {
      val result = route(application, FakeRequest(GET, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }

  private val agentClient = AgentSelectedClient(vrn = "123456789", name = Some("Company 1 Ltd"))

  "AddingMultipleVehiclesErrorController" - {

    "supplier journey" - {
      // TODO: update route to AVD12.0 when built
      "must return OK and the correct view with the link to the AVD12.0 for a GET" in {
        val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers))

        running(application) {
          val request = FakeRequest(GET, supplierRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[AddingMultipleVehiclesErrorView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            vehicledetails.routes.VehiclesBoughtFromSupplierAvd12PlaceholderController.onPageLoad(supplierNumber)
          )(request, messages(application)).toString
        }
      }

      "must return OK for a GET from a private individual" in {
        mustReturnOk(applicationFor(classOf[FakeIdentifierAction], Some(supplierAnswers)), supplierRoute)
      }

      "must return OK for a GET from a non-VAT organisation" in {
        mustReturnOk(applicationFor(classOf[FakeOrganisationIdentifierAction], Some(supplierAnswers)), supplierRoute)
      }

      "must return OK for a GET from a VAT organisation" in {
        mustReturnOk(applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers)), supplierRoute)
      }

      "must return OK for a GET from an agent with the agent enrolment" in {
        mustReturnOk(applicationFor(classOf[FakeAgentIdentifierAction], Some(supplierAnswers)), supplierRoute)
      }

      "must return OK for a GET from an agent without agent enrolment" in {
        mustReturnOk(applicationFor(classOf[FakeAgentNoEnrolmentsIdentifierAction], Some(supplierAnswers)), supplierRoute)
      }

      "must return OK for a GET from an agent with a client selected" in {
        mustReturnOk(
          applicationFor(classOf[FakeAgentIdentifierAction], Some(supplierAnswers.unsafeSet(AgentSelectedClientPage, agentClient))),
          supplierRoute
        )
      }

      "must redirect to Unauthorised for a GET if no existing session data is found" in {
        mustBeUnauthorised(applicationFor(classOf[FakeVatTraderIdentifierAction], None), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if no draft is in progress" in {
        mustBeUnauthorised(
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers.remove(DraftIdPage).success.value)),
          supplierRoute
        )
      }

      "must redirect to Unauthorised for a GET if the user is amending a submitted notification" in {
        mustBeUnauthorised(
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers.unsafeSet(AmendSubmittedNotificationPage, true))),
          supplierRoute
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle was not brought from the EU" in {
        mustBeUnauthorised(
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers.unsafeSet(VehicleFromEuPage, false))),
          supplierRoute
        )
      }

      "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers in session" in {
        mustBeUnauthorised(
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers)),
          vehicledetails.routes.AddingMultipleVehiclesErrorController.supplierOnPageLoad(SupplierNumber(2), vehicleNumber).url
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle type details are missing" in {
        mustBeUnauthorised(
          applicationFor(
            classOf[FakeVatTraderIdentifierAction],
            Some(supplierAnswers.remove(AddVehicleDetailsHeavyCommercialPage(vehicleNumber)).success.value)
          ),
          supplierRoute
        )
      }

      "must redirect to Unauthorised for a GET if there is only one vehicle in the notification" in {
        val answers = supplierAnswers.unsafeSet(
          AllVehiclesQuery,
          Map("1" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true)))
        )

        mustBeUnauthorised(applicationFor(classOf[FakeVatTraderIdentifierAction], Some(withVehicleDetails(answers))), supplierRoute)
      }
    }

    "import journey" - {
      // TODO: update route to AVD-IE5.0 when built
      "must return OK and the correct view with the link to the landing page for a GET" in {
        val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers))

        running(application) {
          val request = FakeRequest(GET, importRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[AddingMultipleVehiclesErrorView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(routes.LandingPageController.onPageLoad())(request, messages(application)).toString
        }
      }

      "must return OK for a GET from a VAT organisation" in {
        mustReturnOk(applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers)), importRoute)
      }

      "must return OK for a GET from an agent with the agent enrolment" in {
        mustReturnOk(applicationFor(classOf[FakeAgentIdentifierAction], Some(importAnswers)), importRoute)
      }

      "must return OK for a GET from an agent without agent enrolment" in {
        mustReturnOk(applicationFor(classOf[FakeAgentNoEnrolmentsIdentifierAction], Some(importAnswers)), importRoute)
      }

      "must return OK for a GET from an agent with a client selected" in {
        mustReturnOk(
          applicationFor(classOf[FakeAgentIdentifierAction], Some(importAnswers.unsafeSet(AgentSelectedClientPage, agentClient))),
          importRoute
        )
      }

      "must redirect to Unauthorised for a GET from a private individual" in {
        mustBeUnauthorised(applicationFor(classOf[FakeIdentifierAction], Some(importAnswers)), importRoute)
      }

      "must redirect to Unauthorised for a GET from a non-VAT organisation" in {
        mustBeUnauthorised(applicationFor(classOf[FakeOrganisationIdentifierAction], Some(importAnswers)), importRoute)
      }

      "must redirect to Unauthorised for a GET if the user is amending a submitted notification" in {
        mustBeUnauthorised(
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers.unsafeSet(AmendSubmittedNotificationPage, true))),
          importRoute
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle was brought from the EU" in {
        mustBeUnauthorised(
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers.unsafeSet(VehicleFromEuPage, true))),
          importRoute
        )
      }

      "must redirect to Unauthorised for a GET if the import number in the URL is not one of the user's imports" in {
        mustBeUnauthorised(
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers)),
          vehicledetails.routes.AddingMultipleVehiclesErrorController.importOnPageLoad(ImportNumber(2), vehicleNumber).url
        )
      }

      "must redirect to Unauthorised for a GET if there is only one vehicle in the notification" in {
        val answers = importAnswers.unsafeSet(
          AllVehiclesQuery,
          Map("1" -> Json.obj("importNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true)))
        )

        mustBeUnauthorised(applicationFor(classOf[FakeVatTraderIdentifierAction], Some(withVehicleDetails(answers))), importRoute)
      }
    }
  }
}
