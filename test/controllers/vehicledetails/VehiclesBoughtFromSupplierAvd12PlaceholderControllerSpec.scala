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
import controllers.vehicledetails
import models.{DraftId, NormalMode, SupplierNumber, UserAnswers, VehicleNumber}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository

import scala.concurrent.Future

class VehiclesBoughtFromSupplierAvd12PlaceholderControllerSpec extends SpecBase with MockitoSugar {

  private val supplierNumber = SupplierNumber(1)

  private val answersWithSupplierOne: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))

  private lazy val vehiclesFromSupplierRoute: String =
    vehicledetails.routes.VehiclesBoughtFromSupplierAvd12PlaceholderController.onPageLoad(supplierNumber).url

  private def deleteRoute(supplier: SupplierNumber, vehicle: VehicleNumber): String =
    vehicledetails.routes.VehiclesBoughtFromSupplierAvd12PlaceholderController.onDelete(supplier, vehicle).url

  private def vehicleFor(supplier: Int, invoiceNumber: String): JsObject =
    Json.obj("supplierNumber" -> supplier, "details" -> Json.obj("purchaseInvoiceNumber" -> invoiceNumber))

  private val deleted: JsObject = Json.obj("deleted" -> true)

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

  "VehiclesBoughtFromSupplierAvd12PlaceholderController (AVD12.0)" - {

    "must add vehicle 3 for supplier 1 and send the user to AVD3.0 for vehicle 3" in {

      val answers = answersWithSupplierOne
        .unsafeSet(AllVehiclesQuery, Map("1" -> vehicleFor(1, "INV-2026-001"), "2" -> vehicleFor(1, "INV-2026-002")))

      val application = applicationWith(answers, mockSessionRepository(answers))

      running(application) {
        val result = route(application, FakeRequest(POST, vehiclesFromSupplierRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          vehicledetails.routes.VehicleDatesController.onPageLoad(supplierNumber, VehicleNumber(3), NormalMode).url
      }
    }

    "must return BAD_REQUEST with the 100 vehicle error and not add a vehicle when the session has 100 vehicles" in {

      val answers     = answersWithSupplierOne.unsafeSet(AllVehiclesQuery, vehiclesForSupplierOne(100))
      val repo        = mockSessionRepository(answers)
      val application = applicationWith(answers, repo)

      running(application) {
        val result = route(application, FakeRequest(POST, vehiclesFromSupplierRoute)).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include("You cannot add another vehicle, maximum 100 vehicles reached")
        verify(repo, never).setPage(any(), any(), any())(any())
      }
    }

    "must delete vehicle 1 and return to supplier 1's vehicles list" in {

      val answers = answersWithSupplierOne
        .unsafeSet(AllVehiclesQuery, Map("1" -> vehicleFor(1, "INV-2026-001"), "2" -> vehicleFor(1, "INV-2026-002")))

      val repo        = mockSessionRepository(answers)
      val application = applicationWith(answers, repo)

      running(application) {
        val result = route(application, FakeRequest(POST, deleteRoute(supplierNumber, VehicleNumber(1)))).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehiclesFromSupplierRoute

        // vehicle 1 keeps its numbered key, so its number is not used again.
        verify(repo).setPage(any(), eqTo(AllVehiclesQuery), eqTo(Map("1" -> deleted, "2" -> vehicleFor(1, "INV-2026-002"))))(any())
      }
    }
  }
}
