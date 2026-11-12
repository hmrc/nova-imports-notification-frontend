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
import forms.VatReliefFormProvider
import models.*
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.sections.initialquestions.VehicleFromEuPage
import pages.sections.vehicledetails.{AddVehicleDetailsHeavyCommercialPage, AddVehicleTypePage, VatReliefPage}
import pages.{AgentSelectedClientPage, DraftIdPage}
import play.api.Application
import play.api.http.Writeable
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllImportsQuery, AllSuppliersQuery, AllVehiclesQuery}
import repositories.SessionRepository
import viewmodels.VehicleDetailsInset
import views.html.VatReliefView

import scala.concurrent.Future

class VatReliefControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val form = new VatReliefFormProvider()()

  val supplierNumber = SupplierNumber(1)
  val importNumber   = ImportNumber(1)
  val vehicleNumber  = VehicleNumber(1)

  lazy val supplierRoute = vehicledetails.routes.VatReliefController.supplierOnPageLoad(supplierNumber, vehicleNumber, NormalMode).url
  lazy val importRoute   = vehicledetails.routes.VatReliefController.importOnPageLoad(importNumber, vehicleNumber, NormalMode).url

  lazy val supplierSubmit = vehicledetails.routes.VatReliefController.supplierOnSubmit(supplierNumber, vehicleNumber, NormalMode)
  lazy val importSubmit   = vehicledetails.routes.VatReliefController.importOnSubmit(importNumber, vehicleNumber, NormalMode)

  private def withVehicleDetails(answers: UserAnswers): UserAnswers =
    answers
      .unsafeSet(AddVehicleTypePage(vehicleNumber), AddVehicleType.Hcv)
      .unsafeSet(AddVehicleDetailsHeavyCommercialPage(vehicleNumber), AddVehicleDetailsHeavyCommercial("Make", "Model", "HcvType", "CabType"))

  val supplierAnswers: UserAnswers = withVehicleDetails(
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(VehicleFromEuPage, true)
      .unsafeSet(AllSuppliersQuery, Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false)))
      .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))))
  )

  val importAnswers: UserAnswers = withVehicleDetails(
    emptyUserAnswers
      .unsafeSet(DraftIdPage, DraftId("DRAFT-001"))
      .unsafeSet(VehicleFromEuPage, false)
      .unsafeSet(AllImportsQuery, Map("1" -> Json.obj("importEntryNumber" -> "123-456789A")))
      .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1, "details" -> Json.obj("bringingVehicleNI" -> true))))
  )

  private def applicationFor(
    identifierAction: Class[? <: IdentifierAction],
    userAnswers: Option[UserAnswers]
  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].qualifiedWith(Names.named("standard")).to(identifierAction),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  private def applicationWithMockRepository(builder: GuiceApplicationBuilder): (Application, SessionRepository) = {
    val mockSessionRepository = mock[SessionRepository]
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

    val application = builder
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

  private def mustBeUnauthorised[A: Writeable](application: Application, request: FakeRequest[A]) =
    running(application) {
      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
    }

  "VatReliefController" - {

    "supplier journey" - {

      "must return OK and the correct view with the vehicle details for a GET" in {
        val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[VatReliefView]
          val msgs    = messages(application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, VehicleDetailsInset(supplierAnswers, vehicleNumber)(msgs), supplierSubmit)(
            request,
            msgs
          ).toString
          contentAsString(result) must include("<strong>Make Model</strong>")
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = supplierAnswers.unsafeSet(VatReliefPage(vehicleNumber), true)
        val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, supplierRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[VatReliefView]
          val msgs    = messages(application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(true),
            VehicleDetailsInset(userAnswers, vehicleNumber)(msgs),
            supplierSubmit
          )(request, msgs).toString
        }
      }

      "must redirect to the next page and save the answer when Yes is selected" in {
        val (application, mockSessionRepository) =
          applicationWithMockRepository(applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers)))

        running(application) {
          val request = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", "true"))
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(VatReliefPage(vehicleNumber)) mustEqual Some(true)
        }
      }

      "must return a Bad Request and an error message when nothing is selected on continue" in {
        val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers)).build()

        running(application) {
          val request   = FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", ""))
          val result    = route(application, request).value
          val view      = application.injector.instanceOf[VatReliefView]
          val msgs      = messages(application)
          val boundForm = form.bind(Map("value" -> ""))

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, VehicleDetailsInset(supplierAnswers, vehicleNumber)(msgs), supplierSubmit)(
            request,
            msgs
          ).toString
        }
      }

      "must return OK for a GET from a private individual" in {
        val application = applicationFor(classOf[FakeIdentifierAction], Some(supplierAnswers)).build()

        running(application) {
          status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from a non-VAT organisation" in {
        val application = applicationFor(classOf[FakeOrganisationIdentifierAction], Some(supplierAnswers)).build()

        running(application) {
          status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from a VAT organisation" in {
        val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers)).build()

        running(application) {
          status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent with the agent enrolment" in {
        val application = applicationFor(classOf[FakeAgentIdentifierAction], Some(supplierAnswers)).build()

        running(application) {
          status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent without agent enrolment" in {
        val application = applicationFor(classOf[FakeAgentNoEnrolmentsIdentifierAction], Some(supplierAnswers)).build()

        running(application) {
          status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent with a client selected" in {
        val answersWithClient =
          supplierAnswers.unsafeSet(AgentSelectedClientPage, AgentSelectedClient(vrn = "123456789", name = Some("Company 1 Ltd")))
        val application = applicationFor(classOf[FakeAgentIdentifierAction], Some(answersWithClient)).build()

        running(application) {
          status(route(application, FakeRequest(GET, supplierRoute)).value) mustEqual OK
        }
      }

      "must redirect to Unauthorised for a GET if no session data is in session" in {
        mustBeUnauthorised(applicationFor(classOf[FakeVatTraderIdentifierAction], None).build(), FakeRequest(GET, supplierRoute))
      }

      "must redirect to Unauthorised for a GET if no draft is in progress" in {
        val application =
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers.remove(DraftIdPage).success.value)).build()

        mustBeUnauthorised(application, FakeRequest(GET, supplierRoute))
      }

      "must redirect to Unauthorised for a POST if no draft is in progress" in {
        val application =
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers.remove(DraftIdPage).success.value)).build()

        mustBeUnauthorised(application, FakeRequest(POST, supplierRoute).withFormUrlEncodedBody(("value", "LHD")))
      }

      "must redirect to Unauthorised for a GET if the vehicle was not brought from the EU" in {
        val application =
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers.unsafeSet(VehicleFromEuPage, false))).build()

        mustBeUnauthorised(application, FakeRequest(GET, supplierRoute))
      }

      "must redirect to Unauthorised for a GET if the supplier number in the URL is not one of the user's suppliers in session" in {
        mustBeUnauthorised(
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(supplierAnswers)).build(),
          FakeRequest(GET, vehicledetails.routes.VatReliefController.supplierOnPageLoad(SupplierNumber(2), vehicleNumber, NormalMode).url)
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different supplier" in {
        val answers = supplierAnswers
          .unsafeSet(
            AllSuppliersQuery,
            Map("1" -> Json.obj("usePersonalDetailsAsSupplier" -> false), "2" -> Json.obj("usePersonalDetailsAsSupplier" -> false))
          )
          .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 2, "details" -> Json.obj("bringingVehicleNI" -> true))))

        mustBeUnauthorised(applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answers)).build(), FakeRequest(GET, supplierRoute))
      }

      "must redirect to Unauthorised for a GET when the vehicle has no answers yet" in {
        val answers = supplierAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("supplierNumber" -> 1)))

        mustBeUnauthorised(applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answers)).build(), FakeRequest(GET, supplierRoute))
      }
    }

    "import journey" - {

      "must return OK and the correct view with the vehicle details for a GET" in {
        val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, importRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[VatReliefView]
          val msgs    = messages(application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, VehicleDetailsInset(importAnswers, vehicleNumber)(msgs), importSubmit)(request, msgs).toString
        }
      }

      "must redirect to the next page and save the answer when No is selected" in {
        val (application, mockSessionRepository) =
          applicationWithMockRepository(applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers)))

        running(application) {
          val request = FakeRequest(POST, importRoute).withFormUrlEncodedBody(("value", "false"))
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          savedAnswers(mockSessionRepository).get(VatReliefPage(vehicleNumber)) mustEqual Some(false)
        }
      }

      "must return OK for a GET from a VAT organisation" in {
        val application = applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers)).build()

        running(application) {
          status(route(application, FakeRequest(GET, importRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent with the agent enrolment" in {
        val application = applicationFor(classOf[FakeAgentIdentifierAction], Some(importAnswers)).build()

        running(application) {
          status(route(application, FakeRequest(GET, importRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent without agent enrolment" in {
        val application = applicationFor(classOf[FakeAgentNoEnrolmentsIdentifierAction], Some(importAnswers)).build()

        running(application) {
          status(route(application, FakeRequest(GET, importRoute)).value) mustEqual OK
        }
      }

      "must return OK for a GET from an agent with a client selected" in {
        val answersWithClient =
          importAnswers.unsafeSet(AgentSelectedClientPage, AgentSelectedClient(vrn = "123456789", name = Some("Company 1 Ltd")))
        val application = applicationFor(classOf[FakeAgentIdentifierAction], Some(answersWithClient)).build()

        running(application) {
          status(route(application, FakeRequest(GET, importRoute)).value) mustEqual OK
        }
      }

      "must redirect to Unauthorised for a GET from a private individual" in {
        mustBeUnauthorised(applicationFor(classOf[FakeIdentifierAction], Some(importAnswers)).build(), FakeRequest(GET, importRoute))
      }

      "must redirect to Unauthorised for a GET from a non-VAT organisation" in {
        mustBeUnauthorised(applicationFor(classOf[FakeOrganisationIdentifierAction], Some(importAnswers)).build(), FakeRequest(GET, importRoute))
      }

      "must redirect to Unauthorised for a GET if no existing session data is found" in {
        mustBeUnauthorised(applicationFor(classOf[FakeVatTraderIdentifierAction], None).build(), FakeRequest(GET, importRoute))
      }

      "must redirect to Unauthorised for a GET if no draft is in progress" in {
        val application =
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers.remove(DraftIdPage).success.value)).build()

        mustBeUnauthorised(application, FakeRequest(GET, importRoute))
      }

      "must redirect to Unauthorised for a POST if no draft is in progress" in {
        val application =
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers.remove(DraftIdPage).success.value)).build()

        mustBeUnauthorised(application, FakeRequest(POST, importRoute).withFormUrlEncodedBody(("value", "RHD")))
      }

      "must redirect to Unauthorised for a GET if the vehicle was brought from the EU" in {
        val application =
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers.unsafeSet(VehicleFromEuPage, true))).build()

        mustBeUnauthorised(application, FakeRequest(GET, importRoute))
      }

      "must redirect to Unauthorised for a GET if the import number in the URL is not one of the user's imports" in {
        mustBeUnauthorised(
          applicationFor(classOf[FakeVatTraderIdentifierAction], Some(importAnswers)).build(),
          FakeRequest(GET, vehicledetails.routes.VatReliefController.importOnPageLoad(ImportNumber(2), vehicleNumber, NormalMode).url)
        )
      }

      "must redirect to Unauthorised for a GET if the vehicle in the URL belongs to a different import" in {
        val answers = importAnswers
          .unsafeSet(
            AllImportsQuery,
            Map("1" -> Json.obj("importEntryNumber" -> "123-456789A"), "2" -> Json.obj("importEntryNumber" -> "987-654321B"))
          )
          .unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 2, "details" -> Json.obj("bringingVehicleNI" -> true))))

        mustBeUnauthorised(applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answers)).build(), FakeRequest(GET, importRoute))
      }

      "must redirect to Unauthorised for a GET when the vehicle has no answers yet" in {
        val answers = importAnswers.unsafeSet(AllVehiclesQuery, Map("1" -> Json.obj("importNumber" -> 1)))

        mustBeUnauthorised(applicationFor(classOf[FakeVatTraderIdentifierAction], Some(answers)).build(), FakeRequest(GET, importRoute))
      }
    }
  }
}
