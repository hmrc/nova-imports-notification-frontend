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

package controllers.supplierdetails

import config.FrontendAppConfig
import connectors.NovaImportsBackendConnector
import controllers.BaseController
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import forms.UseClientDetailsAsSupplierFormProvider
import models.requests.DataRequest
import models.{Mode, NovaUserType, SupplierNumber, TraderInformation}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplierdetails.UseClientDetailsAsSupplierPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.SupplierService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.checkAnswers.SupplierClientDetailsSummary
import views.html.UseClientDetailsAsSupplierView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class UseClientDetailsAsSupplierController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: UseClientDetailsAsSupplierFormProvider,
  supplierService: SupplierService,
  view: UseClientDetailsAsSupplierView,
  connector: NovaImportsBackendConnector,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import UseClientDetailsAsSupplierController.*

  val form: Form[Boolean] = formProvider()

  private def authenticate(supplierNumber: SupplierNumber) =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber))

  def onPageLoad(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] = authenticate(supplierNumber).async { implicit request =>
    clientDetails.map { details =>
      Ok(
        view(
          form.withDefault(request.userAnswers.get(UseClientDetailsAsSupplierPage(supplierNumber))),
          supplierNumber,
          mode,
          details,
          appConfig.vatNotice728Url
        )
      )
    }
  }

  def onSubmit(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] = authenticate(supplierNumber).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => clientDetails.map(details => BadRequest(view(formWithErrors, supplierNumber, mode, details, appConfig.vatNotice728Url))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UseClientDetailsAsSupplierPage(supplierNumber), value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(
              UseClientDetailsAsSupplierPage(supplierNumber),
              mode,
              updatedAnswers,
              NovaUserType.from(request.affinityGroup, request.enrolments)
            )
          )
      )
  }

  private def clientDetails(implicit request: DataRequest[?], messages: Messages): Future[SummaryList] = {
    val client = request.userContext.selectedClient

    client.map(_.vrn) match {
      case Some(vrn) =>
        clientTraderInformation(vrn)(HeaderCarrierConverter.fromRequestAndSession(request, request.session))
          .map(details => SupplierClientDetailsSummary(details.flatMap(_.name).orElse(client.flatMap(_.name)), details))
      case None => Future.successful(SupplierClientDetailsSummary(client.flatMap(_.name), None))
    }
  }

  private def clientTraderInformation(clientVrn: String)(implicit hc: HeaderCarrier): Future[Option[TraderInformation]] =
    connector
      .getClientTraderInformation(clientVrn)
      .map {
        case Right(traderInformation) => Some(traderInformation)
        case Left(error)              =>
          logger.warn(s"Failed to fetch client trader information for the supplier details: $error")
          None
      }
      .recover { case NonFatal(e) =>
        logger.warn("Failed to fetch client trader information for the supplier details", e)
        None
      }
}

object UseClientDetailsAsSupplierController {

  def guardPredicate(supplierService: SupplierService, supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean = {
    val answers = request.userAnswers

    request.userContext.isAgentWithClient &&
    IsDraftIdDefined(answers) &&
    answers.get(VehicleFromEuPage).contains(true) &&
    supplierService.numberExists(answers, supplierNumber)
  }
}
