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
import models.{SupplierNumber, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import queries.AllSuppliersQuery
import repositories.SessionRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

class SupplierServiceSpec extends SpecBase with MockitoSugar {

  private def newService(repo: SessionRepository, vehicles: VehicleService = mock[VehicleService]): SupplierService =
    new SupplierServiceImpl(repo, vehicles)

  private def mockSessionRepoThatSaves(answers: UserAnswers): SessionRepository = {
    val repo = mock[SessionRepository]
    when(repo.setPage(any(), eqTo(AllSuppliersQuery), any())(any())).thenReturn(Future.successful(answers))
    when(repo.set(any())).thenReturn(Future.successful(true))
    repo
  }

  private def mockVehicleServiceThatRemoves(answers: UserAnswers): VehicleService = {
    val vehicles = mock[VehicleService]
    when(vehicles.removeForSupplier(any(), any())).thenReturn(Success(answers))
    vehicles
  }

  "SupplierService.add" - {

    "must return supplier number 1 when there are no suppliers yet" in {
      val repo = mockSessionRepoThatSaves(emptyUserAnswers)

      val number = newService(repo).add(emptyUserAnswers).futureValue

      number mustBe SupplierNumber(1)
    }

    "must store the new supplier as an empty record" in {
      val repo = mockSessionRepoThatSaves(emptyUserAnswers)

      newService(repo).add(emptyUserAnswers).futureValue

      verify(repo).setPage(any(), eqTo(AllSuppliersQuery), eqTo(Map("1" -> Json.obj())))(any())
    }

    "must return the next number after the highest, even when there are gaps" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("2" -> Json.obj(), "5" -> Json.obj()))
      val repo    = mockSessionRepoThatSaves(answers)

      val number = newService(repo).add(answers).futureValue

      number mustBe SupplierNumber(6)
    }

    "must keep the existing suppliers when adding a new one" in {
      val existing = Map("2" -> Json.obj("supplierBusinessName" -> "Test Ltd"))
      val answers  = emptyUserAnswers.unsafeSet(AllSuppliersQuery, existing)
      val repo     = mockSessionRepoThatSaves(answers)

      newService(repo).add(answers).futureValue

      verify(repo).setPage(any(), eqTo(AllSuppliersQuery), eqTo(existing + ("3" -> Json.obj())))(any())
    }
  }

  "SupplierService.delete" - {

    "must remove the supplier from the session" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj(), "2" -> Json.obj()))

      val result =
        newService(mockSessionRepoThatSaves(answers), mockVehicleServiceThatRemoves(answers)).delete(answers, SupplierNumber(1)).futureValue

      result.get(AllSuppliersQuery).value mustBe Map("2" -> Json.obj())
    }

    "must ask VehicleService to remove supplier 1's vehicles when supplier 1 is deleted" in {
      val answers  = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj()))
      val vehicles = mockVehicleServiceThatRemoves(answers)

      newService(mockSessionRepoThatSaves(answers), vehicles).delete(answers, SupplierNumber(1)).futureValue

      verify(vehicles).removeForSupplier(any(), eqTo(SupplierNumber(1)))
    }

    "must delete the supplier and its vehicles in one write" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj()))
      val repo    = mockSessionRepoThatSaves(answers)

      newService(repo, mockVehicleServiceThatRemoves(answers)).delete(answers, SupplierNumber(1)).futureValue

      verify(repo, times(1)).set(any())
      verify(repo, never).setPage(any(), any(), any())(any())
    }

    "must leave suppliers 1 and 2 unchanged when deleting supplier 9, which does not exist" in {
      val existing = Map("1" -> Json.obj(), "2" -> Json.obj())
      val answers  = emptyUserAnswers.unsafeSet(AllSuppliersQuery, existing)

      val result =
        newService(mockSessionRepoThatSaves(answers), mockVehicleServiceThatRemoves(answers)).delete(answers, SupplierNumber(9)).futureValue

      result.get(AllSuppliersQuery).value mustBe existing
    }
  }

  "SupplierService.inOrder" - {

    "must sort 10 after 2" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("10" -> Json.obj(), "2" -> Json.obj()))

      newService(mock[SessionRepository]).inOrder(answers).map { case (number, _) => number } mustBe
        Seq(SupplierNumber(2), SupplierNumber(10))
    }

    "must return nothing when there are no suppliers" in {
      newService(mock[SessionRepository]).inOrder(emptyUserAnswers) mustBe empty
    }
  }

  "SupplierService.numberExists" - {

    "must return true for a supplier that exists" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("2" -> Json.obj(), "5" -> Json.obj()))

      newService(mock[SessionRepository]).numberExists(answers, SupplierNumber(5)) mustBe true
    }

    "must return false for a supplier that does not exist" in {
      val answers = emptyUserAnswers.unsafeSet(AllSuppliersQuery, Map("2" -> Json.obj(), "5" -> Json.obj()))

      newService(mock[SessionRepository]).numberExists(answers, SupplierNumber(3)) mustBe false
    }

    "must return false when there are no suppliers" in {
      newService(mock[SessionRepository]).numberExists(emptyUserAnswers, SupplierNumber(1)) mustBe false
    }
  }
}
