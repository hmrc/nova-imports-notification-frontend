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
import models.{AgentSelectedClient, DraftId, SpreadsheetValidationError, UserAnswers}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.{AgentSelectedClientPage, DraftIdPage, SpreadsheetValidationErrorsPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.UploadSpreadsheetErrorDataView

class UploadSpreadsheetErrorDataControllerSpec extends SpecBase {

  private lazy val onPageLoadRoute = vehicledetails.routes.UploadSpreadsheetErrorDataController.onPageLoad().url

  private val answersWithDraftIdAndEuAnswer: UserAnswers =
    emptyUserAnswers.unsafeSet(DraftIdPage, DraftId("DRAFT-001")).unsafeSet(VehicleFromEuPage, true)

  private val validationErrors: Seq[SpreadsheetValidationError] = Seq(
    SpreadsheetValidationError(1, "Registration number is missing"),
    SpreadsheetValidationError(3, "Date of availability must be a real date")
  )

  private def applicationFor(standardIdentifier: Class[? <: IdentifierAction], userAnswers: Option[UserAnswers]): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(standardIdentifier),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )
      .build()

  "UploadSpreadsheetErrorDataController" - {

    "must return OK and render UVS4.0 with no validation errors for a VAT-registered organisation" in {
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answersWithDraftIdAndEuAnswer))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[UploadSpreadsheetErrorDataView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Seq.empty)(request, messages(application)).toString
      }
    }

    "must render the validation errors stored against the draft" in {
      val answers     = answersWithDraftIdAndEuAnswer.unsafeSet(SpreadsheetValidationErrorsPage, validationErrors)
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answers))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[UploadSpreadsheetErrorDataView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(validationErrors)(request, messages(application)).toString

        val body = contentAsString(result)
        body must include("1")
        body must include("Registration number is missing")
        body must include("3")
        body must include("Date of availability must be a real date")
      }
    }

    "must return OK for an agent who has selected a client" in {
      val answers     = answersWithDraftIdAndEuAnswer.unsafeSet(AgentSelectedClientPage, AgentSelectedClient("700011916", Some("Client Co")))
      val application = applicationFor(classOf[FakeAgentIdentifierAction], Some(answers))

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Unauthorised for an agent who has not selected a client" in {
      val application = applicationFor(classOf[FakeAgentIdentifierAction], Some(answersWithDraftIdAndEuAnswer))

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a private individual" in {
      val application = applicationFor(classOf[FakeIdentifierAction], Some(answersWithDraftIdAndEuAnswer))

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a non-VAT organisation" in {
      val application = applicationFor(classOf[FakeOrganisationIdentifierAction], Some(answersWithDraftIdAndEuAnswer))

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when the vehicle from EU question has not been answered" in {
      val answersWithoutEuAnswer = emptyUserAnswers.unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      val application            = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answersWithoutEuAnswer))

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised if draftId is missing" in {
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(emptyUserAnswers))

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised if no existing session data is found" in {
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], None)

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
