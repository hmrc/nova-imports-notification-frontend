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
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import controllers.{BaseController, routes}
import forms.SupplierVatRegistrationDetailsFormProvider
import models.requests.DataRequest
import models.{Country, Mode, NovaUserType, SupplierNumber, UserAnswers, VatNumberDetails}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplieraddress.SupplierAddressJourneyIdPage
import pages.sections.supplierdetails.{IsSupplierVatRegisteredPage, SupplierEuMemberStatesPage, SupplierVatRegistrationNumberPage}
import play.api.Logging
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.SupplierService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.SupplierVatRegistrationDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SupplierVatRegistrationDetailsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  appConfig: FrontendAppConfig,
  formProvider: SupplierVatRegistrationDetailsFormProvider,
  backendConnector: NovaImportsBackendConnector,
  view: SupplierVatRegistrationDetailsView,
  supplierService: SupplierService
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import SupplierVatRegistrationDetailsController.*

  private def form(euCountries: Seq[Country]): Form[VatNumberDetails] = formProvider(euCountries, appConfig.vrnValidationList)

  def onPageLoad(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>
      withEuMemberStates(supplierNumber, request.userAnswers) { (euCountries, answers) =>
        Future.successful(
          Ok(
            view(
              euCountries,
              form(euCountries).withDefault(answers.get(SupplierVatRegistrationNumberPage(supplierNumber))),
              supplierNumber,
              mode
            )
          )
        )
      }
    }

  def onSubmit(supplierNumber: SupplierNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>
      withEuMemberStates(supplierNumber, request.userAnswers) { (euCountries, answers) =>
        form(euCountries)
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(euCountries, formWithErrors, supplierNumber, mode))),
            supplierVatNumberDetails =>
              for {
                updatedAnswers <- Future.fromTry(answers.set(SupplierVatRegistrationNumberPage(supplierNumber), supplierVatNumberDetails))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(
                navigator
                  .nextPage(
                    SupplierVatRegistrationNumberPage(supplierNumber),
                    mode,
                    updatedAnswers,
                    NovaUserType.from(request.affinityGroup, request.enrolments)
                  )
              )
          )
      }
    }

  private def withEuMemberStates(supplierNumber: SupplierNumber, userAnswers: UserAnswers)(
    block: (Seq[Country], UserAnswers) => Future[Result]
  )(implicit request: DataRequest[?]): Future[Result] =
    userAnswers.get(SupplierEuMemberStatesPage(supplierNumber)) match {
      case Some(countries) => block(countries.toSeq, userAnswers)
      case None            =>
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
        backendConnector.getEuMemberStates().flatMap {
          case Right(states) =>
            for {
              updatedAnswers <- Future.fromTry(userAnswers.set(SupplierEuMemberStatesPage(supplierNumber), states.countries))
              _              <- sessionRepository.set(updatedAnswers)
              result         <- block(states.countries.toSeq, updatedAnswers)
            } yield result
          case Left(error) =>
            logger.warn(s"Failed to retrieve EU member states for supplier ${supplierNumber.value}: $error")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
    }

}

object SupplierVatRegistrationDetailsController {
  def guardPredicate(supplierService: SupplierService, supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(true) &&
      request.userAnswers.get(IsSupplierVatRegisteredPage(supplierNumber)).contains(true) &&
      supplierService.numberExists(request.userAnswers, supplierNumber) &&
      request.userAnswers.get(SupplierAddressJourneyIdPage(supplierNumber)).isDefined
}
