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

package controllers.utils

import connectors.NovaImportsBackendConnector
import controllers.routes
import models.requests.DataRequest
import models.{AddressJourney, SupplierNumber, UserAnswers}
import pages.sections.supplierdetails.SupplierEuMemberStatesPage
import play.api.Logging
import play.api.mvc.Result
import play.api.mvc.Results.*
import repositories.SessionRepository
import services.AddressLookupService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

object SupplierAlfUtil extends Logging {

  def initialiseAlfJourney(
    backendConnector: NovaImportsBackendConnector,
    addressLookupService: AddressLookupService,
    sessionRepository: SessionRepository,
    supplierNumber: SupplierNumber,
    userAnswers: UserAnswers
  )(implicit
    request: DataRequest[?],
    ec: ExecutionContext
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val journey                    = AddressJourney.Supplier(supplierNumber)

    backendConnector.getEuMemberStates().flatMap {
      case Right(states) =>
        addressLookupService.initJourney(journey, false, states.countries.map(_.code).toSeq).flatMap {
          case Right(journeyUrl) =>
            for {
              ua <- Future.fromTry(userAnswers.set(SupplierEuMemberStatesPage(supplierNumber), states.getCountriesCurrentlyInEu()))
              _  <- sessionRepository.set(ua)
            } yield Redirect(journeyUrl)
          case Left(error) =>
            logger.warn(s"Failed to init supplier ALF journey : $error")
            Future successful Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
      case Left(error) =>
        logger.warn(s"Failed to init supplier ALF journey : $error")
        Future successful Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }
}
