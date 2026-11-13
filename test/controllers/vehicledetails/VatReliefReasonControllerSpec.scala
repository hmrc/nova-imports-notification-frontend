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
import forms.VatReliefReasonFormProvider
import models.{AgentSelectedClient, DraftId, ImportNumber, Mode, NormalMode, SupplierNumber, UserAnswers, VatReliefReason, VehicleNumber}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AgentSelectedClientPage, DraftIdPage}
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{VatReliefPage, VatReliefReasonPage}
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import views.html.VatReliefReasonView

import scala.concurrent.Future

class VatReliefReasonControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val supplierNumber = SupplierNumber(1)
  val importNumber   = ImportNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  lazy val supplierRoute = vehicledetails.routes.VatReliefReasonController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode).url
  lazy val importRoute   = vehicledetails.routes.VatReliefReasonController.importOnPageLoad(importNumber, vehicleNumber, NormalMode).url

  val supplierJourneyAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, true)
    .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))
    .unsafeSet(VatReliefPage(vehicleNumber), true)

  val importJourneyAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
    .unsafeSet(VehicleFromEuPage, false)
    .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123456789A")))
    .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1)))
    .unsafeSet(VatReliefPage(vehicleNumber), true)

  private def formFor(application: Application, isSupplier: Boolean): Form[VatReliefReason] =
    application.injector.instanceOf[VatReliefReasonFormProvider].apply(isSupplier)

  private def supplierSubmitCall(mode: Mode): Call =
    vehicledetails.routes.VatReliefReasonController.supplierOnSubmit(supplierNumber, vehicleNumber, mode)

  private def importSubmitCall(mode: Mode): Call =
    vehicledetails.routes.VatReliefReasonController.importOnSubmit(importNumber, vehicleNumber, mode)

  private def applicationWithMockRepository(userAnswers: UserAnswers): (Application, SessionRepository) = {

    val mockSessionRepository = mock[SessionRepository]
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

    val application =
      applicationBuilderWithVatTrader(userAnswers = Some(userAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

    (application, mockSessionRepository)
  }

  private def savedAnswers(mockSessionRepository: SessionRepository): UserAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
    verify(mockSessionRepository).set(captor.capture())
    captor.getValue
  }

  private def assertUnauthorisedOnGet(userAnswers: Option[UserAnswers], url: String) = {

    val application = applicationBuilderWithVatTrader(userAnswers = userAnswers).build()

    running(application) {
      val result = route(application, FakeRequest(GET, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }
  }

  private def assertUnauthorisedOnPost(userAnswers: Option[UserAnswers], url: String, formData: Map[String, String]) = {

    val application = applicationBuilderWithVatTrader(userAnswers = userAnswers).build()

    running(application) {
      val result = route(application, FakeRequest(POST, url).withFormUrlEncodedBody(formData.toSeq*)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }
  }

  private def unauthorisedIdentifierApplication(userAnswers: UserAnswers): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to[UnauthorisedIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("vatTrader")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("novaAgent")).to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith(Names.named("ogd")).to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswers)))
      )
      .build()

  private def applicationFor(identifierAction: Class[? <: IdentifierAction], userAnswers: UserAnswers): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(Some(userAnswers)))
      )
      .build()

  private def assertOkForUserType(identifierAction: Class[? <: IdentifierAction], userAnswers: UserAnswers, url: String) = {

    val application = applicationFor(identifierAction, userAnswers)

    running(application) {
      status(route(application, FakeRequest(GET, url)).value) mustEqual OK
    }
  }

  private def assertUnauthorisedForUserType(identifierAction: Class[? <: IdentifierAction], userAnswers: UserAnswers, url: String) = {

    val application = applicationFor(identifierAction, userAnswers)

    running(application) {
      val result = route(application, FakeRequest(GET, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }
  }

  private def withSelectedClient(userAnswers: UserAnswers): UserAnswers =
    userAnswers.unsafeSet(AgentSelectedClientPage, AgentSelectedClient(vrn = "123456789", name = Some("Company 1 Ltd")))

  private def assertUnauthorisedWhenIdentifierRejectsOnGet(userAnswers: UserAnswers, url: String) = {

    val application = unauthorisedIdentifierApplication(userAnswers)

    running(application) {
      val result = route(application, FakeRequest(GET, url)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }
  }

  private def assertBadRequestWithError(userAnswers: UserAnswers, url: String, formData: Map[String, String])(
    expectedError: Messages => String
  ) = {

    val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswers)).build()

    running(application) {
      val result = route(application, FakeRequest(POST, url).withFormUrlEncodedBody(formData.toSeq*)).value

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) must include(expectedError(messages(application)))
    }
  }

  "VatReliefReasonController" - {

    "on the supplier journey" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilderWithVatTrader(userAnswers = Some(supplierJourneyAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[VatReliefReasonView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            formFor(application, isSupplier = true),
            VatReliefReason.supplierOptions,
            None,
            supplierSubmitCall(NormalMode)
          )(request, messages(application)).toString
        }
      }

      "must show a back link on the page" in {

        val application = applicationBuilderWithVatTrader(userAnswers = Some(supplierJourneyAnswers)).build()

        running(application) {
          val result = route(application, FakeRequest(GET, supplierRoute)).value

          contentAsString(result) must include("govuk-back-link")
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = supplierJourneyAnswers.unsafeSet(VatReliefReasonPage(vehicleNumber), VatReliefReason.SubjectToReliefForCharity)

        val application = applicationBuilderWithVatTrader(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[VatReliefReasonView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            formFor(application, isSupplier = true).fill(VatReliefReason.SubjectToReliefForCharity),
            VatReliefReason.supplierOptions,
            None,
            supplierSubmitCall(NormalMode)
          )(request, messages(application)).toString
        }
      }

      "must redirect to the next page and save the answer when a valid supplier reason is submitted" in {

        val (application, mockSessionRepository) = applicationWithMockRepository(supplierJourneyAnswers)

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody("value" -> VatReliefReason.ModifiedForDisabledPassenger.toString)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(VatReliefReasonPage(vehicleNumber)) mustEqual Some(VatReliefReason.ModifiedForDisabledPassenger)
        }
      }

      "must return a Bad Request and the supplier required error when no option is selected" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, Map("value" -> ""))(msgs => msgs("vatReliefReason.supplier.error.required"))
      }

      "must return a Bad Request and the supplier required error when an import-only reason is submitted" in {
        assertBadRequestWithError(supplierJourneyAnswers, supplierRoute, Map("value" -> VatReliefReason.InheritedGoods.toString))(msgs =>
          msgs("vatReliefReason.supplier.error.required")
        )
      }

      "must return OK for a GET from a private individual" in {
        assertOkForUserType(classOf[FakeIdentifierAction], supplierJourneyAnswers, supplierRoute)
      }

      "must return OK for a GET from a non-VAT organisation" in {
        assertOkForUserType(classOf[FakeOrganisationIdentifierAction], supplierJourneyAnswers, supplierRoute)
      }

      "must return OK for a GET from a VAT organisation" in {
        assertOkForUserType(classOf[FakeVatTraderIdentifierAction], supplierJourneyAnswers, supplierRoute)
      }

      "must return OK for a GET from an agent with the agent enrolment" in {
        assertOkForUserType(classOf[FakeAgentIdentifierAction], supplierJourneyAnswers, supplierRoute)
      }

      "must return OK for a GET from an agent without the agent enrolment" in {
        assertOkForUserType(classOf[FakeAgentNoEnrolmentsIdentifierAction], supplierJourneyAnswers, supplierRoute)
      }

      "must return OK for a GET from an agent with a client selected" in {
        assertOkForUserType(classOf[FakeAgentIdentifierAction], withSelectedClient(supplierJourneyAnswers), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if no existing session data is found" in {
        assertUnauthorisedOnGet(None, supplierRoute)
      }

      "must redirect to Unauthorised when the identifier rejects the user" in {
        assertUnauthorisedWhenIdentifierRejectsOnGet(supplierJourneyAnswers, supplierRoute)
      }

      "must redirect to Unauthorised for a POST if no existing session data is found" in {
        assertUnauthorisedOnPost(None, supplierRoute, Map("value" -> VatReliefReason.ModifiedForDisabledPassenger.toString))
      }

      "must redirect to Unauthorised for a GET if draftId is missing" in {
        assertUnauthorisedOnGet(Some(supplierJourneyAnswers.remove(DraftIdPage).success.value), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if the vehicle was not brought from the EU" in {
        assertUnauthorisedOnGet(Some(supplierJourneyAnswers.unsafeSet(VehicleFromEuPage, false)), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if AVD11.0 has not been answered yes" in {
        assertUnauthorisedOnGet(Some(supplierJourneyAnswers.remove(VatReliefPage(vehicleNumber)).success.value), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if AVD11.0 was answered no" in {
        assertUnauthorisedOnGet(Some(supplierJourneyAnswers.unsafeSet(VatReliefPage(vehicleNumber), false)), supplierRoute)
      }

      "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers" in {
        assertUnauthorisedOnGet(
          Some(supplierJourneyAnswers),
          vehicledetails.routes.VatReliefReasonController.supplierOnPageLoad(SupplierNumber(2), vehicleNumber, NormalMode).url
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different supplier" in {

        val answers = supplierJourneyAnswers
          .unsafeSet(
            AllSuppliersQuery,
            Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false), "2" -> Json.obj("usePersonalDetailsAsSupplier" -> false))
          )
          .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 2)))

        assertUnauthorisedOnGet(Some(answers), supplierRoute)
      }
    }

    "on the import journey" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilderWithVatTrader(userAnswers = Some(importJourneyAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, importRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[VatReliefReasonView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            formFor(application, isSupplier = false),
            VatReliefReason.importOptions,
            None,
            importSubmitCall(NormalMode)
          )(request, messages(application)).toString
        }
      }

      "must redirect to the next page and save the answer when a valid import reason is submitted" in {

        val (application, mockSessionRepository) = applicationWithMockRepository(importJourneyAnswers)

        running(application) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody("value" -> VatReliefReason.InheritedGoods.toString)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(VatReliefReasonPage(vehicleNumber)) mustEqual Some(VatReliefReason.InheritedGoods)
        }
      }

      "must return a Bad Request and the import required error when no option is selected" in {
        assertBadRequestWithError(importJourneyAnswers, importRoute, Map("value" -> ""))(msgs => msgs("vatReliefReason.import.error.required"))
      }

      "must return OK for a GET from a VAT organisation" in {
        assertOkForUserType(classOf[FakeVatTraderIdentifierAction], importJourneyAnswers, importRoute)
      }

      "must return OK for a GET from an agent with the agent enrolment" in {
        assertOkForUserType(classOf[FakeAgentIdentifierAction], importJourneyAnswers, importRoute)
      }

      "must return OK for a GET from an agent without the agent enrolment" in {
        assertOkForUserType(classOf[FakeAgentNoEnrolmentsIdentifierAction], importJourneyAnswers, importRoute)
      }

      "must return OK for a GET from an agent with a client selected" in {
        assertOkForUserType(classOf[FakeAgentIdentifierAction], withSelectedClient(importJourneyAnswers), importRoute)
      }

      "must redirect to Unauthorised for a GET from a private individual" in {
        assertUnauthorisedForUserType(classOf[FakeIdentifierAction], importJourneyAnswers, importRoute)
      }

      "must redirect to Unauthorised for a GET from a non-VAT organisation" in {
        assertUnauthorisedForUserType(classOf[FakeOrganisationIdentifierAction], importJourneyAnswers, importRoute)
      }

      "must redirect to Unauthorised for a POST from a private individual" in {

        val application = applicationFor(classOf[FakeIdentifierAction], importJourneyAnswers)

        running(application) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody("value" -> VatReliefReason.InheritedGoods.toString)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised for a GET if no existing session data is found" in {
        assertUnauthorisedOnGet(None, importRoute)
      }

      "must redirect to Unauthorised for a GET if the vehicle was brought from the EU" in {
        assertUnauthorisedOnGet(Some(importJourneyAnswers.unsafeSet(VehicleFromEuPage, true)), importRoute)
      }

      "must redirect to Unauthorised for a GET if AVD11.0 has not been answered yes" in {
        assertUnauthorisedOnGet(Some(importJourneyAnswers.remove(VatReliefPage(vehicleNumber)).success.value), importRoute)
      }

      "must redirect to Unauthorised for a GET if the import number in the URL is not one of the user's imports" in {
        assertUnauthorisedOnGet(
          Some(importJourneyAnswers),
          vehicledetails.routes.VatReliefReasonController.importOnPageLoad(ImportNumber(2), vehicleNumber, NormalMode).url
        )
      }
    }
  }
}
