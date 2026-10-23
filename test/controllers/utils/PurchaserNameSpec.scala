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

package controllers.utils

import models.{DraftNotification, DraftNotificationSection, NovaUserType, UserContext}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsObject, Json}

class PurchaserNameSpec extends AnyFreeSpec with Matchers {

  private def draftWith(sections: (String, JsObject)*): DraftNotification =
    DraftNotification("12345", "2026-03-12", None, sections.map { case (id, data) => id -> DraftNotificationSection(Some(data)) }.toMap)

  private def userContextFor(userType: NovaUserType): UserContext =
    UserContext(
      userType = userType,
      selectedClient = None,
      notDeregistered = true,
      isAgentWithClientNoEnrolments = false,
      agentHasVatAgentEnrolment = false,
      isForBusinessUse = false
    )

  private val individual         = userContextFor(NovaUserType.PrivateIndividual)
  private val agentWithoutClient = userContextFor(NovaUserType.Agent)

  private val notifier  = "notifierDetails"  -> Json.obj("firstName" -> "NotifierFirstName", "lastName" -> "NotifierLastName")
  private val purchaser = "purchaserDetails" -> Json.obj("firstName" -> "PurchaserFirstName", "lastName" -> "PurchaserLastName")

  "PurchaserName.purchaserOrNotifierName" - {

    "must return the purchaser name when IQ3 is behalfOfPurchaser" in {
      val draft = draftWith("initialQuestions" -> Json.obj("notifyingAsPurchaser" -> "behalfOfPurchaser"), notifier, purchaser)

      PurchaserName.purchaserOrNotifierName(draft, individual) mustBe Some("PurchaserFirstName PurchaserLastName")
    }

    "must return the notifier name when IQ3 is self" in {
      val draft = draftWith("initialQuestions" -> Json.obj("notifyingAsPurchaser" -> "self"), notifier, purchaser)

      PurchaserName.purchaserOrNotifierName(draft, individual) mustBe Some("NotifierFirstName NotifierLastName")
    }

    "must put the title before the purchaser's first and last name when there is one" in {
      val draft = draftWith(
        "initialQuestions" -> Json.obj("notifyingAsPurchaser" -> "behalfOfPurchaser"),
        notifier,
        "purchaserDetails" -> Json.obj("title" -> "Mrs", "firstName" -> "PurchaserFirstName", "lastName" -> "PurchaserLastName")
      )

      PurchaserName.purchaserOrNotifierName(draft, individual) mustBe Some("Mrs PurchaserFirstName PurchaserLastName")
    }

    "must put the title before the notifier's first and last name when there is one" in {
      val draft = draftWith(
        "initialQuestions" -> Json.obj("notifyingAsPurchaser" -> "self"),
        "notifierDetails"  -> Json.obj("title" -> "Mr", "firstName" -> "NotifierFirstName", "lastName" -> "NotifierLastName")
      )

      PurchaserName.purchaserOrNotifierName(draft, individual) mustBe Some("Mr NotifierFirstName NotifierLastName")
    }

    "must ignore a blank title" in {
      val draft = draftWith("notifierDetails" -> Json.obj("title" -> " ", "firstName" -> "FirstName", "lastName" -> "LastName"))

      PurchaserName.purchaserOrNotifierName(draft, individual) mustBe Some("FirstName LastName")
    }

    "must return the business name over the first and last name" in {
      val draft = draftWith("notifierDetails" -> Json.obj("businessName" -> "Company 1 Ltd", "firstName" -> "FirstName", "lastName" -> "LastName"))

      PurchaserName.purchaserOrNotifierName(draft, individual) mustBe Some("Company 1 Ltd")
    }

    "must return the purchaser name for an agent without a client when IQ3 is self" in {
      val draft = draftWith("initialQuestions" -> Json.obj("notifyingAsPurchaser" -> "self"), notifier, purchaser)

      PurchaserName.purchaserOrNotifierName(draft, agentWithoutClient) mustBe Some("PurchaserFirstName PurchaserLastName")
    }

    "must return None for an agent without a client when purchaserDetails is not in the draft" in {
      val draft = draftWith("initialQuestions" -> Json.obj("notifyingAsPurchaser" -> "self"), notifier)

      PurchaserName.purchaserOrNotifierName(draft, agentWithoutClient) mustBe None
    }
  }
}
