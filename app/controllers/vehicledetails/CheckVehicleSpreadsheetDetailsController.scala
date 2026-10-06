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
import controllers.vehicledetails.VehicleSpreadsheetUploadController.guardPredicate
import models.draftsections.{ImportDetails, ImportVehicleAdditionalInformation, ImportVehicleType, SupplierDetails, VehicleAdditionalInformation, VehicleDetails, VehicleType}
import models.responses.{SpreadsheetAgriculturalTractorEuVehicle, SpreadsheetAgriculturalTractorNonEuVehicle, SpreadsheetConstructionVehiclesEuVehicle, SpreadsheetConstructionVehiclesNonEuVehicle, SpreadsheetEuVehicle, SpreadsheetHeavyCommercialEuVehicle, SpreadsheetHeavyCommercialNonEuVehicle, SpreadsheetMotorCaravansEuVehicle, SpreadsheetMotorCaravansNonEuVehicle, SpreadsheetMotorcyclesEuVehicle, SpreadsheetMotorcyclesNonEuVehicle, SpreadsheetNonEuVehicle, UploadResultResponse}
import models.{BusinessOrPrivateIndividual, DraftId, UserContext}
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.UserDataService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.Pager
import views.html.CheckVehicleSpreadsheetDetailsView

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckVehicleSpreadsheetDetailsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  connector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  userDataService: UserDataService,
  view: CheckVehicleSpreadsheetDetailsView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import CheckVehicleSpreadsheetDetailsController.*

  def onPageLoad(page: Int): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      connector.getUploadResult(request.userAnswers.get(DraftIdPage).get).map {
        case Right(result) if result.fileStatus == "VALIDATED" =>
          Ok(view(Pager.page(result.vehicles, page), p => routes.CheckVehicleSpreadsheetDetailsController.onPageLoad(p).url))
        case Right(result) if result.fileStatus == "VALIDATION_FAILED" => Redirect(routes.CheckVehicleSpreadsheetErrorsController.onPageLoad())
        case Right(_)                                                  => Redirect(routes.VehicleSpreadsheetUploadController.onPageLoad())
        case Left(error)                                               =>
          logger.warn(s"Could not retrieve the vehicle spreadsheet details: $error")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

  def onSubmit(): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val draftId     = request.userAnswers.get(DraftIdPage).get
      val isAmendment = request.userAnswers.get(AmendSubmittedNotificationPage).getOrElse(false)

      def failureRecovery = Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

      request.userAnswers.get(DraftVersionIdPage) match {
        case None =>
          logger.warn(s"Could not save the vehicle spreadsheet sections for draftId ${draftId.value}: versionId missing")
          failureRecovery

        case Some(versionId) =>
          connector.getUploadResult(draftId).flatMap {
            case Right(result) if result.fileStatus == "VALIDATED" && result.validationType.exists(euValidationTypes.contains) =>
              saveAllVehicles(draftId, result.euVehicles, isAmendment, versionId).flatMap(
                afterSave(draftId, request.userAnswers, request.userContext, _)
              )

            case Right(result) if result.fileStatus == "VALIDATED" && result.validationType.exists(nonEuValidationTypes.contains) =>
              saveAllImportVehicles(draftId, result.nonEuVehicles, result.validationType.get, isAmendment, versionId)
                .flatMap(afterSave(draftId, request.userAnswers, request.userContext, _))

            case Right(result) if result.fileStatus == "VALIDATED" && result.validationType.contains("AgriculturalTractorsEu") =>
              saveAllAgriculturalTractorEuVehicles(draftId, result.agriculturalTractorEuVehicles, isAmendment, versionId)
                .flatMap(afterSave(draftId, request.userAnswers, request.userContext, _))

            case Right(result) if result.fileStatus == "VALIDATED" && result.validationType.contains("AgriculturalTractorsNonEu") =>
              saveAllAgriculturalTractorNonEuVehicles(draftId, result.agriculturalTractorNonEuVehicles, isAmendment, versionId)
                .flatMap(afterSave(draftId, request.userAnswers, request.userContext, _))

            case Right(result) if result.fileStatus == "VALIDATED" && result.validationType.contains("MotorCaravansEu") =>
              saveAllMotorCaravansEuVehicles(draftId, result.motorCaravansEuVehicles, isAmendment, versionId)
                .flatMap(afterSave(draftId, request.userAnswers, request.userContext, _))

            case Right(result) if result.fileStatus == "VALIDATED" && result.validationType.contains("MotorCaravansNonEu") =>
              saveAllMotorCaravansNonEuVehicles(draftId, result.motorCaravansNonEuVehicles, isAmendment, versionId)
                .flatMap(afterSave(draftId, request.userAnswers, request.userContext, _))

            case Right(result) if result.fileStatus == "VALIDATED" && result.validationType.contains("HeavyCommercialVehiclesEu") =>
              saveAllHeavyCommercialEuVehicles(draftId, result.heavyCommercialEuVehicles, isAmendment, versionId)
                .flatMap(afterSave(draftId, request.userAnswers, request.userContext, _))

            case Right(result) if result.fileStatus == "VALIDATED" && result.validationType.contains("HeavyCommercialVehiclesNonEu") =>
              saveAllHeavyCommercialNonEuVehicles(draftId, result.heavyCommercialNonEuVehicles, isAmendment, versionId)
                .flatMap(afterSave(draftId, request.userAnswers, request.userContext, _))

            case Right(result) if result.fileStatus == "VALIDATED" && result.validationType.contains("MotorcyclesEu") =>
              saveAllMotorcyclesEuVehicles(draftId, result.motorcyclesEuVehicles, isAmendment, versionId)
                .flatMap(afterSave(draftId, request.userAnswers, request.userContext, _))

            case Right(result) if result.fileStatus == "VALIDATED" && result.validationType.contains("MotorcyclesNonEu") =>
              saveAllMotorcyclesNonEuVehicles(draftId, result.motorcyclesNonEuVehicles, isAmendment, versionId)
                .flatMap(afterSave(draftId, request.userAnswers, request.userContext, _))

            case Right(result) if result.fileStatus == "VALIDATED" && result.validationType.contains("ConstructionVehiclesEu") =>
              saveAllConstructionVehiclesEuVehicles(draftId, result.constructionVehiclesEuVehicles, isAmendment, versionId)
                .flatMap(afterSave(draftId, request.userAnswers, request.userContext, _))

            case Right(result) if result.fileStatus == "VALIDATED" && result.validationType.contains("ConstructionVehiclesNonEu") =>
              saveAllConstructionVehiclesNonEuVehicles(draftId, result.constructionVehiclesNonEuVehicles, isAmendment, versionId)
                .flatMap(afterSave(draftId, request.userAnswers, request.userContext, _))

            case Right(result) if result.fileStatus == "VALIDATED" =>
              logger.warn(
                s"Saving vehicle spreadsheet sections is not yet implemented for validationType ${result.validationType} (draftId ${draftId.value})"
              )
              failureRecovery
            case Right(result) if result.fileStatus == "VALIDATION_FAILED" =>
              Future.successful(Redirect(routes.CheckVehicleSpreadsheetErrorsController.onPageLoad()))
            case Right(_) =>
              Future.successful(Redirect(routes.VehicleSpreadsheetUploadController.onPageLoad()))
            case Left(error) =>
              logger.warn(s"Could not retrieve the vehicle spreadsheet details to save for draftId ${draftId.value}: $error")
              failureRecovery
          }
      }
    }

  private def afterSave(draftId: DraftId, userAnswers: models.UserAnswers, userContext: UserContext, result: Either[String, Long])(implicit
    hc: HeaderCarrier
  ): Future[Result] =
    result match {
      case Right(newVersionId) =>
        for {
          updatedAnswers <- sessionRepository.setPage(userAnswers, DraftVersionIdPage, newVersionId)
          _              <- connector.deleteFileUpload(draftId).map {
                 case Right(_)    => ()
                 case Left(error) => logger.warn(s"Could not delete the vehicle spreadsheet upload after saving for draftId ${draftId.value}: $error")
               }
          refreshed <- userDataService.retrieveAndStoreDraftNotification(draftId, updatedAnswers, userContext)
        } yield refreshed match {
          case Right(_)    => Redirect(routes.UploadSuccessfulController.onPageLoad())
          case Left(error) =>
            logger.warn(s"Failed to refresh the draft notification after saving the vehicle spreadsheet for draftId ${draftId.value}: $error")
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
      case Left(error) =>
        logger.warn(s"Failed to save the vehicle spreadsheet sections for draftId ${draftId.value}: $error")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  private def saveAllVehicles(draftId: DraftId, vehicles: Seq[SpreadsheetEuVehicle], isAmendment: Boolean, versionId: Long)(implicit
    hc: HeaderCarrier
  ): Future[Either[String, Long]] = {
    val supplierNumbers = groupNumbersFor(vehicles.map(supplierDetailsSection))
    val sections        = vehicles.zipWithIndex.flatMap { case (vehicle, index) =>
      val supplierNumber = supplierNumbers(index)
      val vehicleNumber  = index + 1
      Seq(
        s"supplier/$supplierNumber/details"                                       -> supplierDetailsSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/type"                   -> vehicleTypeSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/details"                -> vehicleDetailsSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/additional-information" -> vehicleAdditionalInformationSection(vehicle, isAmendment)
      )
    }
    replaceSections(draftId, sections, versionId)
  }

  private def saveAllImportVehicles(
    draftId: DraftId,
    vehicles: Seq[SpreadsheetNonEuVehicle],
    validationType: String,
    isAmendment: Boolean,
    versionId: Long
  )(implicit hc: HeaderCarrier): Future[Either[String, Long]] = {
    val importNumbers = groupNumbersFor(vehicles.map(importDetailsSection))
    val sections      = vehicles.zipWithIndex.flatMap { case (vehicle, index) =>
      val importNumber  = importNumbers(index)
      val vehicleNumber = index + 1
      Seq(
        s"import/$importNumber/details"                                       -> importDetailsSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/type"                   -> importVehicleTypeSection(vehicle, validationType),
        s"import/$importNumber/vehicle/$vehicleNumber/details"                -> importVehicleDetailsSection(vehicle, validationType),
        s"import/$importNumber/vehicle/$vehicleNumber/additional-information" -> importVehicleAdditionalInformationSection(vehicle, isAmendment)
      )
    }
    replaceSections(draftId, sections, versionId)
  }

  private def saveAllAgriculturalTractorEuVehicles(
    draftId: DraftId,
    vehicles: Seq[SpreadsheetAgriculturalTractorEuVehicle],
    isAmendment: Boolean,
    versionId: Long
  )(implicit hc: HeaderCarrier): Future[Either[String, Long]] = {
    val supplierNumbers = groupNumbersFor(vehicles.map(agriculturalTractorSupplierDetailsSection))
    val sections        = vehicles.zipWithIndex.flatMap { case (vehicle, index) =>
      val supplierNumber = supplierNumbers(index)
      val vehicleNumber  = index + 1
      Seq(
        s"supplier/$supplierNumber/details"                                       -> agriculturalTractorSupplierDetailsSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/type"                   -> agriculturalTractorVehicleTypeSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/details"                -> agriculturalTractorVehicleDetailsSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/additional-information" -> agriculturalTractorVehicleAdditionalInformationSection(
          vehicle,
          isAmendment
        )
      )
    }
    replaceSections(draftId, sections, versionId)
  }

  private def saveAllAgriculturalTractorNonEuVehicles(
    draftId: DraftId,
    vehicles: Seq[SpreadsheetAgriculturalTractorNonEuVehicle],
    isAmendment: Boolean,
    versionId: Long
  )(implicit hc: HeaderCarrier): Future[Either[String, Long]] = {
    val importNumbers = groupNumbersFor(vehicles.map(agriculturalTractorImportDetailsSection))
    val sections      = vehicles.zipWithIndex.flatMap { case (vehicle, index) =>
      val importNumber  = importNumbers(index)
      val vehicleNumber = index + 1
      Seq(
        s"import/$importNumber/details"                                       -> agriculturalTractorImportDetailsSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/type"                   -> agriculturalTractorImportVehicleTypeSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/details"                -> agriculturalTractorImportVehicleDetailsSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/additional-information" -> agriculturalTractorImportVehicleAdditionalInformationSection(
          vehicle,
          isAmendment
        )
      )
    }
    replaceSections(draftId, sections, versionId)
  }

  private def saveAllMotorCaravansEuVehicles(
    draftId: DraftId,
    vehicles: Seq[SpreadsheetMotorCaravansEuVehicle],
    isAmendment: Boolean,
    versionId: Long
  )(implicit hc: HeaderCarrier): Future[Either[String, Long]] = {
    val supplierNumbers = groupNumbersFor(vehicles.map(motorCaravansSupplierDetailsSection))
    val sections        = vehicles.zipWithIndex.flatMap { case (vehicle, index) =>
      val supplierNumber = supplierNumbers(index)
      val vehicleNumber  = index + 1
      Seq(
        s"supplier/$supplierNumber/details"                                       -> motorCaravansSupplierDetailsSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/type"                   -> motorCaravansVehicleTypeSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/details"                -> motorCaravansVehicleDetailsSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/additional-information" -> motorCaravansVehicleAdditionalInformationSection(
          vehicle,
          isAmendment
        )
      )
    }
    replaceSections(draftId, sections, versionId)
  }

  private def saveAllMotorCaravansNonEuVehicles(
    draftId: DraftId,
    vehicles: Seq[SpreadsheetMotorCaravansNonEuVehicle],
    isAmendment: Boolean,
    versionId: Long
  )(implicit hc: HeaderCarrier): Future[Either[String, Long]] = {
    val importNumbers = groupNumbersFor(vehicles.map(motorCaravansImportDetailsSection))
    val sections      = vehicles.zipWithIndex.flatMap { case (vehicle, index) =>
      val importNumber  = importNumbers(index)
      val vehicleNumber = index + 1
      Seq(
        s"import/$importNumber/details"                                       -> motorCaravansImportDetailsSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/type"                   -> motorCaravansImportVehicleTypeSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/details"                -> motorCaravansImportVehicleDetailsSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/additional-information" -> motorCaravansImportVehicleAdditionalInformationSection(
          vehicle,
          isAmendment
        )
      )
    }
    replaceSections(draftId, sections, versionId)
  }

  private def saveAllHeavyCommercialEuVehicles(
    draftId: DraftId,
    vehicles: List[SpreadsheetHeavyCommercialEuVehicle],
    isAmendment: Boolean,
    versionId: Long
  )(implicit hc: HeaderCarrier): Future[Either[String, Long]] = {
    val supplierNumbers = groupNumbersFor(vehicles.map(heavyCommercialSupplierDetailsSection))
    val sections        = vehicles.zipWithIndex.flatMap { case (vehicle, index) =>
      val supplierNumber = supplierNumbers(index)
      val vehicleNumber  = index + 1
      Seq(
        s"supplier/$supplierNumber/details"                                       -> heavyCommercialSupplierDetailsSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/type"                   -> heavyCommercialVehicleTypeSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/details"                -> heavyCommercialVehicleDetailsSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/additional-information" -> heavyCommercialVehicleAdditionalInformationSection(
          vehicle,
          isAmendment
        )
      )
    }
    replaceSections(draftId, sections, versionId)
  }

  private def saveAllHeavyCommercialNonEuVehicles(
    draftId: DraftId,
    vehicles: List[SpreadsheetHeavyCommercialNonEuVehicle],
    isAmendment: Boolean,
    versionId: Long
  )(implicit hc: HeaderCarrier): Future[Either[String, Long]] = {
    val importNumbers = groupNumbersFor(vehicles.map(heavyCommercialImportDetailsSection))
    val sections      = vehicles.zipWithIndex.flatMap { case (vehicle, index) =>
      val importNumber  = importNumbers(index)
      val vehicleNumber = index + 1
      Seq(
        s"import/$importNumber/details"                                       -> heavyCommercialImportDetailsSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/type"                   -> heavyCommercialImportVehicleTypeSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/details"                -> heavyCommercialImportVehicleDetailsSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/additional-information" -> heavyCommercialImportVehicleAdditionalInformationSection(
          vehicle,
          isAmendment
        )
      )
    }
    replaceSections(draftId, sections, versionId)
  }

  private def saveAllMotorcyclesEuVehicles(
    draftId: DraftId,
    vehicles: Seq[SpreadsheetMotorcyclesEuVehicle],
    isAmendment: Boolean,
    versionId: Long
  )(implicit hc: HeaderCarrier): Future[Either[String, Long]] = {
    val supplierNumbers = groupNumbersFor(vehicles.map(motorcyclesSupplierDetailsSection))
    val sections        = vehicles.zipWithIndex.flatMap { case (vehicle, index) =>
      val supplierNumber = supplierNumbers(index)
      val vehicleNumber  = index + 1
      Seq(
        s"supplier/$supplierNumber/details"                                       -> motorcyclesSupplierDetailsSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/type"                   -> motorcyclesVehicleTypeSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/details"                -> motorcyclesVehicleDetailsSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/additional-information" -> motorcyclesVehicleAdditionalInformationSection(
          vehicle,
          isAmendment
        )
      )
    }
    replaceSections(draftId, sections, versionId)
  }

  private def saveAllMotorcyclesNonEuVehicles(
    draftId: DraftId,
    vehicles: Seq[SpreadsheetMotorcyclesNonEuVehicle],
    isAmendment: Boolean,
    versionId: Long
  )(implicit hc: HeaderCarrier): Future[Either[String, Long]] = {
    val importNumbers = groupNumbersFor(vehicles.map(motorcyclesImportDetailsSection))
    val sections      = vehicles.zipWithIndex.flatMap { case (vehicle, index) =>
      val importNumber  = importNumbers(index)
      val vehicleNumber = index + 1
      Seq(
        s"import/$importNumber/details"                                       -> motorcyclesImportDetailsSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/type"                   -> motorcyclesImportVehicleTypeSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/details"                -> motorcyclesImportVehicleDetailsSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/additional-information" -> motorcyclesImportVehicleAdditionalInformationSection(
          vehicle,
          isAmendment
        )
      )
    }
    replaceSections(draftId, sections, versionId)
  }

  private def saveAllConstructionVehiclesEuVehicles(
    draftId: DraftId,
    vehicles: Seq[SpreadsheetConstructionVehiclesEuVehicle],
    isAmendment: Boolean,
    versionId: Long
  )(implicit hc: HeaderCarrier): Future[Either[String, Long]] = {
    val supplierNumbers = groupNumbersFor(vehicles.map(constructionVehicleSupplierDetailsSection))
    val sections        = vehicles.zipWithIndex.flatMap { case (vehicle, index) =>
      val supplierNumber = supplierNumbers(index)
      val vehicleNumber  = index + 1
      Seq(
        s"supplier/$supplierNumber/details"                                       -> constructionVehicleSupplierDetailsSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/type"                   -> constructionVehicleVehicleTypeSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/details"                -> constructionVehicleVehicleDetailsSection(vehicle),
        s"supplier/$supplierNumber/vehicle/$vehicleNumber/additional-information" -> constructionVehicleVehicleAdditionalInformationSection(
          vehicle,
          isAmendment
        )
      )
    }
    replaceSections(draftId, sections, versionId)
  }

  private def saveAllConstructionVehiclesNonEuVehicles(
    draftId: DraftId,
    vehicles: Seq[SpreadsheetConstructionVehiclesNonEuVehicle],
    isAmendment: Boolean,
    versionId: Long
  )(implicit hc: HeaderCarrier): Future[Either[String, Long]] = {
    val importNumbers = groupNumbersFor(vehicles.map(constructionVehicleImportDetailsSection))
    val sections      = vehicles.zipWithIndex.flatMap { case (vehicle, index) =>
      val importNumber  = importNumbers(index)
      val vehicleNumber = index + 1
      Seq(
        s"import/$importNumber/details"                                       -> constructionVehicleImportDetailsSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/type"                   -> constructionVehicleImportVehicleTypeSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/details"                -> constructionVehicleImportVehicleDetailsSection(vehicle),
        s"import/$importNumber/vehicle/$vehicleNumber/additional-information" -> constructionVehicleImportVehicleAdditionalInformationSection(
          vehicle,
          isAmendment
        )
      )
    }
    replaceSections(draftId, sections, versionId)
  }

  private def replaceSections(draftId: DraftId, sections: Seq[(String, JsObject)], versionId: Long)(implicit
    hc: HeaderCarrier
  ): Future[Either[String, Long]] =
    connector.replaceVehicleSections(draftId, sections.toMap, versionId).map {
      case Right(newVersionId) => Right(newVersionId)
      case Left(error)         => Left(s"failed to replace vehicle sections: $error")
    }
}

