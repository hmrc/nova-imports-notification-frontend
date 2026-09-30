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
import connectors.NovaImportsBackendConnector
import controllers.BaseController
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import models.draftsections.{ImportVehicleType, VehicleType}
import models.requests.DataRequest
import models.{ImportNumber, SupplierNumber, UserAnswers, VehicleDates, VehicleNumber}
import navigation.ConfirmVehicleDetailsJourney
import pages.sections.initialquestions.VehicleFromEuPage
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
import viewmodels.checkAnswers.ConfirmVehicleDetailsHelper
import views.html.ConfirmVehicleDetailsView

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmVehicleDetailsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  appConfig: FrontendAppConfig,
  backendConnector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  supplierService: SupplierService,
  importService: ImportService,
  vehicleService: VehicleService,
  view: ConfirmVehicleDetailsView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import ConfirmVehicleDetailsController.*

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)) {
      implicit request =>
        handlePageLoad(
          ConfirmVehicleDetailsHelper
            .supplierSummaryList(request.userAnswers, supplierNumber, vehicleNumber, appConfig.countries, appConfig.currencies),
          routes.ConfirmVehicleDetailsController.supplierOnSubmit(supplierNumber, vehicleNumber)
        )
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)) { implicit request =>
      handlePageLoad(
        ConfirmVehicleDetailsHelper.importSummaryList(request.userAnswers, importNumber, vehicleNumber, appConfig.countries),
        routes.ConfirmVehicleDetailsController.importOnSubmit(importNumber, vehicleNumber)
      )
    }

  def supplierOnSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(supplierGuardPredicate(supplierService, vehicleService, supplierNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(
          s"supplier/${supplierNumber.value}/vehicle/${vehicleNumber.value}/type",
          supplierVehicleTypeSection(request.userAnswers, supplierNumber, vehicleNumber),
          vehicleNumber
        )
    }

  def importOnSubmit(importNumber: ImportNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(importGuardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async {
      implicit request =>
        handleSubmit(
          s"import/${importNumber.value}/vehicle/${vehicleNumber.value}/type",
          importVehicleTypeSection(request.userAnswers, vehicleNumber),
          vehicleNumber
        )
    }

  private def handlePageLoad(summaryList: SummaryList, submitCall: Call)(implicit
    request: DataRequest[AnyContent]
  ): Result =
    Ok(view(summaryList, submitCall))

  private def handleSubmit(sectionId: String, sectionData: Option[JsObject], vehicleNumber: VehicleNumber)(implicit
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
              .map(_ => Redirect(ConfirmVehicleDetailsJourney.confirmedRoute(request.userAnswers, vehicleNumber)))
          case Left(error) =>
            logger.warn(s"Failed to update '$sectionId' for draftId ${draftId.value}: $error")
            Future.successful(failure)
        }
      case _ =>
        logger.warn(s"Failed to submit '$sectionId', draftId, versionId or vehicle type missing")
        Future.successful(failure)
    }
  }
}

object ConfirmVehicleDetailsController {

  private val formPDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

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
      ConfirmVehicleDetailsJourney.supplierFirstUnanswered(request.userAnswers, supplierNumber, vehicleNumber).isEmpty

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
      ConfirmVehicleDetailsJourney.importFirstUnanswered(request.userAnswers, importNumber, vehicleNumber).isEmpty

  def supplierVehicleTypeSection(answers: UserAnswers, supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Option[JsObject] = {
    val dates       = answers.get(VehicleDatesPage(supplierNumber, vehicleNumber)).getOrElse(Set.empty)
    val invoiceDate = dates.contains(VehicleDates.PurchaseInvoiceDate)

    answers.get(AddVehicleTypePage(vehicleNumber)).map { vehicleType =>
      Json
        .toJson(
          VehicleType(
            vehicleType = vehicleType.jsonValue,
            doYouHaveAPurchaseInvoice = invoiceDate,
            dateRoadUseKnown = dates.contains(VehicleDates.AvailabilityAndFirstRegistration),
            currencyUsed = answers.get(PaymentCurrencyPage(vehicleNumber)),
            purchaseInvoiceNumber = Option.when(invoiceDate)(answers.get(PurchaseInvoiceNumberPage(supplierNumber, vehicleNumber))).flatten,
            purchaseInvoiceDate =
              Option.when(invoiceDate)(answers.get(PurchaseInvoiceDatePage(supplierNumber, vehicleNumber)).map(formPDateFormat.format)).flatten,
            pricePaidForVehicle = answers.get(TotalAmountPaidPage(vehicleNumber))
          )
        )
        .as[JsObject]
    }
  }

  def importVehicleTypeSection(answers: UserAnswers, vehicleNumber: VehicleNumber): Option[JsObject] = {
    val dateOfFirstRegistration = answers.get(DateOfFirstRegistrationPage(vehicleNumber))

    answers.get(AddVehicleTypePage(vehicleNumber)).map { vehicleType =>
      Json
        .toJson(
          ImportVehicleType(
            vehicleType = vehicleType.jsonValue,
            dateRoadUseKnown = dateOfFirstRegistration.isDefined,
            dateOfFirstRegistration = dateOfFirstRegistration.map(formPDateFormat.format)
          )
        )
        .as[JsObject]
    }
  }
}
