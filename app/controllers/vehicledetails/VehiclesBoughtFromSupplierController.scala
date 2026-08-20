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
import controllers.utils.{IsDraftIdDefined, IsSupplierNumberInSession}
import controllers.vehicledetails.VehiclesBoughtFromSupplierController.*
import models.requests.DataRequest
import models.{BusinessOrPrivateIndividual, NormalMode, SupplierNumber, VehicleNumber}
import pages.sections.initialquestions.{BusinessOrPrivatePage, VehicleFromEuPage}
import pages.sections.notifierdetails.{BusinessNamePage, NameDetailsPage}
import pages.sections.supplierdetails.{SupplierBusinessNamePage, SupplierBusinessOrIndividualPage, SupplierNamePage, UsePersonalDetailsAsSupplierPage}
import pages.sections.vehicledetails.VehicleNumberPage
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
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
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  def onPageLoad(supplierNumber: SupplierNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierNumber)).async { implicit request =>
      supplierName.map { name =>
        Ok(
          view(name, supplierNumber, appConfig.personalTransportUnitUrl)
        )
      }
    }

  def onSubmit(supplierNumber: SupplierNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierNumber)) { implicit request =>
      Redirect(
        routes.VehicleDatesController
          .onPageLoad(supplierNumber, VehicleNumber(request.userAnswers.get(VehicleNumberPage).getOrElse(1)), NormalMode)
      )
    }

  private def supplierName(implicit request: DataRequest[?]): Future[Option[String]] =
    request.userAnswers.get(UsePersonalDetailsAsSupplierPage) match {
      case Some(true) if request.userContext.usesTraderDetails => traderName
      case Some(true)                                          => Future.successful(notifierName)
      case _                                                   => Future.successful(enteredSupplierName)
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

  private def enteredSupplierName(implicit request: DataRequest[?]): Option[String] =
    request.userAnswers.get(SupplierBusinessOrIndividualPage) match {
      case Some(BusinessOrPrivateIndividual.Business)          => request.userAnswers.get(SupplierBusinessNamePage)
      case Some(BusinessOrPrivateIndividual.PrivateIndividual) => request.userAnswers.get(SupplierNamePage).map(_.displayName)
      case None                                                => None
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

  def guardPredicate(supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      IsSupplierNumberInSession(request.userAnswers, supplierNumber)
}
