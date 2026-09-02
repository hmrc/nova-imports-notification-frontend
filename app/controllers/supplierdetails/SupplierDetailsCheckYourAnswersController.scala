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

import connectors.NovaImportsBackendConnector
import controllers.BaseController
import controllers.actions.*
import controllers.utils.IsDraftIdDefined
import models.draftsections.{SupplierDetails, SupplierSelfSupplyDetails}
import models.requests.DataRequest
import models.{Address, BusinessOrPrivateIndividual, NameDetails, NormalMode, SupplierNumber, UserAnswers, UserContext, VatNumberDetails}
import pages.*
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.supplieraddress.{SupplierAddressJourneyIdPage, SupplierAddressPage}
import pages.sections.supplierdetails.*
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.SupplierService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.SupplierDetailsCheckYourAnswersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SupplierDetailsCheckYourAnswersController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  actions: Actions,
  backendConnector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository,
  view: SupplierDetailsCheckYourAnswersView,
  supplierService: SupplierService
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  import SupplierDetailsCheckYourAnswersController.*

  def onPageLoad(supplierNumber: SupplierNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)) { implicit request =>
      Ok(view(request.userContext, request.userAnswers, supplierNumber))
    }

  def onChangeAddress(supplierNumber: SupplierNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>
      for {
        cleared <- Future.fromTry(
                     request.userAnswers.remove(SupplierAddressPage(supplierNumber)).flatMap(_.remove(SupplierAddressJourneyIdPage(supplierNumber)))
                   )
        _ <- sessionRepository.set(cleared)
      } yield Redirect(controllers.supplieraddress.routes.IsSupplierAddressInTheUKController.onPageLoad(supplierNumber, NormalMode))
    }

  def onSubmit(supplierNumber: SupplierNumber): Action[AnyContent] =
    actions.authAndGetDataWithUserTypeGuard(guardPredicate(supplierService, supplierNumber)).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val submissionIDs = for {
        draftId   <- request.userAnswers.get(DraftIdPage)
        versionId <- request.userAnswers.get(DraftVersionIdPage)
      } yield (draftId, versionId)

      def failureRecovery = {
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }

      def navigateToNextPage(newVersionId: Long) = {
        sessionRepository
          .setPage(request.userAnswers, DraftVersionIdPage, newVersionId)
          .map(_ => Redirect(nextPage(supplierNumber)))
      }

      submissionIDs match {
        case None =>
          logger.warn(
            s"Failed to submit 'supplier/${supplierNumber.value.toString}/self-supply' of type SupplierSelfSupply — draftId, versionId or section data missing"
          )
          failureRecovery

        case Some((draftId, versionId)) =>
          // Save SupplierSelfSupply
          val selfSupply                  = isSelfSupply(request.userAnswers, supplierNumber)
          val selfSupplySectionData       = buildSelfSupplySectionData(selfSupply)
          val selfSupplierSectionJsonBody = selfSupplySectionData + ("versionId" -> Json.toJson(versionId))
          backendConnector
            .updateDraftSection(draftId, s"supplier/${supplierNumber.value.toString}/self-supply", selfSupplierSectionJsonBody)
            .flatMap {
              case Right(selfSupplierNewVersionId) =>

                if (selfSupply) {
                  navigateToNextPage(selfSupplierNewVersionId)
                } else {
                  // Save SupplierDetails if the self supply is false
                  buildSupplierDetailsSectionData(request.userContext, request.userAnswers, supplierNumber) match {
                    case Some(supplierDetailsSectionData) =>
                      val supplierDetailsSectionJsonBody = selfSupplySectionData + ("versionId" -> Json.toJson(selfSupplierNewVersionId))
                      backendConnector
                        .updateDraftSection(draftId, s"supplier/${supplierNumber.value.toString}/details", supplierDetailsSectionJsonBody)
                        .flatMap {
                          case Right(supplierDetailsNewVersionId) =>
                            navigateToNextPage(supplierDetailsNewVersionId)
                          case Left(error) =>
                            logger.warn(
                              s"Failed to update 'supplier/${supplierNumber.value.toString}/details' of type SupplierDetails for draftId ${draftId.value}: $error"
                            )
                            failureRecovery
                        }
                    case None =>
                      failureRecovery
                  }
                }

              case Left(error) =>
                logger.warn(
                  s"Failed to update 'supplier/${supplierNumber.value.toString}/self-supply' section of type SupplierSelfSupply for draftId ${draftId.value}: $error"
                )
                failureRecovery
            }
      }
    }

}

