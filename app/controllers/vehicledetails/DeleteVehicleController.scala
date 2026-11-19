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

import connectors.{NovaImportsBackendConnector, UpdateSectionError}
import controllers.BaseController
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import forms.DeleteVehicleFormProvider
import models.requests.DataRequest
import models.{AddVehicleType, ImportNumber, SupplierNumber, UserAnswers, VehicleNumber}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.*
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Logging
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{ImportService, SupplierService, VehicleService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.DeleteVehicleView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteVehicleController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  backendConnector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  formProvider: DeleteVehicleFormProvider,
  supplierService: SupplierService,
  importService: ImportService,
  vehicleService: VehicleService,
  view: DeleteVehicleView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import DeleteVehicleController.*

  val form: Form[Boolean] = formProvider()

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) {
      implicit request =>
        handlePageLoad(vehicleNumber, routes.DeleteVehicleController.supplierOnSubmit(supplierNumber, vehicleNumber))
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)) { implicit request =>
      handlePageLoad(vehicleNumber, routes.DeleteVehicleController.importOnSubmit(importNumber, vehicleNumber))
    }

  def supplierOnSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(
          vehicleNumber,
          s"supplier/${supplierNumber.value}/vehicle/${vehicleNumber.value}",
          routes.DeleteVehicleController.supplierOnSubmit(supplierNumber, vehicleNumber),
          routes.VehiclesBoughtFromSupplierController.onPageLoad(supplierNumber)
        )
    }

  def importOnSubmit(importNumber: ImportNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(
          vehicleNumber,
          s"import/${importNumber.value}/vehicle/${vehicleNumber.value}",
          routes.DeleteVehicleController.importOnSubmit(importNumber, vehicleNumber),
          routes.AddVehicleByImportEntryController.onPageLoad(importNumber)
        )
    }

  private def handlePageLoad(vehicleNumber: VehicleNumber, submitCall: Call)(implicit request: DataRequest[AnyContent]): Result =
    Ok(view(form, vehicleDetails(request.userAnswers, vehicleNumber), submitCall))

  private def handleSubmit(vehicleNumber: VehicleNumber, sectionPrefix: String, submitCall: Call, vehiclesList: Call)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] =
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, vehicleDetails(request.userAnswers, vehicleNumber), submitCall))),
        {
          case true  => deleteVehicle(vehicleNumber, sectionPrefix, vehiclesList)
          case false => Future.successful(Redirect(vehiclesList))
        }
      )

  private def deleteVehicle(vehicleNumber: VehicleNumber, sectionPrefix: String, vehiclesList: Call)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val failure    = Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    val sectionIds = Seq("type", "details", "additional-information").map(section => s"$sectionPrefix/$section")

    (request.userAnswers.get(DraftIdPage), request.userAnswers.get(DraftVersionIdPage)) match {
      case (Some(draftId), Some(versionId)) =>
        backendConnector.deleteDraftSections(draftId, sectionIds, versionId).flatMap {
          case Right(newVersionId) =>
            for {
              answers <- sessionRepository.setPage(request.userAnswers, DraftVersionIdPage, newVersionId)
              _       <- vehicleService.deleteValues(answers, vehicleNumber)
            } yield Redirect(vehiclesList)
          case Left(UpdateSectionError.Forbidden) =>
            logger.warn("Failed to delete vehicle: Forbidden")
            Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
          case Left(error) =>
            val reason = error match {
              case UpdateSectionError.UpstreamError(status, _) => s"upstream status $status"
              case other                                       => other.toString
            }
            logger.warn(s"Failed to delete vehicle: $reason")
            Future.successful(failure)
        }
      case _ =>
        logger.warn("Failed to delete vehicle, draftId or versionId missing")
        Future.successful(failure)
    }
  }
}

object DeleteVehicleController {

  def supplierGuardPredicate(
    supplierService: SupplierService,
    vehicleService: VehicleService,
    supplierNumber: SupplierNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      supplierService.numberHasValues(request.userAnswers, supplierNumber) &&
      vehicleService.belongsToSupplier(request.userAnswers, vehicleNumber, supplierNumber) &&
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber)

  def importGuardPredicate(
    importService: ImportService,
    vehicleService: VehicleService,
    importNumber: ImportNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(false) &&
      (request.userContext.isVatRegisteredOrganisation || request.userContext.isAgent) &&
      importService.numberHasValues(request.userAnswers, importNumber) &&
      vehicleService.belongsToImport(request.userAnswers, vehicleNumber, importNumber) &&
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber)

  def vehicleDetails(answers: UserAnswers, vehicleNumber: VehicleNumber): Seq[(String, String)] = {
    val vin = answers.get(VehicleIdentificationNumberPage(vehicleNumber)).map("deleteVehicle.vin" -> _)

    val makeAndModel = answers.get(AddVehicleTypePage(vehicleNumber)).flatMap {
      case AddVehicleType.Car                 => answers.get(AddVehicleDetailsCarPage(vehicleNumber)).map(d => (d.make, d.model))
      case AddVehicleType.Lcv                 => answers.get(AddVehicleDetailsLightCommercialPage(vehicleNumber)).map(d => (d.make, d.model))
      case AddVehicleType.Hcv                 => answers.get(AddVehicleDetailsHeavyCommercialPage(vehicleNumber)).map(d => (d.make, d.model))
      case AddVehicleType.Motorcycle          => answers.get(AddVehicleDetailsMotorcyclePage(vehicleNumber)).map(d => (d.make, d.model))
      case AddVehicleType.MotorCaravan        => answers.get(AddVehicleDetailsMotorCaravanPage(vehicleNumber)).map(d => (d.make, d.model))
      case AddVehicleType.AgriculturalTractor => answers.get(AddVehicleDetailsAgriculturalTractorPage(vehicleNumber)).map(d => (d.make, d.model))
      case AddVehicleType.ContractorsPlant    => answers.get(AddVehicleDetailsContractorsPlantPage(vehicleNumber)).map(d => (d.make, d.model))
    }

    vin.toSeq ++ makeAndModel.toSeq.flatMap { case (make, model) => Seq("deleteVehicle.make" -> make, "deleteVehicle.model" -> model) }
  }
}
