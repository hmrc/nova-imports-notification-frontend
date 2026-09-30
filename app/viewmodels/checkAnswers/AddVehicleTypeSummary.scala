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

package viewmodels.checkAnswers

import controllers.vehicledetails.routes
import models.{AddVehicleType, CheckMode, UserAnswers, VehicleNumber}
import pages.sections.vehicledetails.AddVehicleTypePage
import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object AddVehicleTypeSummary {

  def row(answers: UserAnswers, vehicleNumber: VehicleNumber)(implicit messages: Messages): Option[SummaryListRow] =
    for {
      answer <- answers.get(AddVehicleTypePage(vehicleNumber))
      change <- supplierOrImportChangeLink(answers, vehicleNumber)
    } yield {

      val value = answer match {
        case AddVehicleType.AgriculturalTractor => "addVehicleType.radio.tractor"
        case AddVehicleType.Car                 => "addVehicleType.radio.car"
        case AddVehicleType.ContractorsPlant    => "addVehicleType.radio.plant"
        case AddVehicleType.Hcv                 => "addVehicleType.radio.hcv"
        case AddVehicleType.Lcv                 => "addVehicleType.radio.lcv"
        case AddVehicleType.Motorcycle          => "addVehicleType.radio.motorcycle"
        case AddVehicleType.MotorCaravan        => "addVehicleType.radio.caravan"
      }

      SummaryListRowViewModel(
        key = "addVehicleType.checkYourAnswersLabel",
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel("site.change", change.url)
            .withVisuallyHiddenText(messages("addVehicleType.change.hidden"))
        )
      )
    }

  private def supplierOrImportChangeLink(answers: UserAnswers, vehicleNumber: VehicleNumber): Option[Call] =
    answers
      .vehicleSupplierNumber(vehicleNumber)
      .map(supplierNumber => routes.AddVehicleTypeController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode))
      .orElse(
        answers
          .vehicleImportNumber(vehicleNumber)
          .map(importNumber => routes.AddVehicleTypeController.importOnPageLoad(importNumber, vehicleNumber, CheckMode))
      )
}
