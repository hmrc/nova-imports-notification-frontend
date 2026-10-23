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

import javax.inject.Inject
import connectors.NovaImportsBackendConnector
import controllers.BaseController
import controllers.actions.*
import controllers.routes
import controllers.utils.IsDraftIdDefined
import models.draftsections.ImportDetails
import models.requests.DataRequest
import models.{ImportNumber, UserAnswers}
import pages.*
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{ImportEntryDatePage, ImportEntryNumberPage}
import play.api.libs.json.{JsObject, Json}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.ImportService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.ConfirmImportEntryDetailsView

import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

class ConfirmImportEntryDetailsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  backendConnector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  importService: ImportService,
  view: ConfirmImportEntryDetailsView
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import ConfirmImportEntryDetailsController.*

  def onPageLoad(importNumber: ImportNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(importService, importNumber)) { implicit request =>
      Ok(view(request.userAnswers, importNumber))
    }

  def onSubmit(importNumber: ImportNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(importService, importNumber)).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val sectionId = s"import/${importNumber.value}/details"

      val submissionData = for {
        draftId     <- request.userAnswers.get(DraftIdPage)
        versionId   <- request.userAnswers.get(DraftVersionIdPage)
        sectionData <- buildSectionData(request.userAnswers, importNumber)
      } yield (draftId, versionId, sectionData)

      submissionData match {
        case None =>
          logger.warn(s"Failed to submit '$sectionId' — draftId, versionId or section data missing")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

        case Some((draftId, versionId, sectionData)) =>
          val sectionJsonBody = sectionData + ("versionId" -> Json.toJson(versionId))
          backendConnector.updateDraftSection(draftId, sectionId, sectionJsonBody).flatMap {
            case Right(newVersionId) =>
              sessionRepository
                .setPage(request.userAnswers, DraftVersionIdPage, newVersionId)
                .map(_ => Redirect(nextPage(importNumber)))
            case Left(error) =>
              logger.warn(s"Failed to update '$sectionId' section for draftId ${draftId.value}: $error")
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          }
      }
    }
}

object ConfirmImportEntryDetailsController {

  private val formPDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  def nextPage(importNumber: ImportNumber): play.api.mvc.Call =
    routes.LandingPageController.onPageLoad() // TODO: navigate to AVD-IE3.0 when built

  def guardPredicate(importService: ImportService, importNumber: ImportNumber)(request: DataRequest[?]): Boolean =
    IsDraftIdDefined(request.userAnswers) &&
      request.userAnswers.get(VehicleFromEuPage).contains(false) &&
      (request.userContext.isVatRegisteredOrganisation || request.userContext.isAgent) &&
      importService.numberHasValues(request.userAnswers, importNumber) &&
      request.userAnswers.get(ImportEntryNumberPage(importNumber)).isDefined &&
      request.userAnswers.get(ImportEntryDatePage(importNumber)).isDefined

  def buildSectionData(answers: UserAnswers, importNumber: ImportNumber): Option[JsObject] =
    for {
      importEntryNumber <- answers.get(ImportEntryNumberPage(importNumber))
      importEntryDate   <- answers.get(ImportEntryDatePage(importNumber))
    } yield Json.toJson(ImportDetails(importEntryNumber, formPDateFormat.format(importEntryDate))).as[JsObject]
}
