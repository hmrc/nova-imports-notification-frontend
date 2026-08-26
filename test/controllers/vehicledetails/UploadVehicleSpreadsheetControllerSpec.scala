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
import config.FrontendAppConfig
import connectors.{CreateUploadTrackingError, NovaImportsBackendConnector}
import controllers.actions.*
import controllers.{routes, vehicledetails}
import models.responses.CreateUploadTrackingResponse
import models.{AgentSelectedClient, DraftId, SpreadsheetValidationType, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.{AgentSelectedClientPage, DraftIdPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import views.html.UploadVehicleSpreadsheetView

import scala.concurrent.Future

class UploadVehicleSpreadsheetControllerSpec extends SpecBase with MockitoSugar {

  private lazy val onPageLoadRoute = vehicledetails.routes.UploadVehicleSpreadsheetController.onPageLoad().url

  private val draftId = DraftId("DRAFT-001")

  private val uploadTracking = CreateUploadTrackingResponse(
    reference = "11370e18-6e24-453e-b45a-76d3e32ea33d",
    uploadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com",
    fields = Map("key" -> "11370e18-6e24-453e-b45a-76d3e32ea33d", "policy" -> "xxxxxxxx==")
  )

  private val acquisitionAnswers: UserAnswers =
    emptyUserAnswers.unsafeSet(DraftIdPage, draftId).unsafeSet(VehicleFromEuPage, true)

  private val importAnswers: UserAnswers =
    emptyUserAnswers.unsafeSet(DraftIdPage, draftId).unsafeSet(VehicleFromEuPage, false)

  private def connectorReturning(
    result: Either[CreateUploadTrackingError, CreateUploadTrackingResponse]
  ): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.createUploadTracking(any[DraftId], any[SpreadsheetValidationType])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(result))
    connector
  }

  private def applicationFor(
    standardIdentifier: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers],
    connector: NovaImportsBackendConnector = connectorReturning(Right(uploadTracking))
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(standardIdentifier),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[NovaImportsBackendConnector].toInstance(connector)
      )
      .build()

  "UploadVehicleSpreadsheetController" - {

    "must return OK and render the upload form from the upscan details for a VAT-registered organisation" in {
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(acquisitionAnswers))

      running(application) {
        val request   = FakeRequest(GET, onPageLoadRoute)
        val result    = route(application, request).value
        val view      = application.injector.instanceOf[UploadVehicleSpreadsheetView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          uploadTracking.uploadUrl,
          uploadTracking.fields,
          appConfig.multipleVehiclesSpreadsheetsUrl
        )(request, messages(application)).toString
      }
    }

    "must ask the backend for a CarsEu upload when the vehicles came from the EU" in {
      val connector   = connectorReturning(Right(uploadTracking))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(acquisitionAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual OK
        verify(connector).createUploadTracking(eqTo(draftId), eqTo(SpreadsheetValidationType.CarsEu))(using any[HeaderCarrier])
      }
    }

    "must ask the backend for a CarsNonEu upload when the vehicles came from outside the EU" in {
      val connector   = connectorReturning(Right(uploadTracking))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual OK
        verify(connector).createUploadTracking(eqTo(draftId), eqTo(SpreadsheetValidationType.CarsNonEu))(using any[HeaderCarrier])
      }
    }

    "must allow an agent who has selected a client" in {
      val answers     = acquisitionAnswers.unsafeSet(AgentSelectedClientPage, AgentSelectedClient("700011916", Some("Client Co")))
      val application = applicationFor(classOf[FakeAgentIdentifierAction], Some(answers))

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Unauthorised for an agent who has not selected a client" in {
      val application = applicationFor(classOf[FakeAgentIdentifierAction], Some(acquisitionAnswers))

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a private individual" in {
      val application = applicationFor(classOf[FakeIdentifierAction], Some(acquisitionAnswers))

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a non-VAT organisation" in {
      val application = applicationFor(classOf[FakeOrganisationIdentifierAction], Some(acquisitionAnswers))

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when the draft id is missing" in {
      val answersWithoutDraft = emptyUserAnswers.unsafeSet(VehicleFromEuPage, true)
      val application         = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answersWithoutDraft))

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when the vehicle origin question has not been answered" in {
      val answersWithoutOrigin = emptyUserAnswers.unsafeSet(DraftIdPage, draftId)
      val application          = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answersWithoutOrigin))

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised if no session data is found" in {
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], None)

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when the backend has no draft with that id" in {
      val application =
        applicationFor(classOf[FakeVatTraderIdentifierAction], Some(acquisitionAnswers), connectorReturning(Left(CreateUploadTrackingError.NotFound)))

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when the draft belongs to another user" in {
      val application =
        applicationFor(
          classOf[FakeVatTraderIdentifierAction],
          Some(acquisitionAnswers),
          connectorReturning(Left(CreateUploadTrackingError.Forbidden))
        )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when the backend cannot start the upload" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Left(CreateUploadTrackingError.UpstreamError(502, "upscan unavailable")))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
