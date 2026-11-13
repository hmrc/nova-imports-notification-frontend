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

import models.{OneMultiVehicle, UserAnswers, VehicleNumber}
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.vehicledetails.NumberOfVehiclesPage

object IsOneVehicleOrAmend {

  // amend sessions skip AVD10.0 (C-AVD2.0 goes to AVD9.0), so the one-vehicle answer only applies to non-amend sessions
  def apply(answers: UserAnswers, vehicleNumber: VehicleNumber): Boolean =
    answers.get(AmendSubmittedNotificationPage).contains(true) ||
      answers.get(NumberOfVehiclesPage(vehicleNumber)).exists(_.oneMultiVehicle == OneMultiVehicle.One)
}
