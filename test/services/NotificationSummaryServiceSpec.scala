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

import base.SpecBase
import connectors.{GetNotificationSummaryError, NovaImportsBackendConnector}
import models.NotificationSummary
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.IsDeregisteredPage
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NotificationSummaryServiceSpec extends SpecBase with MockitoSugar {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val orgSummary = NotificationSummary.IndividualOrOrganisation(
    traderName = Some("Trader"),
    vrn = Some("123456789"),
    hasDraftNotifications = true,
    isDeregistered = true
  )

  private val agentSummary = NotificationSummary.AgentWithoutClient(
    agentName = Some("ABC Consultancy"),
    hasDraftNotifications = false
  )

  private def newService(connector: NovaImportsBackendConnector, repo: SessionRepository): NotificationSummaryService =
    new NotificationSummaryServiceImpl(connector, repo)

  "NotificationSummaryService.getSummaryAndStoreDeregisteredStatus" - {

    "for an IndividualOrOrganisation summary saves the deregistered flag and returns the summary with the updated answers" in {
      val connector    = mock[NovaImportsBackendConnector]
      val repo         = mock[SessionRepository]
      val savedAnswers = emptyUserAnswers.set(IsDeregisteredPage, true).success.value

      when(connector.getNotificationSummary(any[Option[String]])(any[HeaderCarrier])).thenReturn(Future.successful(Right(orgSummary)))
      when(repo.setPage(any(), eqTo(IsDeregisteredPage), eqTo(true))(any())).thenReturn(Future.successful(savedAnswers))

      val result = newService(connector, repo).getSummaryAndStoreDeregisteredStatus(emptyUserAnswers, None).futureValue

      result mustBe Right((orgSummary, savedAnswers))
      verify(repo).setPage(any(), eqTo(IsDeregisteredPage), eqTo(true))(any())
    }

    "for an agent summary returns the summary unchanged and does not save the deregistered flag" in {
      val connector = mock[NovaImportsBackendConnector]
      val repo      = mock[SessionRepository]
      val answers   = emptyUserAnswers

      when(connector.getNotificationSummary(any[Option[String]])(any[HeaderCarrier])).thenReturn(Future.successful(Right(agentSummary)))

      val result = newService(connector, repo).getSummaryAndStoreDeregisteredStatus(answers, None).futureValue

      result mustBe Right((agentSummary, answers))
      verify(repo, never()).setPage(any(), any(), any())(any())
    }

    "when the summary call fails returns the error and does not save anything" in {
      val connector = mock[NovaImportsBackendConnector]
      val repo      = mock[SessionRepository]
      val error     = GetNotificationSummaryError.UpstreamError(500, "error message")

      when(connector.getNotificationSummary(any[Option[String]])(any[HeaderCarrier])).thenReturn(Future.successful(Left(error)))

      val result = newService(connector, repo).getSummaryAndStoreDeregisteredStatus(emptyUserAnswers, None).futureValue

      result mustBe Left(error)
      verify(repo, never()).setPage(any(), any(), any())(any())
    }

    "forwards the clientVrn to the connector" in {
      val connector = mock[NovaImportsBackendConnector]
      val repo      = mock[SessionRepository]

      when(connector.getNotificationSummary(any[Option[String]])(any[HeaderCarrier])).thenReturn(Future.successful(Right(agentSummary)))

      newService(connector, repo).getSummaryAndStoreDeregisteredStatus(emptyUserAnswers, Some("700011916")).futureValue

      verify(connector).getNotificationSummary(eqTo(Some("700011916")))(any[HeaderCarrier])
    }
  }
}
