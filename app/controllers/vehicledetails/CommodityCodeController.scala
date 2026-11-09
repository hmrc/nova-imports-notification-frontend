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
import controllers.BaseController
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import forms.CommodityCodeFormProvider
import models.requests.DataRequest
import models.{ImportNumber, Mode, NovaUserType, VehicleNumber}
import navigation.Navigator
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.CommodityCodePage
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{ImportService, VehicleService}
import viewmodels.VehicleDetailsInset
import views.html.CommodityCodeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CommodityCodeController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  actions: Actions,
  formProvider: CommodityCodeFormProvider,
  importService: ImportService,
  vehicleService: VehicleService,
  appConfig: FrontendAppConfig,
  view: CommodityCodeView
)(implicit ec: ExecutionContext)
    extends BaseController {

  import CommodityCodeController.*

  val form: Form[String] = formProvider()

  def onPageLoad(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(importService, vehicleService, importNumber, vehicleNumber)) { implicit request =>
      Ok(
        view(
          form.withDefault(request.userAnswers.get(CommodityCodePage(vehicleNumber))),
          VehicleDetailsInset(request.userAnswers, vehicleNumber),
          routes.CommodityCodeController.onSubmit(importNumber, vehicleNumber, mode),
          appConfig.findingCommodityCodesUrl
        )
      )
    }

  def onSubmit(importNumber: ImportNumber, vehicleNumber: VehicleNumber, mode: Mode): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(importService, vehicleService, importNumber, vehicleNumber)).async { implicit request =>
      val page = CommodityCodePage(vehicleNumber)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  VehicleDetailsInset(request.userAnswers, vehicleNumber),
                  routes.CommodityCodeController.onSubmit(importNumber, vehicleNumber, mode),
                  appConfig.findingCommodityCodesUrl
                )
              )
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(page, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(page, mode, updatedAnswers, NovaUserType.fromRequest))
        )
    }
}

object CommodityCodeController {

  def guardPredicate(
    importService: ImportService,
    vehicleService: VehicleService,
    importNumber: ImportNumber,
    vehicleNumber: VehicleNumber
  )(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(false) &&
      (request.userContext.isVatRegisteredOrganisation || request.userContext.isAgent) &&
      importService.numberHasValues(request.userAnswers, importNumber) &&
      vehicleService.belongsToImport(request.userAnswers, vehicleNumber, importNumber) &&
      vehicleService.numberHasValues(request.userAnswers, vehicleNumber)
}
