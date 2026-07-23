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
import models.DraftNotification.SectionId
import models.{Address, BusinessOrPrivateIndividual, ContactNumbers, Country, DraftNotification, DraftNotificationSection, NameDetails, SectionStatus, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.BusinessOrPrivatePage
import pages.sections.notifierDetails.{EmailAddressPage, NameDetailsPage, PhoneNumberPage}
import pages.sections.notifieraddress.AddressPage
import pages.sections.purchaseraddress.IsPurchaserAddressInTheUkPage
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

  private val contactNumbers = ContactNumbers(Some("01234567890"), None)
  private val gb             = Country("GB", "United Kingdom")
  private val sampleAddress  = Address(lines = List("12 High Street", "Reading"), postcode = Some("RE12 9GC"), country = gb)

  "UserDataService.privateIndividual" - {

    "must mark the purchaser details section Completed when a purchaser name is present" in {
      val answers = emptyUserAnswers.unsafeSet(PurchaserNamePage, NameDetails("Mr", "John", "Smith"))
      UserDataService.privateIndividual(answers)(SectionId.PurchaserDetails) mustBe SectionStatus.Completed
    }

    "must mark the purchaser details section Completed when a purchaser business name is present" in {
      val answers = emptyUserAnswers.unsafeSet(PurchaserBusinessNamePage, "Acme Trading Ltd")
      UserDataService.privateIndividual(answers)(SectionId.PurchaserDetails) mustBe SectionStatus.Completed
    }

    "must mark the purchaser details section NotYetSaved when no purchaser name is present" in {
      UserDataService.privateIndividual(emptyUserAnswers)(SectionId.PurchaserDetails) mustBe SectionStatus.NotYetSaved
    }

    "must mark the purchaser address section Incomplete when the in-UK question is answered" in {
      val answers = emptyUserAnswers.unsafeSet(IsPurchaserAddressInTheUkPage, true)
      UserDataService.privateIndividual(answers)(SectionId.PurchaserAddress) mustBe SectionStatus.Incomplete
    }

    "must mark the purchaser address section NotYetSaved when the in-UK question is unanswered" in {
      UserDataService.privateIndividual(emptyUserAnswers)(SectionId.PurchaserAddress) mustBe SectionStatus.NotYetSaved
    }

    "must mark the notifier address section Completed when an address is stored" in {
      val answers = emptyUserAnswers.unsafeSet(AddressPage, sampleAddress)
      UserDataService.privateIndividual(answers)(SectionId.NotifierAddress) mustBe SectionStatus.Completed
    }

    "must mark the notifier address section Incomplete when no address is stored" in {
      UserDataService.privateIndividual(emptyUserAnswers)(SectionId.NotifierAddress) mustBe SectionStatus.Incomplete
    }

    "must mark the notifier details section Completed for a private individual with name, phone and email" in {
      val answers = emptyUserAnswers
        .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.PrivateIndividual)
        .unsafeSet(NameDetailsPage, NameDetails("Mr", "John", "Smith"))
        .unsafeSet(PhoneNumberPage, contactNumbers)
        .unsafeSet(EmailAddressPage, "john@example.com")
      UserDataService.privateIndividual(answers)(SectionId.NotifierDetails) mustBe SectionStatus.Completed
    }

    "must mark the notifier details section Completed for a business with phone and email but no name" in {
      val answers = emptyUserAnswers
        .unsafeSet(BusinessOrPrivatePage, BusinessOrPrivateIndividual.Business)
        .unsafeSet(PhoneNumberPage, contactNumbers)
        .unsafeSet(EmailAddressPage, "acme@example.com")
      UserDataService.privateIndividual(answers)(SectionId.NotifierDetails) mustBe SectionStatus.Completed
    }

    "must mark the notifier details section NotYetSaved when name, phone and email are all absent" in {
      UserDataService.privateIndividual(emptyUserAnswers)(SectionId.NotifierDetails) mustBe SectionStatus.NotYetSaved
    }

    "must mark the notifier details section Incomplete when only some details are present" in {
      val answers = emptyUserAnswers.unsafeSet(PhoneNumberPage, contactNumbers)
      UserDataService.privateIndividual(answers)(SectionId.NotifierDetails) mustBe SectionStatus.Incomplete
    }
  }

  "UserDataService.agentWithoutClient" - {

    "must mark the notifier details section Completed with only phone and email (no name required)" in {
      val answers = emptyUserAnswers
        .unsafeSet(PhoneNumberPage, contactNumbers)
        .unsafeSet(EmailAddressPage, "agent@example.com")
      UserDataService.agentWithoutClient(answers)(SectionId.NotifierDetails) mustBe SectionStatus.Completed
    }

    "must mark the notifier details section NotYetSaved when both phone and email are absent" in {
      UserDataService.agentWithoutClient(emptyUserAnswers)(SectionId.NotifierDetails) mustBe SectionStatus.NotYetSaved
    }

    "must mark the notifier details section Incomplete when only phone is present" in {
      val answers = emptyUserAnswers.unsafeSet(PhoneNumberPage, contactNumbers)
      UserDataService.agentWithoutClient(answers)(SectionId.NotifierDetails) mustBe SectionStatus.Incomplete
    }

    "must mark the notifier address section NotYetSaved" in {
      val answers = emptyUserAnswers.unsafeSet(AddressPage, sampleAddress)
      UserDataService.agentWithoutClient(answers)(SectionId.NotifierAddress) mustBe SectionStatus.NotYetSaved
    }

    "must include the purchaser details section, Completed when a purchaser name is present" in {
      val answers = emptyUserAnswers.unsafeSet(PurchaserNamePage, NameDetails("Mr", "John", "Smith"))
      UserDataService.agentWithoutClient(answers)(SectionId.PurchaserDetails) mustBe SectionStatus.Completed
    }

    "must include the purchaser address section, NotYetSaved when unanswered" in {
      UserDataService.agentWithoutClient(emptyUserAnswers)(SectionId.PurchaserAddress) mustBe SectionStatus.NotYetSaved
    }
  }
}
