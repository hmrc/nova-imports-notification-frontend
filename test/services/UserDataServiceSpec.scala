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
import models.{DraftNotification, DraftNotificationSection, NameDetails, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.purchaserDetails.{PurchaserBusinessNamePage, PurchaserNamePage}
import play.api.libs.json.{JsObject, Json, Writes}
import repositories.SessionRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserDataServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  // Applies the set for real so we can assert the rehydrated pages, without needing Mongo.
  private def stubSessionRepository(): SessionRepository = {
    val repo = mock[SessionRepository]
    when(repo.setPage(any(), any(), any())(any())).thenAnswer { (invocation: org.mockito.invocation.InvocationOnMock) =>
      val answers = invocation.getArgument[UserAnswers](0)
      val page    = invocation.getArgument[queries.Settable[Any]](1)
      val value   = invocation.getArgument[Any](2)
      val writes  = invocation.getArgument[Writes[Any]](3)
      Future.successful(answers.set(page, value)(writes).get)
    }
    repo
  }

  private def draftWith(sections: Map[String, DraftNotificationSection]): DraftNotification =
    DraftNotification(draftId = "1", createdDate = "2026-01-01", lastUpdatedDate = None, sections = sections)

  private def draftWithPurchaser(section: Option[JsObject]): DraftNotification =
    draftWith(Map("purchaserDetails" -> DraftNotificationSection(section)))

  "UserDataService.storePurchaserDetailsPages" - {

    "must rehydrate PurchaserNamePage from an individual purchaser section" in {
      val draft  = draftWithPurchaser(Some(Json.obj("title" -> "Mr", "firstName" -> "John", "lastName" -> "Smith")))
      val result = UserDataService.storePurchaserDetailsPages(draft, emptyUserAnswers, stubSessionRepository()).futureValue

      result.get(PurchaserNamePage) mustBe Some(NameDetails("Mr", "John", "Smith"))
      result.get(PurchaserBusinessNamePage) mustBe None
    }

    "must rehydrate PurchaserBusinessNamePage from a business purchaser section" in {
      val draft  = draftWithPurchaser(Some(Json.obj("businessName" -> "Acme Trading Ltd")))
      val result = UserDataService.storePurchaserDetailsPages(draft, emptyUserAnswers, stubSessionRepository()).futureValue

      result.get(PurchaserBusinessNamePage) mustBe Some("Acme Trading Ltd")
      result.get(PurchaserNamePage) mustBe None
    }

    "must leave answers unchanged when the purchaser section has no data" in {
      val result = UserDataService.storePurchaserDetailsPages(draftWithPurchaser(None), emptyUserAnswers, stubSessionRepository()).futureValue

      result.get(PurchaserNamePage) mustBe None
      result.get(PurchaserBusinessNamePage) mustBe None
    }

    "must leave answers unchanged when the draft has no purchaser section" in {
      val result = UserDataService.storePurchaserDetailsPages(draftWith(Map.empty), emptyUserAnswers, stubSessionRepository()).futureValue

      result.get(PurchaserNamePage) mustBe None
      result.get(PurchaserBusinessNamePage) mustBe None
    }
  }
}
