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
import controllers.actions.Actions
import controllers.vehicledetails.UploadVinNumbersController.{importGuardPredicate, supplierGuardPredicate}
import models.requests.DataRequest
import models.{AddVehicleType, DraftId, ImportNumber, SupplierNumber, VehicleNumber}
import pages.sections.vehicledetails.{AddVehicleTypePage, NumberOfVehiclesPage}
import pages.{DraftIdPage, DraftVersionIdPage, ValidatedVinListPage}
import play.api.Logging
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{ImportService, SupplierService, VehicleService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.Pager
import views.html.CheckVinNumbersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckVinNumbersController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  connector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  supplierService: SupplierService,
  importService: ImportService,
  vehicleService: VehicleService,
  view: CheckVinNumbersView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import CheckVinNumbersController.*

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, page: Int): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) {
      implicit request =>
        handlePageLoad(
          vehicleNumber,
          page,
          routes.CheckVinNumbersController.supplierOnPageLoad(supplierNumber, vehicleNumber, _),
          routes.CheckVinNumbersController.supplierOnSubmit(supplierNumber, vehicleNumber),
          routes.VehicleIdentificationNumbersController.supplierOnPageLoad(supplierNumber, vehicleNumber)
        )
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber, page: Int): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)) { implicit request =>
      handlePageLoad(
        vehicleNumber,
        page,
        routes.CheckVinNumbersController.importOnPageLoad(importNumber, vehicleNumber, _),
        routes.CheckVinNumbersController.importOnSubmit(importNumber, vehicleNumber),
        routes.VehicleIdentificationNumbersController.importOnPageLoad(importNumber, vehicleNumber)
      )
    }

  def supplierOnSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request => handleSubmit(journey = "supplier", groupNumber = supplierNumber.value, vehicleNumber)
    }

  def importOnSubmit(importNumber: ImportNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async {
      implicit request => handleSubmit(journey = "import", groupNumber = importNumber.value, vehicleNumber)
    }

  private def handlePageLoad(
    vehicleNumber: VehicleNumber,
    page: Int,
    urlForPage: Int => play.api.mvc.Call,
    submitCall: play.api.mvc.Call,
    updateUrl: play.api.mvc.Call
  )(implicit request: DataRequest[AnyContent]): Result =
    request.userAnswers.get(ValidatedVinListPage) match {
      case Some(vins) =>
        val vehicleType = request.userAnswers.get(AddVehicleTypePage(vehicleNumber)).map(vehicleTypeLabel)
        Ok(
          view(
            Pager.page(vins, page),
            vehicleType,
            vins.size,
            p => urlForPage(p).url,
            submitCall,
            updateUrl.url
          )
        )
      case None =>
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

  private def handleSubmit(journey: String, groupNumber: Int, vehicleNumber: VehicleNumber)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    def failureRecovery = Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

    (
      request.userAnswers.get(DraftIdPage),
      request.userAnswers.get(DraftVersionIdPage),
      request.userAnswers.get(ValidatedVinListPage),
      request.userAnswers.get(NumberOfVehiclesPage(vehicleNumber))
    ) match {
      case (Some(draftId), Some(versionId), Some(vins), Some(numberOfVehicles)) =>
        connector.getDraftNotification(draftId).flatMap {
          case Right(notification) =>
            baseVehicleSections(notification.sections, journey, groupNumber, vehicleNumber) match {
              case Some(base) =>
                val multipleVehiclesSectionId = s"$journey/$groupNumber/vehicle/${vehicleNumber.value}/multiple-vehicles"
                val multipleVehiclesSection   =
                  NumberOfVehiclesController.multiplicationSection(numberOfVehicles) + ("versionId" -> Json.toJson(versionId))

                connector.updateDraftSection(draftId, multipleVehiclesSectionId, multipleVehiclesSection).flatMap {
                  case Right(versionIdAfterMultiplicationSave) =>
                    val sections = fanOutSections(journey, groupNumber, base, vins)
                    connector.replaceVehicleSections(draftId, sections, versionIdAfterMultiplicationSave).flatMap {
                      case Right(newVersionId) =>
                        sessionRepository.setPage(request.userAnswers, DraftVersionIdPage, newVersionId).map { _ =>
                          Redirect(controllers.routes.LandingPageController.onPageLoad()) // TODO: navigate to AVD10.3 when built
                        }
                      case Left(error) =>
                        logger.warn(s"Failed to save the multiplied vehicle sections for draftId ${draftId.value}: $error")
                        failureRecovery
                    }
                  case Left(error) =>
                    logger.warn(s"Failed to update '$multipleVehiclesSectionId' for draftId ${draftId.value}: $error")
                    failureRecovery
                }
              case None =>
                logger.warn(
                  s"Could not find the base vehicle sections to multiply for draftId ${draftId.value}, $journey/$groupNumber/vehicle/${vehicleNumber.value}"
                )
                failureRecovery
            }
          case Left(error) =>
            logger.warn(s"Could not retrieve the draft notification to save the VINs for draftId ${draftId.value}: $error")
            failureRecovery
        }
      case _ =>
        logger.warn("Could not save the VINs, draftId, versionId, validated VIN list or number of vehicles missing")
        failureRecovery
    }
  }
}

