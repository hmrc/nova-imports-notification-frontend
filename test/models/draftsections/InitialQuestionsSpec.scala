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

package models.draftsections

import models.{BusinessOrPrivateIndividual, PurchaserBusinessOrIndividual, PurchaserOrOnBehalf}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class InitialQuestionsSpec extends AnyFreeSpec with Matchers {

  "InitialQuestions.writes" - {

    "does not include fields that have no value" in {
      val model = InitialQuestions(
        vehicleFromEuToNi = true,
        isForBusinessUse = Some(true),
        areYouBusinessOrPrivate = None,
        notifyingAsPurchaserOrOnBehalf = None,
        isPurchaserBusinessOrPrivateIndividual = None,
        agentClientVehicleBusinessUse = None
      )

      Json.toJson(model) mustEqual Json.parse(
        """{"vehicleFromEuToNi":true,"isForBusinessUse":true}"""
      )
    }

    "includes the agent client vehicle business use field when it has a value" in {
      val model = InitialQuestions(
        vehicleFromEuToNi = true,
        isForBusinessUse = None,
        areYouBusinessOrPrivate = None,
        notifyingAsPurchaserOrOnBehalf = None,
        isPurchaserBusinessOrPrivateIndividual = None,
        agentClientVehicleBusinessUse = Some(false)
      )

      Json.toJson(model) mustEqual Json.parse(
        """{"vehicleFromEuToNi":true,"agentClientVehicleBusinessUse":false}"""
      )
    }

    "writes the correct JSON value when the notifier is a business" in {
      val model = InitialQuestions(
        vehicleFromEuToNi = true,
        isForBusinessUse = None,
        areYouBusinessOrPrivate = Some(BusinessOrPrivateIndividual.Business),
        notifyingAsPurchaserOrOnBehalf = Some(PurchaserOrOnBehalf.Purchaser),
        isPurchaserBusinessOrPrivateIndividual = None,
        agentClientVehicleBusinessUse = None
      )

      Json.toJson(model) mustEqual Json.parse(
        """{"vehicleFromEuToNi":true,"areYouBusinessOrPrivate":"business","notifyingAsPurchaserOrOnBehalf":"self"}"""
      )
    }

    "writes the correct JSON value when the purchaser is a non-VAT registered business" in {
      val model = InitialQuestions(
        vehicleFromEuToNi = true,
        isForBusinessUse = None,
        areYouBusinessOrPrivate = Some(BusinessOrPrivateIndividual.PrivateIndividual),
        notifyingAsPurchaserOrOnBehalf = Some(PurchaserOrOnBehalf.OnBehalfOfPurchaser),
        isPurchaserBusinessOrPrivateIndividual = Some(PurchaserBusinessOrIndividual.NonVatRegisteredBusiness),
        agentClientVehicleBusinessUse = None
      )

      Json.toJson(model) mustEqual Json.parse(
        """{
          |  "vehicleFromEuToNi":true,
          |  "areYouBusinessOrPrivate":"individual",
          |  "notifyingAsPurchaserOrOnBehalf":"behalfOfPurchaser",
          |  "isPurchaserBusinessOrPrivateIndividual":"NON_VAT_REG_BUSINESS"
          |}""".stripMargin
      )
    }

    "writes the correct JSON value when the purchaser is a private individual" in {
      val model = InitialQuestions(
        vehicleFromEuToNi = true,
        isForBusinessUse = None,
        areYouBusinessOrPrivate = Some(BusinessOrPrivateIndividual.PrivateIndividual),
        notifyingAsPurchaserOrOnBehalf = Some(PurchaserOrOnBehalf.OnBehalfOfPurchaser),
        isPurchaserBusinessOrPrivateIndividual = Some(PurchaserBusinessOrIndividual.NonVatRegisteredPrivateIndividual),
        agentClientVehicleBusinessUse = None
      )

      Json.toJson(model) mustEqual Json.parse(
        """{
          |  "vehicleFromEuToNi":true,
          |  "areYouBusinessOrPrivate":"individual",
          |  "notifyingAsPurchaserOrOnBehalf":"behalfOfPurchaser",
          |  "isPurchaserBusinessOrPrivateIndividual":"PRIVATE_INDIVIDUAL"
          |}""".stripMargin
      )
    }
  }
}
