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

import base.SpecBase
import models.{NumberOfVehicles, OneMultiVehicle, VehicleNumber}
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.vehicledetails.NumberOfVehiclesPage

class IsOneVehicleOrAmendSpec extends SpecBase {

  private val vehicleNumber = VehicleNumber(1)

  "IsOneVehicleOrAmend" - {

    "must return true when AVD10.0 is One vehicle" in {
      val answers = emptyUserAnswers.unsafeSet(NumberOfVehiclesPage(vehicleNumber), NumberOfVehicles(OneMultiVehicle.One, None))
      IsOneVehicleOrAmend(answers, vehicleNumber) mustBe true
    }

    "must return false when AVD10.0 is More than one vehicle" in {
      val answers = emptyUserAnswers.unsafeSet(NumberOfVehiclesPage(vehicleNumber), NumberOfVehicles(OneMultiVehicle.More, Some(2)))
      IsOneVehicleOrAmend(answers, vehicleNumber) mustBe false
    }

    "must return false when AVD10.0 has not been answered" in {
      IsOneVehicleOrAmend(emptyUserAnswers, vehicleNumber) mustBe false
    }

    "must return true in an amend session when AVD10.0 has not been answered" in {
      val answers = emptyUserAnswers.unsafeSet(AmendSubmittedNotificationPage, true)
      IsOneVehicleOrAmend(answers, vehicleNumber) mustBe true
    }
  }
}
