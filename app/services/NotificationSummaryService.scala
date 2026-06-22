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
import connectors.{GetNotificationSummaryError, NovaImportsBackendConnector}
import models.{NotificationSummary, UserAnswers}
import pages.IsDeregisteredPage
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[NotificationSummaryServiceImpl])
trait NotificationSummaryService {

  def getSummaryAndStoreDeregisteredStatus(answers: UserAnswers, clientVrn: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[Either[GetNotificationSummaryError, (NotificationSummary, UserAnswers)]]
}

@Singleton
class NotificationSummaryServiceImpl @Inject() (
  backendConnector: NovaImportsBackendConnector,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends NotificationSummaryService {

  def getSummaryAndStoreDeregisteredStatus(answers: UserAnswers, clientVrn: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[Either[GetNotificationSummaryError, (NotificationSummary, UserAnswers)]] =
    backendConnector.getNotificationSummary(clientVrn).flatMap {
      case Right(summary: NotificationSummary.IndividualOrOrganisation) =>
        sessionRepository.setPage(answers, IsDeregisteredPage, summary.isDeregistered).map(saved => Right((summary, saved)))
      case Right(summary) =>
        Future.successful(Right((summary, answers)))
      case Left(error) =>
        Future.successful(Left(error))
    }
}
