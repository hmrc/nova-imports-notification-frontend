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

package services

import base.SpecBase
import models.{ImportNumber, SupplierNumber, UserAnswers, VehicleNumber}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import queries.AllVehiclesQuery
import repositories.SessionRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VehicleServiceSpec extends SpecBase with MockitoSugar {

  private def newService(repo: SessionRepository): VehicleService = new VehicleServiceImpl(repo)

  private def mockSessionRepoThatSaves(answers: UserAnswers): SessionRepository = {
    val repo = mock[SessionRepository]
    when(repo.setPage(any(), eqTo(AllVehiclesQuery), any())(any())).thenReturn(Future.successful(answers))
    repo
  }

  // with answer value so is not available
  private def vehiclesForSupplierOne(total: Int): Map[String, JsObject] =
    (1 to total).map(number => number.toString -> forSupplier(1, "INV-001")).toMap

  private def forSupplier(supplierNumber: Int, invoiceNumber: String): JsObject =
    Json.obj("supplierNumber" -> supplierNumber, "details" -> Json.obj("purchaseInvoiceNumber" -> invoiceNumber))

  private def forImport(importNumber: Int, invoiceNumber: String): JsObject =
    Json.obj("importNumber" -> importNumber, "details" -> Json.obj("purchaseInvoiceNumber" -> invoiceNumber))

  // with no answer value yet so is available
  private def startedForSupplier(supplierNumber: Int): JsObject =
    Json.obj("supplierNumber" -> supplierNumber)

  private val deleted: JsObject = Json.obj("deleted" -> true)

  "VehicleService.addForSupplier" - {

    "must return vehicle number 1 when there are no vehicles yet" in {
      val repo = mockSessionRepoThatSaves(emptyUserAnswers)

      val number = newService(repo).addForSupplier(emptyUserAnswers, SupplierNumber(1)).futureValue

      number mustBe VehicleNumber(1)
    }

    "must save the supplier number the vehicle belongs to" in {
      val repo = mockSessionRepoThatSaves(emptyUserAnswers)

      newService(repo).addForSupplier(emptyUserAnswers, SupplierNumber(2)).futureValue

      verify(repo).setPage(any(), eqTo(AllVehiclesQuery), eqTo(Map("1" -> Json.obj("supplierNumber" -> 2))))(any())
    }

    "must return vehicle number 2 when supplier 1 already has vehicle 1" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> forSupplier(1, "INV-2026-001")))
      val repo    = mockSessionRepoThatSaves(answers)

      val number = newService(repo).addForSupplier(answers, SupplierNumber(2)).futureValue

      number mustBe VehicleNumber(2)
    }

    "must return the next vehicle number after the highest, even when there are gaps" in {
      val existing = Map("1" -> forSupplier(1, "INV-2026-001"), "3" -> forSupplier(1, "INV-2026-003"))
      val answers  = emptyUserAnswers.unsafeSet(AllVehiclesQuery, existing)
      val repo     = mockSessionRepoThatSaves(answers)

      val number = newService(repo).addForSupplier(answers, SupplierNumber(1)).futureValue

      number mustBe VehicleNumber(4)
    }

    "must keep the existing vehicles when adding a new one" in {
      val existing = Map("1" -> forSupplier(1, "INV-2026-001"))
      val answers  = emptyUserAnswers.unsafeSet(AllVehiclesQuery, existing)
      val repo     = mockSessionRepoThatSaves(answers)

      newService(repo).addForSupplier(answers, SupplierNumber(2)).futureValue

      verify(repo).setPage(any(), eqTo(AllVehiclesQuery), eqTo(existing + ("2" -> Json.obj("supplierNumber" -> 2))))(any())
    }

    "must reuse vehicle 2 when it holds no values yet" in {
      val answers = emptyUserAnswers
        .unsafeSet(AllVehiclesQuery, Map("1" -> forSupplier(1, "INV-2026-001"), "2" -> startedForSupplier(1)))
      val repo = mockSessionRepoThatSaves(answers)

      val number = newService(repo).addForSupplier(answers, SupplierNumber(1)).futureValue

      number mustBe VehicleNumber(2)
    }

    "must write supplierNumber 1 on vehicle 3 when it was abandoned under supplier 2" in {
      val answers = emptyUserAnswers.unsafeSet(
        AllVehiclesQuery,
        Map("1" -> forSupplier(1, "INV-001"), "2" -> forSupplier(1, "INV-002"), "3" -> startedForSupplier(2))
      )
      val repo = mockSessionRepoThatSaves(answers)

      newService(repo).addForSupplier(answers, SupplierNumber(1)).futureValue

      verify(repo).setPage(
        any(),
        eqTo(AllVehiclesQuery),
        eqTo(Map("1" -> forSupplier(1, "INV-001"), "2" -> forSupplier(1, "INV-002"), "3" -> Json.obj("supplierNumber" -> 1)))
      )(any())
    }

    "must not return a deleted vehicle's number again, must be next highest number" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> forSupplier(1, "INV-2026-001"), "2" -> deleted))
      val repo    = mockSessionRepoThatSaves(answers)

      val number = newService(repo).addForSupplier(answers, SupplierNumber(1)).futureValue

      number mustBe VehicleNumber(3)
    }
  }

  "VehicleService.addForImport" - {

    "must save the import number the vehicle belongs to" in {
      val repo = mockSessionRepoThatSaves(emptyUserAnswers)

      newService(repo).addForImport(emptyUserAnswers, ImportNumber(3)).futureValue

      verify(repo).setPage(any(), eqTo(AllVehiclesQuery), eqTo(Map("1" -> Json.obj("importNumber" -> 3))))(any())
    }
  }

  "VehicleService.deleteValues" - {

    "must empty vehicle 1 and mark it deleted, keeping its number in the session" in {
      val answers = emptyUserAnswers.unsafeSet(
        AllVehiclesQuery,
        Map("1" -> forSupplier(1, "INV-2026-001"), "2" -> forSupplier(1, "INV-2026-002"))
      )
      val repo = mockSessionRepoThatSaves(answers)

      newService(repo).deleteValues(answers, VehicleNumber(1)).futureValue

      verify(repo).setPage(any(), eqTo(AllVehiclesQuery), eqTo(Map("1" -> deleted, "2" -> forSupplier(1, "INV-2026-002"))))(any())
    }

    "must leave vehicle 1 unchanged when vehicle 9 is not in the session" in {
      val existing = Map("1" -> forSupplier(1, "INV-2026-001"))
      val answers  = emptyUserAnswers.unsafeSet(AllVehiclesQuery, existing)
      val repo     = mockSessionRepoThatSaves(answers)

      newService(repo).deleteValues(answers, VehicleNumber(9)).futureValue

      verify(repo).setPage(any(), eqTo(AllVehiclesQuery), eqTo(existing))(any())
    }
  }

  "VehicleService.deleteValuesForSupplier" - {

    "must empty every vehicle belonging to that supplier and keep the rest unchanged" in {
      val answers = emptyUserAnswers.unsafeSet(
        AllVehiclesQuery,
        Map(
          "1" -> forSupplier(1, "INV-2026-001"),
          "2" -> forSupplier(2, "INV-2026-002"),
          "3" -> forSupplier(1, "INV-2026-003")
        )
      )

      val result = newService(mock[SessionRepository]).deleteValuesForSupplier(answers, SupplierNumber(1)).success.value

      result.get(AllVehiclesQuery).value mustBe
        Map("1" -> deleted, "2" -> forSupplier(2, "INV-2026-002"), "3" -> deleted)
    }

    "must not mark an empty vehicle collection deleted, so its number stays available" in {
      val answers = emptyUserAnswers.unsafeSet(
        AllVehiclesQuery,
        Map("1" -> forSupplier(1, "INV-2026-001"), "2" -> startedForSupplier(1))
      )

      val result = newService(mock[SessionRepository]).deleteValuesForSupplier(answers, SupplierNumber(1)).success.value

      result.get(AllVehiclesQuery).value mustBe Map("1" -> deleted, "2" -> startedForSupplier(1))
    }
  }

  "VehicleService.deleteValuesForImport" - {

    "must empty every vehicle belonging to that import and keep the rest unchanged" in {
      val answers = emptyUserAnswers.unsafeSet(
        AllVehiclesQuery,
        Map("1" -> forImport(1, "INV-2026-001"), "2" -> forImport(2, "INV-2026-002"))
      )

      val result = newService(mock[SessionRepository]).deleteValuesForImport(answers, ImportNumber(1)).success.value

      result.get(AllVehiclesQuery).value mustBe Map("1" -> deleted, "2" -> forImport(2, "INV-2026-002"))
    }
  }

  "VehicleService.numberHasValues" - {

    "must be false for a vehicle holding only its supplierNumber" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> startedForSupplier(1)))

      newService(mock[SessionRepository]).numberHasValues(answers, VehicleNumber(1)) mustBe false
    }

    "must be true for a vehicle holding an answer" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> forSupplier(1, "INV-2026-001")))

      newService(mock[SessionRepository]).numberHasValues(answers, VehicleNumber(1)) mustBe true
    }
  }

  "VehicleService.count" - {

    "must count the vehicles, not the highest number" in {
      val answers = emptyUserAnswers.unsafeSet(
        AllVehiclesQuery,
        Map("101" -> forSupplier(1, "INV-2026-101"), "102" -> forSupplier(1, "INV-2026-102"))
      )

      newService(mock[SessionRepository]).count(answers) mustBe 2
    }

    "must return 0 when there are no vehicles" in {
      newService(mock[SessionRepository]).count(emptyUserAnswers) mustBe 0
    }

    "must not count deleted or empty vehicles" in {
      val answers = emptyUserAnswers.unsafeSet(
        AllVehiclesQuery,
        Map("1" -> forSupplier(1, "INV-2026-001"), "2" -> deleted, "3" -> startedForSupplier(1))
      )

      newService(mock[SessionRepository]).count(answers) mustBe 1
    }
  }

  "VehicleService.inOrder" - {

    "must sort 10 after 2" in {
      val answers = emptyUserAnswers
        .unsafeSet(AllVehiclesQuery, Map("10" -> forSupplier(1, "INV-2026-010"), "2" -> forSupplier(1, "INV-2026-002")))

      newService(mock[SessionRepository]).inOrder(answers).map { case (number, _) => number } mustBe
        Seq(VehicleNumber(2), VehicleNumber(10))
    }

    "must return nothing when there are no vehicles" in {
      newService(mock[SessionRepository]).inOrder(emptyUserAnswers) mustBe empty
    }

    "must leave out deleted and empty vehicles" in {
      val answers = emptyUserAnswers.unsafeSet(
        AllVehiclesQuery,
        Map("1" -> forSupplier(1, "INV-2026-001"), "2" -> deleted, "3" -> startedForSupplier(1))
      )

      newService(mock[SessionRepository]).inOrder(answers).map { case (number, _) => number } mustBe
        Seq(VehicleNumber(1))
    }
  }

  "VehicleService.numberExists" - {

    "must return true for a vehicle that exists" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj(), "3" -> Json.obj()))

      newService(mock[SessionRepository]).numberExists(answers, VehicleNumber(3)) mustBe true
    }

    "must return false for a vehicle that does not exist" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj(), "3" -> Json.obj()))

      newService(mock[SessionRepository]).numberExists(answers, VehicleNumber(2)) mustBe false
    }

    "must return false when there are no vehicles" in {
      newService(mock[SessionRepository]).numberExists(emptyUserAnswers, VehicleNumber(1)) mustBe false
    }

    "must return false when vehicle 3 is marked deleted" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("3" -> Json.obj("deleted" -> true)))

      newService(mock[SessionRepository]).numberExists(answers, VehicleNumber(3)) mustBe false
    }
  }

  "VehicleService.belongsToSupplier" - {

    "must return true for a vehicle bought from that supplier" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 2)))

      newService(mock[SessionRepository]).belongsToSupplier(answers, VehicleNumber(1), SupplierNumber(2)) mustBe true
    }

    "must return false for a vehicle bought from a different supplier" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 2)))

      newService(mock[SessionRepository]).belongsToSupplier(answers, VehicleNumber(1), SupplierNumber(3)) mustBe false
    }

    "must return false for a vehicle that does not exist" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 2)))

      newService(mock[SessionRepository]).belongsToSupplier(answers, VehicleNumber(9), SupplierNumber(2)) mustBe false
    }

    "must return false for a vehicle that belongs to an import" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 2)))

      newService(mock[SessionRepository]).belongsToSupplier(answers, VehicleNumber(1), SupplierNumber(2)) mustBe false
    }
  }

  "VehicleService.belongsToImport" - {

    "must return true for a vehicle added under that import" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 2)))

      newService(mock[SessionRepository]).belongsToImport(answers, VehicleNumber(1), ImportNumber(2)) mustBe true
    }

    "must return false for a vehicle added under a different import" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 2)))

      newService(mock[SessionRepository]).belongsToImport(answers, VehicleNumber(1), ImportNumber(3)) mustBe false
    }
  }

  "VehicleService.limitReached" - {

    "must return false when the notification has 99 vehicles" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, vehiclesForSupplierOne(99))

      newService(mock[SessionRepository]).limitReached(answers) mustBe false
    }

    "must return true when the notification has 100 vehicles" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, vehiclesForSupplierOne(100))

      newService(mock[SessionRepository]).limitReached(answers) mustBe true
    }

    "must return false when there are no vehicles" in {
      newService(mock[SessionRepository]).limitReached(emptyUserAnswers) mustBe false
    }
  }
}
