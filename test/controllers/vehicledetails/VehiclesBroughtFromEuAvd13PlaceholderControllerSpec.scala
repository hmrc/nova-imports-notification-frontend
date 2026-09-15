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
import controllers.{supplierdetails, vehicledetails}
import models.{DraftId, NormalMode, SupplierNumber, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
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

class VehiclesBroughtFromEuAvd13PlaceholderControllerSpec extends SpecBase with MockitoSugar {

  private val answersWithDraftId: UserAnswers = emptyUserAnswers.unsafeSet(DraftIdPage, DraftId("DRAFT-001"))

  private lazy val suppliersRoute: String = vehicledetails.routes.VehiclesBroughtFromEuAvd13PlaceholderController.onPageLoad().url

  private val supplierWithAnswers: JsObject = Json.obj("usePersonalDetailsAsSupplier" -> false)

  private def vehiclesForSupplierOne(total: Int): Map[String, JsObject] =
    (1 to total).map(number => number.toString -> vehicleFor(1, "INV-001")).toMap

  private def vehicleFor(supplier: Int, invoiceNumber: String): JsObject =
    Json.obj("supplierNumber" -> supplier, "details" -> Json.obj("purchaseInvoiceNumber" -> invoiceNumber))

  private def mockSessionRepository(userAnswers: UserAnswers): SessionRepository = {
    val repo = mock[SessionRepository]
    when(repo.setPage(any(), any(), any())(any())) thenReturn Future.successful(userAnswers)
    when(repo.set(any())) thenReturn Future.successful(true)
    repo
  }

  private def applicationWith(userAnswers: UserAnswers, repo: SessionRepository): play.api.Application =
    applicationBuilder(userAnswers = Some(userAnswers))
      .overrides(bind[SessionRepository].toInstance(repo))
      .build()

  "VehiclesBroughtFromEuAvd13PlaceholderController (AVD13.0)" - {

    "must add supplier 2 and send the user to AVD-S1.0 for supplier 2 when supplier 1 already exists with an answer" in {

      val answers     = answersWithDraftId.unsafeSet(AllSuppliersQuery, Map("1" -> supplierWithAnswers))
      val application = applicationWith(answers, mockSessionRepository(answers))

      running(application) {
        val result = route(application, FakeRequest(POST, suppliersRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          supplierdetails.routes.UsePersonalDetailsAsSupplierController.onPageLoad(SupplierNumber(2), NormalMode).url
      }
    }

    "must return BAD_REQUEST with max 100 vehicle error and not add supplier when session has 100 vehicles with values" in {

      val answers = answersWithDraftId
        .unsafeSet(AllSuppliersQuery, Map("1" -> supplierWithAnswers))
        .unsafeSet(AllVehiclesQuery, vehiclesForSupplierOne(100))

      val repo        = mockSessionRepository(answers)
      val application = applicationWith(answers, repo)

      running(application) {
        val result = route(application, FakeRequest(POST, suppliersRoute)).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include("You cannot add another supplier, maximum 100 vehicles reached")
        verify(repo, never).setPage(any(), any(), any())(any())
      }
    }

    "must delete supplier 1 and its vehicles and redirect back to AVD13.0" in {

      val vehicleOne = vehicleFor(1, "INV-2026-001")
      val vehicleTwo = vehicleFor(2, "INV-2026-002")
      val deleted    = Json.obj("deleted" -> true)

      val answers = answersWithDraftId
        .unsafeSet(AllSuppliersQuery, Map("1" -> supplierWithAnswers, "2" -> supplierWithAnswers))
        .unsafeSet(AllVehiclesQuery, Map("1" -> vehicleOne, "2" -> vehicleTwo))

      val repo        = mockSessionRepository(answers)
      val application = applicationWith(answers, repo)

      running(application) {
        val deleteRoute = vehicledetails.routes.VehiclesBroughtFromEuAvd13PlaceholderController.onDelete(SupplierNumber(1)).url

        val result = route(application, FakeRequest(POST, deleteRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual suppliersRoute

        // supplier 1 and its vehicle keep their numbered keys, so the numbers are not used again
        val saved = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(repo).set(saved.capture())
        saved.getValue.get(AllSuppliersQuery).value mustBe Map("1" -> deleted, "2" -> supplierWithAnswers)
        saved.getValue.get(AllVehiclesQuery).value mustBe Map("1" -> deleted, "2" -> vehicleTwo)
      }
    }
  }
}
