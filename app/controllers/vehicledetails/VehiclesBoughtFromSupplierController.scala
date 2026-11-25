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

package controllers.vehicledetails

import config.FrontendAppConfig
import controllers.BaseController
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import controllers.vehicledetails.VehiclesBoughtFromSupplierController.*
import forms.AdditionalVehicleFormProvider
import models.requests.DataRequest
import models.{AddVehicleType, NormalMode, NumberOfVehicles, OneMultiVehicle, SupplierNumber, UserAnswers, VehicleNumber, VehicleVinSummary}
import pages.ValidatedVinListPage
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.*
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import services.{SupplierService, VehicleService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.Pager
import views.html.VehiclesBoughtFromSupplierView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VehiclesBoughtFromSupplierController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  view: VehiclesBoughtFromSupplierView,
  supplierService: SupplierService,
  vehicleService: VehicleService,
  appConfig: FrontendAppConfig,
  formProvider: AdditionalVehicleFormProvider
)(implicit ec: ExecutionContext)
    extends BaseController {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(supplierNumber: SupplierNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>
      renderPage(supplierNumber, form).map(Ok(_))
    }

  // When adding new vehicle it sets up a new vehicle collection in session, it carries the supplierNumber it was bought from in the current URL
  def onSubmit(supplierNumber: SupplierNumber): Action[AnyContent] = {
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>

      if (getVehicleNumbersForSupplier(supplierNumber).isEmpty) {
        // AVD2 Add Vehicle pressed
        vehicleService.addForSupplier(request.userAnswers, supplierNumber).flatMap { vehicleNumber =>
          Future.successful(Redirect(routes.VehicleDatesController.onPageLoad(supplierNumber, vehicleNumber, NormalMode)))
        }
      } else {
        form
          .bindFromRequest()
          .fold(
            formWithErrors => renderPage(supplierNumber, formWithErrors).map(BadRequest(_)),
            addAnotherVehicleAns =>
              if (addAnotherVehicleAns) {
                // AVD12 Yes answered
                vehicleService.addForSupplier(request.userAnswers, supplierNumber).map { vehicleNumber =>
                  Redirect(routes.VehicleDatesController.onPageLoad(supplierNumber, vehicleNumber, NormalMode))
                }
              } else {
                // AVD12 No answered
                // TODO: Update to AVD13.0 when built
                Future.successful(Redirect(controllers.routes.LandingPageController.onPageLoad()))
              }
          )
      }
    }
  }

  private def getVehicleNumbersForSupplier(supplierNumber: SupplierNumber)(implicit request: DataRequest[?]) = {
    // only gets vehicles from this supplier
    vehicleService.inOrder(request.userAnswers).collect {
      case (vehicleNumber, _) if vehicleService.belongsToSupplier(request.userAnswers, vehicleNumber, supplierNumber) => vehicleNumber
    }
  }

  private def renderPage(supplierNumber: SupplierNumber, formWithErrors: Form[Boolean])(implicit request: DataRequest[?]): Future[Html] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val vehicleNumbers = getVehicleNumbersForSupplier(supplierNumber)

    val vehicleVinSummarySeq = vehicleNumbers.flatMap { vehicleNumber =>
      request.userAnswers.get(NumberOfVehiclesPage(vehicleNumber)) match {
        case Some(NumberOfVehicles(OneMultiVehicle.One, _)) =>
          request.userAnswers
            .get(VehicleIdentificationNumberPage(vehicleNumber))
            .flatMap { vin =>
              buildVehicleVinSummary(request.userAnswers, vehicleNumber, vin)
            }
            .toSeq
        case Some(NumberOfVehicles(OneMultiVehicle.More, Some(sameVehicleCount))) =>

          (for {
            validVinSeq <- request.userAnswers.get(ValidatedVinListPage)
            baseSummary <- buildVehicleVinSummary(request.userAnswers, vehicleNumber, "")
          } yield {
            validVinSeq.map(vin => baseSummary.copy(vin = vin))
          }).getOrElse(Seq.empty)
        case _ =>
          Seq.empty
      }
    }

    supplierService.supplierName(request.userAnswers, request.userContext, supplierNumber).map { name =>
      view(
        formWithErrors,
        Pager.page(vehicleVinSummarySeq, page = 1),
        pageNumber => routes.VehiclesBoughtFromSupplierController.onPageLoad(supplierNumber).url,
        name,
        supplierNumber,
        appConfig.personalTransportUnitUrl
      )
    }
  }

}

object VehiclesBoughtFromSupplierController {

  // The supplier number in the URL must be one of the suppliers the user has in session
  def guardPredicate(supplierService: SupplierService, supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      supplierService.numberHasValues(request.userAnswers, supplierNumber)

  def buildVehicleVinSummary(answers: UserAnswers, vehicleNumber: VehicleNumber, vin: String): Option[VehicleVinSummary] = {
    answers.get(AddVehicleTypePage(vehicleNumber)) match {
      case Some(AddVehicleType.Car) =>
        answers.get(AddVehicleDetailsCarPage(vehicleNumber)).map { d =>
          VehicleVinSummary(vehicleNumber, vin, d.model, d.make, d.bodyType)
        }
      case Some(AddVehicleType.Lcv) =>
        answers.get(AddVehicleDetailsLightCommercialPage(vehicleNumber)).map { d =>
          VehicleVinSummary(vehicleNumber, vin, d.model, d.make, d.bodyType)
        }
      case Some(AddVehicleType.Hcv) =>
        answers.get(AddVehicleDetailsHeavyCommercialPage(vehicleNumber)).map { d =>
          VehicleVinSummary(vehicleNumber, vin, d.model, d.make, "")
        }
      case Some(AddVehicleType.Motorcycle) =>
        answers.get(AddVehicleDetailsMotorcyclePage(vehicleNumber)).map { d =>
          VehicleVinSummary(vehicleNumber, vin, d.model, d.make, "")
        }
      case Some(AddVehicleType.MotorCaravan) =>
        answers.get(AddVehicleDetailsMotorCaravanPage(vehicleNumber)).map { d =>
          VehicleVinSummary(vehicleNumber, vin, d.model, d.make, d.bodyType)
        }
      case Some(AddVehicleType.AgriculturalTractor) =>
        answers.get(AddVehicleDetailsAgriculturalTractorPage(vehicleNumber)).map { d =>
          VehicleVinSummary(vehicleNumber, vin, d.model, d.make, "")
        }
      case Some(AddVehicleType.ContractorsPlant) =>
        answers.get(AddVehicleDetailsContractorsPlantPage(vehicleNumber)).map { d =>
          VehicleVinSummary(vehicleNumber, vin, d.model, d.make, "")
        }
      case None => None
    }
  }

}
