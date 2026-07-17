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
import connectors.{NovaImportsBackendConnector, UpdateSectionError}
import controllers.actions.*
import models.{DraftId, NameDetails, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{DraftIdPage, DraftVersionIdPage}
import pages.sections.purchaserDetails.{PurchaserBusinessNamePage, PurchaserNamePage}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsObject
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

import PurchaserDetailsCheckYourAnswersControllerSpec.*

class PurchaserDetailsCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar {

  private lazy val purchaserDetailsCheckYourAnswersRoute = routes.PurchaserDetailsCheckYourAnswersController.onPageLoad().url

  private def applicationForPageLoad(
    identifierAction: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers]
  ): Application =
    applicationForSubmit(identifierAction, userAnswers, mock[NovaImportsBackendConnector])

  private def applicationForSubmit(
    identifierAction: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers],
    connector: NovaImportsBackendConnector
  ): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[NovaImportsBackendConnector].toInstance(connector)
      )
      .build()

  "PurchaserDetailsCheckYourAnswersController" - {

    "onPageLoad" - {

      "for an individual purchaser must return OK with the purchaser name row" in {
        given application: Application = applicationForPageLoad(classOf[FakeIdentifierAction], Some(individualAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must (include("Mr") and include("John") and include("Smith"))
        }
      }

      "for a business purchaser must return OK with the purchaser business name row" in {
        given application: Application = applicationForPageLoad(classOf[FakeIdentifierAction], Some(businessAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value
          val body   = contentAsString(result)

          status(result) mustEqual OK
          body must include("Check your answers")
          body must include("Acme Trading Ltd")
        }
      }

      "must redirect to Unauthorised for a VatRegisteredOrganisation who cannot reach purchaser details" in {
        given application: Application = applicationForPageLoad(classOf[FakeVatTraderIdentifierAction], Some(individualAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised if no existing data is found" in {
        given application: Application = applicationBuilder(userAnswers = None).build()

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised when the purchaser question has not been answered" in {
        given application: Application = applicationForPageLoad(classOf[FakeIdentifierAction], Some(onlyDraftIdAnswers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised when the draftId is missing" in {
        val answers = UserAnswers(userAnswersId).set(PurchaserNamePage, name).success.value

        given application: Application = applicationForPageLoad(classOf[FakeIdentifierAction], Some(answers))

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      // TODO: change correct downstream redirect once NTL set up for all user types
      "when it succeeds for an individual purchaser must redirect to the notification task list" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(2L)))

        given application: Application = applicationForSubmit(classOf[FakeIdentifierAction], Some(individualAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url
        }
      }

      "when it succeeds for a business purchaser must redirect to the notification task list" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(2L)))

        given application: Application = applicationForSubmit(classOf[FakeIdentifierAction], Some(businessAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotificationTaskListController.onPageLoad().url
        }
      }

      "must send the individual purchaser-details body with the current versionId" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(2L)))

        given application: Application = applicationForSubmit(classOf[FakeIdentifierAction], Some(individualAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER

          val bodyCaptor = ArgumentCaptor.forClass(classOf[JsObject])
          verify(connector).updateDraftSection(any[DraftId], eqTo("purchaser-details"), bodyCaptor.capture())(any[HeaderCarrier])
          val sentBody = bodyCaptor.getValue

          (sentBody \ "versionId").as[Long] mustEqual 1L
          (sentBody \ "title").as[String] mustEqual "Mr"
          (sentBody \ "firstName").as[String] mustEqual "John"
          (sentBody \ "lastName").as[String] mustEqual "Smith"
          (sentBody \ "businessName").toOption mustBe None
        }
      }

      "must send the business purchaser-details body with the current versionId" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(2L)))

        given application: Application = applicationForSubmit(classOf[FakeIdentifierAction], Some(businessAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER

          val bodyCaptor = ArgumentCaptor.forClass(classOf[JsObject])
          verify(connector).updateDraftSection(any[DraftId], eqTo("purchaser-details"), bodyCaptor.capture())(any[HeaderCarrier])
          val sentBody = bodyCaptor.getValue

          (sentBody \ "versionId").as[Long] mustEqual 1L
          (sentBody \ "businessName").as[String] mustEqual "Acme Trading Ltd"
          (sentBody \ "title").toOption mustBe None
        }
      }

      "when the backend call returns an UpstreamError must redirect to Journey Recovery" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.UpstreamError(500, "error"))))

        given application: Application = applicationForSubmit(classOf[FakeIdentifierAction], Some(individualAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when the backend call returns Forbidden must redirect to Journey Recovery" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.Forbidden)))

        given application: Application = applicationForSubmit(classOf[FakeIdentifierAction], Some(individualAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when the backend call returns NotFound must redirect to Journey Recovery" in {
        val connector = mock[NovaImportsBackendConnector]
        when(connector.updateDraftSection(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(UpdateSectionError.NotFound)))

        given application: Application = applicationForSubmit(classOf[FakeIdentifierAction], Some(individualAnswers), connector)

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery if the versionId is missing" in {
        val answersWithoutVersionId = UserAnswers(userAnswersId)
          .set(PurchaserNamePage, name)
          .success
          .value
          .set(DraftIdPage, DraftId("DRAFT-001"))
          .success
          .value

        given application: Application =
          applicationForSubmit(classOf[FakeIdentifierAction], Some(answersWithoutVersionId), mock[NovaImportsBackendConnector])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised if the draftId is missing" in {
        val answersWithoutDraftId = UserAnswers(userAnswersId)
          .set(PurchaserNamePage, name)
          .success
          .value
          .set(DraftVersionIdPage, 1L)
          .success
          .value

        given application: Application =
          applicationForSubmit(classOf[FakeIdentifierAction], Some(answersWithoutDraftId), mock[NovaImportsBackendConnector])

        running(application) {
          given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, purchaserDetailsCheckYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }
  }
}

object PurchaserDetailsCheckYourAnswersControllerSpec {

  import org.scalatest.TryValues.*

  private val userAnswersId = "id"
  private val name          = NameDetails("Mr", "John", "Smith")
  private val businessName  = "Acme Trading Ltd"

  private val emptyUserAnswers = UserAnswers(userAnswersId)

  private val individualAnswers = emptyUserAnswers
    .set(PurchaserNamePage, name)
    .success
    .value
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(DraftVersionIdPage, 1L)
    .success
    .value

  private val businessAnswers = emptyUserAnswers
    .set(PurchaserBusinessNamePage, businessName)
    .success
    .value
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(DraftVersionIdPage, 1L)
    .success
    .value

  private val onlyDraftIdAnswers = emptyUserAnswers
    .set(DraftIdPage, DraftId("DRAFT-001"))
    .success
    .value
    .set(DraftVersionIdPage, 1L)
    .success
    .value
}
