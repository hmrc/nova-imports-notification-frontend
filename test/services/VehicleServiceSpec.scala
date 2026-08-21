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
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
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
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
      val repo    = mockSessionRepoThatSaves(answers)

      val number = newService(repo).addForSupplier(answers, SupplierNumber(2)).futureValue

      number mustBe VehicleNumber(2)
    }

    "must return the next vehicle number after the highest, even when there are gaps" in {
      val existing = Map("1" -> Json.obj("supplierNumber" -> 1), "3" -> Json.obj("supplierNumber" -> 1))
      val answers  = emptyUserAnswers.unsafeSet(AllVehiclesQuery, existing)
      val repo     = mockSessionRepoThatSaves(answers)

      val number = newService(repo).addForSupplier(answers, SupplierNumber(1)).futureValue

      number mustBe VehicleNumber(4)
    }

    "must keep the existing vehicles when adding a new one" in {
      val existing = Map("1" -> Json.obj("supplierNumber" -> 1, "vin" -> "WBA0001"))
      val answers  = emptyUserAnswers.unsafeSet(AllVehiclesQuery, existing)
      val repo     = mockSessionRepoThatSaves(answers)

      newService(repo).addForSupplier(answers, SupplierNumber(2)).futureValue

      verify(repo).setPage(any(), eqTo(AllVehiclesQuery), eqTo(existing + ("2" -> Json.obj("supplierNumber" -> 2))))(any())
    }
  }

  "VehicleService.addForImport" - {

    "must save the import number the vehicle belongs to" in {
      val repo = mockSessionRepoThatSaves(emptyUserAnswers)

      newService(repo).addForImport(emptyUserAnswers, ImportNumber(3)).futureValue

      verify(repo).setPage(any(), eqTo(AllVehiclesQuery), eqTo(Map("1" -> Json.obj("importNumber" -> 3))))(any())
    }
  }

  "VehicleService.delete" - {

    "must remove the vehicle from the session" in {
      val answers = emptyUserAnswers.unsafeSet(
        AllVehiclesQuery,
        Map("1" -> Json.obj("supplierNumber" -> 1), "2" -> Json.obj("supplierNumber" -> 1))
      )
      val repo = mockSessionRepoThatSaves(answers)

      newService(repo).delete(answers, VehicleNumber(1)).futureValue

      verify(repo).setPage(any(), eqTo(AllVehiclesQuery), eqTo(Map("2" -> Json.obj("supplierNumber" -> 1))))(any())
    }
  }

  "VehicleService.removeForSupplier" - {

    "must remove every vehicle belonging to that supplier and keep the rest" in {
      val answers = emptyUserAnswers.unsafeSet(
        AllVehiclesQuery,
        Map(
          "1" -> Json.obj("supplierNumber" -> 1),
          "2" -> Json.obj("supplierNumber" -> 2),
          "3" -> Json.obj("supplierNumber" -> 1)
        )
      )

      val result = newService(mock[SessionRepository]).removeForSupplier(answers, SupplierNumber(1)).success.value

      result.get(AllVehiclesQuery).value mustBe Map("2" -> Json.obj("supplierNumber" -> 2))
    }
  }

  "VehicleService.removeForImport" - {

    "must remove every vehicle belonging to that import and keep the rest" in {
      val answers = emptyUserAnswers.unsafeSet(
        AllVehiclesQuery,
        Map("1" -> Json.obj("importNumber" -> 1), "2" -> Json.obj("importNumber" -> 2))
      )

      val result = newService(mock[SessionRepository]).removeForImport(answers, ImportNumber(1)).success.value

      result.get(AllVehiclesQuery).value mustBe Map("2" -> Json.obj("importNumber" -> 2))
    }
  }

  "VehicleService.count" - {

    "must count the vehicles, not the highest number" in {
      val answers = emptyUserAnswers.unsafeSet(
        AllVehiclesQuery,
        Map("101" -> Json.obj("supplierNumber" -> 1), "102" -> Json.obj("supplierNumber" -> 1))
      )

      newService(mock[SessionRepository]).count(answers) mustBe 2
    }

    "must return 0 when there are no vehicles" in {
      newService(mock[SessionRepository]).count(emptyUserAnswers) mustBe 0
    }
  }

  "VehicleService.inOrder" - {

    "must sort 10 after 2" in {
      val answers = emptyUserAnswers.unsafeSet(AllVehiclesQuery, Map("10" -> Json.obj(), "2" -> Json.obj()))

      newService(mock[SessionRepository]).inOrder(answers).map { case (number, _) => number } mustBe
        Seq(VehicleNumber(2), VehicleNumber(10))
    }

    "must return nothing when there are no vehicles" in {
      newService(mock[SessionRepository]).inOrder(emptyUserAnswers) mustBe empty
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
  }
}
