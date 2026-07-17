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

package services

import com.google.inject.{ImplementedBy, Inject, Singleton}
import connectors.{GetDraftNotificationError, NovaImportsBackendConnector}
import models.*
import models.DraftNotification.SectionId
import models.draftsections.*
import pages.sections.introduction.*
import pages.sections.initialquestions.*
import pages.sections.notifieraddress.*
import play.api.libs.json.*
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import services.UserDataService.*
import pages.{AgentClientVehicleBusinessUsePage, AgentSelectedClientPage}
import pages.sections.notifierDetails.{EmailAddressPage, NameDetailsPage, PhoneNumberPage}
import pages.sections.purchaserDetails.{PurchaserBusinessNamePage, PurchaserNamePage}
import pages.sections.purchaseraddress.IsPurchaserAddressInTheUkPage

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[UserDataServiceImpl])
trait UserDataService {

  def retrieveAndStoreDraftNotification(draftId: DraftId, userAnswers: UserAnswers)(implicit
    hc: HeaderCarrier
  ): Future[Either[GetDraftNotificationError, UserAnswers]]

  def determineAndUpdateStatus(userAnswers: UserAnswers, userContext: UserContext): Map[String, SectionStatus]
}

@Singleton
class UserDataServiceImpl @Inject() (
  repository: SessionRepository,
  backendConnector: NovaImportsBackendConnector
)(implicit ec: ExecutionContext)
    extends UserDataService {

  def retrieveAndStoreDraftNotification(draftId: DraftId, userAnswers: UserAnswers)(implicit
    hc: HeaderCarrier
  ): Future[Either[GetDraftNotificationError, UserAnswers]] =
    backendConnector.getDraftNotification(draftId).flatMap {
      case Left(e) =>
        Future.successful(Left(e))

      case Right(draft) =>
        for {
          u0 <- storeIntroductionPages(draft, userAnswers, repository)
          u1 <- storeInitialQuestionsPages(draft, u0, repository)
          u2 <- storeNotifierDetailsPages(draft, u1, repository)
          u3 <- storeNotifierAddressPages(draft, u2, repository)
          u4 <- storePurchaserDetailsPages(draft, u3, repository)
        } yield Right(u4)
    }

  def determineAndUpdateStatus(userAnswers: UserAnswers, userContext: UserContext): Map[String, SectionStatus] =
    userContext.userType match {
      case NovaUserType.VatRegisteredOrganisation                                   => orgWithEnrolments(userAnswers)
      case NovaUserType.Agent if userAnswers.get(AgentSelectedClientPage).isDefined => agentWithSelectedClient(userAnswers)
      case NovaUserType.Agent                                                       => agentWithoutClient(userAnswers)
      case _                                                                        => privateIndividual(userAnswers)
    }
}

object UserDataService {

  def storeIntroductionPages(draft: DraftNotification, answers: UserAnswers, sessionRepository: SessionRepository)(implicit
    ec: ExecutionContext
  ): Future[UserAnswers] =
    draft.sections
      .get(SectionId.Introduction)
      .flatMap(_.data)
      .flatMap(_.asOpt[Introduction]) match {
      case None        => Future.successful(answers)
      case Some(intro) =>
        for {
          a1 <- sessionRepository.setPage(answers, IntroductionAcknowledgePage, intro.acknowledged)
          a2 <- sessionRepository.setPage(a1, AmendSubmittedNotificationPage, intro.amendSubmittedNotification)
        } yield a2
    }

