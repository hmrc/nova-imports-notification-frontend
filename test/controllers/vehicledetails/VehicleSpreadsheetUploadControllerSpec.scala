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
import connectors.{DeleteFileUploadError, GetFileUploadSummaryError, NovaImportsBackendConnector}
import controllers.actions.*
import controllers.{routes, vehicledetails}
import models.responses.{DeleteFileUploadResponse, GetFileUploadSummaryResponse}
import models.{AgentSelectedClient, DraftId, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.{AgentSelectedClientPage, DraftIdPage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNull, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import views.html.VehicleSpreadsheetUploadView

import scala.concurrent.Future

class VehicleSpreadsheetUploadControllerSpec extends SpecBase with MockitoSugar {

  private lazy val onPageLoadRoute = vehicledetails.routes.VehicleSpreadsheetUploadController.onPageLoad().url
  private lazy val removeRoute     = vehicledetails.routes.VehicleSpreadsheetUploadController.onPageLoad(remove = true).url
  private lazy val statusRoute     = vehicledetails.routes.VehicleSpreadsheetUploadController.status().url

  private val draftId = DraftId("DRAFT-001")

  private val acquisitionAnswers: UserAnswers =
    emptyUserAnswers.unsafeSet(DraftIdPage, draftId).unsafeSet(VehicleFromEuPage, true)

  private val importAnswers: UserAnswers =
    emptyUserAnswers.unsafeSet(DraftIdPage, draftId).unsafeSet(VehicleFromEuPage, false)

  private def connectorReturning(
    summaryResult: Either[GetFileUploadSummaryError, GetFileUploadSummaryResponse],
    deleteResult: Either[DeleteFileUploadError, DeleteFileUploadResponse] = Right(DeleteFileUploadResponse(true))
  ): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getFileUploadSummary(any[DraftId])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(summaryResult))
    when(connector.deleteFileUpload(any[DraftId])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(deleteResult))
    connector
  }

  private def applicationFor(
    standardIdentifier: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers],
    connector: NovaImportsBackendConnector
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

  "VehicleSpreadsheetUploadController.onPageLoad" - {

    "must render the page as Uploading, with Continue disabled, while the upload is in progress" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATING", None, None)))
      )

      running(application) {
        val request   = FakeRequest(GET, onPageLoadRoute)
        val result    = route(application, request).value
        val view      = application.injector.instanceOf[VehicleSpreadsheetUploadView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          isFinal = false,
          fileName = None,
          removeUrl = vehicledetails.routes.VehicleSpreadsheetUploadController.onPageLoad(remove = true),
          statusUrl = statusRoute,
          refreshIntervalSeconds = appConfig.uploadStatusRefreshIntervalSeconds,
          maxPollSeconds = appConfig.uploadStatusMaxPollSeconds
        )(request, messages(application)).toString
      }
    }

    "must render the actual uploaded file name once it is known" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATING", None, Some("car_spreadsheet.ods"))))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include("car_spreadsheet.ods")
      }
    }

    "must render the page as Uploaded, with Continue enabled, once the file has been validated" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATED", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include("""class="govuk-tag  govuk-tag--green"""")
      }
    }

    "must wire the Continue button with its destination immediately when the page loads already validated" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATED", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include(
          s"""data-continue-href="${vehicledetails.routes.CheckVehicleSpreadsheetDetailsController.onPageLoad().url}""""
        )
      }
    }

    "must render the page as Uploaded once validation has failed with errors" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATION_FAILED", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include("""class="govuk-tag  govuk-tag--green"""")
      }
    }

    "must wire the Continue button with the errors page when the page loads already failed validation" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATION_FAILED", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual OK
        contentAsString(result) must include(
          s"""data-continue-href="${vehicledetails.routes.CheckVehicleSpreadsheetErrorsController.onPageLoad().url}""""
        )
      }
    }

    "must redirect to the virus error page when verification failed due to a virus" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VERIFICATION_FAILED", Some("QUARANTINE"), None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSpreadsheetErrorQuarantineController.onPageLoad().url
      }
    }

    "must redirect to the file rejected error page when verification failed due to the file type" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VERIFICATION_FAILED", Some("REJECTED"), None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSpreadsheetErrorRejectedController.onPageLoad().url
      }
    }

    "must redirect to the unknown problem error page when verification failed for an unrecognised reason" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VERIFICATION_FAILED", Some("UNKNOWN"), None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSpreadsheetErrorUnknownController.onPageLoad().url
      }
    }

    "must redirect to the unknown problem error page when verification failed without a failure reason" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VERIFICATION_FAILED", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.UploadSpreadsheetErrorUnknownController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when the backend call fails" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Left(GetFileUploadSummaryError.NotFound))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must allow an agent who has selected a client" in {
      val answers     = acquisitionAnswers.unsafeSet(AgentSelectedClientPage, AgentSelectedClient("700011916", Some("Client Co")))
      val application = applicationFor(
        classOf[FakeAgentIdentifierAction],
        Some(answers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATING", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Unauthorised for an agent who has not selected a client" in {
      val application = applicationFor(
        classOf[FakeAgentIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATING", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a private individual" in {
      val application = applicationFor(
        classOf[FakeIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATING", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised for a non-VAT organisation" in {
      val application = applicationFor(
        classOf[FakeOrganisationIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATING", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when the draft id is missing" in {
      val answersWithoutDraft = emptyUserAnswers.unsafeSet(VehicleFromEuPage, true)
      val application         = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(answersWithoutDraft),
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATING", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised when the vehicle origin question has not been answered" in {
      val answersWithoutOrigin = emptyUserAnswers.unsafeSet(DraftIdPage, draftId)
      val application          = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(answersWithoutOrigin),
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATING", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to Unauthorised if no session data is found" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        None,
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATING", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }
  }

  "VehicleSpreadsheetUploadController.onPageLoad(remove = true)" - {

    "must delete the tracked upload and redirect to AVD1.0 when the vehicles are from the EU" in {
      val connector   = connectorReturning(summaryResult = Right(GetFileUploadSummaryResponse("VALIDATED", None, None)))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(acquisitionAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, removeRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.AddVehicleDetailsController.onPageLoad(models.NormalMode).url
        verify(connector).deleteFileUpload(org.mockito.ArgumentMatchers.eq(draftId))(using any[HeaderCarrier])
      }
    }

    "must delete the tracked upload and redirect to AVD1.1 when the vehicles are not from the EU" in {
      val connector   = connectorReturning(summaryResult = Right(GetFileUploadSummaryResponse("VALIDATED", None, None)))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, removeRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.AddImportVehicleDetailsController.onPageLoad(models.NormalMode).url
      }
    }

    "must still redirect away even when the delete call fails" in {
      val connector = connectorReturning(
        summaryResult = Right(GetFileUploadSummaryResponse("VALIDATED", None, None)),
        deleteResult = Left(DeleteFileUploadError.UpstreamError(502, "boom"))
      )
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(acquisitionAnswers), connector)

      running(application) {
        val result = route(application, FakeRequest(GET, removeRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual vehicledetails.routes.AddVehicleDetailsController.onPageLoad(models.NormalMode).url
      }
    }

    "must not call getFileUploadSummary when removing" in {
      val connector   = connectorReturning(summaryResult = Right(GetFileUploadSummaryResponse("VALIDATED", None, None)))
      val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(acquisitionAnswers), connector)

      running(application) {
        route(application, FakeRequest(GET, removeRoute)).value.futureValue

        verify(connector, never).getFileUploadSummary(any[DraftId])(using any[HeaderCarrier])
      }
    }
  }

  "VehicleSpreadsheetUploadController.status" - {

    "must report not final while the file is still being processed" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VERIFYING", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, statusRoute)).value

        status(result) mustEqual OK
        (contentAsJson(result) \ "isFinal").as[Boolean] mustEqual false
        (contentAsJson(result) \ "continueUrl").asOpt[String] mustBe None
        (contentAsJson(result) \ "redirectUrl").asOpt[String] mustBe None
      }
    }

    "must include the file name once it is known" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VERIFYING", None, Some("car_spreadsheet.ods"))))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, statusRoute)).value

        status(result) mustEqual OK
        (contentAsJson(result) \ "fileName").as[String] mustEqual "car_spreadsheet.ods"
      }
    }

    "must report final with the details continue url once validated" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATED", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, statusRoute)).value

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.obj(
          "statusText"  -> "Uploaded",
          "isFinal"     -> true,
          "fileName"    -> JsNull,
          "continueUrl" -> vehicledetails.routes.CheckVehicleSpreadsheetDetailsController.onPageLoad().url
        )
      }
    }

    "must report final with the errors continue url once validation failed" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VALIDATION_FAILED", None, None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, statusRoute)).value

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.obj(
          "statusText"  -> "Uploaded",
          "isFinal"     -> true,
          "fileName"    -> JsNull,
          "continueUrl" -> vehicledetails.routes.CheckVehicleSpreadsheetErrorsController.onPageLoad().url
        )
      }
    }

    "must return a redirectUrl once verification has failed" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Right(GetFileUploadSummaryResponse("VERIFICATION_FAILED", Some("QUARANTINE"), None)))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, statusRoute)).value

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.obj("redirectUrl" -> vehicledetails.routes.UploadSpreadsheetErrorQuarantineController.onPageLoad().url)
      }
    }

    "must return InternalServerError when the backend call fails" in {
      val application = applicationFor(
        classOf[FakeVatTraderIdentifierAction],
        Some(acquisitionAnswers),
        connectorReturning(Left(GetFileUploadSummaryError.NotFound))
      )

      running(application) {
        val result = route(application, FakeRequest(GET, statusRoute)).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }
}
