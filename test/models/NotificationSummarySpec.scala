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

    "parses an IndividualOrOrganisation response with vrn present" in {
      val json = Json.parse(
        """{"traderName":"ABC LTD","vrn":"123456789","hasDraftNotifications":true,"isDeregistered":false}"""
      )

      json.validate[NotificationSummary] mustEqual JsSuccess(
        NotificationSummary.IndividualOrOrganisation(
          traderName = Some("ABC LTD"),
          vrn = Some("123456789"),
          hasDraftNotifications = true,
          isDeregistered = false
        )
      )
    }

    "parses an IndividualOrOrganisation response with vrn omitted" in {
      val json = Json.parse(
        """{"traderName":"ABC LTD","hasDraftNotifications":false,"isDeregistered":false}"""
      )

      json.validate[NotificationSummary] mustEqual JsSuccess(
        NotificationSummary.IndividualOrOrganisation(
          traderName = Some("ABC LTD"),
          vrn = None,
          hasDraftNotifications = false,
          isDeregistered = false
        )
      )
    }

    "parses an IndividualOrOrganisation response with traderName omitted" in {
      val json = Json.parse(
        """{"hasDraftNotifications":false,"isDeregistered":false}"""
      )

      json.validate[NotificationSummary] mustEqual JsSuccess(
        NotificationSummary.IndividualOrOrganisation(
          traderName = None,
          vrn = None,
          hasDraftNotifications = false,
          isDeregistered = false
        )
      )
    }

    "parses an IndividualOrOrganisation response with isDeregistered true" in {
      val json = Json.parse(
        """{"traderName":"ABC LTD","hasDraftNotifications":false,"isDeregistered":true}"""
      )

      json.validate[NotificationSummary] mustEqual JsSuccess(
        NotificationSummary.IndividualOrOrganisation(
          traderName = Some("ABC LTD"),
          vrn = None,
          hasDraftNotifications = false,
          isDeregistered = true
        )
      )
    }

    "parses an AgentWithoutClient response when hasClients is null" in {
      val json = Json.parse(
        """{"agentName":"ABC Consultancy","hasDraftNotifications":false,"hasClients":null}"""
      )

      json.validate[NotificationSummary] mustEqual JsSuccess(
        NotificationSummary.AgentWithoutClient(
          agentName = Some("ABC Consultancy"),
          hasDraftNotifications = false
        )
      )
    }

    "parses an AgentWithoutClient response with no agentName as AgentWithoutClient not IndividualOrOrganisation" in {
      val json = Json.parse(
        """{"hasDraftNotifications":false,"hasClients":null}"""
      )

      json.validate[NotificationSummary] mustEqual JsSuccess(
        NotificationSummary.AgentWithoutClient(
          agentName = None,
          hasDraftNotifications = false
        )
      )
    }

    "parses an AgentWithClient response" in {
      val json = Json.parse(
        """{
          |  "agentName":"ABC Consultancy",
          |  "clientTraderName":"CLIENT ABC LTD",
          |  "clientVrn":"123456789",
          |  "clientHasDraftNotifications":true,
          |  "clientIsDeregistered":false,
          |  "hasClients":true
          |}""".stripMargin
      )

      json.validate[NotificationSummary] mustEqual JsSuccess(
        NotificationSummary.AgentWithClient(
          agentName = Some("ABC Consultancy"),
          clientTraderName = Some("CLIENT ABC LTD"),
          clientVrn = "123456789",
          clientHasDraftNotifications = true,
          clientIsDeregistered = false
        )
      )
    }

    "parses an AgentWithClient response with clientIsDeregistered true" in {
      val json = Json.parse(
        """{
          |  "agentName":"ABC Consultancy",
          |  "clientTraderName":"CLIENT ABC LTD",
          |  "clientVrn":"123456789",
          |  "clientHasDraftNotifications":true,
          |  "clientIsDeregistered":true,
          |  "hasClients":true
          |}""".stripMargin
      )

      json.validate[NotificationSummary] mustEqual JsSuccess(
        NotificationSummary.AgentWithClient(
          agentName = Some("ABC Consultancy"),
          clientTraderName = Some("CLIENT ABC LTD"),
          clientVrn = "123456789",
          clientHasDraftNotifications = true,
          clientIsDeregistered = true
        )
      )
    }

    "parses an AgentWithClient response with no agentName or clientTraderName" in {
      val json = Json.parse(
        """{
          |  "clientVrn":"123456789",
          |  "clientHasDraftNotifications":true,
          |  "clientIsDeregistered":false,
          |  "hasClients":true
          |}""".stripMargin
      )

      json.validate[NotificationSummary] mustEqual JsSuccess(
        NotificationSummary.AgentWithClient(
          agentName = None,
          clientTraderName = None,
          clientVrn = "123456789",
          clientHasDraftNotifications = true,
          clientIsDeregistered = false
        )
      )
    }

    "prefers AgentWithClient whenever clientVrn is present" in {
      val json = Json.parse(
        """{
          |  "agentName":"ABC Consultancy",
          |  "clientTraderName":"CLIENT ABC LTD",
          |  "clientVrn":"123456789",
          |  "clientHasDraftNotifications":false,
          |  "clientIsDeregistered":false,
          |  "hasClients":true
          |}""".stripMargin
      )

      json.validate[NotificationSummary] mustBe a[JsSuccess[?]]
      json.validate[NotificationSummary].get mustBe a[NotificationSummary.AgentWithClient]
    }

    "fails when an IndividualOrOrganisation response is missing hasDraftNotifications" in {
      val json = Json.parse(
        """{"traderName":"ABC LTD","isDeregistered":false}"""
      )

      json.validate[NotificationSummary] mustBe a[JsError]
    }

    "fails when an IndividualOrOrganisation response is missing isDeregistered" in {
      val json = Json.parse(
        """{"traderName":"ABC LTD","hasDraftNotifications":false}"""
      )

      json.validate[NotificationSummary] mustBe a[JsError]
    }

    "fails when an AgentWithClient response is missing clientHasDraftNotifications" in {
      val json = Json.parse(
        """{
          |  "agentName":"ABC Consultancy",
          |  "clientTraderName":"CLIENT ABC LTD",
          |  "clientVrn":"123456789",
          |  "clientIsDeregistered":false,
          |  "hasClients":true
          |}""".stripMargin
      )

      json.validate[NotificationSummary] mustBe a[JsError]
    }
  }
}
