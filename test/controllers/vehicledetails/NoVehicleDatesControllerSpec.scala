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
import config.FrontendAppConfig
import controllers.{routes, vehicledetails}
import models.{DraftId, SupplierNumber, UserAnswers, VehicleDates, VehicleNumber}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.VehicleDatesPage
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllSuppliersQuery, AllVehiclesQuery}
import views.html.NoVehicleDatesView

class NoVehicleDatesControllerSpec extends SpecBase with MockitoSugar {

  val supplierNumber: SupplierNumber = SupplierNumber(1)
  val vehicleNumber: VehicleNumber   = VehicleNumber(1)

  lazy val noVehicleDatesRoute: String =
    vehicledetails.routes.NoVehicleDatesController.onPageLoad(supplierNumber, vehicleNumber).url

  val userAnswersWithGuardData: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj()))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
    .unsafeSet(VehicleDatesPage(supplierNumber, vehicleNumber), Set(VehicleDates.NoDates))

  "NoVehicleDatesController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, noVehicleDatesRoute)

        val result = route(application, request).value

        val view      = application.injector.instanceOf[NoVehicleDatesView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(appConfig.personalTransportUnitUrl)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised if no existing session data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, FakeRequest(GET, noVehicleDatesRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised if draftId is missing" in {

      val answers     = emptyUserAnswers.unsafeSet(VehicleFromEuPage, true)
      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, noVehicleDatesRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised if IQ1 was answered No" in {

      val answers = userAnswersWithGuardData.unsafeSet(VehicleFromEuPage, false)

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, noVehicleDatesRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, vehicledetails.routes.NoVehicleDatesController.onPageLoad(SupplierNumber(2), vehicleNumber).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised if the vehicle in the URL belongs to a different supplier" in {

      val answers = userAnswersWithGuardData
        .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj(), "2" -> Json.obj()))
        .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 2)))

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, noVehicleDatesRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised if the user did not select 'No dates' on AVD3.0 for this vehicle" in {

      val answers = userAnswersWithGuardData.unsafeSet(VehicleDatesPage(supplierNumber, vehicleNumber), Set(VehicleDates.PurchaseInvoiceDate))

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, noVehicleDatesRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised if AVD3.0 has not been answered for this vehicle" in {

      val answers = userAnswersWithGuardData
        .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1), "2" -> Json.obj("supplierNumber" -> 1)))

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, vehicledetails.routes.NoVehicleDatesController.onPageLoad(supplierNumber, VehicleNumber(2)).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
