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

import base.SpecBase
import com.google.inject.name.Names
import connectors.{NovaImportsBackendConnector, UpdateSectionError}
import controllers.actions.*
import controllers.{routes, vehicledetails}
import models.{AgentSelectedClient, DraftId, ImportNumber, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AgentSelectedClientPage, DraftIdPage, DraftVersionIdPage}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{ImportEntryDatePage, ImportEntryNumberPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import views.html.ConfirmImportEntryDetailsView

import java.time.LocalDate
import scala.concurrent.Future

class ConfirmImportEntryDetailsControllerSpec extends SpecBase with MockitoSugar {

  val importNumber = ImportNumber(1)

  lazy val confirmImportEntryDetailsRoute =
    vehicledetails.routes.ConfirmImportEntryDetailsController.onPageLoad(importNumber).url

  lazy val importTwoRoute =
    vehicledetails.routes.ConfirmImportEntryDetailsController.onPageLoad(ImportNumber(2)).url

  val userAnswersWithGuardData: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(DraftVersionIdPage, 1L)
    .unsafeSet(VehicleFromEuPage, false)
    .unsafeSet(ImportEntryNumberPage(importNumber), "12GB3456ACD789EF21")
    .unsafeSet(ImportEntryDatePage(importNumber), LocalDate.of(2026, 3, 27))

  private def applicationFor(
    identifierAction: Class[? <: IdentifierAction],
    userAnswers: UserAnswers = userAnswersWithGuardData
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswers)))
      )
      .build()

  private def applicationForSubmit(userAnswers: UserAnswers, connector: NovaImportsBackendConnector): Application = {
    val mockSessionRepository = mock[SessionRepository]
    when(mockSessionRepository.setPage(any(), any(), any())(any())).thenReturn(Future.successful(userAnswers))

    applicationBuilderWithVatTrader(userAnswers = Some(userAnswers))
      .overrides(
        bind[NovaImportsBackendConnector].toInstance(connector),
        bind[SessionRepository].toInstance(mockSessionRepository)
      )
      .build()
  }

  private def connectorReturning(result: Either[UpdateSectionError, Long]): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(result))
    connector
  }

  "ConfirmImportEntryDetailsController" - {

    "onPageLoad" - {

      "must return OK and the AVD-IE2.1 view for import 1 for a GET" in {

        val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswersWithGuardData)).build()

        running(application) {
          val request = FakeRequest(GET, confirmImportEntryDetailsRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ConfirmImportEntryDetailsView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(userAnswersWithGuardData, importNumber)(request, messages(application)).toString
        }
      }

      "must redirect a GET to Unauthorised when there is no session data" in {

        val application = applicationBuilderWithVatTrader(userAnswers = None).build()

        running(application) {
          val result = route(application, FakeRequest(GET, confirmImportEntryDetailsRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect a GET to Unauthorised when IQ1.0 was answered Yes" in {

        val answersIq1Yes = userAnswersWithGuardData.unsafeSet(VehicleFromEuPage, true)

        val application = applicationBuilderWithVatTrader(userAnswers = Some(answersIq1Yes)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, confirmImportEntryDetailsRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect a GET to Unauthorised for a non-VAT organisation" in {

        val application = applicationFor(classOf[FakeOrganisationIdentifierAction])

        running(application) {
          val result = route(application, FakeRequest(GET, confirmImportEntryDetailsRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must return OK for a GET from an agent without agent enrolment" in {

        val application = applicationFor(classOf[FakeAgentNoEnrolmentsIdentifierAction])

        running(application) {
          status(route(application, FakeRequest(GET, confirmImportEntryDetailsRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent with the agent enrolment" in {

        val application = applicationFor(classOf[FakeAgentIdentifierAction])

        running(application) {
          status(route(application, FakeRequest(GET, confirmImportEntryDetailsRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent with a client selected" in {

        val answersWithClient =
          userAnswersWithGuardData.unsafeSet(AgentSelectedClientPage, AgentSelectedClient(vrn = "123456789", name = Some("Company 1 Ltd")))

        val application = applicationFor(classOf[FakeAgentIdentifierAction], answersWithClient)

        running(application) {
          status(route(application, FakeRequest(GET, confirmImportEntryDetailsRoute)).value) mustEqual OK
        }
      }

      "must redirect a GET to Unauthorised for an private individual" in {

        val application = applicationFor(classOf[FakeIdentifierAction])

        running(application) {
          val result = route(application, FakeRequest(GET, confirmImportEntryDetailsRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect a GET to Unauthorised when import 1 has no import entry number saved in session" in {

        val answersWithoutNumber = userAnswersWithGuardData.remove(ImportEntryNumberPage(importNumber)).success.value

        val application = applicationBuilderWithVatTrader(userAnswers = Some(answersWithoutNumber)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, confirmImportEntryDetailsRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect a GET to Unauthorised when import 1 has no import entry date saved in session" in {

        val answersWithoutDate = userAnswersWithGuardData.remove(ImportEntryDatePage(importNumber)).success.value

        val application = applicationBuilderWithVatTrader(userAnswers = Some(answersWithoutDate)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, confirmImportEntryDetailsRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect a GET to Unauthorised when import 2 is not in the current session" in {

        val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswersWithGuardData)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, importTwoRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      // TODO: change to AVD-IE3.0 when built
      "must redirect a POST to the landing page when the save succeeds" in {

        val application = applicationForSubmit(userAnswersWithGuardData, connectorReturning(Right(2L)))

        running(application) {
          val result = route(application, FakeRequest(POST, confirmImportEntryDetailsRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.LandingPageController.onPageLoad().url
        }
      }

      "must call updateDraftSection with import/1/details, the import entry number, 27/03/2026 and versionId 1" in {

        val connector = connectorReturning(Right(2L))

        val application = applicationForSubmit(userAnswersWithGuardData, connector)

        running(application) {
          val result = route(application, FakeRequest(POST, confirmImportEntryDetailsRoute)).value
          status(result) mustEqual SEE_OTHER

          val bodyCaptor = ArgumentCaptor.forClass(classOf[JsObject])
          verify(connector).updateDraftSection(any[DraftId], eqTo("import/1/details"), bodyCaptor.capture())(any[HeaderCarrier])
          val sentBody = bodyCaptor.getValue

          (sentBody \ "versionId").as[Long] mustEqual 1L
          (sentBody \ "importEntryNumber").as[String] mustEqual "12GB3456ACD789EF21"
          (sentBody \ "importEntryDate").as[String] mustEqual "27/03/2026"
        }
      }

      "must redirect a POST to Journey Recovery when the backend returns an UpstreamError" in {

        val application =
          applicationForSubmit(userAnswersWithGuardData, connectorReturning(Left(UpdateSectionError.UpstreamError(500, "error"))))

        running(application) {
          val result = route(application, FakeRequest(POST, confirmImportEntryDetailsRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect a POST to Journey Recovery when the backend returns Forbidden" in {

        val application = applicationForSubmit(userAnswersWithGuardData, connectorReturning(Left(UpdateSectionError.Forbidden)))

        running(application) {
          val result = route(application, FakeRequest(POST, confirmImportEntryDetailsRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect a POST to Journey Recovery when the backend returns NotFound" in {

        val application = applicationForSubmit(userAnswersWithGuardData, connectorReturning(Left(UpdateSectionError.NotFound)))

        running(application) {
          val result = route(application, FakeRequest(POST, confirmImportEntryDetailsRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect a POST to Journey Recovery when the versionId is missing" in {

        val answersWithoutVersionId = userAnswersWithGuardData.remove(DraftVersionIdPage).success.value

        val application = applicationForSubmit(answersWithoutVersionId, mock[NovaImportsBackendConnector])

        running(application) {
          val result = route(application, FakeRequest(POST, confirmImportEntryDetailsRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect a POST to Unauthorised when there is no draftId in session" in {

        val answersWithoutDraftId = userAnswersWithGuardData.remove(DraftIdPage).success.value

        val application = applicationForSubmit(answersWithoutDraftId, mock[NovaImportsBackendConnector])

        running(application) {
          val result = route(application, FakeRequest(POST, confirmImportEntryDetailsRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect a POST to Unauthorised when import 1 has no import entry date saved in session" in {

        val answersWithoutDate = userAnswersWithGuardData.remove(ImportEntryDatePage(importNumber)).success.value

        val application = applicationForSubmit(answersWithoutDate, mock[NovaImportsBackendConnector])

        running(application) {
          val result = route(application, FakeRequest(POST, confirmImportEntryDetailsRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }
  }
}