  def storeInitialQuestionsPages(draft: DraftNotification, answers: UserAnswers, sessionRepository: SessionRepository)(implicit
    ec: ExecutionContext
  ): Future[UserAnswers] =
    draft.sections
      .get(SectionId.InitialQuestions)
      .flatMap(_.data)
      .flatMap(_.asOpt[InitialQuestions]) match {
      case None     => Future.successful(answers)
      case Some(iq) =>
        for {
          a1 <- sessionRepository.setPage(answers, VehicleFromEuPage, iq.vehicleFromEuToNi)
          a2 <- iq.isForBusinessUse.fold(Future.successful(a1))(v => sessionRepository.setPage(a1, VehicleBusinessUsePage, v))
          a3 <- iq.areYouBusinessOrPrivate.fold(Future.successful(a2))(v => sessionRepository.setPage(a2, BusinessOrPrivatePage, v))
          a4 <- iq.notifyingAsPurchaserOrOnBehalf.fold(Future.successful(a3))(v => sessionRepository.setPage(a3, PurchaserOrOnBehalfPage, v))
          a5 <- iq.isPurchaserBusinessOrPrivateIndividual.fold(Future.successful(a4))(v =>
                  sessionRepository.setPage(a4, PurchaserBusinessOrIndividualPage, v)
                )
          a6 <- iq.agentClientVehicleBusinessUse.fold(Future.successful(a5))(v => sessionRepository.setPage(a5, AgentClientVehicleBusinessUsePage, v))
        } yield a6
    }

  def storeNotifierDetailsPages(draft: DraftNotification, answers: UserAnswers, sessionRepository: SessionRepository)(implicit
    ec: ExecutionContext
  ): Future[UserAnswers] =
    draft.sections.get(SectionId.NotifierDetails).flatMap(_.data) match {
      case None       => Future.successful(answers)
      case Some(data) =>
        data.asOpt[NotifierDetailsIndividual] match {
          case Some(nd) =>
            for {
              a1 <- sessionRepository.setPage(answers, NameDetailsPage, NameDetails(nd.title, nd.firstName, nd.lastName))
              a2 <- sessionRepository.setPage(a1, EmailAddressPage, nd.emailAddress)
              a3 <- sessionRepository.setPage(a2, PhoneNumberPage, ContactNumbers(nd.phoneNumber, nd.mobileNumber))
            } yield a3
          case None =>
            data.asOpt[NotifierDetailsOrganisation] match {
              case Some(nd) =>
                for {
                  a1 <- sessionRepository.setPage(answers, EmailAddressPage, nd.emailAddress)
                  a2 <- sessionRepository.setPage(a1, PhoneNumberPage, ContactNumbers(nd.phoneNumber, nd.mobileNumber))
                } yield a2
              case None =>
                Future.successful(answers)
            }
        }
    }

  def storeNotifierAddressPages(draft: DraftNotification, answers: UserAnswers, sessionRepository: SessionRepository)(implicit
    ec: ExecutionContext
  ): Future[UserAnswers] =
    draft.sections.get(SectionId.NotifierAddress).flatMap(_.data).flatMap(_.asOpt[NotifierAddress]) match {
      case Some(a) =>
        val address = Address(
          lines = Seq(Option(a.line1), Option(a.line2), a.line3, a.line4).flatten,
          postcode = a.postCode,
          country = a.country
        )
        sessionRepository.setPage(answers, AddressPage, address)
      case None => Future.successful(answers)
    }

  def storePurchaserDetailsPages(draft: DraftNotification, answers: UserAnswers, sessionRepository: SessionRepository)(implicit
    ec: ExecutionContext
  ): Future[UserAnswers] =
    draft.sections.get(SectionId.PurchaserDetails).flatMap(_.data) match {
      case None       => Future.successful(answers)
      case Some(data) =>
        data.asOpt[PurchaserDetailsIndividual] match {
          case Some(pd) =>
            sessionRepository.setPage(answers, PurchaserNamePage, NameDetails(pd.title, pd.firstName, pd.lastName))
          case None =>
            data.asOpt[PurchaserDetailsBusiness] match {
              case Some(pd) =>
                sessionRepository.setPage(answers, PurchaserBusinessNamePage, pd.businessName)
              case None =>
                Future.successful(answers)
            }
        }
    }