object CheckVehicleSpreadsheetDetailsController {

  private val euValidationTypes    = Set("CarsEu", "LightCommercialVehiclesEu")
  private val nonEuValidationTypes = Set("CarsNonEu", "LightCommercialVehiclesNonEu")

  private val agriculturalTractorVehicleType = "AGRICULTURAL_TRACTOR"
  private val motorCaravansVehicleType       = "MOTOR_CARAVAN"
  private val hcvVehicleType                 = "HCV"
  private val motorcycleVehicleType          = "MOTORCYCLE"
  private val constructionVehicleType        = "CONTRACTORS_PLANT" // Matches legacy's VehicleTypeEnum.CONTRACTORS_PLANT not CONSTRUCTION_VEHICLE

  private val formPDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private def groupNumbersFor(sections: Seq[JsObject]): Seq[Int] = {
    val assigned = scala.collection.mutable.LinkedHashMap.empty[Map[String, String], Int]
    sections.map(section => assigned.getOrElseUpdate(groupingKey(section), assigned.size + 1))
  }

  private def groupingKey(section: JsObject): Map[String, String] =
    section.value.map { case (key, value) =>
      val raw = value match {
        case play.api.libs.json.JsString(s) => s
        case other                          => Json.stringify(other)
      }
      key -> raw.replaceAll("\\s", "").toLowerCase
    }.toMap

