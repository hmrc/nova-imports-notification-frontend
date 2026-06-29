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
import com.google.inject.name.Names
import connectors.{GetDraftNotificationError, GetNotificationSummaryError, NovaImportsBackendConnector}
import controllers.actions.*
import models.NormalMode
import models.{ContactNumbers, DraftId, DraftNotification, DraftNotificationSection, NotificationSummary, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{DraftIdPage, IsDeregisteredPage, NotificationTaskListPage}
import pages.sections.initialquestions.VehicleBusinessUsePage
import pages.sections.notifierDetails.PhoneNumberPage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class NotificationTaskListControllerSpec extends SpecBase with MockitoSugar {

  private lazy val notificationTaskListRoute =
    routes.NotificationTaskListController.onPageLoad().url

  private val testDraftId = DraftId("12345")

  private val baseAnswers =
    emptyUserAnswers.set(DraftIdPage, testDraftId).success.value

  private val answersBusinessUse =
    baseAnswers.set(VehicleBusinessUsePage, true).success.value

  private val answersPrivateUse =
    baseAnswers.set(VehicleBusinessUsePage, false).success.value

  private val orgSummary = NotificationSummary.IndividualOrOrganisation(
    traderName = Some("Harbourview Limited"),
    vrn = Some("123456789"),
    hasDraftNotifications = true,
    isDeregistered = false
  )

  private val deregisteredOrgSummary = orgSummary.copy(isDeregistered = true)

  private val emptySection: DraftNotificationSection =
    DraftNotificationSection(None)

  private val incompleteDraft = DraftNotification(
    draftId = testDraftId.value,
    createdDate = "2026-03-01",
    lastUpdatedDate = Some("2026-03-20"),
    sections = Map(
      "introduction"       -> emptySection,
      "initialQuestions"   -> emptySection,
      "notifierDetails"    -> emptySection,
      "notifierAddress"    -> emptySection,
      "supplierSelfSupply" -> emptySection,
      "vehicles"           -> emptySection,
      "declaration"        -> emptySection
    )
  )

  private def stubConnector(
    summary: Either[GetNotificationSummaryError, NotificationSummary] = Right(orgSummary),
    draft: Either[GetDraftNotificationError, DraftNotification] = Right(incompleteDraft)
  ): NovaImportsBackendConnector = {
    val m = mock[NovaImportsBackendConnector]

    when(m.getNotificationSummary(any[Option[String]])(any[HeaderCarrier]))
      .thenReturn(Future.successful(summary))

    when(m.getDraftNotification(any[DraftId])(any[HeaderCarrier]))
      .thenReturn(Future.successful(draft))

    m
  }

  private def stubSessionRepository(): SessionRepository = {
    val sessionRepo = mock[SessionRepository]
    when(sessionRepo.setPage(any(), any(), any())(any())).thenAnswer { (invocation: org.mockito.invocation.InvocationOnMock) =>
      val answers = invocation.getArgument[UserAnswers](0)
      val page    = invocation.getArgument[queries.Settable[Any]](1)
      val value   = invocation.getArgument[Any](2)
      val writes  = invocation.getArgument[play.api.libs.json.Writes[Any]](3)
      Future.successful(answers.set(page, value)(writes).get)
    }
    when(sessionRepo.set(any())).thenReturn(Future.successful(true))
    sessionRepo
  }

  private def applicationWith(
    identifierAction: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers],
    connector: NovaImportsBackendConnector = stubConnector(),
    sessionRepo: SessionRepository = stubSessionRepository()
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[NovaImportsBackendConnector].toInstance(connector),
        bind[SessionRepository].toInstance(sessionRepo)
      )
      .build()

  "NotificationTaskListController" - {

    "onPageLoad" - {

      "must return OK and render the task list with the trader name and VRN caption from the summary" in {
        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], Some(answersBusinessUse))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Create a vehicle notification")
          body must include("Harbourview Limited")
          body must include("VAT registration number: GB123456789")
          body must include("About you")
          body must include("Add your details")
          body must include("About the vehicles")
          body must include("Add vehicle details")
          body must include("Declaration")
          body must include("Read declaration")
          body must include("Cannot start yet")
          body must include("Return to home")
          body must include("Delete notification")
        }
      }

      "must persist NotificationTaskListPage = true in the session" in {
        val sessionRepo = stubSessionRepository()
        val captor      = ArgumentCaptor.forClass(classOf[UserAnswers])

        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], Some(answersBusinessUse), sessionRepo = sessionRepo)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          status(route(application, request).value) mustEqual OK

          verify(sessionRepo).set(captor.capture())
          captor.getValue.get(NotificationTaskListPage) mustBe Some(true)
        }
      }

      "must save the deregistered status from the summary into the session" in {
        val sessionRepo                = stubSessionRepository()
        given application: Application =
          applicationWith(
            classOf[FakeVatTraderIdentifierAction],
            Some(answersBusinessUse),
            stubConnector(summary = Right(deregisteredOrgSummary)),
            sessionRepo
          )

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          status(route(application, request).value) mustEqual OK
          verify(sessionRepo).setPage(any(), eqTo(IsDeregisteredPage), eqTo(true))(any())
        }
      }

      "must hide the 'Add your address' row when OQ1.0 was answered Yes" in {
        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], Some(answersBusinessUse))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must not include "Add your address"
        }
      }

      "must show the 'Add your address' row when OQ1.0 was answered No, linking to AYA1.0" in {
        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], Some(answersPrivateUse))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Add your address")
          body must include(routes.IsYourAddressInTheUkController.onPageLoad(NormalMode).url)
        }
      }

      "must link 'Add your details' to AYD1.0 and 'Return to home' to the landing page" in {
        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], Some(answersBusinessUse))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include(routes.AboutYourDetailsController.onPageLoad().url)
          body must include(routes.LandingPageController.onPageLoad().url)
        }
      }

      "must render a Completed tag for any section whose status is completed" in {
        val answersWithPhone =
          answersBusinessUse.set(PhoneNumberPage, ContactNumbers(Some("01234567890"), None)).success.value

        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], Some(answersWithPhone))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Completed")
        }
      }

      "must redirect to Unauthorised when the user is not a VAT-registered organisation" in {
        val sessionRepo                = stubSessionRepository()
        given application: Application =
          applicationWith(classOf[UnauthorisedIdentifierAction], Some(answersBusinessUse), sessionRepo = sessionRepo)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
          verify(sessionRepo, never).set(any())
        }
      }

      "must redirect to Journey Recovery when no user answers exist" in {
        val sessionRepo                = stubSessionRepository()
        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], None, sessionRepo = sessionRepo)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(sessionRepo, never).set(any())
        }
      }

      "must redirect to Journey Recovery when OQ1.0 has not been answered" in {
        val onlyDraftId =
          emptyUserAnswers.set(DraftIdPage, testDraftId).success.value
        val sessionRepo = stubSessionRepository()

        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], Some(onlyDraftId), sessionRepo = sessionRepo)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(sessionRepo, never).set(any())
        }
      }

      "must redirect to Journey Recovery when the draft id is missing" in {
        val onlyBusinessUse =
          emptyUserAnswers.set(VehicleBusinessUsePage, true).success.value
        val sessionRepo = stubSessionRepository()

        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], Some(onlyBusinessUse), sessionRepo = sessionRepo)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(sessionRepo, never).set(any())
        }
      }

      "must redirect to Journey Recovery when the summary fetch fails" in {
        val sessionRepo                = stubSessionRepository()
        given application: Application =
          applicationWith(
            classOf[FakeVatTraderIdentifierAction],
            Some(answersBusinessUse),
            stubConnector(summary = Left(GetNotificationSummaryError.UpstreamError(500, "boom"))),
            sessionRepo
          )

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(sessionRepo, never).set(any())
        }
      }

      "must redirect to Journey Recovery when the summary returns an unexpected shape" in {
        val agentSummary = NotificationSummary.AgentWithoutClient(
          agentName = Some("ABC Consultancy"),
          hasDraftNotifications = false
        )
        val sessionRepo = stubSessionRepository()

        given application: Application =
          applicationWith(
            classOf[FakeVatTraderIdentifierAction],
            Some(answersBusinessUse),
            stubConnector(summary = Right(agentSummary)),
            sessionRepo
          )

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(sessionRepo, never).set(any())
        }
      }

      "must redirect to Journey Recovery when the draft fetch returns Forbidden" in {
        val sessionRepo                = stubSessionRepository()
        given application: Application =
          applicationWith(
            classOf[FakeVatTraderIdentifierAction],
            Some(answersBusinessUse),
            stubConnector(draft = Left(GetDraftNotificationError.Forbidden)),
            sessionRepo
          )

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(sessionRepo, never).set(any())
        }
      }

      "must redirect to Journey Recovery when the draft fetch returns NotFound" in {
        val sessionRepo                = stubSessionRepository()
        given application: Application =
          applicationWith(
            classOf[FakeVatTraderIdentifierAction],
            Some(answersBusinessUse),
            stubConnector(draft = Left(GetDraftNotificationError.NotFound)),
            sessionRepo
          )

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(sessionRepo, never).set(any())
        }
      }

      "must redirect to Journey Recovery when the draft fetch returns an UpstreamError" in {
        val sessionRepo                = stubSessionRepository()
        given application: Application =
          applicationWith(
            classOf[FakeVatTraderIdentifierAction],
            Some(answersBusinessUse),
            stubConnector(draft = Left(GetDraftNotificationError.UpstreamError(500, "boom"))),
            sessionRepo
          )

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(sessionRepo, never).set(any())
        }
      }
    }
  }
}
