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
import controllers.vehicledetails.VehiclesBoughtFromSupplierController.*
import models.requests.DataRequest
import models.{BusinessOrPrivateIndividual, NormalMode, SupplierNumber, VehicleNumber}
import pages.sections.initialquestions.{BusinessOrPrivatePage, VehicleFromEuPage}
import pages.sections.notifierdetails.{BusinessNamePage, NameDetailsPage}
import pages.sections.supplierdetails.{SupplierBusinessNamePage, SupplierBusinessOrIndividualPage, SupplierNamePage, UsePersonalDetailsAsSupplierPage}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SupplierService, VehicleService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.VehiclesBoughtFromSupplierView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class VehiclesBoughtFromSupplierController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  view: VehiclesBoughtFromSupplierView,
  connector: NovaImportsBackendConnector,
  supplierService: SupplierService,
  vehicleService: VehicleService,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  def onPageLoad(supplierNumber: SupplierNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>
      supplierName(supplierNumber).map { name =>
        Ok(
          view(name, supplierNumber, appConfig.personalTransportUnitUrl)
        )
      }
    }

  // sets up a new vehicle collection in session, it carries the supplierNumber it was bought from in the current URL
  def onSubmit(supplierNumber: SupplierNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>
      vehicleService.addForSupplier(request.userAnswers, supplierNumber).map { vehicleNumber =>
        Redirect(routes.VehicleDatesController.onPageLoad(supplierNumber, vehicleNumber, NormalMode))
      }
    }

  private def supplierName(supplierNumber: SupplierNumber)(implicit request: DataRequest[?]): Future[Option[String]] =
    request.userAnswers.get(UsePersonalDetailsAsSupplierPage(supplierNumber)) match {
      case Some(true) if request.userContext.usesTraderDetails => traderName
      case Some(true)                                          => Future.successful(notifierName)
      case _                                                   => Future.successful(enteredSupplierName(supplierNumber))
    }

  private def notifierName(implicit request: DataRequest[?]): Option[String] =
    if (request.userContext.isVatRegisteredOrganisation)
      request.userAnswers.get(NameDetailsPage).map(_.displayName)
    else
      request.userAnswers.get(BusinessOrPrivatePage) match {
        case Some(BusinessOrPrivateIndividual.Business)          => request.userAnswers.get(BusinessNamePage)
        case Some(BusinessOrPrivateIndividual.PrivateIndividual) => request.userAnswers.get(NameDetailsPage).map(_.displayName)
        case None                                                => None
      }

  private def enteredSupplierName(supplierNumber: SupplierNumber)(implicit request: DataRequest[?]): Option[String] =
    request.userAnswers.get(SupplierBusinessOrIndividualPage(supplierNumber)) match {
      case Some(BusinessOrPrivateIndividual.Business)          => request.userAnswers.get(SupplierBusinessNamePage(supplierNumber))
      case Some(BusinessOrPrivateIndividual.PrivateIndividual) =>
        request.userAnswers.get(SupplierNamePage(supplierNumber)).map(_.displayName)
      case None => None
    }

  private def traderName(implicit request: DataRequest[?]): Future[Option[String]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    connector
      .getTraderInformation()
      .map {
        case Right(traderInformation) => traderInformation.name
        case Left(error)              =>
          logger.warn(s"Failed to fetch trader information for the vehicles bought from supplier heading: $error")
          None
      }
      .recover { case NonFatal(e) =>
        logger.warn("Failed to fetch trader information for the vehicles bought from supplier heading", e)
        None
      }
  }
}

object VehiclesBoughtFromSupplierController {

  // The supplier number in the URL must be one of the suppliers the user has in session
  def guardPredicate(supplierService: SupplierService, supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      supplierService.numberExists(request.userAnswers, supplierNumber)
}