  private def supplierDetailsSection(vehicle: SpreadsheetEuVehicle): JsObject =
    Json
      .toJson(
        SupplierDetails(
          supplierBusinessIndividual =
            if (vehicle.supplierBusinessPrivate.exists(_.equalsIgnoreCase("business"))) BusinessOrPrivateIndividual.Business
            else BusinessOrPrivateIndividual.PrivateIndividual,
          supplierBusinessName = vehicle.supplierBusinessName,
          supplierTitle = vehicle.supplierTitle,
          supplierFirstName = vehicle.supplierFirstName,
          supplierLastName = vehicle.supplierLastName,
          addressLine1 = vehicle.addressLine1.getOrElse(""),
          addressLine2 = vehicle.addressLine2.getOrElse(""),
          addressLine3 = vehicle.addressLine3,
          addressLine4 = vehicle.addressLine4,
          addressLine5 = vehicle.addressLine5,
          postcode = vehicle.postcode,
          country = vehicle.country.getOrElse(""),
          isSupplierVatReg = vehicle.supplierVatRegistered.getOrElse(false),
          euStateVatReg = vehicle.euMemberState,
          vatRegistrationNumber = vehicle.supplierVatNumber
        )
      )
      .as[JsObject]

  private def vehicleTypeSection(vehicle: SpreadsheetEuVehicle): JsObject =
    Json
      .toJson(
        VehicleType(
          vehicleType = "CAR",
          doYouHaveAPurchaseInvoice = vehicle.purchaseInvoice.getOrElse(false),
          dateRoadUseKnown = vehicle.knownDateFirstRegistered.getOrElse(false),
          currencyUsed = vehicle.currency,
          purchaseInvoiceNumber = vehicle.purchaseInvoiceNumber,
          purchaseInvoiceDate = vehicle.purchaseInvoiceDate.map(formPDateFormat.format),
          pricePaidForVehicle = vehicle.pricePaid.map(_.toString)
        )
      )
      .as[JsObject]

