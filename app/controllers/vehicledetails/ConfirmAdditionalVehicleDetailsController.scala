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
import connectors.{NovaImportsBackendConnector, UpdateSectionError}
import controllers.BaseController
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import models.requests.DataRequest
import models.{DraftId, DraftNotificationSection, ImportNumber, OneMultiVehicle, SupplierNumber, UserAnswers, VehicleNumber}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.introduction.AmendSubmittedNotificationPage
import pages.sections.supplierdetails.IsSupplierVatRegisteredPage
import pages.sections.vehicledetails.*
import pages.{DraftIdPage, DraftVersionIdPage}
import play.api.Logging
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.checkAnswers.ConfirmAdditionalVehicleDetailsHelper
import views.html.ConfirmAdditionalVehicleDetailsView

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmAdditionalVehicleDetailsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  backendConnector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  appConfig: FrontendAppConfig,
  view: ConfirmAdditionalVehicleDetailsView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import ConfirmAdditionalVehicleDetailsController.*

  def supplierOnPageLoad(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
      handlePageLoad(
        ConfirmAdditionalVehicleDetailsHelper.supplierSummaryList(request.userAnswers, supplierNumber, vehicleNumber, appConfig.currencies),
        routes.ConfirmAdditionalVehicleDetailsController.supplierOnSubmit(supplierNumber, vehicleNumber)
      )
    }

  def importOnPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate) { implicit request =>
      handlePageLoad(
        ConfirmAdditionalVehicleDetailsHelper.importSummaryList(request.userAnswers, importNumber, vehicleNumber, appConfig.currencies),
        routes.ConfirmAdditionalVehicleDetailsController.importOnSubmit(importNumber, vehicleNumber)
      )
    }

  def supplierOnSubmit(supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      handleSubmit(
        SupplierJourney,
        supplierNumber.value,
        vehicleNumber,
        supplierFields(request.userAnswers, supplierNumber, vehicleNumber)
      )
    }

  def importOnSubmit(importNumber: ImportNumber, vehicleNumber: VehicleNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate).async { implicit request =>
      handleSubmit(
        ImportJourney,
        importNumber.value,
        vehicleNumber,
        importFields(request.userAnswers, vehicleNumber)
      )
    }

  private def handlePageLoad(summaryList: SummaryList, submitCall: Call)(implicit request: DataRequest[AnyContent]): Result =
    Ok(view(summaryList, submitCall))

  private def handleSubmit(journey: String, groupNumber: Int, vehicleNumber: VehicleNumber, fields: JsObject)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val failure = Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

    (
      request.userAnswers.get(DraftIdPage),
      request.userAnswers.get(DraftVersionIdPage),
      request.userAnswers.get(NumberOfVehiclesPage(vehicleNumber))
    ) match {
      case (Some(draftId), Some(versionId), Some(numberOfVehicles)) =>
        backendConnector.getDraftNotification(draftId).flatMap {
          case Right(draft) =>
            sectionsToUpdate(draft.sections, journey, groupNumber, vehicleNumber, numberOfVehicles.oneMultiVehicle, fields) match {
              case Nil =>
                logger.warn(s"No additional-information sections to update for draftId ${draftId.value}, $journey/$groupNumber")
                failure
              case sections =>
                saveSections(draftId, sections, versionId).flatMap {
                  case Right(newVersionId) =>
                    sessionRepository
                      .setPage(request.userAnswers, DraftVersionIdPage, newVersionId)
                      .map(_ => Redirect(nextPage))
                  case Left(error) =>
                    logger.warn(s"Failed to update the additional-information sections for draftId ${draftId.value}: $error")
                    failure
                }
            }
          case Left(error) =>
            logger.warn(s"Could not retrieve the draft notification to save the additional vehicle details for draftId ${draftId.value}: $error")
            failure
        }
      case _ =>
        logger.warn("Failed to submit the additional vehicle details, draftId, versionId or number of vehicles missing")
        failure
    }
  }

  private def saveSections(draftId: DraftId, sections: Seq[(String, JsObject)], versionId: Long)(implicit
    hc: HeaderCarrier
  ): Future[Either[UpdateSectionError, Long]] =
    sections.foldLeft(Future.successful[Either[UpdateSectionError, Long]](Right(versionId))) { case (previous, (sectionId, data)) =>
      previous.flatMap {
        case Right(currentVersionId) =>
          backendConnector.updateDraftSection(draftId, sectionId, data + ("versionId" -> Json.toJson(currentVersionId)))
        case left => Future.successful(left)
      }
    }

  private def nextPage: Call = controllers.routes.LandingPageController.onPageLoad()
}

object ConfirmAdditionalVehicleDetailsController {

  val SupplierJourney = "supplier"
  val ImportJourney   = "import"

