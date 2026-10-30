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

import connectors.NovaImportsBackendConnector
import controllers.BaseController
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import models.draftsections.VehicleTypeDetails
import models.requests.DataRequest
import models.{AddVehicleType, ImportNumber, NormalMode, SupplierNumber, UserAnswers, VehicleNumber}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.vehicledetails.*
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{ImportService, SupplierService, VehicleService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.checkAnswers.ConfirmVehicleTypeDetailsHelper
import views.html.ConfirmVehicleTypeDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmVehicleTypeDetailsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  backendConnector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  supplierService: SupplierService,
  importService: ImportService,
  vehicleService: VehicleService,
  view: ConfirmVehicleTypeDetailsView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import ConfirmVehicleTypeDetailsController.*

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) {
      implicit request =>
        handlePageLoad(
          ConfirmVehicleTypeDetailsHelper.supplierSummaryList(request.userAnswers, supplierNumber, vehicleNumber),
          routes.ConfirmVehicleTypeDetailsController.supplierOnSubmit(supplierNumber, vehicleNumber)
        )
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)) { implicit request =>
      handlePageLoad(
        ConfirmVehicleTypeDetailsHelper.importSummaryList(request.userAnswers, importNumber, vehicleNumber),
        routes.ConfirmVehicleTypeDetailsController.importOnSubmit(importNumber, vehicleNumber)
      )
    }

  def supplierOnSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(
          s"supplier/${supplierNumber.value}/vehicle/${vehicleNumber.value}/details",
          vehicleDetailsSection(request.userAnswers, vehicleNumber),
          nextPage(request.userAnswers, vehicleNumber, supplierNumber = Some(supplierNumber))
        )
    }

  def importOnSubmit(importNumber: ImportNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(
          s"import/${importNumber.value}/vehicle/${vehicleNumber.value}/details",
          vehicleDetailsSection(request.userAnswers, vehicleNumber),
          nextPage(request.userAnswers, vehicleNumber, importNumber = Some(importNumber))
        )
    }

  private def handlePageLoad(summaryList: SummaryList, submitCall: Call)(implicit
    request: DataRequest[AnyContent]
  ): Result =
    Ok(view(summaryList, submitCall))

  private def handleSubmit(sectionId: String, sectionData: Option[JsObject], redirectTo: Call)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val failure = Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

    (request.userAnswers.get(DraftIdPage), request.userAnswers.get(DraftVersionIdPage), sectionData) match {
      case (Some(draftId), Some(versionId), Some(sectionData)) =>
        backendConnector.updateDraftSection(draftId, sectionId, sectionData + ("versionId" -> Json.toJson(versionId))).flatMap {
          case Right(newVersionId) =>
            sessionRepository
              .setPage(request.userAnswers, DraftVersionIdPage, newVersionId)
              .map(_ => Redirect(redirectTo))
          case Left(error) =>
            logger.warn(s"Failed to update '$sectionId' for draftId ${draftId.value}: $error")
            Future.successful(failure)
        }
      case _ =>
        logger.warn(s"Failed to submit '$sectionId', draftId, versionId or vehicle type details missing")
        Future.successful(failure)
    }
  }

  private def nextPage(
    answers: UserAnswers,
    vehicleNumber: VehicleNumber,
    supplierNumber: Option[SupplierNumber] = None,
    importNumber: Option[ImportNumber] = None
  ): Call =
    if (answers.get(AmendSubmittedNotificationPage).contains(true))
      (supplierNumber, importNumber) match {
        case (Some(supplierNumber), _) =>
          routes.NotificationReferenceController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode)
        case (_, Some(importNumber)) =>
          routes.NotificationReferenceController.importOnPageLoad(importNumber, vehicleNumber, NormalMode)
        case _ =>
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    else
      controllers.routes.LandingPageController.onPageLoad() // TODO: navigate to AVD10.0 when built
}

object ConfirmVehicleTypeDetailsController {

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
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber) &&
      vehicleDetailsSection(request.userAnswers, vehicleNumber).isDefined

  def importGuardPredicate(
    importService: ImportService,
    vehicleService: VehicleService,
    importNumber: ImportNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(false) &&
      importService.numberHasValues(request.userAnswers, importNumber) &&
      vehicleService.belongsToImport(request.userAnswers, vehicleNumber, importNumber) &&
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber) &&
      vehicleDetailsSection(request.userAnswers, vehicleNumber).isDefined

  def vehicleDetailsSection(answers: UserAnswers, vehicleNumber: VehicleNumber): Option[JsObject] = {
    val details: Option[VehicleTypeDetails] = answers.get(AddVehicleTypePage(vehicleNumber)) match {
      case Some(AddVehicleType.Car) =>
        answers.get(AddVehicleDetailsCarPage(vehicleNumber)).map { d =>
          VehicleTypeDetails(
            make = Some(d.make),
            model = Some(d.model),
            derivative = Some(d.derivative),
            trim = Some(d.trim),
            bodyType = Some(d.bodyType)
          )
        }
      case Some(AddVehicleType.Lcv) =>
        answers.get(AddVehicleDetailsLightCommercialPage(vehicleNumber)).map { d =>
          VehicleTypeDetails(
            make = Some(d.make),
            model = Some(d.model),
            derivative = Some(d.derivative),
            trim = Some(d.trim),
            bodyType = Some(d.bodyType),
            lcvBodyType = Some(d.bodyType)
          )
        }
      case Some(AddVehicleType.Hcv) =>
        answers.get(AddVehicleDetailsHeavyCommercialPage(vehicleNumber)).map { d =>
          VehicleTypeDetails(make = Some(d.make), model = Some(d.model), hcvType = Some(d.hcvType), cabType = Some(d.cabType))
        }
      case Some(AddVehicleType.Motorcycle) =>
        answers.get(AddVehicleDetailsMotorcyclePage(vehicleNumber)).map { d =>
          VehicleTypeDetails(
            make = Some(d.make),
            model = Some(d.model),
            derivative = Some(d.derivative),
            motorcycleVersion = Some(d.motorcycleVersion),
            motorcycleType = Some(d.motorcycleType),
            motorcycleStyle = Some(d.motorcycleStyle),
            transmission = Some(d.transmission),
            fuelType = Some(d.fuelType.jsonValue),
            engineSize = Some(d.engineSize)
          )
        }
      case Some(AddVehicleType.MotorCaravan) =>
        answers.get(AddVehicleDetailsMotorCaravanPage(vehicleNumber)).map { d =>
          VehicleTypeDetails(
            caravanMake = Some(d.make),
            modelNameNumber = Some(d.model),
            caravanVersion = Some(d.version),
            caravanBody = Some(d.bodyType),
            makeOfBaseVehicle = Some(d.makeOfBaseVehicle),
            derivative = Some(d.derivative)
          )
        }
      case Some(AddVehicleType.AgriculturalTractor) =>
        answers.get(AddVehicleDetailsAgriculturalTractorPage(vehicleNumber)).map { d =>
          VehicleTypeDetails(
            make = Some(d.make),
            seriesModel = Some(d.model),
            versionDerivative = Some(d.derivative),
            brakeHorsePower = Some(d.brakeHorsepower)
          )
        }
      case Some(AddVehicleType.ContractorsPlant) =>
        answers.get(AddVehicleDetailsContractorsPlantPage(vehicleNumber)).map { d =>
          VehicleTypeDetails(make = Some(d.make), seriesModel = Some(d.model), versionDerivative = Some(d.derivative))
        }
      case None => None
    }
    details.map(Json.toJson(_).as[JsObject])
  }
}