  private def vehicleDetailsSection(vehicle: SpreadsheetEuVehicle): JsObject =
    Json
      .toJson(
        VehicleDetails(
          make = vehicle.make.getOrElse(""),
          model = vehicle.model.getOrElse(""),
          derivative = vehicle.derivative.getOrElse(""),
          trim = vehicle.trim.getOrElse(""),
          bodyType = vehicle.bodyType.getOrElse("")
        )
      )
      .as[JsObject]

  private def vehicleAdditionalInformationSection(vehicle: SpreadsheetEuVehicle, isAmendment: Boolean): JsObject =
    Json
      .toJson(
        VehicleAdditionalInformation(
          dateArrivedInUk = vehicle.dateArrivedInUk.map(formPDateFormat.format).getOrElse(""),
          businessUnableToReclaimVat = vehicle.obtainedFromUnableToReclaimVat.getOrElse(false),
          leftOrRightHand = vehicle.leftOrRightHandDrive.getOrElse(""),
          vehicleSoldUnderMarginScheme = vehicle.soldUnderMarginScheme.getOrElse(false),
          confirmVehicleIdNumber = vehicle.vin.getOrElse(""),
          isAmendment,
          vehicleIdNumber = vehicle.vin.getOrElse(""),
          totalValueOfOptions = vehicle.totalValueOfOptions.map(_.toString).getOrElse(""),
          isSupplierVatReg = vehicle.supplierVatRegistered.getOrElse(false),
          mileage = vehicle.mileage.getOrElse(""),
          mileageUnits = vehicle.mileageUnits.getOrElse(""),
          areYouClaimingRelief = vehicle.claimingVatRelief.getOrElse(false)
        )
      )
      .as[JsObject]

  private def importDetailsSection(vehicle: SpreadsheetNonEuVehicle): JsObject =
    Json
      .toJson(
        ImportDetails(
          importEntryNumber = vehicle.importEntryNumber.getOrElse(""),
          importEntryDate = vehicle.importEntryDate.map(formPDateFormat.format).getOrElse("")
        )
      )
      .as[JsObject]

  private def importVehicleTypeSection(vehicle: SpreadsheetNonEuVehicle, validationType: String): JsObject =
    Json
      .toJson(
        ImportVehicleType(
          vehicleType = if (validationType == "LightCommercialVehiclesNonEu") "LCV" else "CAR",
          dateRoadUseKnown = vehicle.knownDateFirstRegistered.getOrElse(false),
          dateOfFirstRegistration = vehicle.dateOfFirstRegistration.map(formPDateFormat.format)
        )
      )
      .as[JsObject]

  private def importVehicleDetailsSection(vehicle: SpreadsheetNonEuVehicle, validationType: String): JsObject = {
    val bodyTypeKey = if (validationType == "LightCommercialVehiclesNonEu") "lcvBodyType" else "bodyType"
    Json.obj(
      "make"       -> vehicle.make.getOrElse(""),
      "model"      -> vehicle.model.getOrElse(""),
      "derivative" -> vehicle.derivative.getOrElse(""),
      "trim"       -> vehicle.trim.getOrElse(""),
      bodyTypeKey  -> vehicle.bodyType.getOrElse("")
    )
  }

  private def importVehicleAdditionalInformationSection(vehicle: SpreadsheetNonEuVehicle, isAmendment: Boolean): JsObject =
    Json
      .toJson(
        ImportVehicleAdditionalInformation(
          dateArrivedInUk = vehicle.dateArrivedInUk.map(formPDateFormat.format).getOrElse(""),
          pricePaidForVehicleEntry = vehicle.pricePaid.map(_.toString).getOrElse(""),
          leftOrRightHand = vehicle.leftOrRightHandDrive.getOrElse(""),
          currencyUsed = vehicle.currency.getOrElse(""),
          commodityCode = vehicle.commodityCode.getOrElse(""),
          isAmendment = isAmendment,
          vehicleIdNumber = vehicle.vin.getOrElse(""),
          mileage = vehicle.mileage.getOrElse(""),
          mileageUnits = vehicle.mileageUnits.getOrElse(""),
          areYouClaimingRelief = vehicle.claimingVatRelief.getOrElse(false)
        )
      )
      .as[JsObject]

  private def agriculturalTractorSupplierDetailsSection(vehicle: SpreadsheetAgriculturalTractorEuVehicle): JsObject =
    Json
      .toJson(
        SupplierDetails(
          supplierBusinessIndividual =
            if (vehicle.supplierBusinessPrivate.exists(_.equalsIgnoreCase("business"))) BusinessOrPrivateIndividual.Business
            else BusinessOrPrivateIndividual.PrivateIndividual,
          supplierBusinessName = vehicle.supplierBusinessName,
          supplierTitle = vehicle.supplierTitle,
          supplierFirstName = vehicle.supplierFirstName,
          supplierLastName = vehicle.supplierLastName,
          addressLine1 = vehicle.addressLine1.getOrElse(""),
          addressLine2 = vehicle.addressLine2.getOrElse(""),
          addressLine3 = vehicle.addressLine3,
          addressLine4 = vehicle.addressLine4,
          addressLine5 = vehicle.addressLine5,
          postcode = vehicle.postcode,
          country = vehicle.country.getOrElse(""),
          isSupplierVatReg = vehicle.supplierVatRegistered.getOrElse(false),
          euStateVatReg = vehicle.euMemberState,
          vatRegistrationNumber = vehicle.supplierVatNumber
        )
      )
      .as[JsObject]

  private def agriculturalTractorVehicleTypeSection(vehicle: SpreadsheetAgriculturalTractorEuVehicle): JsObject =
    Json
      .toJson(
        VehicleType(
          vehicleType = agriculturalTractorVehicleType,
          doYouHaveAPurchaseInvoice = vehicle.purchaseInvoice.getOrElse(false),
          dateRoadUseKnown = vehicle.knownDateFirstRegistered.getOrElse(false),
          currencyUsed = vehicle.currency,
          purchaseInvoiceNumber = vehicle.purchaseInvoiceNumber,
          purchaseInvoiceDate = vehicle.purchaseInvoiceDate.map(formPDateFormat.format),
          pricePaidForVehicle = vehicle.pricePaid.map(_.toString)
        )
      )
      .as[JsObject]