  private val OneOrMultipleVehiclesKey = "oneOrMultipleVehicls"
  private val Multiple                 = "Multiple"
  private val One                      = "One"
  private val FormPDateFormat          = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  val ManagedKeys: Set[String] = Set(
    "isAmendment",
    "notificationReference",
    OneOrMultipleVehiclesKey,
    "mileage",
    "mileageUnits",
    "currentRegistrationNumber",
    "dateArrivedInUk",
    "commodityCode",
    "leftOrRightHand",
    "totalValueOfOptions",
    "isSupplierVatRegistered",
    "businessUnableToReclaimVat",
    "vehicleSoldUnderMarginScheme",
    "pricePaidForVehicleEntry",
    "currencyUsed"
  )

  def guardPredicate(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers)

  def supplierFields(answers: UserAnswers, supplierNumber: SupplierNumber, vehicleNumber: VehicleNumber): JsObject = {
    val supplierVatRegistered = answers.get(IsSupplierVatRegisteredPage(supplierNumber))
    val vatRegistered         = supplierVatRegistered.contains(true)

    commonFields(answers, vehicleNumber) ++
      optional(
        "totalValueOfOptions",
        answers.get(TotalValueOfOptionsPage(vehicleNumber)).filter(_ => answers.get(VehicleFromEuPage).contains(true))
      ) ++
      optional("isSupplierVatRegistered", supplierVatRegistered) ++
      optional("businessUnableToReclaimVat", answers.get(CouldNotClaimVatPage(vehicleNumber)).filter(_ => vatRegistered)) ++
      optional("vehicleSoldUnderMarginScheme", answers.get(VatMarginSchemePage(vehicleNumber)).filter(_ => vatRegistered))
  }

  def importFields(answers: UserAnswers, vehicleNumber: VehicleNumber): JsObject =
    commonFields(answers, vehicleNumber) ++
      optional("commodityCode", answers.get(CommodityCodePage(vehicleNumber))) ++
      optional("pricePaidForVehicleEntry", answers.get(TotalAmountPaidPage(vehicleNumber))) ++
      optional("currencyUsed", answers.get(PaymentCurrencyPage(vehicleNumber)))

  private def commonFields(answers: UserAnswers, vehicleNumber: VehicleNumber): JsObject = {
    val isAmendment      = answers.get(AmendSubmittedNotificationPage)
    val mileage          = answers.get(MileagePage(vehicleNumber))
    val numberOfVehicles = answers.get(NumberOfVehiclesPage(vehicleNumber)).map(_.oneMultiVehicle)
    val singleVin        = answers.get(VehicleIdentificationNumberPage(vehicleNumber)).filter(_ => numberOfVehicles.contains(OneMultiVehicle.One))
    val registration     = answers
      .get(VehicleRegistrationNumberPage(vehicleNumber))
      .filter(_ => numberOfVehicles.contains(OneMultiVehicle.One) && answers.get(VehicleRegistrationNumberKnownPage(vehicleNumber)).contains(true))

    optional("isAmendment", isAmendment) ++
      optional("notificationReference", answers.get(NotificationReferencePage(vehicleNumber)).filter(_ => isAmendment.contains(true))) ++
      optional(
        OneOrMultipleVehiclesKey,
        numberOfVehicles.map {
          case OneMultiVehicle.One  => One
          case OneMultiVehicle.More => Multiple
        }
      ) ++
      optional("vehicleIdNumber", singleVin) ++
      optional("confirmVehicleIdNumber", singleVin) ++
      optional("currentRegistrationNumber", registration) ++
      optional("dateArrivedInUk", answers.get(DateOfArrivalPage(vehicleNumber)).map(_.format(FormPDateFormat))) ++
      optional("mileage", mileage.map(_.mileage)) ++
      optional("mileageUnits", mileage.map(_.mileageUnits.jsonValue)) ++
      optional("leftOrRightHand", answers.get(LeftOrRightHandDrivePage(vehicleNumber)).map(_.jsonValue))
  }

  def sectionsToUpdate(
    sections: Map[String, DraftNotificationSection],
    journey: String,
    groupNumber: Int,
    vehicleNumber: VehicleNumber,
    oneMultiVehicle: OneMultiVehicle,
    fields: JsObject
  ): Seq[(String, JsObject)] = {
    val groupSection = raw"$journey/$groupNumber/vehicle/(\d+)/additional-information".r

    val existing: Map[Int, JsObject] = sections.collect { case (groupSection(number), section) =>
      number.toInt -> section.data.getOrElse(Json.obj())
    }

    val targets: Seq[(Int, JsObject)] = oneMultiVehicle match {
      case OneMultiVehicle.More =>
        existing.toSeq.filter { case (_, data) => (data \ OneOrMultipleVehiclesKey).asOpt[String].contains(Multiple) }.sortBy(_._1)
      case OneMultiVehicle.One =>
        Seq(vehicleNumber.value -> existing.getOrElse(vehicleNumber.value, Json.obj()))
    }

    targets.map { case (number, data) =>
      s"$journey/$groupNumber/vehicle/$number/additional-information" ->
        (JsObject(data.fields.filterNot { case (key, _) => ManagedKeys.contains(key) }) ++ fields)
    }
  }

  private def optional[A: Writes](key: String, value: Option[A]): JsObject =
    value.fold(Json.obj())(v => Json.obj(key -> (Json.toJson(v): JsValue)))
}
