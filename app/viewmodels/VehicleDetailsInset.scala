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

package viewmodels

import models.{AddVehicleType, UserAnswers, VehicleNumber}
import pages.sections.vehicledetails.*
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText
import viewmodels.govuk.inset.*

object VehicleDetailsInset {

  def apply(answers: UserAnswers, vehicleNumber: VehicleNumber)(implicit messages: Messages): Option[InsetText] =
    details(answers, vehicleNumber).map { values =>
      InsetTextViewModel(s"""${messages("vehicleDetailsInset.label")} <b>${HtmlFormat.escape(values.mkString(" ")).body}</b>""")
    }

  def details(answers: UserAnswers, vehicleNumber: VehicleNumber): Option[Seq[String]] =
    answers.get(AddVehicleTypePage(vehicleNumber)).flatMap {
      case AddVehicleType.Car =>
        answers.get(AddVehicleDetailsCarPage(vehicleNumber)).map(d => Seq(d.make, d.model, d.derivative, d.trim, d.bodyType))
      case AddVehicleType.Lcv =>
        answers.get(AddVehicleDetailsLightCommercialPage(vehicleNumber)).map(d => Seq(d.make, d.model, d.derivative, d.trim, d.bodyType))
      case AddVehicleType.Hcv =>
        answers.get(AddVehicleDetailsHeavyCommercialPage(vehicleNumber)).map(d => Seq(d.make, d.model))
      case AddVehicleType.Motorcycle =>
        answers.get(AddVehicleDetailsMotorcyclePage(vehicleNumber)).map(d => Seq(d.make, d.model, d.derivative))
      case AddVehicleType.MotorCaravan =>
        answers.get(AddVehicleDetailsMotorCaravanPage(vehicleNumber)).map(d => Seq(d.makeOfBaseVehicle, d.derivative))
      case AddVehicleType.AgriculturalTractor =>
        answers.get(AddVehicleDetailsAgriculturalTractorPage(vehicleNumber)).map(d => Seq(d.make, d.model))
      case AddVehicleType.ContractorsPlant =>
        answers.get(AddVehicleDetailsContractorsPlantPage(vehicleNumber)).map(d => Seq(d.make, d.model))
    }
}