  private def agriculturalTractorVehicleDetailsSection(vehicle: SpreadsheetAgriculturalTractorEuVehicle): JsObject =
    Json.obj(
      "make"              -> vehicle.make.getOrElse(""),
      "seriesModel"       -> vehicle.seriesModel.getOrElse(""),
      "versionDerivative" -> vehicle.versionDerivative.getOrElse(""),
      "brakeHorsePower"   -> vehicle.brakeHorsepower.getOrElse("")
    )

  private def agriculturalTractorVehicleAdditionalInformationSection(
    vehicle: SpreadsheetAgriculturalTractorEuVehicle,
    isAmendment: Boolean
  ): JsObject =
    Json
      .toJson(
        VehicleAdditionalInformation(
          dateArrivedInUk = vehicle.dateArrivedInUk.map(formPDateFormat.format).getOrElse(""),
          businessUnableToReclaimVat = vehicle.obtainedFromUnableToReclaimVat.getOrElse(false),
          leftOrRightHand = vehicle.leftOrRightHandDrive.getOrElse(""),
          vehicleSoldUnderMarginScheme = vehicle.soldUnderMarginScheme.getOrElse(false),
          confirmVehicleIdNumber = vehicle.vin.getOrElse(""),
          isAmendment,
          vehicleIdNumber = vehicle.vin.getOrElse(""),
          totalValueOfOptions = vehicle.totalValueOfOptions.map(_.toString).getOrElse(""),
          isSupplierVatReg = vehicle.supplierVatRegistered.getOrElse(false),
          mileage = vehicle.mileage.getOrElse(""),
          mileageUnits = vehicle.mileageUnits.getOrElse(""),
          areYouClaimingRelief = vehicle.claimingVatRelief.getOrElse(false)
        )
      )
      .as[JsObject]

  private def agriculturalTractorImportDetailsSection(vehicle: SpreadsheetAgriculturalTractorNonEuVehicle): JsObject =
    Json
      .toJson(
        ImportDetails(
          importEntryNumber = vehicle.importEntryNumber.getOrElse(""),
          importEntryDate = vehicle.importEntryDate.map(formPDateFormat.format).getOrElse("")
        )
      )
      .as[JsObject]

  private def agriculturalTractorImportVehicleTypeSection(vehicle: SpreadsheetAgriculturalTractorNonEuVehicle): JsObject =
    Json
      .toJson(
        ImportVehicleType(
          vehicleType = agriculturalTractorVehicleType,
          dateRoadUseKnown = vehicle.knownDateFirstRegistered.getOrElse(false),
          dateOfFirstRegistration = vehicle.dateOfFirstRegistration.map(formPDateFormat.format)
        )
      )
      .as[JsObject]

  private def agriculturalTractorImportVehicleDetailsSection(vehicle: SpreadsheetAgriculturalTractorNonEuVehicle): JsObject =
    Json.obj(
      "make"              -> vehicle.make.getOrElse(""),
      "seriesModel"       -> vehicle.seriesModel.getOrElse(""),
      "versionDerivative" -> vehicle.versionDerivative.getOrElse(""),
      "brakeHorsePower"   -> vehicle.brakeHorsepower.getOrElse("")
    )

  private def agriculturalTractorImportVehicleAdditionalInformationSection(
    vehicle: SpreadsheetAgriculturalTractorNonEuVehicle,
    isAmendment: Boolean
  ): JsObject =
    Json
      .toJson(
        ImportVehicleAdditionalInformation(
          dateArrivedInUk = vehicle.dateArrivedInUk.map(formPDateFormat.format).getOrElse(""),
          pricePaidForVehicleEntry = vehicle.pricePaid.map(_.toString).getOrElse(""),
          leftOrRightHand = vehicle.leftOrRightHandDrive.getOrElse(""),
          currencyUsed = vehicle.currency.getOrElse(""),
          commodityCode = vehicle.commodityCode.getOrElse(""),
          isAmendment = isAmendment,
          vehicleIdNumber = vehicle.vin.getOrElse(""),
          mileage = vehicle.mileage.getOrElse(""),
          mileageUnits = vehicle.mileageUnits.getOrElse(""),
          areYouClaimingRelief = vehicle.claimingVatRelief.getOrElse(false)
        )
      )
      .as[JsObject]

  private def motorCaravansSupplierDetailsSection(vehicle: SpreadsheetMotorCaravansEuVehicle): JsObject =
    Json
      .toJson(
        SupplierDetails(
          supplierBusinessIndividual =
            if (vehicle.supplierBusinessPrivate.exists(_.equalsIgnoreCase("business"))) BusinessOrPrivateIndividual.Business
            else BusinessOrPrivateIndividual.PrivateIndividual,
          supplierBusinessName = vehicle.supplierBusinessName,
          supplierTitle = vehicle.supplierTitle,
          supplierFirstName = vehicle.supplierFirstName,
          supplierLastName = vehicle.supplierLastName,
          addressLine1 = vehicle.addressLine1.getOrElse(""),
          addressLine2 = vehicle.addressLine2.getOrElse(""),
          addressLine3 = vehicle.addressLine3,
          addressLine4 = vehicle.addressLine4,
          addressLine5 = vehicle.addressLine5,
          postcode = vehicle.postcode,
          country = vehicle.country.getOrElse(""),
          isSupplierVatReg = vehicle.supplierVatRegistered.getOrElse(false),
          euStateVatReg = vehicle.euMemberState,
          vatRegistrationNumber = vehicle.supplierVatNumber
        )
      )
      .as[JsObject]

  private def motorCaravansVehicleTypeSection(vehicle: SpreadsheetMotorCaravansEuVehicle): JsObject =
    Json
      .toJson(
        VehicleType(
          vehicleType = motorCaravansVehicleType,
          doYouHaveAPurchaseInvoice = vehicle.purchaseInvoice.getOrElse(false),
          dateRoadUseKnown = vehicle.knownDateFirstRegistered.getOrElse(false),
          currencyUsed = vehicle.currency,
          purchaseInvoiceNumber = vehicle.purchaseInvoiceNumber,
          purchaseInvoiceDate = vehicle.purchaseInvoiceDate.map(formPDateFormat.format),
          pricePaidForVehicle = vehicle.pricePaid.map(_.toString)
        )
      )
      .as[JsObject]

  private def motorCaravansVehicleDetailsSection(vehicle: SpreadsheetMotorCaravansEuVehicle): JsObject =
    Json.obj(
      "caravanMake"       -> vehicle.caravanMake.getOrElse(""),
      "modelNameNumber"   -> vehicle.modelNameNumber.getOrElse(""),
      "caravanVersion"    -> vehicle.caravanVersion.getOrElse(""),
      "caravanBody"       -> vehicle.caravanBody.getOrElse(""),
      "makeOfBaseVehicle" -> vehicle.makeOfBaseVehicle.getOrElse(""),
      "derivative"        -> vehicle.derivative.getOrElse("")
    )

