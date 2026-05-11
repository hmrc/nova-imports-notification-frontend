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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsSuccess, Json}

class NotificationSummarySpec extends AnyFreeSpec with Matchers {

  "NotificationSummary.reads" - {

    "parses an IndividualOrOrganisation response with hasClients explicitly null" in {
      val json = Json.parse(
        """{"traderName":"ABC LTD","vrn":"123456789","hasDraftNotifications":true,"hasClients":null}"""
      )

      json.validate[NotificationSummary] mustEqual JsSuccess(
        NotificationSummary.IndividualOrOrganisation(
          traderName = "ABC LTD",
          vrn = "123456789",
          hasDraftNotifications = true
        )
      )
    }

    "parses an IndividualOrOrganisation response with hasClients omitted" in {
      val json = Json.parse(
        """{"traderName":"ABC LTD","vrn":"123456789","hasDraftNotifications":false}"""
      )

      json.validate[NotificationSummary] mustEqual JsSuccess(
        NotificationSummary.IndividualOrOrganisation(
          traderName = "ABC LTD",
          vrn = "123456789",
          hasDraftNotifications = false
        )
      )
    }

    "parses an AgentWithoutClient response when hasClients is true and clientVrn is absent" in {
      val json = Json.parse(
        """{"traderName":"ABC LTD","vrn":"123456789","hasDraftNotifications":true,"hasClients":true}"""
      )

      json.validate[NotificationSummary] mustEqual JsSuccess(
        NotificationSummary.AgentWithoutClient(
          traderName = "ABC LTD",
          vrn = "123456789",
          hasDraftNotifications = true,
          hasClients = true
        )
      )
    }

    "parses an AgentWithoutClient response when hasClients is false and clientVrn is absent" in {
      val json = Json.parse(
        """{"traderName":"ABC LTD","vrn":"123456789","hasDraftNotifications":false,"hasClients":false}"""
      )

      json.validate[NotificationSummary] mustEqual JsSuccess(
        NotificationSummary.AgentWithoutClient(
          traderName = "ABC LTD",
          vrn = "123456789",
          hasDraftNotifications = false,
          hasClients = false
        )
      )
    }

    "parses an AgentWithClient response whenever clientVrn is present (regardless of hasClients value)" in {
      val json = Json.parse(
        """{
          |  "traderName":"ABC LTD",
          |  "vrn":"0",
          |  "clientTraderName":"CLIENT ABC LTD",
          |  "clientVrn":"123456789",
          |  "clientHasDraftNotifications":true,
          |  "hasClients":true
          |}""".stripMargin
      )

      json.validate[NotificationSummary] mustEqual JsSuccess(
        NotificationSummary.AgentWithClient(
          traderName = "ABC LTD",
          vrn = "0",
          clientTraderName = "CLIENT ABC LTD",
          clientVrn = "123456789",
          clientHasDraftNotifications = true,
          hasClients = true
        )
      )
    }

    "fails when an IndividualOrOrganisation response is missing hasDraftNotifications" in {
      val json = Json.parse(
        """{"traderName":"ABC LTD","vrn":"123456789"}"""
      )

      json.validate[NotificationSummary] mustBe a[JsError]
    }

    "fails when an AgentWithoutClient response is missing hasDraftNotifications" in {
      val json = Json.parse(
        """{"traderName":"ABC LTD","vrn":"123456789","hasClients":true}"""
      )

      json.validate[NotificationSummary] mustBe a[JsError]
    }

    "fails when an AgentWithClient response is missing clientHasDraftNotifications" in {
      val json = Json.parse(
        """{
          |  "traderName":"ABC LTD",
          |  "vrn":"0",
          |  "clientTraderName":"CLIENT ABC LTD",
          |  "clientVrn":"123456789",
          |  "hasClients":true
          |}""".stripMargin
      )

      json.validate[NotificationSummary] mustBe a[JsError]
    }
  }
}
