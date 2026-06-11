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
import models.{DraftId, DraftNotification, DraftNotificationSection, NotificationSummary, SectionStatus, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{DraftIdPage, VehicleBusinessUsePage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class NotificationTaskListControllerSpec extends SpecBase with MockitoSugar {

  private lazy val notificationTaskListRoute = routes.NotificationTaskListController.onPageLoad().url

  private val testDraftId = DraftId("12345")

  private val baseAnswers = emptyUserAnswers.set(DraftIdPage, testDraftId).success.value

  private val answersBusinessUse = baseAnswers.set(VehicleBusinessUsePage, true).success.value
  private val answersPrivateUse  = baseAnswers.set(VehicleBusinessUsePage, false).success.value

  private val orgSummary = NotificationSummary.IndividualOrOrganisation(
    traderName = Some("Harbourview Limited"),
    vrn = Some("123456789"),
    hasDraftNotifications = true,
    isDeregistered = false
  )

  private def section(status: SectionStatus): DraftNotificationSection =
    DraftNotificationSection(status, data = None)

  private val incompleteDraft = DraftNotification(
    draftId = testDraftId.value,
    createdDate = "2026-03-01",
    lastUpdatedDate = Some("2026-03-20"),
    sections = Map(
      "introduction"       -> section(SectionStatus.NotYetSaved),
      "initialQuestions"   -> section(SectionStatus.NotYetSaved),
      "notifierDetails"    -> section(SectionStatus.NotYetSaved),
      "notifierAddress"    -> section(SectionStatus.NotYetSaved),
      "supplierSelfSupply" -> section(SectionStatus.NotYetSaved),
      "vehicles"           -> section(SectionStatus.NotYetSaved),
      "declaration"        -> section(SectionStatus.NotYetSaved)
    )
  )

  private def stubConnector(
    summary: Either[GetNotificationSummaryError, NotificationSummary] = Right(orgSummary),
    draft: Either[GetDraftNotificationError, DraftNotification] = Right(incompleteDraft)
  ): NovaImportsBackendConnector = {
    val m = mock[NovaImportsBackendConnector]
    when(m.getNotificationSummary(any[Option[String]])(any[HeaderCarrier])) thenReturn Future.successful(summary)
    when(m.getDraftNotification(eqTo(testDraftId))(any[HeaderCarrier])) thenReturn Future.successful(draft)
    m
  }

  private def applicationWith(
    identifierAction: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers],
    connector: NovaImportsBackendConnector = stubConnector()
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
        bind[NovaImportsBackendConnector].toInstance(connector)
      )
      .build()

  "NotificationTaskListController" - {

    "onPageLoad" - {

      "must return OK and render the task list with the trader name and VRN caption from the summary" in {
        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], Some(answersBusinessUse))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

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

      "must hide the 'Add your address' row when OQ1.0 was answered Yes" in {
        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], Some(answersBusinessUse))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

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
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

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
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include(routes.AboutYourDetailsController.onPageLoad().url)
          body must include(routes.LandingPageController.onPageLoad().url)
        }
      }

      "must render a Completed tag for any section whose status is completed" in {
        val partlyCompleteDraft = incompleteDraft.copy(
          sections = incompleteDraft.sections + ("notifierDetails" -> section(SectionStatus.Completed))
        )

        given application: Application = applicationWith(
          classOf[FakeVatTraderIdentifierAction],
          Some(answersBusinessUse),
          stubConnector(draft = Right(partlyCompleteDraft))
        )

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Completed")
        }
      }

      "must redirect to Unauthorised when the user is not a VAT-registered organisation" in {
        given application: Application =
          applicationWith(classOf[UnauthorisedIdentifierAction], Some(answersBusinessUse))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when no user answers exist" in {
        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], None)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when OQ1.0 has not been answered" in {
        val onlyDraftId = emptyUserAnswers.set(DraftIdPage, testDraftId).success.value

        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], Some(onlyDraftId))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the draft id is missing" in {
        val onlyBusinessUse = emptyUserAnswers.set(VehicleBusinessUsePage, true).success.value

        given application: Application =
          applicationWith(classOf[FakeVatTraderIdentifierAction], Some(onlyBusinessUse))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the summary fetch fails" in {
        given application: Application = applicationWith(
          classOf[FakeVatTraderIdentifierAction],
          Some(answersBusinessUse),
          stubConnector(summary = Left(GetNotificationSummaryError.UpstreamError(500, "boom")))
        )

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the summary returns an unexpected shape" in {
        val agentSummary = NotificationSummary.AgentWithoutClient(
          agentName = Some("ABC Consultancy"),
          hasDraftNotifications = false
        )

        given application: Application = applicationWith(
          classOf[FakeVatTraderIdentifierAction],
          Some(answersBusinessUse),
          stubConnector(summary = Right(agentSummary))
        )

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the draft fetch returns Forbidden" in {
        given application: Application = applicationWith(
          classOf[FakeVatTraderIdentifierAction],
          Some(answersBusinessUse),
          stubConnector(draft = Left(GetDraftNotificationError.Forbidden))
        )

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the draft fetch returns NotFound" in {
        given application: Application = applicationWith(
          classOf[FakeVatTraderIdentifierAction],
          Some(answersBusinessUse),
          stubConnector(draft = Left(GetDraftNotificationError.NotFound))
        )

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the draft fetch returns an UpstreamError" in {
        given application: Application = applicationWith(
          classOf[FakeVatTraderIdentifierAction],
          Some(answersBusinessUse),
          stubConnector(draft = Left(GetDraftNotificationError.UpstreamError(500, "boom")))
        )

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, notificationTaskListRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