  private def motorCaravansVehicleAdditionalInformationSection(
    vehicle: SpreadsheetMotorCaravansEuVehicle,
    isAmendment: Boolean
  ): JsObject =
    Json
      .toJson(
        VehicleAdditionalInformation(
          dateArrivedInUk = vehicle.dateArrivedInUk.map(formPDateFormat.format).getOrElse(""),
          businessUnableToReclaimVat = vehicle.obtainedFromUnableToReclaimVat.getOrElse(false),
          leftOrRightHand = vehicle.leftOrRightHandDrive.getOrElse(""),
          vehicleSoldUnderMarginScheme = vehicle.soldUnderMarginScheme.getOrElse(false),
          confirmVehicleIdNumber = vehicle.vin.getOrElse(""),
          isAmendment,
          vehicleIdNumber = vehicle.vin.getOrElse(""),
          totalValueOfOptions = vehicle.totalValueOfOptions.map(_.toString).getOrElse(""),
          isSupplierVatReg = vehicle.supplierVatRegistered.getOrElse(false),
          mileage = vehicle.mileage.getOrElse(""),
          mileageUnits = vehicle.mileageUnits.getOrElse(""),
          areYouClaimingRelief = vehicle.claimingVatRelief.getOrElse(false)
        )
      )
      .as[JsObject]

  private def motorCaravansImportDetailsSection(vehicle: SpreadsheetMotorCaravansNonEuVehicle): JsObject =
    Json
      .toJson(
        ImportDetails(
          importEntryNumber = vehicle.importEntryNumber.getOrElse(""),
          importEntryDate = vehicle.importEntryDate.map(formPDateFormat.format).getOrElse("")
        )
      )
      .as[JsObject]

  private def motorCaravansImportVehicleTypeSection(vehicle: SpreadsheetMotorCaravansNonEuVehicle): JsObject =
    Json
      .toJson(
        ImportVehicleType(
          vehicleType = motorCaravansVehicleType,
          dateRoadUseKnown = vehicle.knownDateFirstRegistered.getOrElse(false),
          dateOfFirstRegistration = vehicle.dateOfFirstRegistration.map(formPDateFormat.format)
        )
      )
      .as[JsObject]

  private def motorCaravansImportVehicleDetailsSection(vehicle: SpreadsheetMotorCaravansNonEuVehicle): JsObject =
    Json.obj(
      "caravanMake"       -> vehicle.caravanMake.getOrElse(""),
      "modelNameNumber"   -> vehicle.modelNameNumber.getOrElse(""),
      "caravanVersion"    -> vehicle.caravanVersion.getOrElse(""),
      "caravanBody"       -> vehicle.caravanBody.getOrElse(""),
      "makeOfBaseVehicle" -> vehicle.makeOfBaseVehicle.getOrElse(""),
      "derivative"        -> vehicle.derivative.getOrElse("")
    )

  private def motorCaravansImportVehicleAdditionalInformationSection(
    vehicle: SpreadsheetMotorCaravansNonEuVehicle,
    isAmendment: Boolean
  ): JsObject =
    Json
      .toJson(
        ImportVehicleAdditionalInformation(
          dateArrivedInUk = vehicle.dateArrivedInUk.map(formPDateFormat.format).getOrElse(""),
          pricePaidForVehicleEntry = vehicle.pricePaid.map(_.toString).getOrElse(""),
          leftOrRightHand = vehicle.leftOrRightHandDrive.getOrElse(""),
          currencyUsed = vehicle.currency.getOrElse(""),
          commodityCode = vehicle.commodityCode.getOrElse(""),
          isAmendment = isAmendment,
          vehicleIdNumber = vehicle.vin.getOrElse(""),
          mileage = vehicle.mileage.getOrElse(""),
          mileageUnits = vehicle.mileageUnits.getOrElse(""),
          areYouClaimingRelief = vehicle.claimingVatRelief.getOrElse(false)
        )
      )
      .as[JsObject]

  private def heavyCommercialSupplierDetailsSection(vehicle: SpreadsheetHeavyCommercialEuVehicle): JsObject =
    Json
      .toJson(
        SupplierDetails(
          supplierBusinessIndividual =
            if (vehicle.supplierBusinessPrivate.exists(_.equalsIgnoreCase("business"))) BusinessOrPrivateIndividual.Business
            else BusinessOrPrivateIndividual.PrivateIndividual,
          supplierBusinessName = vehicle.supplierBusinessName,
          supplierTitle = vehicle.supplierTitle,
          supplierFirstName = vehicle.supplierFirstName,
          supplierLastName = vehicle.supplierLastName,
          addressLine1 = vehicle.addressLine1.getOrElse(""),
          addressLine2 = vehicle.addressLine2.getOrElse(""),
          addressLine3 = vehicle.addressLine3,
          addressLine4 = vehicle.addressLine4,
          addressLine5 = vehicle.addressLine5,
          postcode = vehicle.postcode,
          country = vehicle.country.getOrElse(""),
          isSupplierVatReg = vehicle.supplierVatRegistered.getOrElse(false),
          euStateVatReg = vehicle.euMemberState,
          vatRegistrationNumber = vehicle.supplierVatNumber
        )
      )
      .as[JsObject]

  private def heavyCommercialVehicleTypeSection(vehicle: SpreadsheetHeavyCommercialEuVehicle): JsObject =
    Json
      .toJson(
        VehicleType(
          vehicleType = hcvVehicleType,
          doYouHaveAPurchaseInvoice = vehicle.purchaseInvoice.getOrElse(false),
          dateRoadUseKnown = vehicle.knownDateFirstRegistered.getOrElse(false),
          currencyUsed = vehicle.currency,
          purchaseInvoiceNumber = vehicle.purchaseInvoiceNumber,
          purchaseInvoiceDate = vehicle.purchaseInvoiceDate.map(formPDateFormat.format),
          pricePaidForVehicle = vehicle.pricePaid.map(_.toString)
        )
      )
      .as[JsObject]

  private def heavyCommercialVehicleDetailsSection(vehicle: SpreadsheetHeavyCommercialEuVehicle): JsObject =
    Json.obj(
      "make"    -> vehicle.make.getOrElse(""),
      "model"   -> vehicle.model.getOrElse(""),
      "hcvType" -> vehicle.heavyCommercialVehicleType.getOrElse(""),
      "cabType" -> vehicle.cabType.getOrElse("")
    )

  private def heavyCommercialVehicleAdditionalInformationSection(
    vehicle: SpreadsheetHeavyCommercialEuVehicle,
    isAmendment: Boolean
  ): JsObject =
    Json
      .toJson(
        VehicleAdditionalInformation(
          dateArrivedInUk = vehicle.dateArrivedInUk.map(formPDateFormat.format).getOrElse(""),
          businessUnableToReclaimVat = vehicle.obtainedFromUnableToReclaimVat.getOrElse(false),
          leftOrRightHand = vehicle.leftOrRightHandDrive.getOrElse(""),
          vehicleSoldUnderMarginScheme = vehicle.soldUnderMarginScheme.getOrElse(false),
          confirmVehicleIdNumber = vehicle.vin.getOrElse(""),
          isAmendment,
          vehicleIdNumber = vehicle.vin.getOrElse(""),
          totalValueOfOptions = vehicle.totalValueOfOptions.map(_.toString).getOrElse(""),
          isSupplierVatReg = vehicle.supplierVatRegistered.getOrElse(false),
          mileage = vehicle.mileage.getOrElse(""),
          mileageUnits = vehicle.mileageUnits.getOrElse(""),
          areYouClaimingRelief = vehicle.claimingVatRelief.getOrElse(false)
        )
      )
      .as[JsObject]

