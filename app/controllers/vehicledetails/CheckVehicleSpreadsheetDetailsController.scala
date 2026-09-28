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
import models.responses.{SpreadsheetEuVehicle, SpreadsheetNonEuVehicle, UploadResultResponse}
import models.{BusinessOrPrivateIndividual, DraftId}
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
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
              saveAllVehicles(draftId, result.euVehicles, isAmendment, versionId).flatMap(afterSave(draftId, request.userAnswers, _))

            case Right(result) if result.fileStatus == "VALIDATED" && result.validationType.exists(nonEuValidationTypes.contains) =>
              saveAllImportVehicles(draftId, result.nonEuVehicles, result.validationType.get, isAmendment, versionId)
                .flatMap(afterSave(draftId, request.userAnswers, _))

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

  private def afterSave(draftId: DraftId, userAnswers: models.UserAnswers, result: Either[String, Long])(implicit
    hc: HeaderCarrier
  ): Future[Result] =
    result match {
      case Right(newVersionId) =>
        for {
          _ <- sessionRepository.setPage(userAnswers, DraftVersionIdPage, newVersionId)
          _ <- connector.deleteFileUpload(draftId).map {
                 case Right(_)    => ()
                 case Left(error) => logger.warn(s"Could not delete the vehicle spreadsheet upload after saving for draftId ${draftId.value}: $error")
               }
        } yield Redirect(controllers.routes.NotificationTaskListController.onPageLoad()) // TODO: navigate to UVS6.0 when built
      case Left(error) =>
        logger.warn(s"Failed to save the vehicle spreadsheet sections for draftId ${draftId.value}: $error")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  private def saveAllVehicles(draftId: DraftId, vehicles: Seq[SpreadsheetEuVehicle], isAmendment: Boolean, versionId: Long)(implicit
    hc: HeaderCarrier
  ): Future[Either[String, Long]] =
    vehicles.zipWithIndex.foldLeft(Future.successful(Right(versionId): Either[String, Long])) { case (acc, (vehicle, index)) =>
      acc.flatMap {
        case Left(error) => Future.successful(Left(error))
        case Right(v)    => saveVehicle(draftId, supplierNumber = index + 1, vehicle, isAmendment, v)
      }
    }

  private def saveVehicle(draftId: DraftId, supplierNumber: Int, vehicle: SpreadsheetEuVehicle, isAmendment: Boolean, versionId: Long)(implicit
    hc: HeaderCarrier
  ): Future[Either[String, Long]] = {
    val sections = Seq(
      s"supplier/$supplierNumber/details"                          -> supplierDetailsSection(vehicle),
      s"supplier/$supplierNumber/vehicle/1/type"                   -> vehicleTypeSection(vehicle),
      s"supplier/$supplierNumber/vehicle/1/details"                -> vehicleDetailsSection(vehicle),
      s"supplier/$supplierNumber/vehicle/1/additional-information" -> vehicleAdditionalInformationSection(vehicle, isAmendment)
    )

    saveSections(draftId, sections, versionId)
  }

  private def saveAllImportVehicles(
    draftId: DraftId,
    vehicles: Seq[SpreadsheetNonEuVehicle],
    validationType: String,
    isAmendment: Boolean,
    versionId: Long
  )(implicit hc: HeaderCarrier): Future[Either[String, Long]] =
    vehicles.zipWithIndex.foldLeft(Future.successful(Right(versionId): Either[String, Long])) { case (acc, (vehicle, index)) =>
      acc.flatMap {
        case Left(error) => Future.successful(Left(error))
        case Right(v)    => saveImportVehicle(draftId, importNumber = index + 1, vehicle, validationType, isAmendment, v)
      }
    }

  private def saveImportVehicle(
    draftId: DraftId,
    importNumber: Int,
    vehicle: SpreadsheetNonEuVehicle,
    validationType: String,
    isAmendment: Boolean,
    versionId: Long
  )(implicit hc: HeaderCarrier): Future[Either[String, Long]] = {
    val sections = Seq(
      s"import/$importNumber/details"                          -> importDetailsSection(vehicle),
      s"import/$importNumber/vehicle/1/type"                   -> importVehicleTypeSection(vehicle, validationType),
      s"import/$importNumber/vehicle/1/details"                -> importVehicleDetailsSection(vehicle, validationType),
      s"import/$importNumber/vehicle/1/additional-information" -> importVehicleAdditionalInformationSection(vehicle, isAmendment)
    )

    saveSections(draftId, sections, versionId)
  }

  private def saveSections(draftId: DraftId, sections: Seq[(String, JsObject)], versionId: Long)(implicit
    hc: HeaderCarrier
  ): Future[Either[String, Long]] =
    sections.foldLeft(Future.successful(Right(versionId): Either[String, Long])) { case (acc, (sectionId, data)) =>
      acc.flatMap {
        case Left(error) => Future.successful(Left(error))
        case Right(v)    => saveSection(draftId, sectionId, data, v)
      }
    }

  private def saveSection(draftId: DraftId, sectionId: String, data: JsObject, versionId: Long)(implicit
    hc: HeaderCarrier
  ): Future[Either[String, Long]] =
    connector.updateDraftSection(draftId, sectionId, data + ("versionId" -> Json.toJson(versionId))).map {
      case Right(newVersionId) => Right(newVersionId)
      case Left(error)         => Left(s"failed to update '$sectionId': $error")
    }
}

object CheckVehicleSpreadsheetDetailsController {

  private val euValidationTypes    = Set("CarsEu", "LightCommercialVehiclesEu")
  private val nonEuValidationTypes = Set("CarsNonEu", "LightCommercialVehiclesNonEu")

  private val formPDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

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
}
