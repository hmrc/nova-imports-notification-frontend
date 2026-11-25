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
import forms.AdditionalVehicleFormProvider
import models.{BusinessOrPrivateIndividual, DraftId, NormalMode, SupplierNumber, UserAnswers, VehicleNumber}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplierdetails.{SupplierBusinessNamePage, SupplierBusinessOrIndividualPage, UsePersonalDetailsAsSupplierPage}
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import viewmodels.Pager
import views.html.VehiclesBoughtFromSupplierView

import scala.concurrent.Future

class VehiclesBoughtFromSupplierControllerSpec extends SpecBase with MockitoSugar {

  val supplierNumber: SupplierNumber = SupplierNumber(1)

  val formProvider = new AdditionalVehicleFormProvider()
  val form         = formProvider()

  val pageUrl = (pageNumber: Int) => controllers.vehicledetails.routes.VehiclesBoughtFromSupplierController.onPageLoad(supplierNumber).url

  val userAnswersWithGuardData: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))

  lazy val vehiclesBoughtFromSupplierRoute: String =
    vehicledetails.routes.VehiclesBoughtFromSupplierController.onPageLoad(supplierNumber).url

  private def vehicleFor(supplier: Int, invoiceNumber: String): JsObject =
    Json.obj("supplierNumber" -> supplier, "details" -> Json.obj("purchaseInvoiceNumber" -> invoiceNumber))

  private def vehiclesForSupplierOne(total: Int): Map[String, JsObject] =
    (1 to total).map(number => number.toString -> vehicleFor(1, "INV-001")).toMap

  private def mockSessionRepository(userAnswers: UserAnswers): SessionRepository = {
    val repo = mock[SessionRepository]
    when(repo.setPage(any(), any(), any())(any())) thenReturn Future.successful(userAnswers)
    repo
  }

  private def applicationWith(userAnswers: UserAnswers, repo: SessionRepository): play.api.Application =
    applicationBuilder(userAnswers = Some(userAnswers))
      .overrides(bind[SessionRepository].toInstance(repo))
      .build()

  private def applicationWithMockRepository(userAnswers: UserAnswers): play.api.Application =
    applicationWith(userAnswers, mockSessionRepository(userAnswers))

  "VehiclesBoughtFromSupplierController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val request = FakeRequest(GET, vehiclesBoughtFromSupplierRoute)

        val result = route(application, request).value

        val view      = application.injector.instanceOf[VehiclesBoughtFromSupplierView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form, Pager.page(Seq.empty, 1), pageUrl, None, supplierNumber, appConfig.personalTransportUnitUrl)(
            request,
            messages(application)
          ).toString
      }
    }

    "must add vehicle 1 for the supplier and send the user to AVD3.0 for that vehicle" in {

      val application = applicationWithMockRepository(userAnswersWithGuardData)

      running(application) {
        val result = route(application, FakeRequest(POST, vehiclesBoughtFromSupplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          vehicledetails.routes.VehicleDatesController.onPageLoad(supplierNumber, VehicleNumber(1), NormalMode).url
      }
    }

    "must pass the supplier number from URL to addForSupplier so the vehicle is saved with supplierNumber 1" in {

      val repo        = mockSessionRepository(userAnswersWithGuardData)
      val application = applicationWith(userAnswersWithGuardData, repo)

      running(application) {
        route(application, FakeRequest(POST, vehiclesBoughtFromSupplierRoute)).value.futureValue

        verify(repo).setPage(any(), eqTo(AllVehiclesQuery), eqTo(Map("1" -> Json.obj("supplierNumber" -> 1))))(any())
      }
    }

    "must redirect to Unauthorised for a POST if there is no draft id in session" in {

      val answers = emptyUserAnswers
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(POST, vehiclesBoughtFromSupplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if there is no draft id in session" in {

      val answers = emptyUserAnswers
        .unsafeSet(VehicleFromEuPage, true)
        .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the vehicle was not brought from the EU" in {

      val answers = userAnswersWithGuardData.unsafeSet(VehicleFromEuPage, false)

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val otherSupplierRoute = vehicledetails.routes.VehiclesBoughtFromSupplierController.onPageLoad(SupplierNumber(2)).url

        val result = route(application, FakeRequest(GET, otherSupplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must render the supplier name in the heading" in {

      val answers = userAnswersWithGuardData
        .unsafeSet(UsePersonalDetailsAsSupplierPage(supplierNumber), false)
        .unsafeSet(SupplierBusinessOrIndividualPage(supplierNumber), BusinessOrPrivateIndividual.Business)
        .unsafeSet(SupplierBusinessNamePage(supplierNumber), "Test Co")

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

        contentAsString(result) must include("Vehicles bought from Test Co")
      }
    }

    "must render the fallback heading when there is no supplier name" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithGuardData)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, vehiclesBoughtFromSupplierRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include("""<h1 class="govuk-heading-l">Vehicles bought from this supplier</h1>""")
      }
    }
  }

  "VehiclesBoughtFromSupplierController - with list of vehicles" - {

    "must add the next vehicle and redirect to AVD3.0 when the notification already has vehicle with an answer and the user selected Yes" in {

      val vehicleOne  = Json.obj("supplierNumber" -> 1, "details" -> Json.obj("purchaseInvoiceNumber" -> "INV-2026-001"))
      val answers     = userAnswersWithGuardData.unsafeSet(AllVehiclesQuery, Map("1" -> vehicleOne))
      val application = applicationWithMockRepository(answers)

      running(application) {
        val result = route(application, FakeRequest(POST, vehiclesBoughtFromSupplierRoute).withFormUrlEncodedBody("value" -> "true")).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          vehicledetails.routes.VehicleDatesController.onPageLoad(supplierNumber, VehicleNumber(2), NormalMode).url
      }
    }

    "must redirect to LandingPage when the notification already has vehicle with an answer and the user selected No" in {

      val vehicleOne  = Json.obj("supplierNumber" -> 1, "details" -> Json.obj("purchaseInvoiceNumber" -> "INV-2026-001"))
      val answers     = userAnswersWithGuardData.unsafeSet(AllVehiclesQuery, Map("1" -> vehicleOne))
      val application = applicationWithMockRepository(answers)

      running(application) {
        val result = route(application, FakeRequest(POST, vehiclesBoughtFromSupplierRoute).withFormUrlEncodedBody("value" -> "false")).value

        // TODO: Redirect to AVD13.0 when built
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url
      }
    }

    "must return a BadRequest if there is an existing vehicle with an answer and no option is selected" in {

      val vehicleOne  = Json.obj("supplierNumber" -> 1, "details" -> Json.obj("purchaseInvoiceNumber" -> "INV-2026-001"))
      val answers     = userAnswersWithGuardData.unsafeSet(AllVehiclesQuery, Map("1" -> vehicleOne))
      val application = applicationWithMockRepository(answers)

      running(application) {
        val result = route(application, FakeRequest(POST, vehiclesBoughtFromSupplierRoute)).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must add vehicle 3 for supplier 1 and send the user to AVD3.0 for vehicle 3 when supplier already has existing vehicles" in {

      val answers = userAnswersWithGuardData
        .unsafeSet(AllVehiclesQuery, Map("1" -> vehicleFor(1, "INV-2026-001"), "2" -> vehicleFor(1, "INV-2026-002")))

      val application = applicationWith(answers, mockSessionRepository(answers))

      running(application) {
        val result = route(application, FakeRequest(POST, vehiclesBoughtFromSupplierRoute).withFormUrlEncodedBody("value" -> "true")).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          vehicledetails.routes.VehicleDatesController.onPageLoad(supplierNumber, VehicleNumber(3), NormalMode).url
      }
    }

  }

}