  private def heavyCommercialImportDetailsSection(vehicle: SpreadsheetHeavyCommercialNonEuVehicle): JsObject =
    Json
      .toJson(
        ImportDetails(
          importEntryNumber = vehicle.importEntryNumber.getOrElse(""),
          importEntryDate = vehicle.importEntryDate.map(formPDateFormat.format).getOrElse("")
        )
      )
      .as[JsObject]

  private def heavyCommercialImportVehicleTypeSection(vehicle: SpreadsheetHeavyCommercialNonEuVehicle): JsObject =
    Json
      .toJson(
        ImportVehicleType(
          vehicleType = hcvVehicleType,
          dateRoadUseKnown = vehicle.knownDateFirstRegistered.getOrElse(false),
          dateOfFirstRegistration = vehicle.dateOfFirstRegistration.map(formPDateFormat.format)
        )
      )
      .as[JsObject]

  private def heavyCommercialImportVehicleDetailsSection(vehicle: SpreadsheetHeavyCommercialNonEuVehicle): JsObject =
    Json.obj(
      "make"    -> vehicle.make.getOrElse(""),
      "model"   -> vehicle.model.getOrElse(""),
      "hcvType" -> vehicle.heavyCommercialVehicleType.getOrElse(""),
      "cabType" -> vehicle.cabType.getOrElse("")
    )

  private def heavyCommercialImportVehicleAdditionalInformationSection(
    vehicle: SpreadsheetHeavyCommercialNonEuVehicle,
    isAmendment: Boolean
  ): JsObject =
    Json
      .toJson(
        ImportVehicleAdditionalInformation(
          dateArrivedInUk = vehicle.dateArrivedInUk.map(formPDateFormat.format).getOrElse(""),
          pricePaidForVehicleEntry = vehicle.pricePaid.map(_.toString).getOrElse(""),
          leftOrRightHand = vehicle.leftOrRightHandDrive.getOrElse(""),
          currencyUsed = vehicle.currency.getOrElse(""),
          commodityCode = vehicle.commodityCode.getOrElse(""),
          isAmendment = isAmendment,
          vehicleIdNumber = vehicle.vin.getOrElse(""),
          mileage = vehicle.mileage.getOrElse(""),
          mileageUnits = vehicle.mileageUnits.getOrElse(""),
          areYouClaimingRelief = vehicle.claimingVatRelief.getOrElse(false)
        )
      )
      .as[JsObject]

  private def motorcyclesSupplierDetailsSection(vehicle: SpreadsheetMotorcyclesEuVehicle): JsObject =
    Json
      .toJson(
        SupplierDetails(
          supplierBusinessIndividual =
            if (vehicle.supplierBusinessPrivate.exists(_.equalsIgnoreCase("business"))) BusinessOrPrivateIndividual.Business
            else BusinessOrPrivateIndividual.PrivateIndividual,
          supplierBusinessName = vehicle.supplierBusinessName,
          supplierTitle = vehicle.supplierTitle,
          supplierFirstName = vehicle.supplierFirstName,
          supplierLastName = vehicle.supplierLastName,
          addressLine1 = vehicle.addressLine1.getOrElse(""),
          addressLine2 = vehicle.addressLine2.getOrElse(""),
          addressLine3 = vehicle.addressLine3,
          addressLine4 = vehicle.addressLine4,
          addressLine5 = vehicle.addressLine5,
          postcode = vehicle.postcode,
          country = vehicle.country.getOrElse(""),
          isSupplierVatReg = vehicle.supplierVatRegistered.getOrElse(false),
          euStateVatReg = vehicle.euMemberState,
          vatRegistrationNumber = vehicle.supplierVatNumber
        )
      )
      .as[JsObject]

  private def motorcyclesVehicleTypeSection(vehicle: SpreadsheetMotorcyclesEuVehicle): JsObject =
    Json
      .toJson(
        VehicleType(
          vehicleType = motorcycleVehicleType,
          doYouHaveAPurchaseInvoice = vehicle.purchaseInvoice.getOrElse(false),
          dateRoadUseKnown = vehicle.knownDateFirstRegistered.getOrElse(false),
          currencyUsed = vehicle.currency,
          purchaseInvoiceNumber = vehicle.purchaseInvoiceNumber,
          purchaseInvoiceDate = vehicle.purchaseInvoiceDate.map(formPDateFormat.format),
          pricePaidForVehicle = vehicle.pricePaid.map(_.toString)
        )
      )
      .as[JsObject]

  private def motorcyclesVehicleDetailsSection(vehicle: SpreadsheetMotorcyclesEuVehicle): JsObject =
    Json.obj(
      "make"              -> vehicle.make.getOrElse(""),
      "model"             -> vehicle.model.getOrElse(""),
      "derivative"        -> vehicle.derivative.getOrElse(""),
      "motorcycleVersion" -> vehicle.version.getOrElse(""),
      "motorcycleType"    -> vehicle.motorcycleType.getOrElse(""),
      "motorcycleStyle"   -> vehicle.style.getOrElse(""),
      "transmission"      -> vehicle.transmissionType.getOrElse(""),
      "fuelType"          -> vehicle.fuelType.getOrElse(""),
      "engineSize"        -> vehicle.engineSize.getOrElse("")
    )

  private def motorcyclesVehicleAdditionalInformationSection(
    vehicle: SpreadsheetMotorcyclesEuVehicle,
    isAmendment: Boolean
  ): JsObject =
    Json
      .toJson(
        VehicleAdditionalInformation(
          dateArrivedInUk = vehicle.dateArrivedInUk.map(formPDateFormat.format).getOrElse(""),
          businessUnableToReclaimVat = vehicle.obtainedFromUnableToReclaimVat.getOrElse(false),
          leftOrRightHand = vehicle.leftOrRightHandDrive.getOrElse(""),
          vehicleSoldUnderMarginScheme = vehicle.soldUnderMarginScheme.getOrElse(false),
          confirmVehicleIdNumber = vehicle.vin.getOrElse(""),
          isAmendment,
          vehicleIdNumber = vehicle.vin.getOrElse(""),
          totalValueOfOptions = vehicle.totalValueOfOptions.map(_.toString).getOrElse(""),
          isSupplierVatReg = vehicle.supplierVatRegistered.getOrElse(false),
          mileage = vehicle.mileage.getOrElse(""),
          mileageUnits = vehicle.mileageUnits.getOrElse(""),
          areYouClaimingRelief = vehicle.claimingVatRelief.getOrElse(false)
        )
      )
      .as[JsObject]

  private def motorcyclesImportDetailsSection(vehicle: SpreadsheetMotorcyclesNonEuVehicle): JsObject =
    Json
      .toJson(
        ImportDetails(
          importEntryNumber = vehicle.importEntryNumber.getOrElse(""),
          importEntryDate = vehicle.importEntryDate.map(formPDateFormat.format).getOrElse("")
        )
      )
      .as[JsObject]

  private def motorcyclesImportVehicleTypeSection(vehicle: SpreadsheetMotorcyclesNonEuVehicle): JsObject =
    Json
      .toJson(
        ImportVehicleType(
          vehicleType = motorcycleVehicleType,
          dateRoadUseKnown = vehicle.knownDateFirstRegistered.getOrElse(false),
          dateOfFirstRegistration = vehicle.dateOfFirstRegistration.map(formPDateFormat.format)
        )
      )
      .as[JsObject]