object CheckVinNumbersController {

  final case class BaseVehicleSections(vehicleType: JsObject, details: JsObject, additionalInformation: JsObject)

  def baseVehicleSections(
    sections: Map[String, models.DraftNotificationSection],
    journey: String,
    groupNumber: Int,
    vehicleNumber: VehicleNumber
  ): Option[BaseVehicleSections] =
    for {
      vehicleType <- sections.get(s"$journey/$groupNumber/vehicle/${vehicleNumber.value}/type").flatMap(_.data)
      details     <- sections.get(s"$journey/$groupNumber/vehicle/${vehicleNumber.value}/details").flatMap(_.data)
    } yield {
      val additional =
        sections.get(s"$journey/$groupNumber/vehicle/${vehicleNumber.value}/additional-information").flatMap(_.data).getOrElse(Json.obj())
      BaseVehicleSections(vehicleType, details, additional)
    }

  def fanOutSections(journey: String, groupNumber: Int, base: BaseVehicleSections, vins: Seq[String]): Map[String, JsObject] =
    vins.zipWithIndex.flatMap { case (vin, index) =>
      val vehicleNumber = index + 1
      val vinFields     = Json.obj(
        "vehicleIdNumber"        -> vin,
        "confirmVehicleIdNumber" -> vin,
        "oneOrMultipleVehicls"   -> "Multiple"
      )
      Seq(
        s"$journey/$groupNumber/vehicle/$vehicleNumber/type"                   -> base.vehicleType,
        s"$journey/$groupNumber/vehicle/$vehicleNumber/details"                -> base.details,
        s"$journey/$groupNumber/vehicle/$vehicleNumber/additional-information" -> (base.additionalInformation ++ vinFields)
      )
    }.toMap

  def vehicleTypeLabel(vehicleType: AddVehicleType)(implicit messages: Messages): String = vehicleType match {
    case AddVehicleType.Car                 => messages("addVehicleType.radio.car")
    case AddVehicleType.Lcv                 => messages("addVehicleType.radio.lcv")
    case AddVehicleType.Hcv                 => messages("addVehicleType.radio.hcv")
    case AddVehicleType.Motorcycle          => messages("addVehicleType.radio.motorcycle")
    case AddVehicleType.MotorCaravan        => messages("addVehicleType.radio.caravan")
    case AddVehicleType.AgriculturalTractor => messages("addVehicleType.radio.tractor")
    case AddVehicleType.ContractorsPlant    => messages("addVehicleType.radio.plant")
  }
}
