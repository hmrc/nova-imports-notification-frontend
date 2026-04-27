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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.mvc.Results.*
import play.api.mvc.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]

object NovaEnrolments {
  val vatMtd: String           = "HMRC-MTD-VAT"
  val vatMtdIdentifier: String = "VRN"

  val vatDec: String           = "HMCE-VATDEC-ORG"
  val vatDecIdentifier: String = "VATRegNo"

  val vatAgent: String           = "HMCE-VAT-AGNT"
  val vatAgentIdentifier: String = "AgentRefNo"

  val novrnAgent: String           = "HMRC-NOVRN-AGNT"
  val novrnAgentIdentifier: String = "VATAgentRefNo"
}

/** Accepts any authenticated Individual, Organisation, or Agent — no enrolment checks. */
class StandardIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised()
      .retrieve(Retrievals.internalId and Retrievals.affinityGroup and Retrievals.allEnrolments) {
        case Some(internalId) ~ Some(affinityGroup) ~ enrolments
            if Set(AffinityGroup.Individual, AffinityGroup.Organisation, AffinityGroup.Agent).contains(affinityGroup) =>
          block(IdentifierRequest(request, internalId, affinityGroup, enrolments))

        case Some(_) ~ affinityGroup ~ _ =>
          logger.warn(s"Unsupported affinity group: $affinityGroup")
          Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))

        case _ =>
          throw new UnauthorizedException("Unable to retrieve internal Id")
      }
      .recover {
        case _: NoActiveSession =>
          Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
        case _: AuthorisationException =>
          Redirect(routes.UnauthorisedController.onPageLoad())
      }
  }
}

/** Requires Organisation with HMRC-MTD-VAT or HMCE-VATDEC-ORG enrolment. */
class VatTraderIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised()
      .retrieve(Retrievals.internalId and Retrievals.affinityGroup and Retrievals.allEnrolments) {
        case Some(internalId) ~ Some(AffinityGroup.Organisation) ~ enrolments
            if hasActiveEnrolment(enrolments, NovaEnrolments.vatMtd) ||
              hasActiveEnrolment(enrolments, NovaEnrolments.vatDec) =>
          block(IdentifierRequest(request, internalId, AffinityGroup.Organisation, enrolments))

        case Some(_) ~ _ ~ _ =>
          logger.warn("VAT trader route accessed without required Organisation enrolment")
          Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))

        case _ =>
          throw new UnauthorizedException("Unable to retrieve internal Id")
      }
      .recover {
        case _: NoActiveSession =>
          Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
        case _: AuthorisationException =>
          Redirect(routes.UnauthorisedController.onPageLoad())
      }
  }

  private def hasActiveEnrolment(enrolments: Enrolments, key: String): Boolean =
    enrolments.getEnrolment(key).exists(_.isActivated)
}

/** Requires Agent affinity group without HMRC-NOVRN-AGNT enrolment (excludes OGD agents) */
class NovaAgentIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised()
      .retrieve(Retrievals.internalId and Retrievals.affinityGroup and Retrievals.allEnrolments) {
        case Some(internalId) ~ Some(AffinityGroup.Agent) ~ enrolments if !enrolments.getEnrolment(NovaEnrolments.novrnAgent).exists(_.isActivated) =>
          block(IdentifierRequest(request, internalId, AffinityGroup.Agent, enrolments))

        case Some(_) ~ _ ~ _ =>
          logger.warn("Nova agent route accessed by non-Agent or OGD agent user")
          Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))

        case _ =>
          throw new UnauthorizedException("Unable to retrieve internal Id")
      }
      .recover {
        case _: NoActiveSession =>
          Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
        case _: AuthorisationException =>
          Redirect(routes.UnauthorisedController.onPageLoad())
      }
  }
}

/** Requires Agent with HMRC-NOVRN-AGNT enrolment (DVLA/DVA OGD users). */
class OgdIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised()
      .retrieve(Retrievals.internalId and Retrievals.affinityGroup and Retrievals.allEnrolments) {
        case Some(internalId) ~ Some(AffinityGroup.Agent) ~ enrolments if enrolments.getEnrolment(NovaEnrolments.novrnAgent).exists(_.isActivated) =>
          block(IdentifierRequest(request, internalId, AffinityGroup.Agent, enrolments))

        case Some(_) ~ _ ~ _ =>
          logger.warn("OGD route accessed without required HMRC-NOVRN-AGNT enrolment")
          Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))

        case _ =>
          throw new UnauthorizedException("Unable to retrieve internal Id")
      }
      .recover {
        case _: NoActiveSession =>
          Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
        case _: AuthorisationException =>
          Redirect(routes.UnauthorisedController.onPageLoad())
      }
  }
}

/** Session-based identifier for testing -- no auth connector needed. */
class SessionIdentifierAction @Inject() (
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    hc.sessionId match {
      case Some(session) =>
        block(IdentifierRequest(request, session.value, AffinityGroup.Individual, Enrolments(Set.empty)))
      case None =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