  private def motorcyclesImportVehicleDetailsSection(vehicle: SpreadsheetMotorcyclesNonEuVehicle): JsObject =
    Json.obj(
      "make"              -> vehicle.make.getOrElse(""),
      "model"             -> vehicle.model.getOrElse(""),
      "derivative"        -> vehicle.derivative.getOrElse(""),
      "motorcycleVersion" -> vehicle.version.getOrElse(""),
      "motorcycleType"    -> vehicle.motorcycleType.getOrElse(""),
      "motorcycleStyle"   -> vehicle.style.getOrElse(""),
      "transmission"      -> vehicle.transmissionType.getOrElse(""),
      "fuelType"          -> vehicle.fuelType.getOrElse(""),
      "engineSize"        -> vehicle.engineSize.getOrElse("")
    )

  private def motorcyclesImportVehicleAdditionalInformationSection(
    vehicle: SpreadsheetMotorcyclesNonEuVehicle,
    isAmendment: Boolean
  ): JsObject =
    Json
      .toJson(
        ImportVehicleAdditionalInformation(
          dateArrivedInUk = vehicle.dateArrivedInUk.map(formPDateFormat.format).getOrElse(""),
          pricePaidForVehicleEntry = vehicle.pricePaid.map(_.toString).getOrElse(""),
          leftOrRightHand = vehicle.leftOrRightHandDrive.getOrElse(""),
          currencyUsed = vehicle.currency.getOrElse(""),
          commodityCode = vehicle.commodityCode.getOrElse(""),
          isAmendment = isAmendment,
          vehicleIdNumber = vehicle.vin.getOrElse(""),
          mileage = vehicle.mileage.getOrElse(""),
          mileageUnits = vehicle.mileageUnits.getOrElse(""),
          areYouClaimingRelief = vehicle.claimingVatRelief.getOrElse(false)
        )
      )
      .as[JsObject]

  private def constructionVehicleSupplierDetailsSection(vehicle: SpreadsheetConstructionVehiclesEuVehicle): JsObject =
    Json
      .toJson(
        SupplierDetails(
          supplierBusinessIndividual =
            if (vehicle.supplierBusinessPrivate.exists(_.equalsIgnoreCase("business"))) BusinessOrPrivateIndividual.Business
            else BusinessOrPrivateIndividual.PrivateIndividual,
          supplierBusinessName = vehicle.supplierBusinessName,
          supplierTitle = vehicle.supplierTitle,
          supplierFirstName = vehicle.supplierFirstName,
          supplierLastName = vehicle.supplierLastName,
          addressLine1 = vehicle.addressLine1.getOrElse(""),
          addressLine2 = vehicle.addressLine2.getOrElse(""),
          addressLine3 = vehicle.addressLine3,
          addressLine4 = vehicle.addressLine4,
          addressLine5 = vehicle.addressLine5,
          postcode = vehicle.postcode,
          country = vehicle.country.getOrElse(""),
          isSupplierVatReg = vehicle.supplierVatRegistered.getOrElse(false),
          euStateVatReg = vehicle.euMemberState,
          vatRegistrationNumber = vehicle.supplierVatNumber
        )
      )
      .as[JsObject]

  private def constructionVehicleVehicleTypeSection(vehicle: SpreadsheetConstructionVehiclesEuVehicle): JsObject =
    Json
      .toJson(
        VehicleType(
          vehicleType = constructionVehicleType,
          doYouHaveAPurchaseInvoice = vehicle.purchaseInvoice.getOrElse(false),
          dateRoadUseKnown = vehicle.knownDateFirstRegistered.getOrElse(false),
          currencyUsed = vehicle.currency,
          purchaseInvoiceNumber = vehicle.purchaseInvoiceNumber,
          purchaseInvoiceDate = vehicle.purchaseInvoiceDate.map(formPDateFormat.format),
          pricePaidForVehicle = vehicle.pricePaid.map(_.toString)
        )
      )
      .as[JsObject]

  private def constructionVehicleVehicleDetailsSection(vehicle: SpreadsheetConstructionVehiclesEuVehicle): JsObject =
    Json.obj(
      "make"              -> vehicle.make.getOrElse(""),
      "seriesModel"       -> vehicle.seriesModel.getOrElse(""),
      "versionDerivative" -> vehicle.versionDerivative.getOrElse("")
    )

  private def constructionVehicleVehicleAdditionalInformationSection(
    vehicle: SpreadsheetConstructionVehiclesEuVehicle,
    isAmendment: Boolean
  ): JsObject =
    Json
      .toJson(
        VehicleAdditionalInformation(
          dateArrivedInUk = vehicle.dateArrivedInUk.map(formPDateFormat.format).getOrElse(""),
          businessUnableToReclaimVat = vehicle.obtainedFromUnableToReclaimVat.getOrElse(false),
          leftOrRightHand = vehicle.leftOrRightHandDrive.getOrElse(""),
          vehicleSoldUnderMarginScheme = vehicle.soldUnderMarginScheme.getOrElse(false),
          confirmVehicleIdNumber = vehicle.vin.getOrElse(""),
          isAmendment,
          vehicleIdNumber = vehicle.vin.getOrElse(""),
          totalValueOfOptions = vehicle.totalValueOfOptions.map(_.toString).getOrElse(""),
          isSupplierVatReg = vehicle.supplierVatRegistered.getOrElse(false),
          mileage = vehicle.mileage.getOrElse(""),
          mileageUnits = vehicle.mileageUnits.getOrElse(""),
          areYouClaimingRelief = vehicle.claimingVatRelief.getOrElse(false)
        )
      )
      .as[JsObject]

  private def constructionVehicleImportDetailsSection(vehicle: SpreadsheetConstructionVehiclesNonEuVehicle): JsObject =
    Json
      .toJson(
        ImportDetails(
          importEntryNumber = vehicle.importEntryNumber.getOrElse(""),
          importEntryDate = vehicle.importEntryDate.map(formPDateFormat.format).getOrElse("")
        )
      )
      .as[JsObject]

  private def constructionVehicleImportVehicleTypeSection(vehicle: SpreadsheetConstructionVehiclesNonEuVehicle): JsObject =
    Json
      .toJson(
        ImportVehicleType(
          vehicleType = constructionVehicleType,
          dateRoadUseKnown = vehicle.knownDateFirstRegistered.getOrElse(false),
          dateOfFirstRegistration = vehicle.dateOfFirstRegistration.map(formPDateFormat.format)
        )
      )
      .as[JsObject]

  private def constructionVehicleImportVehicleDetailsSection(vehicle: SpreadsheetConstructionVehiclesNonEuVehicle): JsObject =
    Json.obj(
      "make"              -> vehicle.make.getOrElse(""),
      "seriesModel"       -> vehicle.seriesModel.getOrElse(""),
      "versionDerivative" -> vehicle.versionDerivative.getOrElse("")
    )

  private def constructionVehicleImportVehicleAdditionalInformationSection(
    vehicle: SpreadsheetConstructionVehiclesNonEuVehicle,
    isAmendment: Boolean
  ): JsObject =
    Json
      .toJson(
        ImportVehicleAdditionalInformation(
          dateArrivedInUk = vehicle.dateArrivedInUk.map(formPDateFormat.format).getOrElse(""),
          pricePaidForVehicleEntry = vehicle.pricePaid.map(_.toString).getOrElse(""),
          leftOrRightHand = vehicle.leftOrRightHandDrive.getOrElse(""),
          currencyUsed = vehicle.currency.getOrElse(""),
          commodityCode = vehicle.commodityCode.getOrElse(""),
          isAmendment = isAmendment,
          vehicleIdNumber = vehicle.vin.getOrElse(""),
          mileage = vehicle.mileage.getOrElse(""),
          mileageUnits = vehicle.mileageUnits.getOrElse(""),
          areYouClaimingRelief = vehicle.claimingVatRelief.getOrElse(false)
        )
      )
      .as[JsObject]
}
