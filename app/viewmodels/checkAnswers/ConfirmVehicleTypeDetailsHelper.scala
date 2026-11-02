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
import models.{AddVehicleDetailsMotorCaravan, AddVehicleType, CheckMode, ImportNumber, MotorcycleFuelType, SupplierNumber, UserAnswers, VehicleNumber}
import pages.sections.vehicledetails.*
import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object ConfirmVehicleTypeDetailsHelper {

  def supplierSummaryList(answers: UserAnswers, supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber)(implicit
    messages: Messages
  ): SummaryList =
    SummaryListViewModel(rows = rowsFor(answers, vehicleNumber, supplierChange(answers, supplierNumber, vehicleNumber)))

  def importSummaryList(answers: UserAnswers, importNumber: ImportNumber, vehicleNumber: VehicleNumber)(implicit
    messages: Messages
  ): SummaryList =
    SummaryListViewModel(rows = rowsFor(answers, vehicleNumber, importChange(answers, importNumber, vehicleNumber)))

  private def supplierChange(answers: UserAnswers, supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Option[Call] =
    answers.get(AddVehicleTypePage(vehicleNumber)).map {
      case AddVehicleType.Car        => routes.AddVehicleDetailsCarController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
      case AddVehicleType.Lcv        => routes.AddVehicleDetailsLightCommercialController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
      case AddVehicleType.Hcv        => routes.AddVehicleDetailsHeavyCommercialController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
      case AddVehicleType.Motorcycle => routes.AddVehicleDetailsMotorcycleController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
      case AddVehicleType.MotorCaravan => routes.AddVehicleDetailsMotorCaravanController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
      case AddVehicleType.AgriculturalTractor =>
        routes.AddVehicleDetailsAgriculturalTractorController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
      case AddVehicleType.ContractorsPlant =>
        routes.AddVehicleDetailsContractorsPlantController.supplierOnPageLoad(supplierNumber, vehicleNumber, CheckMode)
    }

  private def importChange(answers: UserAnswers, importNumber: ImportNumber, vehicleNumber: VehicleNumber): Option[Call] =
    answers.get(AddVehicleTypePage(vehicleNumber)).map {
      case AddVehicleType.Car          => routes.AddVehicleDetailsCarController.importOnPageLoad(importNumber, vehicleNumber, CheckMode)
      case AddVehicleType.Lcv          => routes.AddVehicleDetailsLightCommercialController.importOnPageLoad(importNumber, vehicleNumber, CheckMode)
      case AddVehicleType.Hcv          => routes.AddVehicleDetailsHeavyCommercialController.importOnPageLoad(importNumber, vehicleNumber, CheckMode)
      case AddVehicleType.Motorcycle   => routes.AddVehicleDetailsMotorcycleController.importOnPageLoad(importNumber, vehicleNumber, CheckMode)
      case AddVehicleType.MotorCaravan => routes.AddVehicleDetailsMotorCaravanController.importOnPageLoad(importNumber, vehicleNumber, CheckMode)
      case AddVehicleType.AgriculturalTractor =>
        routes.AddVehicleDetailsAgriculturalTractorController.importOnPageLoad(importNumber, vehicleNumber, CheckMode)
      case AddVehicleType.ContractorsPlant =>
        routes.AddVehicleDetailsContractorsPlantController.importOnPageLoad(importNumber, vehicleNumber, CheckMode)
    }

  private def rowsFor(answers: UserAnswers, vehicleNumber: VehicleNumber, change: Option[Call])(implicit
    messages: Messages
  ): Seq[SummaryListRow] =
    (answers.get(AddVehicleTypePage(vehicleNumber)), change) match {
      case (Some(AddVehicleType.Car), Some(c)) =>
        answers.get(AddVehicleDetailsCarPage(vehicleNumber)).toSeq.flatMap { d =>
          Seq(
            row("make", d.make, c),
            row("model", d.model, c),
            row("derivative", d.derivative, c),
            row("trim", d.trim, c),
            row("bodyType", d.bodyType, c)
          )
        }
      case (Some(AddVehicleType.Lcv), Some(c)) =>
        answers.get(AddVehicleDetailsLightCommercialPage(vehicleNumber)).toSeq.flatMap { d =>
          Seq(
            row("make", d.make, c),
            row("model", d.model, c),
            row("derivative", d.derivative, c),
            row("trim", d.trim, c),
            row("bodyType", d.bodyType, c)
          )
        }
      case (Some(AddVehicleType.Hcv), Some(c)) =>
        answers.get(AddVehicleDetailsHeavyCommercialPage(vehicleNumber)).toSeq.flatMap { d =>
          Seq(row("make", d.make, c), row("model", d.model, c), row("bodyType", d.hcvType, c), row("cabType", d.cabType, c))
        }
      case (Some(AddVehicleType.Motorcycle), Some(c)) =>
        answers.get(AddVehicleDetailsMotorcyclePage(vehicleNumber)).toSeq.flatMap { d =>
          Seq(
            row("make", d.make, c),
            row("model", d.model, c),
            row("derivative", d.derivative, c),
            row("version", d.motorcycleVersion, c),
            row("bodyType", d.motorcycleType, c),
            row("style", d.motorcycleStyle, c),
            row("transmissionType", d.transmission, c),
            row("fuelType", fuelTypeText(d.fuelType), c),
            row("engineSize", d.engineSize, c)
          )
        }
      case (Some(AddVehicleType.MotorCaravan), Some(c)) =>
        answers.get(AddVehicleDetailsMotorCaravanPage(vehicleNumber)).toSeq.flatMap { d =>
          Seq(
            row("make", d.make, c),
            row("bodyType", motorCaravanBodyTypeText(d.bodyType), c),
            row("modelNameNumber", d.model, c),
            row("version", d.version, c),
            row("makeOfBaseVehicle", d.makeOfBaseVehicle, c),
            row("derivative", d.derivative, c)
          )
        }
      case (Some(AddVehicleType.AgriculturalTractor), Some(c)) =>
        answers.get(AddVehicleDetailsAgriculturalTractorPage(vehicleNumber)).toSeq.flatMap { d =>
          Seq(row("make", d.make, c), row("model", d.model, c), row("derivative", d.derivative, c), row("brakeHorsepower", d.brakeHorsepower, c))
        }
      case (Some(AddVehicleType.ContractorsPlant), Some(c)) =>
        answers.get(AddVehicleDetailsContractorsPlantPage(vehicleNumber)).toSeq.flatMap { d =>
          Seq(row("make", d.make, c), row("model", d.model, c), row("derivative", d.derivative, c))
        }
      case _ => Seq.empty
    }

  private def fuelTypeText(fuelType: MotorcycleFuelType)(implicit messages: Messages): String =
    messages(s"addVehicleDetailsMotorcycle.fuelType.option.${fuelType.jsonValue}")

  private def motorCaravanBodyTypeText(bodyType: String)(implicit messages: Messages): String =
    if (bodyType == AddVehicleDetailsMotorCaravan.NoBodyType) messages("confirmVehicleTypeDetails.notProvided") else bodyType

  private def row(field: String, value: String, change: Call)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = s"confirmVehicleTypeDetails.$field.label",
      value = ValueViewModel(Text(value)),
      actions = Seq(
        ActionItemViewModel("site.change", change.url)
          .withVisuallyHiddenText(messages(s"confirmVehicleTypeDetails.$field.change.hidden"))
      )
    )
}
