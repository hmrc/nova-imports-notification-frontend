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
import models.{AgentSelectedClient, DraftId, UserAnswers}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.{AgentSelectedClientPage, DraftIdPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.UploadSpreadsheetErrorQuarantineView

class UploadSpreadsheetErrorQuarantineControllerSpec extends SpecBase {

  private lazy val onPageLoadRoute = vehicledetails.routes.UploadSpreadsheetErrorQuarantineController.onPageLoad().url

  private val answersWithDraftIdAndEuAnswer: UserAnswers =
    emptyUserAnswers.unsafeSet(DraftIdPage, DraftId("DRAFT-001")).unsafeSet(VehicleFromEuPage, true)

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

  "UploadSpreadsheetErrorQuarantineController" - {

    "must return OK and render UVS3.0 for a VAT-registered organisation" in {
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answersWithDraftIdAndEuAnswer))

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[UploadSpreadsheetErrorQuarantineView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
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