  def orgWithEnrolments(answers: UserAnswers): Map[String, SectionStatus] = {
    /* Introduction */
    val acknowledged               = answers.get(IntroductionAcknowledgePage)
    val amendSubmittedNotification = answers.get(AmendSubmittedNotificationPage)

    /* Initial Questions */
    val vehicleFromEu      = answers.get(VehicleFromEuPage)
    val vehicleBusinessUse = answers.get(VehicleBusinessUsePage)

    /* Notifier Details */
    val nameDetails         = answers.get(NameDetailsPage)
    val phoneNumber         = answers.get(PhoneNumberPage)
    val nameDetailsRequired = if (vehicleBusinessUse.contains(false)) nameDetails.isDefined else true

    /* Notifier Address */
    val address = answers.get(AddressPage)

    val introStatus = (acknowledged, amendSubmittedNotification.isDefined) match {
      case (Some(true), true) => SectionStatus.Completed
      case (None, false)      => SectionStatus.NotYetSaved
      case _                  => SectionStatus.Incomplete
    }

    val initialQsStatus = (vehicleFromEu.isDefined, vehicleBusinessUse.isDefined) match {
      case (true, true)   => SectionStatus.Completed
      case (false, false) => SectionStatus.NotYetSaved
      case _              => SectionStatus.Incomplete
    }

    val notifierDetailsStatus = (nameDetailsRequired, phoneNumber.isDefined) match {
      case (true, true)   => SectionStatus.Completed
      case (false, false) => SectionStatus.NotYetSaved
      case _              => SectionStatus.Incomplete
    }

    val notifierAddressStatus =
      if address.isDefined then SectionStatus.Completed
      else SectionStatus.Incomplete

    Map(
      SectionId.Introduction     -> introStatus,
      SectionId.InitialQuestions -> initialQsStatus,
      SectionId.NotifierDetails  -> notifierDetailsStatus,
      SectionId.NotifierAddress  -> notifierAddressStatus,
      SectionId.Vehicles         -> SectionStatus.NotYetSaved,
      SectionId.Declaration      -> SectionStatus.NotYetSaved
    )
  }

  def agentWithSelectedClient(answers: UserAnswers): Map[String, SectionStatus] = {
    /* Introduction */
    val acknowledged               = answers.get(IntroductionAcknowledgePage)
    val amendSubmittedNotification = answers.get(AmendSubmittedNotificationPage)

    /* Initial Questions */
    val vehicleFromEu                 = answers.get(VehicleFromEuPage)
    val agentClientVehicleBusinessUse = answers.get(AgentClientVehicleBusinessUsePage)

    val introStatus = (acknowledged, amendSubmittedNotification.isDefined) match {
      case (Some(true), true) => SectionStatus.Completed
      case (None, false)      => SectionStatus.NotYetSaved
      case _                  => SectionStatus.Incomplete
    }

    val initialQsStatus = (vehicleFromEu.isDefined, agentClientVehicleBusinessUse.isDefined) match {
      case (true, true)   => SectionStatus.Completed
      case (false, false) => SectionStatus.NotYetSaved
      case _              => SectionStatus.Incomplete
    }

    Map(
      SectionId.Introduction     -> introStatus,
      SectionId.InitialQuestions -> initialQsStatus,
      SectionId.NotifierDetails  -> SectionStatus.NotYetSaved,
      SectionId.NotifierAddress  -> SectionStatus.NotYetSaved,
      SectionId.Vehicles         -> SectionStatus.NotYetSaved,
      SectionId.Declaration      -> SectionStatus.NotYetSaved
    )
  }

