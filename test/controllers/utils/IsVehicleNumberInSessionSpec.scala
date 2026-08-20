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
import models.{UserAnswers, VehicleNumber}
import org.scalatest.matchers.must.Matchers
import pages.sections.vehicledetails.VehicleNumberPage

class IsVehicleNumberInSessionSpec extends SpecBase with Matchers {

  "IsVehicleNumberInSession" - {

    "must return true when the vehicle number is the one the user is working on" in {
      val answers = UserAnswers("id").set(VehicleNumberPage, 3).success.value
      IsVehicleNumberInSession(answers, VehicleNumber(3)) mustBe true
    }

    "must return false when the vehicle number belongs to another vehicle" in {
      val answers = UserAnswers("id").set(VehicleNumberPage, 3).success.value
      IsVehicleNumberInSession(answers, VehicleNumber(4)) mustBe false
    }

    "must fall back to the first vehicle when the user has no vehicle number in session yet" in {
      IsVehicleNumberInSession(UserAnswers("id"), VehicleNumber(1)) mustBe true
      IsVehicleNumberInSession(UserAnswers("id"), VehicleNumber(2)) mustBe false
    }
  }
}
