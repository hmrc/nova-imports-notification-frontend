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

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsObject, JsSuccess, Json}

class DraftNotificationSpec extends AnyFreeSpec with Matchers with OptionValues {

  "SectionStatus.format" - {

    "reads the wire values" in {
      Json.parse("\"completed\"").validate[SectionStatus] mustEqual JsSuccess(SectionStatus.Completed)
      Json.parse("\"not-yet-saved\"").validate[SectionStatus] mustEqual JsSuccess(SectionStatus.NotYetSaved)
    }

    "rejects unknown statuses" in {
      Json.parse("\"incomplete\"").validate[SectionStatus] mustBe JsSuccess(SectionStatus.Incomplete)
    }

    "writes the wire values" in {
      Json.toJson[SectionStatus](SectionStatus.Completed).toString mustEqual "\"completed\""
      Json.toJson[SectionStatus](SectionStatus.NotYetSaved).toString mustEqual "\"not-yet-saved\""
    }
  }

  "DraftNotification.format" - {

    "parses a supplier-journey organisation response with notifierAddress not yet saved" in {
      val json = Json.parse(
        """
          |{
          |  "draftId": "12345",
          |  "createdDate": "2026-03-01",
          |  "lastUpdatedDate": "2026-03-20",
          |  "sections": {
          |    "introduction": { "status": "completed", "data": { "acknowledged": true } },
          |    "initialQuestions": { "status": "completed", "data": { "purchasingVehiclesEu": false } },
          |    "notifierDetails": { "status": "completed", "data": { "businessName": "ABC Ltd" } },
          |    "notifierAddress": { "status": "not-yet-saved", "data": null },
          |    "supplierSelfSupply": { "status": "not-yet-saved", "data": null },
          |    "vehicles": { "status": "not-yet-saved", "data": null },
          |    "declaration": { "status": "not-yet-saved", "data": null }
          |  }
          |}
          |""".stripMargin
      )

      val result = json.validate[DraftNotification]
      result mustBe a[JsSuccess[?]]

      val draft = result.get
      draft.draftId mustEqual "12345"
      draft.createdDate mustEqual "2026-03-01"
      draft.lastUpdatedDate mustEqual Some("2026-03-20")
      draft.sections("introduction").data mustEqual Some(Json.obj("acknowledged" -> true))
      draft.sections("notifierDetails").data mustEqual Some(Json.obj("businessName" -> "ABC Ltd"))
      draft.sections("notifierAddress").data mustEqual None
      draft.sections("declaration").data mustEqual None
    }

    "parses a response that omits lastUpdatedDate" in {
      val json = Json.parse(
        """
          |{
          |  "draftId": "12345",
          |  "createdDate": "2026-03-01",
          |  "sections": {
          |    "notifierDetails": { "status": "not-yet-saved", "data": null }
          |  }
          |}
          |""".stripMargin
      )

      val draft = json.validate[DraftNotification].get
      draft.lastUpdatedDate mustEqual None
    }

    "parses a supplier-journey organisation response with notifierAddress completed" in {
      val json = Json.parse(
        """
          |{
          |  "draftId": "12345",
          |  "createdDate": "2026-03-01",
          |  "lastUpdatedDate": "2026-03-20",
          |  "sections": {
          |    "notifierAddress": {
          |      "status": "completed",
          |      "data": {
          |        "line1": "1 Test Street",
          |        "line2": "Testtown",
          |        "line3": "Somerset",
          |        "line4": "",
          |        "postCode": "ZZ01 1ZZ",
          |        "country": "",
          |        "isManualAddress": false
          |      }
          |    }
          |  }
          |}
          |""".stripMargin
      )

      val draft                 = json.validate[DraftNotification].get
      val addressData: JsObject = draft.sections("notifierAddress").data.value
      (addressData \ "line1").as[String] mustEqual "1 Test Street"
      (addressData \ "postCode").as[String] mustEqual "ZZ01 1ZZ"
      (addressData \ "isManualAddress").as[Boolean] mustEqual false
    }
  }
}