object SupplierDetailsCheckYourAnswersController {

  def nextPage(supplierNumber: SupplierNumber): play.api.mvc.Call =
    controllers.routes.JourneyRecoveryController.onPageLoad() // TODO : replace with AVD2.0 once it is built

  def guardPredicate(supplierService: SupplierService, supplierNumber: SupplierNumber)(request: DataRequest[?]): Boolean = {
    val answers     = request.userAnswers
    val userContext = request.userContext

    val avdQuestionAnswered =
      answers.get(UsePersonalDetailsAsSupplierPage(supplierNumber)).isDefined
        || answers.get(UsePurchaserDetailsAsSupplierPage(supplierNumber)).isDefined
//      || answers.get(UseClientDetailsAsSupplierPage(supplierNumber)).isDefined   TODO: include this line once AVD-S1.2 page is added

    IsDraftIdDefined(answers)
    && avdQuestionAnswered
    && request.userAnswers.get(VehicleFromEuPage).contains(true)
    && supplierService.numberExists(request.userAnswers, supplierNumber)
  }

  private def isSelfSupply(answers: UserAnswers, supplierNumber: SupplierNumber): Boolean = {
    def isTrue(page: QuestionPage[Boolean]): Boolean = answers.get(page).contains(true)

    isTrue(UsePersonalDetailsAsSupplierPage(supplierNumber))
    || isTrue(UsePurchaserDetailsAsSupplierPage(supplierNumber))
  }

  private def buildSelfSupplySectionData(selfSupply: Boolean): JsObject = {
    Json
      .toJson(
        SupplierSelfSupplyDetails(
          selfSupply
        )
      )
      .as[JsObject]
  }

  private def buildSupplierDetailsSectionData(userContext: UserContext, answers: UserAnswers, supplierNumber: SupplierNumber): Option[JsObject] = {
    for {
      supplierBusinessOrIndividual  <- answers.get(SupplierBusinessOrIndividualPage(supplierNumber))
      supplierBusinessName          <- answers.get(SupplierBusinessNamePage(supplierNumber))
      supplierName                  <- answers.get(SupplierNamePage(supplierNumber))
      supplierAddress               <- answers.get(SupplierAddressPage(supplierNumber))
      isSupplierVatRegistered       <- answers.get(IsSupplierVatRegisteredPage(supplierNumber))
      supplierVatRegistrationNumber <- answers.get(SupplierVatRegistrationNumberPage(supplierNumber))
    } yield {

      def buildSectionData(businessName: Option[String], name: Option[NameDetails], vatRegDetails: Option[VatNumberDetails]) = {
        Json
          .toJson(
            SupplierDetails(
              Some(supplierBusinessOrIndividual),
              businessName,
              name.map(_.title),
              name.map(_.firstName),
              name.map(_.lastName),
              supplierAddress.lines.lift(0),
              supplierAddress.lines.lift(1),
              supplierAddress.lines.lift(2),
              supplierAddress.lines.lift(3),
              supplierAddress.lines.lift(4),
              supplierAddress.postcode,
              Some(supplierAddress.country),
              Some(isSupplierVatRegistered),
              vatRegDetails.map(_.countryCode),
              vatRegDetails.map(_.vatNumber)
            )
          )
          .as[JsObject]
      }

      answers.get(SupplierBusinessOrIndividualPage(supplierNumber)) match {
        case Some(BusinessOrPrivateIndividual.Business) =>
          answers.get(IsSupplierVatRegisteredPage(supplierNumber)) match {
            case Some(vatRegistered: true) =>
              buildSectionData(Some(supplierBusinessName), None, Some(supplierVatRegistrationNumber))
            case Some(false) =>
              buildSectionData(Some(supplierBusinessName), None, None)
          }
        case Some(BusinessOrPrivateIndividual.PrivateIndividual) =>
          answers.get(IsSupplierVatRegisteredPage(supplierNumber)) match {
            case Some(vatRegistered: true) =>
              buildSectionData(None, Some(supplierName), Some(supplierVatRegistrationNumber))
            case Some(false) =>
              buildSectionData(None, Some(supplierName), None)
          }
      }
    }
  }

}
