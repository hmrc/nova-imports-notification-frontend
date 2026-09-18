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

package controllers.clientselection

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.NovaImportsBackendConnector
import controllers.BaseController
import controllers.actions.*
import forms.ClientSearchFormProvider
import models.{ClientListQuery, ClientSearch}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.ClientListPage
import views.html.ViewClientsView

import scala.concurrent.ExecutionContext

class ViewClientsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  view: ViewClientsView,
  actions: Actions,
  connector: NovaImportsBackendConnector,
  formProvider: ClientSearchFormProvider,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  private val form = formProvider()

  def onPageLoad(searchBy: Option[String], search: Option[String], page: Int): Action[AnyContent] =
    actions.novaAgentAuthAndGetOptionalData().async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val criteria    = ClientSearch.from(searchBy, search)
      val currentPage = math.max(1, page)
      val pageSize    = appConfig.clientListPageSize

      connector.getClientList(ClientListQuery(criteria, (currentPage - 1) * pageSize, pageSize)).map {
        case Right(list) if list.totalCount == 0 && criteria.isEmpty =>
          Redirect(routes.NoAuthorisedClientsController.onPageLoad())
        case Right(list) =>
          val results = ClientListPage(list, currentPage, pageSize, criteria)
          if (results.clients.isEmpty && currentPage > results.totalPages)
            Redirect(routes.ViewClientsController.onPageLoad(searchBy, search, results.totalPages))
          else
            Ok(view(form.withDefault(criteria), Some(results), appConfig.technicalSupportUrl))
        case Left(error) =>
          logger.warn(s"failed to fetch client list: $error")
          Redirect(routes.CouldNotRetrieveClientListController.onPageLoad())
      }
    }

  def onSubmit(): Action[AnyContent] = actions.novaAgentAuthAndGetOptionalData() { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(view(formWithErrors, None, appConfig.technicalSupportUrl)),
        criteria => Redirect(routes.ViewClientsController.onPageLoad(Some(criteria.searchBy.jsonValue), Some(criteria.search), 1))
      )
  }
}
