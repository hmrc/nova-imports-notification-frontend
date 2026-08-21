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
import forms.UsePersonalDetailsAsSupplierFormProvider
import models.requests.DataRequest
import models.{AddVehicleDetails, Mode, NovaUserType, PurchaserOrOnBehalf, TraderInformation}
import navigation.Navigator
import pages.sections.initialquestions.{PurchaserOrOnBehalfPage, VehicleFromEuPage}
import pages.sections.vehicledetails.AddVehicleDetailsPage
import pages.sections.supplierdetails.UsePersonalDetailsAsSupplierPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.checkAnswers.SupplierPersonalDetailsSummary
import views.html.UsePersonalDetailsAsSupplierView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class UsePersonalDetailsAsSupplierController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: UsePersonalDetailsAsSupplierFormProvider,
  view: UsePersonalDetailsAsSupplierView,
  connector: NovaImportsBackendConnector,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import UsePersonalDetailsAsSupplierController.*

  val form: Form[Boolean] = formProvider()

  // The user can reach this page before completing "Add your details"/"Add your address". Any missing
  // personal details render as "Not provided"; the user can continue or choose "No" to add them later.
  private val authenticate = actions.authAndGetDataWithUserTypeGuard(guardPredicate)

  def onPageLoad(mode: Mode): Action[AnyContent] = authenticate.async { implicit request =>
    personalDetails.map { details =>
      Ok(
        view(
          form.withDefault(request.userAnswers.get(UsePersonalDetailsAsSupplierPage)),
          mode,
          details,
          appConfig.vatNotice728Url
        )
      )
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = authenticate.async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          personalDetails.map { details =>
            BadRequest(
              view(
                formWithErrors,
                mode,
                details,
                appConfig.vatNotice728Url
              )
            )
          },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UsePersonalDetailsAsSupplierPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            navigator.nextPage(UsePersonalDetailsAsSupplierPage, mode, updatedAnswers, NovaUserType.from(request.affinityGroup, request.enrolments))
          )
      )
  }

  private def personalDetails(implicit request: DataRequest[?], messages: Messages): Future[SummaryList] =
    if (request.userContext.usesTraderDetails)
      traderInformation(HeaderCarrierConverter.fromRequestAndSession(request, request.session))
        .map(SupplierPersonalDetailsSummary.fromTraderInformation)
    else
      Future.successful(SupplierPersonalDetailsSummary.fromSession(request.userAnswers))

  // a failed lookup must not escape and turn into ERR2.0, the user still gets the page with "Not provided"
  private def traderInformation(implicit hc: HeaderCarrier): Future[Option[TraderInformation]] =
    connector
      .getTraderInformation()
      .map {
        case Right(traderInformation) => Some(traderInformation)
        case Left(error)              =>
          logger.warn(s"Failed to fetch trader information for the supplier details: $error")
          None
      }
      .recover { case NonFatal(e) =>
        logger.warn("Failed to fetch trader information for the supplier details", e)
        None
      }
}

object UsePersonalDetailsAsSupplierController {

  // Access is limited to user types 1, 2, 4 & 5: agents (3 & 6) blocked here, OGD agents (7 & 8)
  // rejected upstream; types 1 & 2 also need IQ3 = As the purchaser, else they belong on AVD-S1.1.
  def guardPredicate(request: DataRequest[?]): Boolean = {
    val answers = request.userAnswers
    !request.userContext.isAgent &&
    IsDraftIdDefined(answers) &&
    answers.get(AddVehicleDetailsPage).contains(AddVehicleDetails.BySupplier) &&
    answers.get(VehicleFromEuPage).contains(true) &&
    (request.userContext.isVatRegisteredOrganisation ||
      answers.get(PurchaserOrOnBehalfPage).contains(PurchaserOrOnBehalf.Purchaser))
  }
}