  def privateIndividual(answers: UserAnswers): Map[String, SectionStatus] = {
    /* Notifier Details */
    val nameDetails             = answers.get(NameDetailsPage)
    val phoneNumber             = answers.get(PhoneNumberPage)
    val emailAddress            = answers.get(EmailAddressPage)
    val expectsName             = answers.get(BusinessOrPrivatePage).contains(BusinessOrPrivateIndividual.PrivateIndividual)
    val notifierDetailsAllUnset = nameDetails.isEmpty && phoneNumber.isEmpty && emailAddress.isEmpty

    val notifierDetailsStatus =
      if phoneNumber.isDefined && emailAddress.isDefined && (nameDetails.isDefined == expectsName) then SectionStatus.Completed
      else if notifierDetailsAllUnset then SectionStatus.NotYetSaved
      else SectionStatus.Incomplete

    val notifierAddressStatus =
      if answers.get(AddressPage).isDefined then SectionStatus.Completed
      else SectionStatus.Incomplete

    Map(
      SectionId.Introduction     -> introStatus(answers),
      SectionId.InitialQuestions -> standardInitialQsStatus(answers),
      SectionId.NotifierDetails  -> notifierDetailsStatus,
      SectionId.NotifierAddress  -> notifierAddressStatus,
      SectionId.PurchaserDetails -> purchaserDetailsStatus(answers),
      SectionId.PurchaserAddress -> purchaserAddressStatus(answers),
      SectionId.Vehicles         -> SectionStatus.NotYetSaved,
      SectionId.Declaration      -> SectionStatus.NotYetSaved
    )
  }

  def agentWithoutClient(answers: UserAnswers): Map[String, SectionStatus] = {
    val phoneNumber  = answers.get(PhoneNumberPage)
    val emailAddress = answers.get(EmailAddressPage)

    val notifierDetailsStatus = (phoneNumber.isDefined, emailAddress.isDefined) match {
      case (true, true)   => SectionStatus.Completed
      case (false, false) => SectionStatus.NotYetSaved
      case _              => SectionStatus.Incomplete
    }

    Map(
      SectionId.Introduction     -> introStatus(answers),
      SectionId.InitialQuestions -> standardInitialQsStatus(answers),
      SectionId.NotifierDetails  -> notifierDetailsStatus,
      SectionId.NotifierAddress  -> SectionStatus.NotYetSaved,
      SectionId.PurchaserDetails -> purchaserDetailsStatus(answers),
      SectionId.PurchaserAddress -> purchaserAddressStatus(answers),
      SectionId.Vehicles         -> SectionStatus.NotYetSaved,
      SectionId.Declaration      -> SectionStatus.NotYetSaved
    )
  }

  private def introStatus(answers: UserAnswers): SectionStatus =
    (answers.get(IntroductionAcknowledgePage), answers.get(AmendSubmittedNotificationPage).isDefined) match {
      case (Some(true), true) => SectionStatus.Completed
      case (None, false)      => SectionStatus.NotYetSaved
      case _                  => SectionStatus.Incomplete
    }

  private def standardInitialQsStatus(answers: UserAnswers): SectionStatus =
    (
      answers.get(VehicleFromEuPage),
      answers.get(BusinessOrPrivatePage),
      answers.get(PurchaserOrOnBehalfPage),
      answers.get(PurchaserBusinessOrIndividualPage).isDefined
    ) match {
      case (Some(true), Some(_), Some(PurchaserOrOnBehalf.Purchaser), _)              => SectionStatus.Completed
      case (Some(true), Some(_), Some(PurchaserOrOnBehalf.OnBehalfOfPurchaser), true) => SectionStatus.Completed
      case (None, None, None, false)                                                  => SectionStatus.NotYetSaved
      case _                                                                          => SectionStatus.Incomplete
    }

  private def purchaserDetailsStatus(answers: UserAnswers): SectionStatus =
    if answers.get(PurchaserNamePage).isDefined || answers.get(PurchaserBusinessNamePage).isDefined then SectionStatus.Completed
    else SectionStatus.NotYetSaved

  private def purchaserAddressStatus(answers: UserAnswers): SectionStatus =
    if answers.get(IsPurchaserAddressInTheUkPage).isDefined then SectionStatus.Incomplete
    else SectionStatus.NotYetSaved

}
