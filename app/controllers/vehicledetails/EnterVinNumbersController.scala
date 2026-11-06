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
import forms.EnterVinNumbersFormProvider
import models.requests.DataRequest
import models.{CheckMode, DraftId, ImportNumber, Mode, NormalMode, SupplierNumber, VehicleNumber}
import pages.sections.vehicledetails.NumberOfVehiclesPage
import pages.{DraftIdPage, DraftVersionIdPage, ValidatedVinListPage}
import play.api.Logging
import play.api.data.{Form, FormError}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{ImportService, SupplierService, VehicleService, VinDuplicateCheckService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.VehicleDetailsInset
import views.html.EnterVinNumbersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnterVinNumbersController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  connector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  actions: Actions,
  formProvider: EnterVinNumbersFormProvider,
  supplierService: SupplierService,
  importService: ImportService,
  vehicleService: VehicleService,
  view: EnterVinNumbersView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  val form: Form[String] = formProvider()

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request =>
        handlePageLoad(vehicleNumber, routes.EnterVinNumbersController.supplierOnSubmit(supplierNumber, vehicleNumber, mode))
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async {
      implicit request =>
        handlePageLoad(vehicleNumber, routes.EnterVinNumbersController.importOnSubmit(importNumber, vehicleNumber, mode))
    }

  def supplierOnSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(
          vehicleNumber,
          routes.EnterVinNumbersController.supplierOnSubmit(supplierNumber, vehicleNumber, mode),
          journey = "supplier",
          groupNumber = supplierNumber.value,
          nextPage = nextPageFor(mode, routes.CheckVinNumbersController.supplierOnPageLoad(supplierNumber, vehicleNumber, page = 1))
        )
    }

  def importOnSubmit(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(
          vehicleNumber,
          routes.EnterVinNumbersController.importOnSubmit(importNumber, vehicleNumber, mode),
          journey = "import",
          groupNumber = importNumber.value,
          nextPage = nextPageFor(mode, routes.CheckVinNumbersController.importOnPageLoad(importNumber, vehicleNumber, page = 1))
        )
    }

  private def nextPageFor(mode: Mode, normalModeNext: Call): Call =
    mode match {
      case NormalMode => normalModeNext
      case CheckMode  => controllers.routes.LandingPageController.onPageLoad() // TODO: navigate to C-AVD3.0 when built
    }

  private def numberOfVehiclesFor(vehicleNumber: VehicleNumber)(implicit request: DataRequest[AnyContent]): Int =
    request.userAnswers.get(NumberOfVehiclesPage(vehicleNumber)).flatMap(_.numberSameVehicles).getOrElse(0)

  private def handlePageLoad(vehicleNumber: VehicleNumber, submitCall: Call)(implicit request: DataRequest[AnyContent]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val draftId          = request.userAnswers.get(DraftIdPage).get
    val numberOfVehicles = numberOfVehiclesFor(vehicleNumber)

    connector.getUploadResult(draftId).map {
      case Right(result) if result.fileStatus == "UPLOADED" && result.rawVinContent.isDefined =>
        Ok(
          view(form.fill(result.rawVinContent.get), Seq.empty, VehicleDetailsInset(request.userAnswers, vehicleNumber), submitCall, numberOfVehicles)
        )
      case _ =>
        Ok(view(form, Seq.empty, VehicleDetailsInset(request.userAnswers, vehicleNumber), submitCall, numberOfVehicles))
    }
  }

  private def handleSubmit(vehicleNumber: VehicleNumber, submitCall: Call, journey: String, groupNumber: Int, nextPage: Call)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    def rerender(boundForm: Form[String], backendErrors: Seq[String] = Seq.empty): Result =
      BadRequest(
        view(boundForm, backendErrors, VehicleDetailsInset(request.userAnswers, vehicleNumber), submitCall, numberOfVehiclesFor(vehicleNumber))
      )

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(rerender(formWithErrors)),
        rawInput => {
          (request.userAnswers.get(DraftIdPage), request.userAnswers.get(NumberOfVehiclesPage(vehicleNumber)).flatMap(_.numberSameVehicles)) match {
            case (Some(draftId), Some(expectedCount)) =>
              connector.validateVins(draftId, rawInput, expectedCount).flatMap {
                case Right(result) if result.valid && hasDuplicateVins(result.vins) =>
                  Future.successful(rerender(form.fill(rawInput).copy(errors = Seq(FormError("enterVinNumbers", "enterVinNumbers.error.duplicate")))))
                case Right(result) if result.valid =>
                  checkForCrossNotificationDuplicates(draftId, result.vins, journey, groupNumber, rawInput, submitCall, vehicleNumber, nextPage)
                case Right(result) =>
                  Future.successful(rerender(form.fill(rawInput), result.errors.map(_.error)))
                case Left(error) =>
                  logger.warn(s"Could not validate VINs for draftId ${draftId.value}: $error")
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              }
            case _ =>
              logger.warn("Could not submit VIN numbers, draftId or numberSameVehicles missing")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
        }
      )
  }

  private def hasDuplicateVins(vins: Seq[String]): Boolean =
    vins.map(_.toUpperCase).distinct.size != vins.size

  private def checkForCrossNotificationDuplicates(
    draftId: DraftId,
    vins: Seq[String],
    journey: String,
    groupNumber: Int,
    rawInput: String,
    submitCall: Call,
    vehicleNumber: VehicleNumber,
    nextPage: Call
  )(implicit request: DataRequest[AnyContent]): Future[Result] =
    connector.getDraftNotification(draftId).flatMap {
      case Right(notification) =>
        val duplicates = VinDuplicateCheckService.crossNotificationDuplicates(notification.sections, vins, journey, groupNumber)
        if (duplicates.nonEmpty) {
          Future.successful(
            BadRequest(
              view(
                form.fill(rawInput),
                Seq(messagesApi.preferred(request)("enterVinNumbers.error.duplicate.crossNotification")),
                VehicleDetailsInset(request.userAnswers, vehicleNumber),
                submitCall,
                numberOfVehiclesFor(vehicleNumber)
              )
            )
          )
        } else {
          saveMultipleVins(draftId, journey, groupNumber, vehicleNumber, vins, nextPage)
        }
      case Left(error) =>
        logger.warn(s"Could not retrieve the draft notification to check for duplicate VINs for draftId ${draftId.value}: $error")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  private def saveMultipleVins(
    draftId: DraftId,
    journey: String,
    groupNumber: Int,
    vehicleNumber: VehicleNumber,
    vins: Seq[String],
    nextPage: Call
  )(implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers.get(DraftVersionIdPage) match {
      case Some(versionId) =>
        val sectionId   = s"$journey/$groupNumber/vehicle/${vehicleNumber.value}/multiple-vins"
        val sectionData = Json.obj("multipleVins" -> vins.mkString(", "), "versionId" -> Json.toJson(versionId))

        connector.updateDraftSection(draftId, sectionId, sectionData).flatMap {
          case Right(newVersionId) =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(ValidatedVinListPage, vins))
              _              <- sessionRepository.setPage(updatedAnswers, DraftVersionIdPage, newVersionId)
            } yield Redirect(nextPage)
          case Left(error) =>
            logger.warn(s"Failed to save the '$sectionId' section for draftId ${draftId.value}: $error")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      case None =>
        logger.warn(s"Could not save the multiple-vins section for draftId ${draftId.value}: versionId missing")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
}
