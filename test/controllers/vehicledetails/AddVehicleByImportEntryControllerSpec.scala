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
import controllers.actions.*
import controllers.{routes, vehicledetails}
import models.{AgentSelectedClient, DraftId, ImportNumber, NormalMode, UserAnswers, VehicleNumber}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AgentSelectedClientPage, DraftIdPage}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{ImportEntryDatePage, ImportEntryNumberPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.AllVehiclesQuery
import repositories.SessionRepository
import views.html.AddVehicleByImportEntryView

import java.time.LocalDate
import scala.concurrent.Future

class AddVehicleByImportEntryControllerSpec extends SpecBase with MockitoSugar {

  val importNumber = ImportNumber(1)

  lazy val addVehicleByImportEntryRoute =
    vehicledetails.routes.AddVehicleByImportEntryController.onPageLoad(importNumber).url

  lazy val importTwoRoute =
    vehicledetails.routes.AddVehicleByImportEntryController.onPageLoad(ImportNumber(2)).url

  val userAnswersWithGuardData: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
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

  private def mockSessionRepository(userAnswers: UserAnswers): SessionRepository = {
    val repo = mock[SessionRepository]
    when(repo.setPage(any(), any(), any())(any())).thenReturn(Future.successful(userAnswers))
    repo
  }

  private def applicationForSubmit(userAnswers: UserAnswers, repo: SessionRepository): Application =
    applicationBuilderWithVatTrader(userAnswers = Some(userAnswers))
      .overrides(bind[SessionRepository].toInstance(repo))
      .build()

  "AddVehicleByImportEntryController" - {

    "onPageLoad" - {

      "must return OK and the AVD-IE3.0 view with import entry 12GB3456ACD789EF21 for a VAT organisation GET" in {

        val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswersWithGuardData)).build()

        running(application) {
          val request = FakeRequest(GET, addVehicleByImportEntryRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[AddVehicleByImportEntryView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view("12GB3456ACD789EF21", importNumber)(request, messages(application)).toString
        }
      }

      "must return OK for a GET from an agent without agent enrolment" in {

        val application = applicationFor(classOf[FakeAgentNoEnrolmentsIdentifierAction])

        running(application) {
          status(route(application, FakeRequest(GET, addVehicleByImportEntryRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent with the agent enrolment" in {

        val application = applicationFor(classOf[FakeAgentIdentifierAction])

        running(application) {
          status(route(application, FakeRequest(GET, addVehicleByImportEntryRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent with a client selected" in {

        val answersWithClient =
          userAnswersWithGuardData.unsafeSet(AgentSelectedClientPage, AgentSelectedClient(vrn = "123456789", name = Some("Company 1 Ltd")))

        val application = applicationFor(classOf[FakeAgentIdentifierAction], answersWithClient)

        running(application) {
          status(route(application, FakeRequest(GET, addVehicleByImportEntryRoute)).value) mustEqual OK
        }
      }

      "must redirect a GET to Unauthorised for a non-VAT organisation" in {

        val application = applicationFor(classOf[FakeOrganisationIdentifierAction])

        running(application) {
          val result = route(application, FakeRequest(GET, addVehicleByImportEntryRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect a GET to Unauthorised for a private individual" in {

        val application = applicationFor(classOf[FakeIdentifierAction])

        running(application) {
          val result = route(application, FakeRequest(GET, addVehicleByImportEntryRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect a GET to Unauthorised when IQ1.0 was answered Yes" in {

        val answersIq1Yes = userAnswersWithGuardData.unsafeSet(VehicleFromEuPage, true)

        val application = applicationBuilderWithVatTrader(userAnswers = Some(answersIq1Yes)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, addVehicleByImportEntryRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect a GET to Unauthorised when import 1 has no import entry number in session" in {

        val answersWithoutNumber = userAnswersWithGuardData.remove(ImportEntryNumberPage(importNumber)).success.value

        val application = applicationBuilderWithVatTrader(userAnswers = Some(answersWithoutNumber)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, addVehicleByImportEntryRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect a GET to Unauthorised when import 2 is not in the session" in {

        val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswersWithGuardData)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, importTwoRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect a GET to Unauthorised when there is no session data" in {

        val application = applicationBuilderWithVatTrader(userAnswers = None).build()

        running(application) {
          val result = route(application, FakeRequest(GET, addVehicleByImportEntryRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must redirect a POST to AVD-IE4.0 for vehicle 1 against import 1" in {

        val application = applicationForSubmit(userAnswersWithGuardData, mockSessionRepository(userAnswersWithGuardData))

        running(application) {
          val result = route(application, FakeRequest(POST, addVehicleByImportEntryRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            vehicledetails.routes.DateOfFirstRegistrationKnownController.onPageLoad(importNumber, VehicleNumber(1), NormalMode).url
        }
      }

      "must save vehicle 1 to the session against import 1 for a POST" in {

        val repo        = mockSessionRepository(userAnswersWithGuardData)
        val application = applicationForSubmit(userAnswersWithGuardData, repo)

        running(application) {
          route(application, FakeRequest(POST, addVehicleByImportEntryRoute)).value.futureValue

          verify(repo).setPage(any(), eqTo(AllVehiclesQuery), eqTo(Map("1" -> Json.obj("importNumber" -> 1))))(any())
        }
      }

      "must redirect a POST to Unauthorised when there is no draftId in session" in {

        val answersWithoutDraftId = userAnswersWithGuardData.remove(DraftIdPage).success.value

        val application = applicationForSubmit(answersWithoutDraftId, mockSessionRepository(answersWithoutDraftId))

        running(application) {
          val result = route(application, FakeRequest(POST, addVehicleByImportEntryRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }
  }
}
