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

    "omits optional fields that have no value, but always writes the required flag fields" in {
      val model = InitialQuestions(
        bringingVehicleNI = true,
        isForBusinessUse = Some(true),
        areYouBusinessPrivate = None,
        notifyingAsPurchaser = None,
        purchaserBusinessPrivate = None,
        agentClientVehicleBusinessUse = None,
        bringingVehicleBusiness = None
      )

      Json.toJson(model) mustEqual Json.obj(
        "bringingVehicleNI"              -> true,
        "isForBusinessUse"               -> true,
        "bringingVehicleBusinessNeeded"  -> false,
        "purchasingVehiclesEuNeeded"     -> true,
        "purchaserBusinessPrivateNeeded" -> true,
        "sccCustomerFieldNeeded"         -> true,
        "deregistered"                   -> false,
        "registered"                     -> false,
        "currentlyRegistered"            -> false
      )
    }

    "includes the agent client vehicle business use field when it has a value" in {
      val model = InitialQuestions(
        bringingVehicleNI = true,
        isForBusinessUse = None,
        areYouBusinessPrivate = None,
        notifyingAsPurchaser = None,
        purchaserBusinessPrivate = None,
        agentClientVehicleBusinessUse = Some(false),
        bringingVehicleBusiness = None
      )

      Json.toJson(model) mustEqual Json.obj(
        "bringingVehicleNI"              -> true,
        "agentClientVehicleBusinessUse"  -> false,
        "bringingVehicleBusinessNeeded"  -> false,
        "purchasingVehiclesEuNeeded"     -> true,
        "purchaserBusinessPrivateNeeded" -> true,
        "sccCustomerFieldNeeded"         -> true,
        "deregistered"                   -> false,
        "registered"                     -> false,
        "currentlyRegistered"            -> false
      )
    }

    "includes bringingVehicleBusiness and bringingVehicleBusinessNeeded when a VAT-registered organisation has answered" in {
      val model = InitialQuestions(
        bringingVehicleNI = true,
        isForBusinessUse = Some(true),
        areYouBusinessPrivate = None,
        notifyingAsPurchaser = None,
        purchaserBusinessPrivate = None,
        agentClientVehicleBusinessUse = None,
        bringingVehicleBusiness = Some(true),
        bringingVehicleBusinessNeeded = true,
        purchaserBusinessPrivateNeeded = false,
        registered = true
      )

      Json.toJson(model) mustEqual Json.obj(
        "bringingVehicleNI"              -> true,
        "isForBusinessUse"               -> true,
        "bringingVehicleBusiness"        -> true,
        "bringingVehicleBusinessNeeded"  -> true,
        "purchasingVehiclesEuNeeded"     -> true,
        "purchaserBusinessPrivateNeeded" -> false,
        "sccCustomerFieldNeeded"         -> true,
        "deregistered"                   -> false,
        "registered"                     -> true,
        "currentlyRegistered"            -> false
      )
    }

    "writes the correct JSON value when the notifier is a business" in {
      val model = InitialQuestions(
        bringingVehicleNI = true,
        isForBusinessUse = None,
        areYouBusinessPrivate = Some(BusinessOrPrivateIndividual.Business),
        notifyingAsPurchaser = Some(PurchaserOrOnBehalf.Purchaser),
        purchaserBusinessPrivate = None,
        agentClientVehicleBusinessUse = None,
        bringingVehicleBusiness = None
      )

      Json.toJson(model) mustEqual Json.obj(
        "bringingVehicleNI"              -> true,
        "areYouBusinessPrivate"          -> "business",
        "notifyingAsPurchaser"           -> "self",
        "bringingVehicleBusinessNeeded"  -> false,
        "purchasingVehiclesEuNeeded"     -> true,
        "purchaserBusinessPrivateNeeded" -> true,
        "sccCustomerFieldNeeded"         -> true,
        "deregistered"                   -> false,
        "registered"                     -> false,
        "currentlyRegistered"            -> false
      )
    }

    "writes the correct JSON value when the purchaser is a non-VAT registered business" in {
      val model = InitialQuestions(
        bringingVehicleNI = true,
        isForBusinessUse = None,
        areYouBusinessPrivate = Some(BusinessOrPrivateIndividual.PrivateIndividual),
        notifyingAsPurchaser = Some(PurchaserOrOnBehalf.OnBehalfOfPurchaser),
        purchaserBusinessPrivate = Some(PurchaserBusinessOrIndividual.NonVatRegisteredBusiness),
        agentClientVehicleBusinessUse = None,
        bringingVehicleBusiness = None
      )

      Json.toJson(model) mustEqual Json.obj(
        "bringingVehicleNI"              -> true,
        "areYouBusinessPrivate"          -> "individual",
        "notifyingAsPurchaser"           -> "behalfOfPurchaser",
        "purchaserBusinessPrivate"       -> "self",
        "bringingVehicleBusinessNeeded"  -> false,
        "purchasingVehiclesEuNeeded"     -> true,
        "purchaserBusinessPrivateNeeded" -> true,
        "sccCustomerFieldNeeded"         -> true,
        "deregistered"                   -> false,
        "registered"                     -> false,
        "currentlyRegistered"            -> false
      )
    }

    "writes the correct JSON value when the purchaser is a private individual" in {
      val model = InitialQuestions(
        bringingVehicleNI = true,
        isForBusinessUse = None,
        areYouBusinessPrivate = Some(BusinessOrPrivateIndividual.PrivateIndividual),
        notifyingAsPurchaser = Some(PurchaserOrOnBehalf.OnBehalfOfPurchaser),
        purchaserBusinessPrivate = Some(PurchaserBusinessOrIndividual.NonVatRegisteredPrivateIndividual),
        agentClientVehicleBusinessUse = None,
        bringingVehicleBusiness = None
      )

      Json.toJson(model) mustEqual Json.obj(
        "bringingVehicleNI"              -> true,
        "areYouBusinessPrivate"          -> "individual",
        "notifyingAsPurchaser"           -> "behalfOfPurchaser",
        "purchaserBusinessPrivate"       -> "other",
        "bringingVehicleBusinessNeeded"  -> false,
        "purchasingVehiclesEuNeeded"     -> true,
        "purchaserBusinessPrivateNeeded" -> true,
        "sccCustomerFieldNeeded"         -> true,
        "deregistered"                   -> false,
        "registered"                     -> false,
        "currentlyRegistered"            -> false
      )
    }

    "writes deregistered = true and registered = true for a deregistered user" in {
      val model = InitialQuestions(
        bringingVehicleNI = true,
        isForBusinessUse = None,
        areYouBusinessPrivate = Some(BusinessOrPrivateIndividual.Business),
        notifyingAsPurchaser = Some(PurchaserOrOnBehalf.Purchaser),
        purchaserBusinessPrivate = None,
        agentClientVehicleBusinessUse = None,
        bringingVehicleBusiness = None,
        purchaserBusinessPrivateNeeded = false,
        deregistered = true,
        registered = true
      )

      Json.toJson(model) mustEqual Json.obj(
        "bringingVehicleNI"              -> true,
        "areYouBusinessPrivate"          -> "business",
        "notifyingAsPurchaser"           -> "self",
        "bringingVehicleBusinessNeeded"  -> false,
        "purchasingVehiclesEuNeeded"     -> true,
        "purchaserBusinessPrivateNeeded" -> false,
        "sccCustomerFieldNeeded"         -> true,
        "deregistered"                   -> true,
        "registered"                     -> true,
        "currentlyRegistered"            -> false
      )
    }
  }
}
