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
import connectors.{GetUploadResultError, NovaImportsBackendConnector}
import controllers.actions.*
import controllers.vehicledetails
import models.{DraftId, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.DraftIdPage
import pages.sections.initialquestions.VehicleFromEuPage
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class CheckVehicleSpreadsheetErrorsControllerSpec extends SpecBase with MockitoSugar {

  private lazy val onPageLoadRoute = vehicledetails.routes.CheckVehicleSpreadsheetErrorsController.onPageLoad().url

  private val draftId = DraftId("DRAFT-001")

  private val answers: UserAnswers =
    emptyUserAnswers
      .unsafeSet(DraftIdPage, draftId)
      .unsafeSet(VehicleFromEuPage, true)

  private def connectorReturning(error: GetUploadResultError): NovaImportsBackendConnector = {
    val connector = mock[NovaImportsBackendConnector]
    when(connector.getUploadResult(eqTo(draftId))(using any[HeaderCarrier]))
      .thenReturn(Future.successful(Left(error)))
    connector
  }

  private def applicationFor(connector: NovaImportsBackendConnector): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[FakeVatTraderIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(answers))),
        bind[NovaImportsBackendConnector].toInstance(connector)
      )
      .build()

  "CheckVehicleSpreadsheetErrorsController.onPageLoad" - {

    "must redirect to Journey Recovery when the upload result cannot be retrieved" in {
      Seq(GetUploadResultError.NotFound, GetUploadResultError.Forbidden, GetUploadResultError.UpstreamError(500, "boom")).foreach { error =>
        val application = applicationFor(connectorReturning(error))

        running(application) {
          val result = route(application, FakeRequest(GET, onPageLoadRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
