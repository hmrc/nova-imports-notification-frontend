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
import models.{ImportNumber, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import queries.AllImportsQuery
import repositories.SessionRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

class ImportServiceSpec extends SpecBase with MockitoSugar {

  private def newService(repo: SessionRepository, vehicles: VehicleService = mock[VehicleService]): ImportService =
    new ImportServiceImpl(repo, vehicles)

  private def mockSessionRepoThatSaves(answers: UserAnswers): SessionRepository = {
    val repo = mock[SessionRepository]
    when(repo.setPage(any(), eqTo(AllImportsQuery), any())(any())).thenReturn(Future.successful(answers))
    when(repo.set(any())).thenReturn(Future.successful(true))
    repo
  }

  private def mockVehicleServiceThatRemoves(answers: UserAnswers): VehicleService = {
    val vehicles = mock[VehicleService]
    when(vehicles.deleteValuesForImport(any(), any())).thenReturn(Success(answers))
    vehicles
  }

  private val deleted: JsObject = Json.obj("deleted" -> true)

  private val answered: JsObject = Json.obj("importEntryNumber" -> "123456789A")

  "ImportService.add" - {

    "must return import number 1 when there are no imports" in {
      val repo = mockSessionRepoThatSaves(emptyUserAnswers)

      val number = newService(repo).add(emptyUserAnswers).futureValue

      number mustBe ImportNumber(1)
    }

    "must store the new numbered import as an empty record" in {
      val repo = mockSessionRepoThatSaves(emptyUserAnswers)

      newService(repo).add(emptyUserAnswers).futureValue

      verify(repo).setPage(any(), eqTo(AllImportsQuery), eqTo(Map("1" -> Json.obj())))(any())
    }

    "must return the next number after the highest, even when there are gaps" in {
      val answers = emptyUserAnswers.unsafeSet(AllImportsQuery, Map("2" -> answered, "5" -> answered))
      val repo    = mockSessionRepoThatSaves(answers)

      val number = newService(repo).add(answers).futureValue

      number mustBe ImportNumber(6)
    }

    "must keep the existing imports when adding a new one" in {
      val existing = Map("2" -> answered)
      val answers  = emptyUserAnswers.unsafeSet(AllImportsQuery, existing)
      val repo     = mockSessionRepoThatSaves(answers)

      newService(repo).add(answers).futureValue

      verify(repo).setPage(any(), eqTo(AllImportsQuery), eqTo(existing + ("3" -> Json.obj())))(any())
    }

    "must reuse import 3 when it holds no values" in {
      val answers = emptyUserAnswers.unsafeSet(AllImportsQuery, Map("2" -> answered, "3" -> Json.obj()))
      val repo    = mockSessionRepoThatSaves(answers)

      val number = newService(repo).add(answers).futureValue

      number mustBe ImportNumber(3)
    }

    "must not return a deleted import's number out again, must be next highest number" in {
      val answers = emptyUserAnswers.unsafeSet(AllImportsQuery, Map("1" -> answered, "2" -> deleted))
      val repo    = mockSessionRepoThatSaves(answers)

      val number = newService(repo).add(answers).futureValue

      number mustBe ImportNumber(3)
    }
  }

  "ImportService.numberHasValues" - {

    "must be false for an empty import collection" in {
      val answers = emptyUserAnswers.unsafeSet(AllImportsQuery, Map("1" -> Json.obj()))

      newService(mock[SessionRepository]).numberHasValues(answers, ImportNumber(1)) mustBe false
    }

    "must be true for an import holding an answer" in {
      val answers = emptyUserAnswers.unsafeSet(AllImportsQuery, Map("1" -> answered))

      newService(mock[SessionRepository]).numberHasValues(answers, ImportNumber(1)) mustBe true
    }
  }

  "ImportService.deleteValues" - {

    "must empty import 1 and mark it deleted, keeping its number in the session" in {
      val answers = emptyUserAnswers.unsafeSet(AllImportsQuery, Map("1" -> answered, "2" -> answered))

      val result = newService(mockSessionRepoThatSaves(answers), mockVehicleServiceThatRemoves(answers))
        .deleteValues(answers, ImportNumber(1))
        .futureValue

      result.get(AllImportsQuery).value mustBe Map("1" -> deleted, "2" -> answered)
    }

    "must empty the import and its vehicles in a single write" in {
      val answers = emptyUserAnswers.unsafeSet(AllImportsQuery, Map("1" -> answered))
      val repo    = mockSessionRepoThatSaves(answers)

      newService(repo, mockVehicleServiceThatRemoves(answers)).deleteValues(answers, ImportNumber(1)).futureValue

      verify(repo, times(1)).set(any())
      verify(repo, never).setPage(any(), any(), any())(any())
    }

    "must leave imports 1 and 2 unchanged when import 9 is not in the session" in {
      val existing = Map("1" -> answered, "2" -> answered)
      val answers  = emptyUserAnswers.unsafeSet(AllImportsQuery, existing)

      val result = newService(mockSessionRepoThatSaves(answers), mockVehicleServiceThatRemoves(answers))
        .deleteValues(answers, ImportNumber(9))
        .futureValue

      result.get(AllImportsQuery).value mustBe existing
    }
  }

  "ImportService.inOrder" - {

    "must sort the imports into ascending number order" in {
      val answers = emptyUserAnswers.unsafeSet(
        AllImportsQuery,
        Map("3" -> answered, "1" -> answered, "4" -> answered, "2" -> answered)
      )

      newService(mock[SessionRepository]).inOrder(answers).map { case (number, _) => number } mustBe
        Seq(ImportNumber(1), ImportNumber(2), ImportNumber(3), ImportNumber(4))
    }

    "must sort 10 after 2" in {
      val answers = emptyUserAnswers.unsafeSet(AllImportsQuery, Map("10" -> answered, "2" -> answered))

      newService(mock[SessionRepository]).inOrder(answers).map { case (number, _) => number } mustBe
        Seq(ImportNumber(2), ImportNumber(10))
    }

    "must return nothing when there are no imports" in {
      newService(mock[SessionRepository]).inOrder(emptyUserAnswers) mustBe empty
    }

    "must ignore deleted and empty imports" in {
      val answers = emptyUserAnswers
        .unsafeSet(AllImportsQuery, Map("1" -> answered, "2" -> deleted, "3" -> Json.obj()))

      newService(mock[SessionRepository]).inOrder(answers).map { case (number, _) => number } mustBe
        Seq(ImportNumber(1))
    }
  }

  "ImportService.numberExists" - {

    "must return true for an import that exists" in {
      val answers = emptyUserAnswers.unsafeSet(AllImportsQuery, Map("2" -> Json.obj(), "5" -> Json.obj()))

      newService(mock[SessionRepository]).numberExists(answers, ImportNumber(5)) mustBe true
    }

    "must return false for an import that does not exist" in {
      val answers = emptyUserAnswers.unsafeSet(AllImportsQuery, Map("2" -> Json.obj(), "5" -> Json.obj()))

      newService(mock[SessionRepository]).numberExists(answers, ImportNumber(3)) mustBe false
    }

    "must return false when there are no imports" in {
      newService(mock[SessionRepository]).numberExists(emptyUserAnswers, ImportNumber(1)) mustBe false
    }

    "must return false when import 3 is marked deleted" in {
      val answers = emptyUserAnswers.unsafeSet(AllImportsQuery, Map("3" -> Json.obj("deleted" -> true)))

      newService(mock[SessionRepository]).numberExists(answers, ImportNumber(3)) mustBe false
    }
  }
}
