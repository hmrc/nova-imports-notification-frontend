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

package controllers

import base.SpecBase
import connectors.{CreateDraftError, NovaImportsBackendConnector}
import models.{DraftId, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class StartControllerSpec extends SpecBase with MockitoSugar {

  "StartController" - {

    "when the F3 call succeeds" - {

      "must persist the returned draft id and redirect to BeforeYouContinue" in {

        val draftId               = DraftId("DRAFT-42")
        val mockSessionRepository = mock[SessionRepository]
        val mockConnector         = mock[NovaImportsBackendConnector]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockConnector.createDraft(any())(any[HeaderCarrier])) thenReturn Future.successful(Right(draftId))

        val application =
          applicationBuilder(userAnswers = None)
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[NovaImportsBackendConnector].toInstance(mockConnector)
            )
            .build()

        running(application) {
          val request = FakeRequest(GET, routes.StartController.start().url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.BeforeYouContinueController.onPageLoad().url

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockSessionRepository).set(captor.capture())
          captor.getValue.get(DraftIdPage).value mustEqual draftId
        }
      }
    }

    "when the F3 call fails" - {

      "must redirect to JourneyRecovery and not write to the session" in {

        val mockSessionRepository = mock[SessionRepository]
        val mockConnector         = mock[NovaImportsBackendConnector]

        when(mockConnector.createDraft(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(CreateDraftError.UpstreamError(500, "boom"))))

        val application =
          applicationBuilder(userAnswers = None)
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[NovaImportsBackendConnector].toInstance(mockConnector)
            )
            .build()

        running(application) {
          val request = FakeRequest(GET, routes.StartController.start().url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, org.mockito.Mockito.never()).set(any())
        }
      }
    }
  }
}
